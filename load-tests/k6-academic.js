import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const failures = new Rate('api_failures');
const latency = new Trend('api_latency');
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;
const centerId = __ENV.CENTER_ID;
if (!token || !centerId) throw new Error('Set ACCESS_TOKEN and CENTER_ID for a test account.');

export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '3m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750'],
    api_failures: ['rate<0.01'],
    api_latency: ['p(95)<750'],
  },
};

export default function () {
  const headers = { Authorization: `Bearer ${token}`, 'X-Center-Id': String(centerId) };
  for (const path of ['/api/v1/centers', '/api/v1/students', '/api/v1/teachers', '/api/v1/courses', '/api/v1/groups']) {
    const response = http.get(`${baseUrl}${path}`, { headers, tags: { endpoint: path } });
    const ok = check(response, { 'status is 200': (r) => r.status === 200 });
    failures.add(!ok);
    latency.add(response.timings.duration);
  }
  sleep(1);
}
