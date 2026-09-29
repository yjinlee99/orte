# ORTE

애니메이션 작품별 게시판을 중심으로 이용자들이 이야기를 나눌 수 있는 커뮤니티 서비스입니다.

이용자가 원하는 작품의 게시판 개설을 신청하고, 운영자가 승인하면 실제 게시판이 생성되는 흐름을 구현하고 있습니다.

현재는 **회원 인증 → 게시판 개설 신청 → 운영자 승인** 흐름까지 구현했습니다.

---

## 주요 구현

- Spring Security + JWT 기반 인증
- Access Token / Refresh Token 발급 및 Rotation
- Refresh Token SHA-256 해시 저장 및 재사용 차단
- 인증된 회원의 게시판 개설 신청 / 본인 신청 조회
- 운영자 권한을 이용한 게시판 개설 승인
- 승인 시 `Board` 생성 및 신청자를 게시판 방장으로 연결
- 중복 승인 방지 및 승인 실패 시 트랜잭션 롤백
- 공통 오류 응답 및 Bean Validation
- 인증 실패 `401` / 권한 부족 `403` 응답 분리
- MockMvc 기반 실제 JWT 인증 흐름 통합 테스트

---

## Tech Stack

`Java 17` `Spring Boot 4.1.1` `Spring Security` `Spring Data JPA`  
`H2` `Gradle` `JUnit` `MockMvc`

---

## 핵심 흐름

```text
회원 로그인
   ↓
JWT 발급
   ↓
게시판 개설 신청
   ↓
PENDING
   ↓
운영자 승인
   ↓
Board 생성
   ↓
신청자를 Board owner로 연결
   ↓
APPROVED
```

---

<details>
<summary><strong>🔐 인증 / JWT</strong></summary>

### JWT 회원 식별

로그인 이후에는 변경 가능한 이메일 대신 JWT `subject`에 저장된  
**`memberId`를 회원 식별값으로 사용**합니다.

```text
로그인
 ↓
JWT sub = memberId
 ↓
JwtAuthenticationFilter
 ↓
memberId로 Member 조회
 ↓
SecurityContext
```

게시판 신청 시에도 신청자 ID를 클라이언트에서 전달받지 않고  
인증된 사용자의 `memberId`를 사용합니다.

### Access Token

```text
sub       = memberId
role      = ROLE_USER / ROLE_ADMIN
tokenType = ACCESS
iat       = 발급 시간
exp       = 만료 시간
```

```http
Authorization: Bearer <Access Token>
```

### Refresh Token Rotation

Refresh Token을 사용하면 기존 토큰을 폐기하고 새로운 토큰을 발급합니다.

```text
R0 발급
 ↓
R0으로 재발급
 ↓
R0 폐기 + R1 발급
 ↓
R0 재사용 거절
 ↓
R1 정상 사용
```

Refresh Token에는 `jti`를 포함해 연속 발급 시에도 서로 다른 값이 생성되도록 했습니다.

Refresh Token 원문은 DB에 저장하지 않고 **SHA-256 해시값만 저장**합니다.

```text
Refresh Token
      ↓
   SHA-256
      ↓
token_hash 저장
```

### 로그아웃

로그아웃 시 Refresh Token 저장 기록을 삭제합니다.

삭제된 Refresh Token은 다시 사용할 수 없습니다.

현재 Access Token은 즉시 폐기하지 않으며, 남은 유효기간 동안 사용할 수 있습니다.

</details>

---

<details>
<summary><strong>📝 게시판 개설 신청</strong></summary>

### 신청

```http
POST /api/board-applications
Authorization: Bearer <Access Token>
```

```json
{
  "title": "진격의 거인",
  "description": "진격의 거인 이야기 게시판"
}
```

성공 시:

```text
201 Created
```

신청 시 서버에서 자동으로 설정합니다.

```text
applicantId → 인증된 회원의 memberId
status      → PENDING
```

입력 길이는 애플리케이션 검증 기준과 DB 컬럼 기준을 동일하게 맞췄습니다.

```text
title       → 최대 100자
description → 최대 1000자
```

허용 길이를 초과한 경우 `400 VALIDATION_FAILED`를 반환하며 DB에는 저장하지 않습니다.

### 내 신청 조회

```http
GET /api/me/board-applications
Authorization: Bearer <Access Token>
```

인증된 회원의 `memberId`를 기준으로 자신의 신청만 조회합니다.

신청이 없는 경우 빈 배열을 반환합니다.

```json
[]
```

</details>

---

<details>
<summary><strong>✅ 게시판 승인</strong></summary>

운영자 권한을 가진 회원만 게시판 개설 신청을 승인할 수 있습니다.

```http
POST /api/board-applications/{id}/approve
Authorization: Bearer <Admin Access Token>
```

성공 시:

```text
204 No Content
```

승인은 하나의 트랜잭션 안에서 처리합니다.

```text
PENDING 신청 조회
      ↓
Board 생성
      ↓
신청자를 Board owner로 연결
      ↓
PENDING → APPROVED
```

### 권한

```text
인증되지 않은 요청
→ 401 Unauthorized

인증됐지만 권한이 없는 USER
→ 403 Forbidden
```

`401` 응답에는 다음 헤더를 포함합니다.

```http
WWW-Authenticate: Bearer
```

### 중복 승인

이미 처리된 신청을 다시 승인하면:

```text
409 Conflict
```

를 반환하며 Board는 추가 생성되지 않습니다.

### 트랜잭션 롤백

승인 과정 중 예외가 발생하면 일부 변경만 남지 않도록 전체 작업을 롤백합니다.

```text
Board INSERT
 ↓
강제 예외
 ↓
ROLLBACK
 ↓
Board 없음
BoardApplication = PENDING
```

</details>

---

<details>
<summary><strong>⚠️ 공통 오류 응답</strong></summary>

일반적인 비즈니스 예외는 `GlobalExceptionHandler`를 통해 공통 형식으로 처리합니다.

```json
{
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 본문을 확인해 주세요.",
  "path": "/api/board-applications",
  "timestamp": "2026-09-23T00:00:00"
}
```

입력값 검증 실패 시 필드별 오류를 포함합니다.

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "입력값을 확인해 주세요.",
  "path": "/api/board-applications",
  "timestamp": "2026-09-23T00:00:00",
  "errors": {
    "title": "게시판 제목은 필수입니다."
  }
}
```

Security Filter 단계에서 발생하는 인증·인가 실패는 각각

```text
AuthenticationEntryPoint → 401
AccessDeniedHandler      → 403
```

에서 동일한 오류 형식으로 처리합니다.

주요 오류 코드:

```text
VALIDATION_FAILED
INVALID_REQUEST
UNSUPPORTED_MEDIA_TYPE
INTERNAL_SERVER_ERROR

EMAIL_ALREADY_EXISTS
NICKNAME_ALREADY_EXISTS
INVALID_CREDENTIALS
INVALID_REFRESH_TOKEN

AUTHENTICATION_REQUIRED
ACCESS_DENIED

BOARD_APPLICATION_NOT_FOUND
BOARD_APPLICATION_ALREADY_PROCESSED
```

</details>

---

<details>
<summary><strong>🧪 테스트</strong></summary>

`SpringBootTest` 기반 통합 테스트를 작성하고 있으며,  
API 전체 흐름은 `MockMvc`를 사용해 검증합니다.

### JWT 인증

```text
로그인
→ JWT 발급
→ JwtAuthenticationFilter
→ SecurityContext
→ Controller
→ Service
→ Repository
→ DB
```

실제 사용자 A/B를 로그인시킨 뒤 각자의 토큰으로 자신의 데이터만 조회되는지 확인합니다.

### Refresh Token Rotation

```text
로그인
 ↓
R0 발급
 ↓
R0 refresh
 ↓
R1 발급
 ↓
R0 재사용 → 401
 ↓
R1 refresh
 ↓
R2 발급
 ↓
R2 logout
 ↓
R2 재사용 → 401
```

### 게시판 신청

```text
정상 신청
최대 길이 정상 저장
길이 초과 → 400
검증 실패 시 DB 변경 없음
본인 신청만 조회
```

### 게시판 승인

```text
USER 승인 요청 → 403
ADMIN 승인 요청 → 성공
Board 생성
신청자 → Board owner
PENDING → APPROVED
재승인 → 409
추가 Board 생성 없음
```

### 승인 롤백

테스트 클래스 자체에는 `@Transactional`을 사용하지 않고  
서비스 트랜잭션이 종료된 이후 DB 상태를 다시 조회합니다.

```text
Board 저장
 ↓
강제 예외
 ↓
ROLLBACK
 ↓
Board 없음
Application = PENDING
```

</details>

---

## 다음 구현

- 게시판 개설 신청 거절
- 게시판 목록 / 상세 조회
- 게시글 / 댓글
- 게시판별 관리자 기능
- 서비스 운영자 권한 관리
- 동시 승인 처리