# load-test — 부하테스트 모음

MAIMU 백엔드의 성능 개선(Write-back / Cache-aside / SSE)을 측정·재현하는 스크립트와 결과.
**무엇을 왜 이렇게 쟀는지**와 **처음부터 다시 돌리는 법**은 [`../docs/IMPLEMENTATION_GUIDE.md`](../docs/IMPLEMENTATION_GUIDE.md) 6·7장에 있다.
결과 요약은 [`../docs/PERFORMANCE_REPORT.md`](../docs/PERFORMANCE_REPORT.md).

## 파일

| 파일 | 역할 |
| --- | --- |
| `common.js` | 공통 설정. `MODE`(throughput/latency)로 closed/open model 전환, p99.9까지 통계, 결과 저장 |
| `write-maimu.js` | 쪽지 작성 부하. `TARGET=hot`(인기 사물함 1곳) / `spread`(전체 분산) |
| `read-guest.js` | 초대 링크 조회 부하 (인기 사물함 1곳) |
| `sse-latency.mjs` | 알림 종단 지연 측정 (작성 요청 → 주인 SSE 도착까지, Node 스크립트) |
| `seed.sql` | 시드 데이터 (회원 50 / 사물함 50 / 인기 사물함에 기존 쪽지 300) |
| `make-token.js` | 로컬 벤치용 회원 JWT 생성기 (테스트 픽스처, 운영과 무관) |
| `make-tokens.sh` | 사물함 50개의 초대 토큰을 발급해 `tokens.json` 생성 |
| `results/` | k6 요약 원본 (버전·시나리오별 JSON) |

`tokens.json` 은 로컬에서 발급한 값이라 커밋하지 않는다(`.gitignore`).

## 빠른 실행 (벤치 MySQL·Redis·앱이 떠 있는 상태 가정)

```bash
# 0) 시드 (한글 깨짐 방지: utf8mb4)
docker exec -i bench-mysql mysql --default-character-set=utf8mb4 -uroot -pbench maimu < seed.sql

# 1) 초대 토큰 발급 (JWT_SECRET 은 벤치 앱의 JWT_SECRET_KEY 와 같은 값)
JWT_SECRET="$BENCH_SECRET" sh make-tokens.sh

# 2) 측정
MODE=throughput TARGET=hot VUS=100 DURATION=30s NAME=v2-write-hot k6 run write-maimu.js
MODE=latency    TARGET=hot RATE=60 DURATION=30s NAME=v2-write-lat k6 run write-maimu.js
MODE=throughput            VUS=100 DURATION=30s NAME=v2-read      k6 run read-guest.js
ACCESS_TOKEN=$(JWT_SECRET="$BENCH_SECRET" node make-token.js 1) SAMPLES=300 node sse-latency.mjs
```

전체 환경 구성(컨테이너 기동·스키마 생성·유실 0 검증 포함)은 가이드 7장을 그대로 따라 하면 된다.
