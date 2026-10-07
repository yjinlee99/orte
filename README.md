
# ORTE

애니메이션 작품별 게시판을 중심으로 이용자들이 이야기를 나눌 수 있는 커뮤니티 서비스입니다.

사용자가 원하는 작품의 게시판 개설을 신청하고, 운영자가 승인하면 실제 게시판이 생성되는 흐름을 구현했습니다.

현재 **회원 인증 → 게시판 개설 신청 → 운영자 승인 → 게시판 생성** 흐름까지 구현되어 있습니다.

---

## 배포

- Railway에서 `Dockerfile` 기반으로 애플리케이션을 배포했습니다.
- 운영 데이터베이스는 PostgreSQL을 사용합니다.
- DB 접속 정보, JWT Secret, SUPER_ADMIN 계정 정보는 환경변수로 관리합니다.

<details>
<summary><strong>배포 주소</strong></summary>

<br>

https://orte-production.up.railway.app/

> 실제 SUPER_ADMIN 로그인 정보와 Secret 값은 저장소에 공개하지 않습니다.

</details>

---

## 주요 기능

- 회원가입 / 로그인
- Spring Security + JWT 기반 인증
- Access Token / Refresh Token 발급
- Refresh Token Rotation 및 재사용 차단
- Refresh Token SHA-256 해시 저장
- 로그아웃
- 게시판 개설 신청
- 본인 게시판 신청 내역 조회
- `ADMIN`, `SUPER_ADMIN` 권한을 이용한 게시판 개설 승인
- 환경변수 기반 초기 `SUPER_ADMIN` 계정 생성
- 중복 승인 방지
- 동시 승인 시 중복 게시판 생성 방지
- 승인 실패 시 트랜잭션 롤백
- 공통 오류 응답 및 입력값 검증

---

## 실행 방법

### 요구 환경

- Java 17
- Git

### 1. 프로젝트 다운로드

```bash
git clone https://github.com/yjinlee99/orte.git
cd orte
```

### 2. 환경변수 설정

로컬 실행 시 필수로 필요한 환경변수는 `JWT_SECRET`입니다.

| 환경변수 | 설명 | 필수 여부 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 서명에 사용하는 Base64 Secret Key | 필수 |
| `SUPER_ADMIN_EMAIL` | 초기 SUPER_ADMIN 이메일 | 관리자 기능 확인 시 필요 |
| `SUPER_ADMIN_PASSWORD` | 초기 SUPER_ADMIN 비밀번호 | 관리자 기능 확인 시 필요 |
| `SUPER_ADMIN_NICKNAME` | 초기 SUPER_ADMIN 닉네임 | 선택 |

`SUPER_ADMIN_EMAIL`과 `SUPER_ADMIN_PASSWORD`를 설정하면 애플리케이션 시작 시 `SUPER_ADMIN`이 존재하지 않는 경우 초기 계정을 자동 생성합니다.

`SUPER_ADMIN_NICKNAME`을 설정하지 않으면 `super-admin`을 사용합니다.

실제 Secret과 비밀번호는 저장소에 포함하지 않습니다.

`JWT_SECRET`은 Base64로 인코딩된 충분한 길이의 값을 사용합니다.

예시:

```bash
openssl rand -base64 32
```

### 3. 로컬 실행

별도의 DB 환경변수를 설정하지 않으면 H2 인메모리 DB를 사용합니다.

Windows:

```bash
gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

기본 실행 주소:

```text
http://localhost:8080
```

> H2 인메모리 DB를 사용하는 경우 애플리케이션을 종료하면 저장된 데이터가 초기화됩니다.

### 4. 테스트 실행

Windows:

```bash
gradlew.bat test
```

macOS / Linux:

```bash
./gradlew test
```

### PostgreSQL 사용

PostgreSQL을 사용하는 경우 다음 환경변수를 추가로 설정합니다.

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | PostgreSQL 사용자명 |
| `DB_PASSWORD` | PostgreSQL 비밀번호 |
| `DDL_AUTO` | Hibernate DDL 설정 |

배포 환경에서는 Railway PostgreSQL을 사용하고 있으며 `DDL_AUTO=update`로 설정합니다.

### Docker 실행

Docker를 사용하는 경우 다음과 같이 이미지를 빌드하고 실행할 수 있습니다.

```bash
docker build -t orte .
```

```bash
docker run --env-file .env -p 8080:8080 orte
```

---

<details>
<summary><strong>서비스 동작 확인 방법</strong></summary>

<br>

아래 순서로 회원가입부터 게시판 승인까지 전체 흐름을 확인할 수 있습니다.

### 1. 일반 회원 가입

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "사용자"
}
```

정상 응답:

```text
201 Created
```

```json
{
  "memberId": 1
}
```

---

### 2. 일반 회원 로그인

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

정상 응답:

```text
200 OK
```

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

이후 인증이 필요한 API에는 Access Token을 전달합니다.

```http
Authorization: Bearer <Access Token>
```

---

### 3. 게시판 개설 신청

```http
POST /api/board-applications
Authorization: Bearer <User Access Token>
Content-Type: application/json
```

```json
{
  "title": "진격의 거인",
  "description": "진격의 거인 이야기를 나누는 게시판"
}
```

정상 응답:

```text
201 Created
```

신청 상태는 `PENDING`으로 생성됩니다.

응답의 `id`는 이후 승인 요청에 사용합니다.

---

### 4. 운영자 로그인

게시판 개설 승인은 `ADMIN` 또는 `SUPER_ADMIN` 권한을 가진 회원이 수행할 수 있습니다.

현재 초기 운영 계정은 환경변수를 이용해 `SUPER_ADMIN`으로 생성하므로, 아래 확인 절차에서는 해당 계정을 사용합니다.

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "<SUPER_ADMIN_EMAIL>",
  "password": "<SUPER_ADMIN_PASSWORD>"
}
```

정상 응답:

```text
200 OK
```

응답으로 받은 Access Token을 관리자 API 호출에 사용합니다.

> 배포 환경의 검증용 SUPER_ADMIN 로그인 정보는 저장소에 공개하지 않고 별도로 전달합니다.

---

### 5. 게시판 개설 승인

`ADMIN` 또는 `SUPER_ADMIN`의 Access Token으로 게시판 개설 신청을 승인합니다.

```http
POST /api/board-applications/{applicationId}/approve
Authorization: Bearer <Admin Access Token>
```

정상 응답:

```text
204 No Content
```

승인이 완료되면 다음 흐름으로 데이터가 변경됩니다.

```text
PENDING
  ↓
Board 생성
  ↓
신청자를 Board owner로 연결
  ↓
APPROVED
```

---

### 권한 확인

일반 `USER`의 Access Token으로 승인 API를 호출하면:

```text
403 Forbidden
```

인증 없이 보호된 API를 호출하면:

```text
401 Unauthorized
```

를 반환합니다.

</details>

---

## 권한 구조

| 권한 | 설명 |
| --- | --- |
| `USER` | 일반 서비스 이용자 |
| `ADMIN` | 게시판 개설 신청 승인 등 운영 기능 수행 |
| `SUPER_ADMIN` | `ADMIN`의 운영 기능을 모두 수행하며, 향후 관리자 권한 관리를 위한 최고 관리자 |

일반 회원가입으로 생성되는 회원은 항상 `USER` 권한을 가집니다.

애플리케이션 시작 시 `SUPER_ADMIN`이 존재하지 않고 관련 환경변수가 설정되어 있으면 초기 `SUPER_ADMIN` 계정을 자동 생성합니다.

---

## Tech Stack

**Backend**

`Java 17` `Spring Boot 4.1.1` `Spring Security` `Spring Data JPA` `JWT`

**Database**

`PostgreSQL` `H2`

**Test**

`JUnit` `MockMvc`

**Infra**

`Docker` `Railway`

---

<details>
<summary><strong>주요 설계 및 문제 해결</strong></summary>

<br>

### JWT 기반 회원 식별

로그인 이후에는 변경 가능한 이메일 대신 JWT의 `subject`에 저장된 `memberId`를 회원 식별값으로 사용합니다.

```text
로그인
  ↓
JWT sub = memberId
  ↓
JwtAuthenticationFilter
  ↓
memberId로 Member 조회
  ↓
SecurityContext 등록
```

게시판 신청 시에도 회원 ID를 요청으로 전달받지 않고 인증된 사용자의 `memberId`를 사용하도록 구성했습니다.

---

### Refresh Token Rotation

Access Token만으로는 서버에서 로그아웃 상태를 관리하기 어려워 Refresh Token을 DB에서 관리합니다.

Refresh Token 원문은 저장하지 않고 SHA-256 해시값을 저장합니다.

```text
Refresh Token
      ↓
   SHA-256
      ↓
token_hash 저장
```

Refresh Token을 사용해 토큰을 재발급하면 기존 토큰을 폐기하고 새로운 Refresh Token을 발급합니다.

```text
R0 발급
  ↓
R0으로 재발급
  ↓
R0 폐기 + R1 발급
  ↓
R0 재사용 시 거절
```

로그아웃 시에도 저장된 Refresh Token을 삭제해 이후 재사용할 수 없도록 했습니다.

---

### 게시판 승인 트랜잭션

게시판 개설 승인은 여러 데이터 변경이 함께 이루어집니다.

```text
PENDING 신청 조회
      ↓
Board 생성
      ↓
신청자를 Board owner로 연결
      ↓
신청 상태 APPROVED 변경
```

이 과정을 하나의 트랜잭션으로 처리해 중간 단계에서 예외가 발생하면 일부 데이터만 변경되지 않도록 했습니다.

통합 테스트에서는 승인 중 강제로 예외를 발생시킨 뒤 트랜잭션 종료 후 DB를 다시 조회하여 다음 상태를 확인했습니다.

```text
Board 없음
BoardApplication = PENDING
```

---

### 동시 승인 시 중복 게시판 생성 방지

동일한 게시판 신청에 두 승인 요청이 동시에 들어오는 테스트를 작성한 결과, 두 요청이 모두 `PENDING` 상태를 읽으면서 Board가 두 개 생성될 수 있는 문제를 확인했습니다.

승인 대상 `BoardApplication`을 조회할 때 비관적 쓰기 락을 적용하여 하나의 승인 처리가 끝날 때까지 다른 요청이 같은 신청을 동시에 처리하지 못하도록 변경했습니다.

```text
요청 A ── PENDING 조회 + LOCK ── 승인 ── COMMIT
                         │
요청 B ───────────────── 대기
                         ↓
                    변경된 상태 확인
                         ↓
                       409
```

이를 통해 하나의 신청에서 하나의 Board만 생성되도록 했습니다.

---

### 인증 실패와 권한 부족 분리

Spring Security Filter 단계에서 발생하는 인증·인가 오류도 애플리케이션의 공통 오류 응답 형식과 맞췄습니다.

```text
인증 정보 없음 / 유효하지 않음
→ AuthenticationEntryPoint
→ 401 Unauthorized

인증은 됐지만 권한 부족
→ AccessDeniedHandler
→ 403 Forbidden
```

---

### 실행 환경과 설정 분리

DB 접속 정보, JWT Secret, 초기 SUPER_ADMIN 계정 정보는 소스 코드에 하드코딩하지 않고 환경변수로 전달합니다.

```text
로컬
→ H2 기본 설정

배포
→ Railway PostgreSQL
→ 환경변수로 DB/JWT/SUPER_ADMIN 설정
```

동일한 애플리케이션을 실행 환경에 따라 설정값만 변경해 사용할 수 있도록 구성했습니다.

</details>

---

<details>
<summary><strong>테스트</strong></summary>

<br>

`SpringBootTest`와 `MockMvc`를 이용해 실제 요청 흐름을 기준으로 통합 테스트를 작성했습니다.

주요 검증 항목:

- 회원가입 / 로그인
- 실제 JWT를 이용한 인증
- 회원별 게시판 신청 데이터 분리
- Refresh Token Rotation
- 폐기된 Refresh Token 재사용 차단
- 로그아웃 후 Refresh Token 재사용 차단
- 입력값 검증 실패 시 DB 변경 여부
- 일반 회원의 관리자 API 접근 거부
- `ADMIN` 권한을 이용한 게시판 승인
- 중복 승인 시 추가 Board 생성 방지
- 동시 승인 요청 시 Board 중복 생성 방지
- 승인 과정 실패 시 트랜잭션 롤백

</details>

---

## 현재 한계

현재는 게시판 개설 신청과 승인 흐름을 중심으로 구현되어 있습니다.

- 게시판 개설 신청 거절 기능은 아직 구현하지 않았습니다.
- `ADMIN` 권한을 추가하거나 관리하는 API는 아직 구현하지 않았습니다.
- 게시판 목록 / 상세 조회, 게시글, 댓글 기능은 아직 구현하지 않았습니다.
- DB 스키마 변경은 별도의 Migration 도구 없이 Hibernate `ddl-auto=update`를 사용하고 있습니다.
