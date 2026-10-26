# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Gradle wrapper (Java 17 toolchain). On Windows use `.\gradlew.bat`; under the Bash tool use `./gradlew`.

```bash
./gradlew build                 # compile + test + jar
./gradlew build -x test         # what CI actually runs (tests need live MySQL/Redis)
./gradlew bootRun               # run on :8080
./gradlew test                  # all tests
./gradlew test --tests "com.example.chosim.chosim.ChosimApplicationTests"   # single test class
./gradlew test --tests "*.MaimuServiceTest.shouldToggleFavorite"           # single test method
./gradlew clean                 # also deletes src/main/generated (QueryDSL Q-classes)
```

Running locally requires a MySQL database named `maimu` and a Redis instance on `localhost:6379`.
`docker-compose.yml` only builds the API container — it does **not** provide MySQL/Redis.
`spring.jpa.hibernate.ddl-auto: none`, so **the schema is not created by the app**; it must exist already.
Audit log DDL is in `docs/sql/audit_logs.sql`; existing business schemas still require manual management.

Swagger UI: `/swagger-ui.html`. Health check: `/health`.

### API 목록

| 메서드 | 경로 | 권한 | 비고 |
| --- | --- | --- | --- |
| GET | `/v1/api/admin/audit-logs?page=` | ADMIN | 삭제·탈퇴 감사 기록, 최신순 20건, page는 0부터 |

## Architecture

Spring Boot 3.2 / Java 17 REST API for MAIMU — a service where a member creates "groups" (lockers)
and shares an invite link; unauthenticated guests write anonymous message cards ("maimu") into that group.

Root package is `com.example.chosim.chosim` (`chosim` is the project's old name; the artifact is still
`chosim`, the product is MAIMU).

Three top-level layers, and features are sliced *across* them, not within:

- `api/<feature>/` — controllers + request/response DTOs. This is the only layer that knows HTTP.
- `domain/<feature>/` — entity, repository, service, enums. Services are `@Transactional(readOnly = true)`
  at class level with `@Transactional` on individual write methods.
- `common/` — cross-cutting infra: `config`, `auth` (OAuth2), `jwt`, `filter`, `error`, `email`, `entity`,
  `invitation`.

Note the split: `auth` exists in both `api/auth` (token reissue/logout) and `domain/auth`
(the `Member` entity, `MemberService`, `MemberRepository`) — `Member` lives under `domain/auth`, not a
`domain/member` package, while its controller is `api/member`. Follow that when adding members-related code.

### Authentication flow

Social login only (Google / Kakao / Naver) — there is no password anywhere.

1. `CustomOAuth2UserService` loads the provider profile, normalizes it via `OAuth2Attribute.of(provider, …)`
   (per-provider attribute unwrapping lives there), then creates or updates the `Member`. New members get
   role `PREMEMBER`.
2. `CustomOauth2SuccessHandler` branches on role:
   - `PREMEMBER` → redirect to the frontend with `?tempToken=<access>`; the user must still call
     `POST /v1/api/member/join` to supply a profile, which promotes them to `MEMBER` and issues real tokens.
   - `MEMBER` → refresh token saved to Redis + set as an HttpOnly/Secure cookie; access token passed as a
     query param on the redirect.
   - The redirect target and the CORS allowed origin are **hardcoded** to `https://maimu.vercel.app`
     (`CustomOauth2SuccessHandler`, `CorsConfig`). Change both when pointing at a different frontend.
3. `JwtAuthorizationFilter` runs before `UsernamePasswordAuthenticationFilter`, extracts the member id and
   role from the bearer token, and sets the authentication principal to the **`Long` member id**. That is why
   controllers take `@AuthenticationPrincipal Long memberId` — there is no `UserDetails` object.
4. `JwtExceptionFilter` sits *before* `JwtAuthorizationFilter` and converts JWT exceptions into the standard
   `ErrorResponse` JSON with 401. Exceptions thrown inside filters never reach `GlobalExceptionHandler`;
   for filter-level failures throw `FilterAuthenticationException`.
5. Filter skip-list (`shouldNotFilter`) and Spring Security's `WHITE_LIST` in `SecurityConfig` are two
   separate lists that must be kept in sync — adding a public endpoint means editing both.

Roles use the `MemberRole` enum whose `key` is the Spring authority string (`ROLE_PREMEMBER`, `ROLE_MEMBER`, …).
Use `MemberRole.fromKey()` to go back from an authority string to the enum.

Redis holds three short-lived things, all as `@RedisHash` entities in `common/`: `RefreshToken` (3d, keyed by
member id), `BlackList` (revoked access/refresh tokens on logout and on reissue), and `Invitation` (1d, a UUID
that maps to `groupId` + `senderId`).

### Guest / invitation path

`POST /v1/api/group/{groupId}/invitation` mints a UUID into Redis. Guests hit `/v1/api/guest/**`
(fully public — skipped by both the filter and Spring Security) and pass that token. `GuestService` resolves
the token to a group and appends a `Maimu` via `group.addMaimu(...)` — the write happens through JPA dirty
checking + `CascadeType.ALL`, not an explicit `maimuRepository.save`.

### Persistence

- MySQL + JPA, with QueryDSL for the one paged query that needs custom ordering
  (`MaimuRepositoryImpl.findMaimusByGroupId` — favorites first, then newest). Q-classes are generated by the
  annotation processor into `build/generated/...`; regenerate with a build after changing an entity.
- p6spy logs formatted SQL.
- `BaseTimeEntity` (`@MappedSuperclass` + auditing, enabled by `JpaConfig`) gives every entity
  `createdAt`/`modifiedAt`.
- Entity table names differ from class names: `Member` → `members`, **`Group` → `lockers`**, `Maimu` → `maimus`.
- Unread counts are computed in JPQL constructor-expression queries in `GroupRepository` that project straight
  into `GroupResponse` — don't add a counter column.
- Deletes of a member or group take pessimistic write locks (`findByIdWithLock`, `findByMemberIdWithLock`)
  and run at `Isolation.REPEATABLE_READ`; member deletion clears Redis in an `afterCommit` synchronization.

### Errors

Every business failure is an `AppException(ErrorCode.X)`; `ErrorCode` is the single enum holding
HTTP status + code + Korean message. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `AppException`,
bean-validation failures, `EntityNotFoundException`, `DataIntegrityViolationException`, and a catch-all.
Add new failures as `ErrorCode` entries rather than throwing raw exceptions — a few older call sites still
throw `EntityNotFoundException`/`IllegalArgumentException` directly, which land on the generic handlers.

### Email digest

`EmailSchedule` runs hourly 09:00–22:00 (`@Scheduled(cron = "0 0 9-22 * * *")`, server timezone assumed
`Asia/Seoul`), `EmailService` groups unread+unnotified maimus per member, sends HTML mail asynchronously on
the `emailTaskExecutor` pool (`ThreadPoolConfig`) with 3 retries, and only then flips `isNotified`.

## Landmines

- `JwtTokenProvider` has `@Value("${jwt.secretKey")` — the closing brace is missing, so the *literal* string
  `${jwt.secretKey` is used as the HMAC key and `JWT_SECRET_KEY` is ignored. Signing and verification both
  use the same literal so nothing breaks visibly. Fixing the typo invalidates all issued tokens.
- `application.yml` has a real MySQL password committed as a default value; deploy overrides everything via
  the GitHub Actions `variable-substitution` step (`.github/workflows/deploy.yml`) rather than profiles.
  There is only one `application.yml` — no `application-local/prod.yml`.
- `EmailService.updateMaimuStatus` is `@Transactional` but `protected` and self-invoked, so the proxy is
  bypassed; the write only works because `@Modifying` on the repository method carries its own transaction.
- `jjwt 0.9.1` is the old pre-`Jwts.parserBuilder` API and needs the `javax.xml.bind:jaxb-api` shim on Java 17.
- Tests are effectively unwritten (only `contextLoads`, which needs a live MySQL and Redis); WireMock and
  spring-security-test are on the classpath but unused. CI skips tests entirely.

## Conventions

- Korean is used for log messages, exception messages, and Swagger `@Operation` descriptions. Match that.
- Lombok everywhere: entities are `@Getter` + `@NoArgsConstructor(access = PROTECTED)` + `@Builder` on a
  constructor, with explicit `updateX()` mutators — no setters. Beans use `@RequiredArgsConstructor`
  constructor injection.
- Every endpoint is annotated with springdoc `@Tag`/`@Operation`.
- Routes are `/v1/api/<feature>`.
- Commit messages follow the README convention: `feat:`, `fix:`, `style:`, `docs:`, `refactor:`, `chore:`
  (the existing history uses a capitalized `Fix : …` / `Refactor : …` variant).
