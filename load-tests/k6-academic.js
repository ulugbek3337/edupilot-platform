import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const failures = new Rate('api_failures');
const latency = new Trend('api_latency');
const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token = __ENV.ACCESS_TOKEN;
const centerId = __ENV.CENTER_ID;
const otherCenterId = __ENV.OTHER_CENTER_ID;
const profile = __ENV.LOAD_PROFILE || 'baseline';

if (!/^https?:\/\//.test(baseUrl)) {
  throw new Error('BASE_URL must be an http(s) URL.');
}
if (!token || !centerId) {
  throw new Error('Set ACCESS_TOKEN and CENTER_ID for a dedicated test account.');
}
if (otherCenterId && String(otherCenterId) === String(centerId)) {
  throw new Error('OTHER_CENTER_ID must identify a different center.');
}

const profiles = {
  smoke: {
    vus: 1,
    iterations: 1,
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<2000'],
      api_failures: ['rate<0.01'],
    },
  },
  baseline: {
    stages: [
      { duration: '1m', target: 10 },
      { duration: '2m', target: 50 },
      { duration: '3m', target: 100 },
      { duration: '1m', target: 0 },
    ],
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<750'],
      api_failures: ['rate<0.01'],
      api_latency: ['p(95)<750'],
    },
  },
  stress: {
    stages: [
      { duration: '1m', target: 25 },
      { duration: '2m', target: 100 },
      { duration: '3m', target: 200 },
      { duration: '2m', target: 300 },
      { duration: '1m', target: 0 },
    ],
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<1500'],
      api_failures: ['rate<0.01'],
      api_latency: ['p(95)<1500'],
    },
  },
};

if (!profiles[profile]) {
  throw new Error('LOAD_PROFILE must be smoke, baseline, or stress.');
}
export const options = profiles[profile];

const expectedResponse = http.expectedStatuses(200, 401, 403);
const paths = [
  '/api/v1/centers',
  '/api/v1/students',
  '/api/v1/teachers',
  '/api/v1/courses',
  '/api/v1/groups',
];

export default function () {
  const headers = {
    Authorization: `Bearer ${token}`,
    'X-Center-Id': String(centerId),
  };

  for (const path of paths) {
    const response = http.get(`${baseUrl}${path}`, {
      headers,
      responseCallback: expectedResponse,
      tags: { endpoint: path, test_type: 'tenant_read' },
      timeout: '10s',
    });
    const ok = check(response, {
      'authorized tenant read returns 200': (r) => r.status === 200,
    });
    failures.add(!ok);
    latency.add(response.timings.duration);
  }

  if (otherCenterId) {
    const response = http.get(`${baseUrl}/api/v1/students`, {
      headers: { ...headers, 'X-Center-Id': String(otherCenterId) },
      responseCallback: expectedResponse,
      tags: { endpoint: '/api/v1/students', test_type: 'cross_tenant_denial' },
      timeout: '10s',
    });
    const denied = check(response, {
      'foreign tenant is denied': (r) => r.status === 401 || r.status === 403,
    });
    failures.add(!denied);
    latency.add(response.timings.duration);
  }

  if (profile === 'smoke') return;
  sleep(1);
}
