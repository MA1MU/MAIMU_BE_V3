// 게스트 마이무 작성 부하. TARGET=hot 이면 인기 사물함 1개에 몰고, spread 면 전체 사물함에 분산한다.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TOKENS, scenario, thresholds, summaryTrendStats, summary } from './common.js';

export const options = { scenarios: { write: scenario() }, thresholds, summaryTrendStats };

const hot = (__ENV.TARGET || 'hot') === 'hot';
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  const t = hot ? TOKENS[0] : TOKENS[Math.floor(Math.random() * TOKENS.length)];
  const body = JSON.stringify({
    title: `첫인상 ${__VU}-${__ITER}`,
    message: '처음 봤을 때 정말 밝아 보였어! 올해도 잘 부탁해.',
    maimuColor: 'RED',
    writerName: '익명',
    sugarContent: 70,
    isAnonymous: true,
  });
  const res = http.post(`${BASE_URL}/v1/api/guest/${t.groupId}/${t.token}/add`, body, params);
  check(res, { '201': (r) => r.status === 201 });
}

export function handleSummary(data) {
  return summary(data, __ENV.NAME || 'write');
}
