// 게스트가 초대 링크를 여는 경로(GET /v1/api/guest/invitation/{token}) 부하. 인기 사물함 1개 대상.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TOKENS, scenario, thresholds, summaryTrendStats, summary } from './common.js';

export const options = { scenarios: { read: scenario() }, thresholds, summaryTrendStats };

export default function () {
  const res = http.get(`${BASE_URL}/v1/api/guest/invitation/${TOKENS[0].token}`);
  check(res, { '200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return summary(data, __ENV.NAME || 'read');
}
