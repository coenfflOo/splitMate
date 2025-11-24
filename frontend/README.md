# 🧮 SplitMate Frontend (Kotlin + Compose Multiplatform)

**SplitMate**는

유학생·여행자·일상 사용자 누구나

- 영수증 입력 → 자동 금액 계산
- N분의 1 / 메뉴별 계산
- 실시간 그룹(WebSocket) 협업 계산

을 간편하고 직관적으로 사용할 수 있는 **Kotlin Full-Stack 프로젝트**입니다.

이 저장소는 **Compose Multiplatform Web 기반 프론트엔드(SPA)** 입니다.

---

## 💡 프로젝트 목표 (Frontend 관점)

### 1. 사용자 친화적인 UI/UX

- 콘솔 기반 흐름을 웹 UI에서 자연스럽게 경험
- 잘못된 입력, 경고, 에러를 **토스트(Toast)** 형태로 깔끔하게 제공

### 2. GROUP 모드 실시간 협업

- WebSocket + STOMP 기반 메시지 브로드캐스트
- 여러 사용자 동일한 계산 흐름 공유

### 3. Front/Back 책임 명확 분리

- Frontend는 입력/상태/UI만 담당
- 금액 계산, 검증, 흐름 관리는 모두 **Backend** 책임

### 4. Kotlin Full-Stack 일관성

- Kotlin/JS + Kotlin/JVM 조합으로 전체 흐름 통일

---

## 🖥️ 기술 스택

- 언어: **Kotlin/JS (IR, Compose Web)**
- UI: **Compose Multiplatform Web (JetBrains Compose HTML)**
- WebSocket: **STOMP over SockJS**
- 빌드: **Gradle Kotlin DSL**
- 상태관리: **Compose State + ViewModel Pattern**
- 라우팅: **Custom Simple Router**

---

## 📁 패키지 구조 (Frontend)

```cpp
frontend
 ├── build.gradle.kts 
 └── composeApp
     ├── build.gradle.kts 
     └── src
         └── jsMain
             ├── kotlin
             │   └── com
             │       └── splitmate
             │           └── frontend
             │               ├── App.kt         
             │               ├── main.kt
             │               ├── style
             │               ├── ui
             │               ├── screens
             │               ├── api
             │               │   ├── client
             │               │   └── dto
             │               ├── websocket
             │               │   └── dto
             │               └── state
             │                   ├── model
             │                   │   ├── menu
             │                   │   └── solo
             │                   ├── steps
             │                   ├── uistate
             │                   └── viewmodel
             └── resources
                 └── index.html          
```

---

# 🗒️ Feature List

## 1. 공통 UI

### 1-1. 메인 화면(Home)

- [x]  SOLO / MENU / GROUP 선택 버튼
- [x]  프로젝트 소개/간단 설명

### 1-2. 공통 메시지 / 에러 UI

- [x]  API 에러 메시지 포맷 통일
- [x]  입력값 검증 컴포넌트
- [x]  로딩 상태 표시

---

## 2. SOLO 모드 화면 (REST 기반 흐름)

### 2-1. 총 금액 입력 화면

- [x]  금액 입력
- [x]  잘못된 값 입력 시 안내

### 2-2. 세금 입력 화면

- [x]  숫자 또는 “없음”
- [x]  UI에서 선택형 옵션도 제공

### 2-3. 팁 입력 모드 선택

- [x]  % / $ / 없음

### 2-4. 팁 값 입력

- [x]  %일 때 0~100
- [x]  $일 때 0보다 큰 실수

### 2-5. 분배 방식 선택

- [x]  현재는 “N분의 1”만 허용

### 2-6. 인원 수 입력

- [x]  자연수만 허용

### 2-7. 환율 모드 선택

- [x]  자동 조회(오토)
- [x]  수동 입력
- [x]  환율 생략(CAD만 보기)

### 2-8. 환율 값 입력(수동)

- [x]  0보다 큰 숫자만

### 2-9. 결과 화면

- [x]  총합 / 1인당 CAD
- [x]  KRW 변환 값(옵션)

---

## 3. 메뉴별 계산 (REST 기반)

### 3-1. 메뉴 등록 화면

- [x]  항목 추가/삭제
- [x]  메뉴 가격 검증

### 3-2. 참가자 등록 화면

- [x]  사람 추가/삭제

### 3-3. 메뉴별 참여 인원 선택

- [x]  체크박스로 참가자 선택

### 3-4. 결과 화면

- [x]  각자 부담 subtotal + 세금/팁 비례 분배 + 총합
- [x]  KRW 변환 옵션 제공

---

## 4. GROUP 모드 (WebSocket 기반)

### 4-1. 방 생성 화면

- [x]  방 ID 입력 or 자동생성
- [x]  생성 시 초기 메시지 출력

### 4-2. 방 입장 화면

- [x]  멤버 ID 입력
- [x]  입장 성공 → 메시지 흐름 공유

### 4-3. 실시간 계산 입력 화면

- [x]  콘솔 엔진과 동일한 메시지 흐름에 따라 입력
- [x]  WebSocket으로 서버와 주고받기

### 4-4. 실시간 브로드캐스트

- [x]  서버 메시지 수신 시 UI 갱신
- [x]  같은 방 모든 멤버가 동일한 상태 표시

### 4-5. 에러 메시지 처리

- [x]  /topic/group/{roomId}.errors 구독
- [x]  INVALID_INPUT, ROOM_NOT_FOUND 등의 에러 표시

---

## 5. 상태 관리

- [x]  각 화면 단위 `ViewModel` 제공
- [x]  Compose State로 UI 자동 업데이트
- [x]  RoomState, ConversationOutput 모델을 백엔드 DTO와 동일하게 유지

---

## 6. API/WSS 연동

### 6-1. REST

- [x]  `/api/split/even`
- [x]  `/api/split/by-menu`
- [x]  `/api/group/*`

### 6-2. WebSocket

- [x]  connect to `/ws`
- [x]  subscribe `/topic/group/{roomId}`
- [x]  subscribe `/topic/group/{roomId}.errors`
- [x]  send `/app/group/{roomId}/messages`