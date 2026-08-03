import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const conflictRate = new Rate('bid_conflict');
const bidDuration = new Trend('http_req_duration', true);
const bidEndpointDuration = new Trend('http_req_duration{endpoint:bid}', true);

export const options = {
  scenarios: {
    bid_storm: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '3m', target: 200 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{endpoint:bid}': ['p(95)<400'],
    checks: ['rate>0.99'],
    bid_conflict: ['rate<0.05'],
  },
};

let lotIds = [];
let tokens = [];

export function setup() {
  const ids = [];
  const authTokens = [];
  for (let i = 0; i < 10; i++) {
    const email = `k6-bidder-${i}-${Date.now()}@load.test`;
    http.post(
      `${BASE_URL}/auth/register`,
      JSON.stringify({ email, password: 'password1234', displayName: `K6 Bidder ${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const login = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email, password: 'password1234' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    authTokens.push(login.json('accessToken'));
  }

  const sellerEmail = `k6-seller-${Date.now()}@load.test`;
  http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email: sellerEmail, password: 'password1234', displayName: 'K6 Seller' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  let sellerToken = http
    .post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email: sellerEmail, password: 'password1234' }),
      { headers: { 'Content-Type': 'application/json' } },
    )
    .json('accessToken');
  http.post(`${BASE_URL}/me/seller-application`, null, {
    headers: { Authorization: `Bearer ${sellerToken}` },
  });
  sellerToken = http
    .post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email: sellerEmail, password: 'password1234' }),
      { headers: { 'Content-Type': 'application/json' } },
    )
    .json('accessToken');

  for (let i = 0; i < 10; i++) {
    const res = http.post(
      `${BASE_URL}/lots`,
      JSON.stringify({
        title: `K6 Lot ${i}`,
        description: 'Load test lot',
        categoryId: 1,
        startingPrice: '100.00',
        minIncrement: '5.00',
      }),
      {
        headers: {
          Authorization: `Bearer ${sellerToken}`,
          'Content-Type': 'application/json',
        },
      },
    );
    ids.push(res.json('id'));
  }
  return { lotIds: ids, tokens: authTokens };
}

export default function (data) {
  const lotId = data.lotIds[__VU % data.lotIds.length];
  const token = data.tokens[__VU % data.tokens.length];
  const amount = (100 + (__ITER % 20) * 5).toFixed(2);
  const clientRequestId = `k6-${__VU}-${__ITER}-${Date.now()}`;

  const res = http.post(
    `${BASE_URL}/lots/${lotId}/bids`,
    JSON.stringify({ amount, clientRequestId }),
    {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      tags: { endpoint: 'bid' },
    },
  );

  bidEndpointDuration.add(res.timings.duration, { endpoint: 'bid' });
  const ok =
    res.status === 201 ||
    res.status === 200 ||
    (res.status === 409 && res.json('error.code') === 'bid_too_low') ||
    res.status === 409 && res.json('error.code') === 'bid_not_live';
  check(res, { 'expected bid response': () => ok });
  conflictRate.add(res.status === 409 && res.json('error.code') === 'bid_conflict');
  sleep(0.2);
}
