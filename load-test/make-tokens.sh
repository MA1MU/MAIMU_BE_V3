#!/bin/sh
# 사물함 1..50 의 초대 토큰을 발급해 tokens.json([{groupId, token}]) 으로 저장한다.
# 벤치 앱이 :8080 에 떠 있어야 하고, 회원·사물함 시드(seed.sql)가 들어가 있어야 한다.
#   JWT_SECRET=<벤치앱과 같은 base64 키> BASE_URL=http://localhost:8080 sh make-tokens.sh
S="$(dirname "$0")"
BASE_URL="${BASE_URL:-http://localhost:8080}"
out="["
for g in $(seq 1 50); do
  token=$(curl -s -X POST -H "Authorization: Bearer $(JWT_SECRET="$JWT_SECRET" node "$S/make-token.js" "$g")" \
    "$BASE_URL/v1/api/group/$g/invitation")
  out="$out{\"groupId\":$g,\"token\":\"$token\"},"
done
echo "${out%,}]" > "$S/tokens.json"
echo "tokens.json 생성 완료 ($(grep -o groupId "$S/tokens.json" | wc -l) 개)"
