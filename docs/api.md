# API 설계

모든 API는 `/api/v1` 기준입니다.

응답은 공통 래퍼 `ApiResponse`를 사용합니다.

- 성공: `success=true`, `code`는 `OK`, `data`에 실제 응답 포함
- 실패: `success=false`, `code`에 오류 코드, `message`에 기본 또는 상세 메시지, `traceId`에 추적 ID
- `ApiResponse`와 회원 관련 응답 DTO에는 `@JsonInclude(JsonInclude.Include.NON_NULL)`이 적용되어, 값이 `null`인 필드는 JSON에 포함되지 않습니다.

회원가입(201), 회원 탈퇴(204)는 예외적으로 본문이 없습니다.

## 회원가입

가입은 **입장 토큰 발급 → 가입 요청** 두 단계로 진행합니다. 서버는 동시에 처리할 수 있는 가입 건수를 슬롯으로 제한합니다. `Idempotency-Key`에는 요청마다 서로 다른 값을 넣어, 같은 가입 시도가 동시에 여러 번 처리되지 않도록 합니다.

### 1) 입장 토큰 발급

- **Endpoint** — `POST /members/admission`
- **인증** — 불필요
- **Request** — Body 없음
- **Response (200 OK)** — `AdmissionTokenResponse`

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "admissionToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresInSeconds": 60
  }
}
```

- 슬롯이 모두 사용 중이면 `429` 및 `TOO_MANY_REQUESTS`가 반환될 수 있습니다. 이 경우 프론트는 대기 상태를 유지하거나 일정 시간 후 `POST /members/admission`을 다시 호출합니다.

### 2) 회원 등록

- **Endpoint** — `POST /members`
- **인증** — 불필요
- **Request**
  - **Header**
    - `Admission-Token` — 1)에서 받은 `admissionToken` (필수)
    - `Idempotency-Key` — 요청마다 고유한 값(예: UUID). 동일 키로 처리 중인 요청이 이미 있으면 `409` 처리 (필수)
  - **Body** (JSON)

```json
{
  "email": "user@example.com",
  "password": "Abcd1234!",
  "name": "홍길동",
  "phone": "01012345678"
}
```

- **Response (201 Created)**
  - Body 없음
  - `Location`: `/api/v1/members/{id}`

가입 처리가 끝나면 입장 토큰은 해제되며, 같은 `Admission-Token`으로 재요청할 수 없습니다. 재시도 시에는 새로 1)을 호출합니다.

## 로그인

- **Endpoint** — `POST /auth/token`
- **Request**
  - **Body** (JSON)

```json
{
  "email": "user@example.com",
  "password": "Abcd1234!"
}
```

- **Response (200 OK)** — TokenResponse

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

## 토큰 재발급

- **Endpoint** — `PUT /auth/token`
- **Request**
  - **Body** (JSON)

```json
{
  "refreshToken": "..."
}
```

- **Response (200 OK)** — TokenResponse

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

## 로그아웃

- **Endpoint** — `DELETE /auth/token`
- **Request**
  - **Header** — `Authorization: Bearer <access_token>`
  - 로그아웃 요청에 한해서는 만료된 access token도 예외적으로 허용합니다.
- **Response (200 OK)**

```json
{
  "success": true,
  "code": "OK"
}
```

## 내 정보 조회

- **Endpoint** — `GET /members/me`
- **Request**
  - **Header** — `Authorization: Bearer <access_token>`
- **Response (200 OK)** — MemberResponse

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "01012345678",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-03-17T06:43:45Z",
    "updatedAt": "2026-03-17T06:43:45Z"
  }
}
```

## 내 정보 수정

- **Endpoint** — `PATCH /members/me`
- **Request**
  - **Header** — `Authorization: Bearer <access_token>`
  - **Body** (JSON)

```json
{
  "phone": "010-9876-5432",
  "name": "김길동"
}
```

- **Response (200 OK)** — MemberResponse

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "김길동",
    "phone": "01098765432",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-03-17T06:43:45Z",
    "updatedAt": "2026-03-18T09:00:00Z"
  }
}
```

## 회원 탈퇴

- **Endpoint** — `DELETE /members/me`
- **Request**
  - **Header** — `Authorization: Bearer <access_token>`
- **Response (204 No Content)**

## 회원 목록 조회 (관리자)

- **Endpoint** — `GET /admin/members`
- **Request**
  - **Header** — `Authorization: Bearer <access_token> (ROLE_ADMIN)`
  - **Query** — Pageable (default size=20)
- **Response (200 OK)**

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "content": [
      {
        "id": 1,
        "email": "u***@example.com",
        "name": "홍*",
        "phone": "010****5678",
        "role": "USER",
        "status": "ACTIVE",
        "createdAt": "2026-03-17T06:43:45Z",
        "updatedAt": "2026-03-17T06:43:45Z"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "size": 20,
    "number": 0
  }
}
```

## 회원 상세 조회 (관리자)

- **Endpoint** — `GET /admin/members/{memberId}`
- **Request**
  - **Header** — `Authorization: Bearer <access_token> (ROLE_ADMIN)`
- **Response (200 OK)** — MemberResponse (마스킹)

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "id": 1,
    "email": "u***@example.com",
    "name": "홍*",
    "phone": "010****5678",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-03-17T06:43:45Z",
    "updatedAt": "2026-03-17T06:43:45Z"
  }
}
```

## 회원 PII 조회 (관리자)

- **Endpoint** — `GET /admin/members/{memberId}/pii`
- **Request**
  - **Header** — `Authorization: Bearer <access_token> (ROLE_ADMIN)`
- **Response (200 OK)** — MemberPiiResponse (원문)

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "01012345678",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-03-17T06:43:45Z",
    "updatedAt": "2026-03-17T06:43:45Z"
  }
}
```

## 오류 코드

실패 응답도 `ApiResponse` 형태로 반환합니다 (`success: false`, `code`, `message`, `traceId` 등).  
`data`는 `INVALID_INPUT`일 때만 필드별 메시지를 담습니다.

회원가입 관련 오류의 경우, `MEMBER_REQUEST_IN_PROGRESS`는 동일한 `Idempotency-Key`를 가진 요청이 아직 처리 중일 때 반환됩니다. 성공 응답을 재사용하는 replay 정책은 이번 범위에 포함하지 않습니다.

### 예시 응답(JSON) — 인증·비즈니스 오류

```json
{
  "success": false,
  "code": "AUTH_UNAUTHORIZED",
  "message": "인증이 필요합니다",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 예시 응답(JSON) — 입력 검증 실패 (`INVALID_INPUT`)

```json
{
  "success": false,
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다",
  "data": {
    "phone": "휴대폰 번호 형식이 올바르지 않습니다",
    "password": "비밀번호는 영문 대소문자, 숫자, 특수문자(@$!%*?&)를 각각 하나 이상 포함해야 합니다"
  },
  "traceId": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

### 공통

| 코드 | HTTP | 설명 (`ErrorCode` 기본 메시지) |
|------|------|------|
| INVALID_INPUT | 400 | 입력값이 올바르지 않습니다 |
| INTERNAL_SERVER_ERROR | 500 | 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요 |

### 인증 / 인가

| 코드 | HTTP | 설명 (`ErrorCode` 기본 메시지) |
|------|------|------|
| AUTH_INVALID_CREDENTIALS | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| AUTH_ACCOUNT_LOCKED | 401 | 로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요 |
| AUTH_UNAUTHORIZED | 401 | 인증이 필요합니다 |
| AUTH_INVALID_TOKEN | 401 | 유효하지 않은 토큰입니다 |
| AUTH_EXPIRED_TOKEN | 401 | 만료된 토큰입니다 |
| AUTH_FORBIDDEN | 403 | 접근 권한이 없습니다 |

### 회원

| 코드 | HTTP | 설명 (`ErrorCode` 기본 메시지) |
|------|------|------|
| MEMBER_NOT_FOUND | 404 | 회원 정보를 찾을 수 없습니다 |
| MEMBER_DUPLICATE_EMAIL | 409 | 이미 사용 중인 이메일입니다 |
| MEMBER_DUPLICATE_PHONE | 409 | 이미 사용 중인 휴대폰 번호입니다 |
| MEMBER_ALREADY_WITHDRAWN | 409 | 이미 탈퇴한 회원입니다 |
| MEMBER_ADMISSION_REQUIRED | 400 | 회원가입 입장권이 필요합니다 |
| MEMBER_ADMISSION_INVALID | 400 | 유효하지 않거나 만료된 회원가입 입장권입니다 |
| MEMBER_IDEMPOTENCY_KEY_REQUIRED | 400 | Idempotency-Key 헤더가 필요합니다 |
| MEMBER_REQUEST_IN_PROGRESS | 409 | 동일한 회원가입 요청이 이미 처리 중입니다 |

### 유입 제한

| 코드 | HTTP | 설명 (`ErrorCode` 기본 메시지) |
|------|------|------|
| TOO_MANY_REQUESTS | 429 | 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요 |
