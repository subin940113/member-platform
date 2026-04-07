import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const expectedAdmissionStatuses = http.expectedStatuses(200, 429);
const expectedRegisterStatuses = http.expectedStatuses(201, 409, 429);

const admissionSuccess = new Counter('admission_success');
const admissionRejected = new Counter('admission_rejected');
const registerSuccess = new Counter('register_success');
const registerFailure = new Counter('register_failure');
const endToEndSuccessRate = new Rate('end_to_end_success_rate');

const admissionDuration = new Trend('admission_duration');
const registerDuration = new Trend('register_duration');
const endToEndDuration = new Trend('end_to_end_duration');

export const options = {
    scenarios: {
        register_flow: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 20),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 50),
            maxVUs: Number(__ENV.MAX_VUS || 200),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.10'],
        end_to_end_success_rate: ['rate>0.80'],
        admission_duration: ['p(95)<1000'],
        register_duration: ['p(95)<2000'],
        end_to_end_duration: ['p(95)<3000'],
    },
};

function uniqueSuffix() {
    return `${Date.now()}-${__VU}-${__ITER}-${Math.random().toString(36).slice(2, 8)}`;
}

function buildRegisterPayload() {
    const suffix = uniqueSuffix();
    const numericSuffix = `${Date.now()}${__VU}${__ITER}${Math.floor(Math.random() * 1000)}`
        .slice(-8);

    return {
        email: `loadtest-${suffix}@example.com`,
        password: 'Password123!',
        name: `user-${suffix}`,
        phone: `010${numericSuffix}`,
    };
}

function issueAdmissionToken() {
    const response = http.post(
        `${BASE_URL}/api/v1/members/admission`,
        null,
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: { name: 'issue_admission' },
            responseCallback: expectedAdmissionStatuses,
        }
    );

    admissionDuration.add(response.timings.duration);

    if (response.status === 200) {
        admissionSuccess.add(1);

        let admissionToken = null;
        try {
            const body = response.json();
            admissionToken = body?.data?.admissionToken ?? null;
        } catch (_) {
            admissionToken = null;
        }

        return { ok: !!admissionToken, admissionToken, response };
    }

    if (response.status === 429) {
        admissionRejected.add(1);
    }

    return { ok: false, admissionToken: null, response };
}

function registerMember(admissionToken, payload) {
    const idempotencyKey = `idem-${uniqueSuffix()}`;

    const response = http.post(
        `${BASE_URL}/api/v1/members`,
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'Admission-Token': admissionToken,
                'Idempotency-Key': idempotencyKey,
            },
            tags: { name: 'register_member' },
            responseCallback: expectedRegisterStatuses,
        }
    );

    registerDuration.add(response.timings.duration);

    if (response.status === 201) {
        registerSuccess.add(1);
    } else {
        registerFailure.add(1);
    }

    return response;
}

export default function () {
    const startedAt = Date.now();

    const admission = issueAdmissionToken();
    if (!admission.ok) {
        endToEndSuccessRate.add(false);
        endToEndDuration.add(Date.now() - startedAt);
        return;
    }

    const payload = buildRegisterPayload();
    const registerResponse = registerMember(admission.admissionToken, payload);

    const success = check(registerResponse, {
        'register status is 201': (r) => r.status === 201,
    });

    endToEndSuccessRate.add(success);
    endToEndDuration.add(Date.now() - startedAt);
}
