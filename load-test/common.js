// 공통 설정. 모든 값은 환경변수로 덮어쓴다.
//   BASE_URL   대상 서버 (기본 http://localhost:8080)
//   TOKENS     [{groupId, token}] 형태의 초대 토큰 JSON 경로 (k6 실행 위치 기준)
//   MODE       throughput: VU 고정 closed model → 최대 TPS 측정
//              latency   : constant-arrival-rate open model → 고정 부하에서 지연 분포 측정
//                          (응답이 느려져도 요청을 계속 보내므로 coordinated omission 이 없다)
//   VUS, RATE, DURATION
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const TOKENS = JSON.parse(open(__ENV.TOKENS || './tokens.json'));

export function scenario() {
  const duration = __ENV.DURATION || '30s';
  if ((__ENV.MODE || 'throughput') === 'latency') {
    return {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 200),
      timeUnit: '1s',
      duration,
      preAllocatedVUs: 200,
      maxVUs: 1000,
    };
  }
  return { executor: 'constant-vus', vus: Number(__ENV.VUS || 100), duration };
}

export const thresholds = { http_req_failed: ['rate<0.01'] };
export const summaryTrendStats = ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)', 'max'];

export function summary(data, name) {
  const out = {};
  out[`results/${name}.json`] = JSON.stringify(data, null, 2);
  const d = data.metrics.http_req_duration.values;
  const line = `${name}: TPS=${data.metrics.http_reqs.values.rate.toFixed(1)} ` +
    `p50=${d.med.toFixed(2)}ms p95=${d['p(95)'].toFixed(2)}ms p99=${d['p(99)'].toFixed(2)}ms ` +
    `p99.9=${d['p(99.9)'].toFixed(2)}ms fail=${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
  out.stdout = line;
  return out;
}
