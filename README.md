# ORTE

애니메이션 작품별 게시판을 중심으로 이용자들이 이야기를 나눌 수 있는 커뮤니티 서비스입니다.

이용자가 원하는 작품의 게시판 개설을 신청하고, 운영자가 승인하면 실제 게시판이 생성되는 흐름을 목표로 개발하고 있습니다.

현재는 **회원 인증과 게시판 개설 신청 흐름**을 구현했습니다.

---

## 주요 구현

- Spring Security + JWT 기반 인증
- Access Token / Refresh Token 발급 및 재발급
- JWT `subject`에 `memberId`를 사용해 회원 식별
- Refresh Token Rotation
- 사용된 Refresh Token 재사용 차단
- Refresh Token SHA-256 해시 저장
- `jti`를 이용한 Refresh Token 발급값 고유성 보장
- 인증된 회원의 게시판 개설 신청
- 본인의 게시판 신청 목록 조회
- 공통 오류 응답 및 Bean Validation
- 실제 JWT 인증을 이용한 회원별 데이터 분리 테스트

---

## Tech Stack

`Java 17` `Spring Boot 4.1.1` `Spring Security` `Spring Data JPA`  
`H2` `Gradle` `JUnit` `MockMvc`

---

## 핵심 설계

### 1. JWT 회원 식별

로그인 시에는 이메일과 비밀번호를 사용하지만,  
로그인 이후 JWT 인증에서는 변경 가능한 이메일 대신 **회원의 `memberId`를 식별값으로 사용**합니다.

```text
email + password
        ↓
로그인 성공
        ↓
JWT sub = memberId
        ↓
JwtAuthenticationFilter
        ↓
memberId로 Member 조회
        ↓
SecurityContext
```

게시판 신청 시 신청자 ID를 요청 본문에서 전달받지 않고,  
인증된 사용자 정보에서 `memberId`를 가져와 사용합니다.

---

### 2. Refresh Token Rotation

Refresh Token을 재발급에 사용하면 기존 토큰을 폐기하고 새로운 Refresh Token을 발급합니다.

```text
로그인
  ↓
R0 발급
  ↓
R0으로 재발급 요청
  ↓
R0 폐기 + R1 발급
  ↓
R0 재사용 거절
  ↓
R1은 정상 사용 가능
```

같은 회원이 짧은 시간 안에 토큰을 연속 발급받더라도 서로 다른 값이 생성되도록  
Refresh Token에 `jti`를 추가했습니다.

---

<details>
<summary><strong>🔐 인증 / JWT 자세히 보기</strong></summary>

### 로그인

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

로그인에 성공하면 Access Token과 Refresh Token을 반환합니다.

```text
Access Token  : 30분
Refresh Token : 7일
```

JWT Secret은 저장소에 직접 저장하지 않고 환경 변수로 주입합니다.

---

### Access Token

Access Token에는 현재 다음 정보를 포함합니다.

```text
sub       = memberId
role      = ROLE_USER / ROLE_ADMIN
tokenType = ACCESS
iat       = 발급 시간
exp       = 만료 시간
```

API 요청 시 다음 형식으로 전달합니다.

```http
Authorization: Bearer <Access Token>
```

`JwtAuthenticationFilter`에서 토큰의 서명·만료와 `tokenType`을 확인하고,  
`sub`에서 `memberId`를 꺼내 회원을 조회한 뒤 `SecurityContext`에 인증 정보를 저장합니다.

---

### Refresh Token 저장

Refresh Token 원문은 DB에 저장하지 않고 **SHA-256 해시값**을 저장합니다.

```text
Refresh Token 원문
        ↓
     SHA-256
        ↓
   token_hash 저장
```

재발급 요청이 들어오면 전달된 토큰을 동일하게 해시하여 저장된 토큰을 찾습니다.

---

### Refresh Token 재발급

```http
POST /api/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<Refresh Token>"
}
```

재발급 과정은 다음과 같습니다.

```text
Refresh Token 검증
        ↓
DB에 저장된 토큰 확인
        ↓
기존 Refresh Token 삭제
        ↓
새 Access Token 발급
        ↓
새 Refresh Token 발급
        ↓
새 Refresh Token 저장
```

기존 토큰 삭제와 새 토큰 저장은 하나의 트랜잭션 안에서 처리합니다.

---

### 로그아웃

```http
POST /api/auth/logout
Content-Type: application/json
```

```json
{
  "refreshToken": "<Refresh Token>"
}
```

로그아웃 시 전달받은 Refresh Token의 저장 기록을 삭제합니다.

따라서 해당 Refresh Token을 이용한 추가 재발급은 불가능합니다.

현재는 이미 발급된 Access Token을 즉시 폐기하지 않으며,  
Access Token은 남은 유효기간 동안 사용할 수 있습니다.

</details>

---

<details>
<summary><strong>📝 게시판 개설 신청 자세히 보기</strong></summary>

### 게시판 개설 신청

```http
POST /api/board-applications
Authorization: Bearer <Access Token>
Content-Type: application/json
```

요청 예시

```json
{
  "title": "진격의 거인",
  "description": "진격의 거인 이야기 게시판"
}
```

성공 시

```text
201 Created
```

신청 데이터에는 현재 다음 정보를 저장합니다.

```text
id
applicantId
title
description
status
createdAt
```

신청자 ID와 신청 상태는 클라이언트가 직접 전달하지 않습니다.

```text
applicantId → 인증된 회원의 memberId
status      → PENDING
```

---

### 내 게시판 신청 조회

```http
GET /api/me/board-applications
Authorization: Bearer <Access Token>
```

인증된 회원의 `memberId`를 기준으로 자신의 신청만 조회합니다.

신청 내역이 없는 경우 오류가 아닌 빈 배열을 반환합니다.

```json
[]
```

</details>

---

<details>
<summary><strong>⚠️ 공통 오류 응답 자세히 보기</strong></summary>

API마다 오류 형식이 달라지지 않도록 `GlobalExceptionHandler`를 통해 공통 응답 형식으로 처리합니다.

일반 오류 응답

```json
{
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 본문을 확인해 주세요.",
  "path": "/api/board-applications",
  "timestamp": "2026-09-23T00:00:00"
}
```

입력값 검증 실패 시 필드별 오류를 함께 반환합니다.

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

주요 오류 코드

```text
VALIDATION_FAILED
INVALID_REQUEST
UNSUPPORTED_MEDIA_TYPE
INTERNAL_SERVER_ERROR

EMAIL_ALREADY_EXISTS
NICKNAME_ALREADY_EXISTS
INVALID_CREDENTIALS
INVALID_REFRESH_TOKEN
```

예상 가능한 비즈니스 오류는 `BusinessException`을 기반으로 처리하고,  
예상하지 못한 예외의 상세 내용은 클라이언트에 노출하지 않고 서버 로그에 기록합니다.

</details>

---

<details>
<summary><strong>🧪 테스트 자세히 보기</strong></summary>

`SpringBootTest`와 `MockMvc`를 이용한 통합 테스트를 작성하고 있습니다.

### 인증

- 정상 회원가입
- 이메일 중복 가입 거절
- 닉네임 중복 가입 거절
- 비밀번호 BCrypt 암호화 확인
- 로그인 후 JWT 발급
- 짧은 시간 안에 발급된 Refresh Token이 서로 다른 값인지 확인
- Refresh Token 재발급 성공
- 사용된 Refresh Token 재사용 거절
- 새로 발급된 Refresh Token 정상 사용

### 게시판 신청

- 정상 개설 신청
- 신규 신청 상태가 `PENDING`인지 확인
- 신청 데이터 DB 저장 확인
- 본인의 신청만 조회
- 필수 입력값 검증
- 잘못된 JSON 요청 거절
- 지원하지 않는 Content-Type 요청 거절

### 실제 JWT 인증 흐름

테스트 사용자 A와 B를 각각 로그인시켜 실제 Access Token을 발급받은 뒤 검증합니다.

```text
회원 A 로그인 → Access Token A
회원 B 로그인 → Access Token B

A 토큰 → A의 게시판 신청 생성
B 토큰 → B의 게시판 신청 생성

A 토큰으로 조회 → A 신청만 반환
B 토큰으로 조회 → B 신청만 반환
```

이를 통해 테스트용 인증 객체를 직접 주입하는 방식뿐 아니라,

```text
로그인
→ JWT 발급
→ JwtAuthenticationFilter
→ SecurityContext
→ Controller
→ Service
```

까지 실제 인증 흐름이 연결되는지 확인합니다.

</details>

---

## 다음 구현

게시판 개설 신청 이후의 **승인 흐름**을 구현할 예정입니다.

```text
운영자 승인
    ↓
Board 생성
    ↓
신청자를 게시판 방장으로 연결
    ↓
BoardApplication
PENDING → APPROVED
```

다음 단계에서는 아래 항목을 구현합니다.

- 운영자 권한을 이용한 게시판 개설 승인
- 실제 `Board` 생성
- 신청자와 게시판 방장 연결
- 승인 과정의 트랜잭션 처리
- 이미 처리된 신청의 중복 승인 방지
- 승인 실패 시 전체 롤백 검증

동시 승인 처리와 게시판 개설 거절 기능은 이후 단계에서 다룰 예정입니다.