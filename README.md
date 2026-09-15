# ORTE

애니메이션 작품별 게시판을 중심으로 이용자들이 이야기를 나눌 수 있는 커뮤니티 서비스입니다.

이용자는 원하는 작품의 게시판 개설을 신청할 수 있으며, 운영자가 신청 내용을 확인하고 승인하면 실제 게시판이 생성되는 구조를 목표로 개발하고 있습니다.

현재는 게시판 개설 신청·조회 기능을 시작으로 회원 및 인증 구조를 구현하고 있습니다.

---

## 개발 환경

- Java 17
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- Spring Security
- H2 Database
- Gradle
- JUnit
- MockMvc

---

## 1. 게시판 개설 신청

이용자가 애니메이션 게시판 개설을 신청하고 자신의 신청 내역을 조회할 수 있습니다.

신청 데이터와 실제 게시판 데이터를 분리하는 구조를 선택했습니다.

승인되기 전 신청 정보가 공개 게시판과 섞이지 않도록 하고, 추후 운영자가 신청을 승인하면 별도의 `Board`를 생성할 수 있도록 하기 위함입니다.

### BoardApplication

현재 저장하는 정보

- 신청 ID
- 신청자 ID
- 게시판 제목
- 게시판 소개
- 이미지 경로
- 신청 상태
- 신청일

신규 신청의 상태는 클라이언트가 전달하지 않고 서버에서 항상 `PENDING`으로 설정합니다.

```text
PENDING
APPROVED
REJECTED
```

현재 회원 인증 연결 작업이 진행 중이므로 게시판 신청자는 임시 사용자 ID를 사용하고 있습니다.

JWT 인증 구현 완료 후 로그인한 회원 정보와 연결할 예정입니다.

### 게시판 개설 신청 API

```http
POST /api/board-applications
Content-Type: application/json
```

요청 예시

```json
{
  "title": "진격의 거인",
  "description": "진격의 거인 이야기 게시판",
  "imagePath": "/images/aot.jpg"
}
```

성공 시

```text
201 Created
```

신청자의 ID와 신청 상태는 요청에서 전달받지 않고 서버에서 결정합니다.

### 내 게시판 신청 조회

```http
GET /api/me/board-applications
```

현재 사용자의 신청 데이터만 조회합니다.

신청 내역이 없는 경우 오류를 반환하지 않고 빈 배열을 반환합니다.

```json
[]
```

---

## 2. 요청 검증

게시판 신청 요청에는 Bean Validation을 적용했습니다.

현재 검증 항목

- 게시판 제목 필수
- 게시판 소개 필수

제목이 없는 경우의 응답 예시입니다.

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "입력값을 확인해 주세요.",
  "path": "/api/board-applications",
  "timestamp": "2026-09-16T00:00:00",
  "errors": {
    "title": "게시판 제목은 필수입니다."
  }
}
```

---

## 3. 공통 예외 처리

API마다 서로 다른 형태의 오류 응답이 발생하지 않도록 `GlobalExceptionHandler`를 통해 공통 형식으로 처리하고 있습니다.

일반 오류 응답 형식

```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "요청한 리소스를 찾을 수 없습니다.",
  "path": "/api/example",
  "timestamp": "2026-09-16T00:00:00"
}
```

입력값 검증 오류에는 필드별 오류 내용을 `errors`로 추가합니다.

### ErrorCode

오류 코드와 HTTP 상태, 사용자에게 반환할 메시지를 `ErrorCode`에서 관리합니다.

현재 정의한 주요 오류 코드는 다음과 같습니다.

```text
VALIDATION_FAILED
INVALID_REQUEST
UNSUPPORTED_MEDIA_TYPE
INTERNAL_SERVER_ERROR

EMAIL_ALREADY_EXISTS
NICKNAME_ALREADY_EXISTS
```

서비스에서 예상 가능한 비즈니스 오류는 `BusinessException`을 기반으로 처리합니다.

`GlobalExceptionHandler`에서는 개별 도메인 예외마다 Handler를 만드는 대신 `BusinessException`을 공통으로 처리합니다.

이를 통해 도메인 예외가 증가하더라도 `GlobalExceptionHandler`가 지나치게 커지지 않도록 구성했습니다.

### 현재 처리 중인 오류

#### 400 Bad Request

`VALIDATION_FAILED`

- Bean Validation 실패

`INVALID_REQUEST`

- 잘못된 JSON
- 요청 본문 파싱 실패

#### 409 Conflict

`EMAIL_ALREADY_EXISTS`

- 이미 사용 중인 이메일

`NICKNAME_ALREADY_EXISTS`

- 이미 사용 중인 닉네임

#### 415 Unsupported Media Type

`UNSUPPORTED_MEDIA_TYPE`

- 지원하지 않는 Content-Type 요청

#### 500 Internal Server Error

`INTERNAL_SERVER_ERROR`

- 애플리케이션에서 예상하지 못한 오류

예상하지 못한 예외의 상세 내용은 클라이언트에 그대로 노출하지 않고 서버 로그에 기록합니다.

---

## 4. 회원

게시판 신청자, 게시판 방장, 게시글 작성자, 관리자 등의 사용자 정보를 관리하기 위해 `Member` 도메인을 추가했습니다.

### Member

현재 필드

- ID
- 이메일
- 비밀번호
- 닉네임
- 권한
- 가입일

회원 권한

```text
USER
ADMIN
```

일반 회원가입을 통해 생성되는 회원은 서버에서 항상 `USER` 권한으로 설정합니다.

회원가입 요청을 통해 클라이언트가 임의로 `ADMIN` 권한을 지정할 수 없도록 구성했습니다.

이메일과 닉네임에는 Unique 제약을 적용했습니다.

---

## 5. 회원가입

```http
POST /api/auth/signup
Content-Type: application/json
```

요청 예시

```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "방토"
}
```

성공 시

```text
201 Created
```

응답 예시

```json
{
  "memberId": 1
}
```

회원가입 과정

```text
요청값 검증
    ↓
이메일 중복 확인
    ↓
닉네임 중복 확인
    ↓
BCrypt 비밀번호 암호화
    ↓
Member 저장
```

비밀번호는 평문으로 저장하지 않고 `PasswordEncoder`를 사용해 암호화한 후 저장합니다.

---

## 6. Spring Security

Spring Security를 적용해 인증이 필요한 API와 공개 API를 구분하기 시작했습니다.

현재 공개 API

```text
/api/auth/**
/h2-console/**
```

그 외 API는 인증된 사용자만 접근하도록 구성했습니다.

세션 기반 인증을 사용하지 않기 위해 다음 설정을 적용했습니다.

```text
SessionCreationPolicy.STATELESS
```

JWT 기반 인증을 위한 기본 구조를 구현하고 있습니다.

### 로그인 인증 구조

Spring Security의 `AuthenticationManager`와 `DaoAuthenticationProvider`를 활용해 회원 인증을 처리하도록 구성하고 있습니다.

```text
email / password
        ↓
AuthenticationManager
        ↓
DaoAuthenticationProvider
        ↓
CustomUserDetailsService
        ↓
MemberRepository
        ↓
PasswordEncoder 검증
```

`CustomUserDetails`에는 인증 이후 필요한 회원 정보를 담습니다.

현재 포함 정보

- memberId
- email
- password
- role

회원 권한은 Spring Security에서 사용할 수 있도록 다음과 같이 변환합니다.

```text
USER  → ROLE_USER
ADMIN → ROLE_ADMIN
```

---

## 7. JWT

Access Token과 Refresh Token을 이용한 인증 구조를 구현하고 있습니다.

현재 설정

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-expiration: 1800000
  refresh-expiration: 604800000
```

토큰 유효기간

```text
Access Token  : 30분
Refresh Token : 7일
```

JWT Secret은 저장소에 직접 저장하지 않고 환경 변수 `JWT_SECRET`을 통해 주입합니다.

Access Token에는 인증 및 인가에 필요한 정보를 포함할 예정입니다.

```text
email
memberId
role
tokenType
```

Refresh Token은 Access Token 재발급을 위한 최소 정보만 포함하도록 구성할 예정입니다.

---

## 8. 테스트

`MockMvc` 기반 통합 테스트를 작성하고 있습니다.

### 게시판 신청

현재 다음 시나리오를 테스트했습니다.

- 정상 게시판 개설 신청
- 신청 상태가 `PENDING`으로 저장되는지 확인
- 요청 내용이 DB에 실제 저장되는지 확인
- 본인의 신청만 조회되는지 확인
- 제목 누락 시 `400 Bad Request`
- 여러 입력값 검증 실패
- 잘못된 JSON 요청 시 `400 Bad Request`
- 지원하지 않는 Content-Type 요청 시 `415 Unsupported Media Type`
- 잘못된 요청이 DB에 저장되지 않는지 확인

### 회원가입

현재 다음 시나리오를 테스트했습니다.

- 정상 회원가입
- 회원의 기본 권한이 `USER`인지 확인
- 비밀번호가 평문으로 저장되지 않는지 확인
- `PasswordEncoder.matches()`를 이용한 암호화 비밀번호 검증
- 이메일 중복 가입 시 `409 Conflict`
- 닉네임 중복 가입 시 `409 Conflict`
- 잘못된 이메일 형식 요청 시 `400 Bad Request`

비밀번호 길이와 닉네임 길이에 대한 경계값 테스트는 인증 기능 구현 이후 추가할 예정입니다.

---

## 다음 구현 예정

1. Refresh Token 저장 구조
2. 로그인 API
3. Access Token / Refresh Token 발급
4. JWT 인증 필터
5. 로그인한 회원과 게시판 신청자 연결
6. Refresh Token 재발급 및 Rotation
7. 로그아웃
8. 운영자의 게시판 개설 신청 목록 조회
9. 게시판 개설 신청 승인
10. 승인 시 실제 `Board` 생성 및 신청자를 방장으로 연결
11. 중복 승인 방지
12. 승인 과정 트랜잭션 처리
13. 공개 게시판 목록 조회

게시글, 댓글, 신고, 이용자 차단, 게시판 활동 제한, 매니저 기능은 게시판 생성 흐름 구현 이후 진행할 예정입니다.