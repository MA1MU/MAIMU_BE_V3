// 알림 종단 지연 측정: 게스트 작성 요청 시작 → 사물함 주인의 SSE 연결에 maimu 이벤트 도착까지.
// k6 는 SSE 를 기본 지원하지 않아 Node(18+) 내장 fetch 로 잰다. 의존성 없음.
//
//   ACCESS_TOKEN=<사물함 주인의 액세스 토큰> node sse-latency.mjs
//   env: BASE_URL, TOKENS(초대 토큰 JSON), SAMPLES(기본 200)
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { performance } from 'node:perf_hooks';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const SAMPLES = Number(process.env.SAMPLES || 200);
const { groupId, token } = JSON.parse(readFileSync(process.env.TOKENS || './tokens.json', 'utf8'))[0];

const res = await fetch(`${BASE_URL}/v1/api/notification/subscribe`, {
  headers: { Authorization: `Bearer ${process.env.ACCESS_TOKEN}`, Accept: 'text/event-stream' },
});
if (!res.ok) throw new Error(`구독 실패: ${res.status} ${await res.text()}`);

// maimu 이벤트가 올 때마다 대기 중인 resolve 를 깨운다
let waiter = null;
(async () => {
  const decoder = new TextDecoder();
  let buf = '';
  for await (const chunk of res.body) {
    buf += decoder.decode(chunk, { stream: true });
    let i;
    while ((i = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, i);
      buf = buf.slice(i + 2);
      if (block.includes('event:maimu') && waiter) waiter(performance.now());
    }
  }
})();

const body = JSON.stringify({
  title: '지연 측정', message: '알림 지연 측정용', maimuColor: 'GREEN',
  writerName: '익명', sugarContent: 50, isAnonymous: true,
});
const latencies = [];
for (let n = 0; n < SAMPLES; n++) {
  const arrived = new Promise((resolve) => { waiter = resolve; });
  const start = performance.now();
  const post = await fetch(`${BASE_URL}/v1/api/guest/${groupId}/${token}/add`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body,
  });
  if (post.status !== 201) throw new Error(`작성 실패: ${post.status}`);
  const end = await Promise.race([arrived, new Promise((r) => setTimeout(() => r(null), 5000))]);
  if (end === null) throw new Error('5초 안에 알림이 오지 않음');
  latencies.push(end - start);
  // 플러시 주기와 위상이 고정되지 않게 0~250ms 랜덤 간격
  await new Promise((r) => setTimeout(r, Math.random() * 250));
}

latencies.sort((a, b) => a - b);
const pct = (p) => latencies[Math.min(latencies.length - 1, Math.ceil((p / 100) * latencies.length) - 1)];
const result = {
  samples: latencies.length,
  p50: pct(50), p90: pct(90), p95: pct(95), p99: pct(99), max: latencies.at(-1),
  avg: latencies.reduce((a, b) => a + b, 0) / latencies.length,
};
mkdirSync('results', { recursive: true });
writeFileSync(`results/${process.env.NAME || 'sse-latency'}.json`, JSON.stringify(result, null, 2));
console.log(`${process.env.NAME || 'sse-latency'}: ` +
  Object.entries(result).map(([k, v]) => `${k}=${k === 'samples' ? v : v.toFixed(1) + 'ms'}`).join(' '));
process.exit(0);
