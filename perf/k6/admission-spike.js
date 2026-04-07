import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const expectedAdmissionStatuses = http.expectedStatuses(200, 429);

const admissionSuccess = new Counter('admission_success');
const admissionRejected = new Counter('admission_rejected');
const admissionUnexpectedFailure = new Counter('admission_unexpected_failure');
const admissionAvailableRate = new Rate('admission_available_rate');
const admissionDuration = new Trend('admission_duration');

export const options = {
    scenarios: {
        admission_burst: {
            executor: 'ramping-arrival-rate',
            startRate: Number(__ENV.START_RATE || 500),
            timeUnit: '1s',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 2000),
            maxVUs: Number(__ENV.MAX_VUS || 8000),
            stages: [
                { target: Number(__ENV.WARMUP_RATE || 1000), duration: __ENV.WARMUP_DURATION || '30s' },
                { target: Number(__ENV.BURST_RATE || 5000), duration: __ENV.RAMP_DURATION || '30s' },
                { target: Number(__ENV.BURST_RATE || 5000), duration: __ENV.HOLD_DURATION || '120s' },
                { target: 0, duration: __ENV.COOLDOWN_DURATION || '10s' },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        admission_duration: ['p(95)<500'],
    },
};

export default function () {
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
        admissionAvailableRate.add(true);
    } else if (response.status === 429) {
        admissionRejected.add(1);
        admissionAvailableRate.add(false);
    } else {
        admissionUnexpectedFailure.add(1);
        admissionAvailableRate.add(false);
    }

    check(response, {
        'admission is 200 or 429': (r) => r.status === 200 || r.status === 429,
    });
}
