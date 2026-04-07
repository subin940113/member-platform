import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const expectedAdmissionStatuses = http.expectedStatuses(200, 429);
const expectedRegisterStatuses = http.expectedStatuses(201, 409);

const admissionSuccess = new Counter('admission_success');
const registerCreated = new Counter('register_created');
const registerConflict = new Counter('register_conflict');
const registerUnexpectedFailure = new Counter('register_unexpected_failure');

const admissionDuration = new Trend('admission_duration');
const registerDuration = new Trend('register_duration');

export const options = {
    scenarios: {
        idempotency_conflict: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 10),
            duration: __ENV.DURATION || '20s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        register_duration: ['p(95)<2000'],
    },
};

function buildPayloadForVu() {
    const runDigits = RUN_ID.replace(/\D/g, '').slice(-4).padStart(4, '0');

    return {
        email: `idem-${RUN_ID}-vu-${__VU}@example.com`,
        password: 'Password123!',
        name: `idem-${RUN_ID}-user-${__VU}`,
        phone: `010${runDigits}${String(__VU).padStart(4, '0')}`,
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

    if (response.status !== 200) {
        return null;
    }

    admissionSuccess.add(1);

    try {
        return response.json()?.data?.admissionToken ?? null;
    } catch (_) {
        return null;
    }
}

export default function () {
    const admissionToken = issueAdmissionToken();
    if (!admissionToken) {
        sleep(0.2);
        return;
    }

    const payload = buildPayloadForVu();
    const idempotencyKey = `fixed-idem-key-${RUN_ID}-vu-${__VU}`;

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
        registerCreated.add(1);
    } else if (response.status === 409) {
        registerConflict.add(1);
    } else {
        registerUnexpectedFailure.add(1);
    }

    check(response, {
        'register is 201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    sleep(0.1);
}
