import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export const options = {
  scenarios: {
    catalog_browse: {
      executor: 'constant-vus',
      vus: 500,
      duration: '2m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<150'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/lots?page=0&size=20`);
  check(res, {
    'catalog 200': (r) => r.status === 200,
  });
  sleep(0.1);
}
