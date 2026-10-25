// 부하테스트용 회원 액세스 토큰 생성기 (로컬 벤치 DB 전용, 운영과 무관).
//
// 부하테스트는 OAuth 로그인을 자동화할 수 없으므로, 로컬에서 실행 중인 벤치 앱과 "같은 서명 키"로
// 회원 JWT를 직접 만들어 쓴다. 이건 인증 우회가 아니라 테스트 픽스처다 — 벤치 앱을 띄울 때 준
// JWT_SECRET_KEY 와 동일한 값을 여기에도 줘야 서명이 맞는다.
//
//   사용법:  JWT_SECRET=<벤치앱과 같은 base64 키> node make-token.js <memberId>
//   예:      JWT_SECRET=dGVzdC1iZW5jaC1zZWNyZXQta2V5... node make-token.js 1
//
// jjwt 0.9.1 은 HS256 서명 시 secretKey 문자열을 base64 로 디코드해 키 바이트로 쓴다.
// 그래서 여기서도 같은 base64 문자열을 디코드해 동일한 바이트를 만든다.
const crypto = require('crypto');

const secretString = process.env.JWT_SECRET;
if (!secretString) {
  console.error('JWT_SECRET 환경변수가 필요합니다 (벤치 앱의 JWT_SECRET_KEY 와 같은 값).');
  process.exit(1);
}
const secret = Buffer.from(secretString, 'base64');

const b64u = (o) => Buffer.from(typeof o === 'string' ? o : JSON.stringify(o)).toString('base64url');
const memberId = Number(process.argv[2] || 1);
const now = Math.floor(Date.now() / 1000);
const head = b64u({ alg: 'HS256' });
const body = b64u({ id: memberId, role: 'ROLE_MEMBER', type: 'ACCESS_TOKEN', iat: now, exp: now + 86400 * 7 });
const sig = crypto.createHmac('sha256', secret).update(head + '.' + body).digest('base64url');
console.log(`${head}.${body}.${sig}`);
