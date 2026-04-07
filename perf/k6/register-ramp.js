import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const expectedAdmissionStatuses = http.expectedStatuses(200, 429);
const expectedRegisterStatuses = http.expectedStatuses(201, 409, 429);

const admissionSuccess = new Counter('admission_success');
const admissionRejected = new Counter('admission_rejected');
const admissionUnexpectedFailure = new Counter('admission_unexpected_failure');

const registerCreated = new Counter('register_created');
const registerConflict = new Counter('register_conflict');
const registerRejected = new Counter('register_rejected');
const registerUnexpectedFailure = new Counter('register_unexpected_failure');

const endToEndCreatedRate = new Rate('end_to_end_created_rate');
const protectedFlowRate = new Rate('protected_flow_rate');

const admissionDuration = new Trend('admission_duration');
const registerDuration = new Trend('register_duration');
const endToEndDuration = new Trend('end_to_end_duration');

export const options = {
    scenarios: {
        register_ramp: {
            executor: 'ramping-arrival-rate',
            startRate: Number(__ENV.START_RATE || 50),
            timeUnit: '1s',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 100),
            maxVUs: Number(__ENV.MAX_VUS || 500),
            stages: [
                { target: Number(__ENV.STAGE1 || 100), duration: __ENV.STAGE1_DURATION || '30s' },
                { target: Number(__ENV.STAGE2 || 200), duration: __ENV.STAGE2_DURATION || '30s' },
                { target: Number(__ENV.STAGE3 || 300), duration: __ENV.STAGE3_DURATION || '30s' },
                { target: Number(__ENV.STAGE4 || 300), duration: __ENV.STAGE4_DURATION || '30s' },
                { target: 0, duration: __ENV.COOLDOWN_DURATION || '10s' },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        admission_duration: ['p(95)<1000'],
        register_duration: ['p(95)<2000'],
        end_to_end_duration: ['p(95)<3000'],
        protected_flow_rate: ['rate>0.99'],
    },
};

function uniqueSuffix() {
    return `${Date.now()}-${__VU}-${__ITER}-${Math.random().toString(36).slice(2, 8)}`;
}

function buildRegisterPayload() {
    const suffix = uniqueSuffix();
    const numericSuffix = `${Date.now()}${__VU}${__ITER}`.slice(-8);

    return {
        email: `ramp-${suffix}@example.com`,
        password: 'Password123!',
        name: `ramp-user-${suffix}`,
        phone: `010${numericSuffix}`,
    };
}

function issueAdmissionToken() {
    const response = http.post(
        `${BASE_URL}/api/v1/members/admission`,
        null,
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'issue_admission' },
            responseCallback: expectedAdmissionStatuses,
        }
    );

    admissionDuration.add(response.timings.duration);

    if (response.status === 200) {
        admissionSuccess.add(1);
        try {
            return {
                status: response.status,
                ok: true,
                admissionToken: response.json()?.data?.admissionToken ?? null,
            };
        } catch (_) {
            return { status: response.status, ok: false, admissionToken: null };
        }
    }

    if (response.status === 429) {
        admissionRejected.add(1);
        return { status: response.status, ok: false, admissionToken: null };
    }

    admissionUnexpectedFailure.add(1);
    return { status: response.status, ok: false, admissionToken: null };
}

function registerMember(admissionToken, payload) {
    const response = http.post(
        `${BASE_URL}/api/v1/members`,
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'Admission-Token': admissionToken,
                'Idempotency-Key': `idem-${uniqueSuffix()}`,
            },
            tags: { name: 'register_member' },
            responseCallback: expectedRegisterStatuses,
        }
    );

    registerDuration.add(response.timings.duration);

    if (response.status === 201) {
        registerCreated.add(1);
    } else if (response.status === 409) {
        registerConflict.add(1);
    } else if (response.status === 429) {
        registerRejected.add(1);
    } else {
        registerUnexpectedFailure.add(1);
    }

    return response;
}

export default function () {
    const startedAt = Date.now();

    const admission = issueAdmissionToken();

    if (!admission.ok || !admission.admissionToken) {
        endToEndCreatedRate.add(false);
        protectedFlowRate.add(admission.status === 429 || admission.status === 200);
        endToEndDuration.add(Date.now() - startedAt);
        return;
    }

    const response = registerMember(admission.admissionToken, buildRegisterPayload());

    endToEndDuration.add(Date.now() - startedAt);
    endToEndCreatedRate.add(response.status === 201);
    protectedFlowRate.add(
        response.status === 201 ||
        response.status === 409 ||
        response.status === 429
    );

    check(response, {
        'register returns expected status': (r) =>
            r.status === 201 || r.status === 409 || r.status === 429,
    });
}
