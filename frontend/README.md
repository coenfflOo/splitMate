# SplitMate Frontend (Kotlin + Compose Multiplatform)

**SplitMate**는

- 영수증 정보를 입력하고
- N분의 1 / 메뉴별 계산을 수행하며
- 실시간 그룹 모드(WebSocket 기반)로 함께 계산을 진행할 수 있는

**Kotlin Full-Stack 프로젝트**입니다.

이 프론트엔드는 **Compose Multiplatform(Compose for Web)** 기반의

SPA 웹 애플리케이션입니다.

---

## 💡 프로젝트 목표 (Frontend 관점)

1. **사용자 친화적인 UI/UX**
    - 콘솔/REST 기반 계산 흐름을 웹 UI에서 직관적으로 경험
    - 잘못된 입력, 에러 메시지를 친절하게 안내
2. **GROUP 모드 실시간 협업**
    - WebSocket + STOMP로 여러 사용자가 동일 방에 참여
    - 계산 단계, 메시지 흐름을 동일하게 공유
3. **도메인 흐름 + 프론트의 명확한 분리**
    - Frontend는 오직 API 결과와 WebSocket 이벤트만 사용
    - 비즈니스 로직(금액 계산 등)은 모두 백엔드 책임
4. **Kotlin Full-Stack 통일성**
    - Kotlin/JS(Compose Web) + Kotlin/Spring Boot를 사용하여

      동일 언어 기반의 개발 경험을 제공


---

## 🖥️ 기술 스택

- 언어: **Kotlin/JS (IR Compiler)**
- 빌드: **Gradle (Kotlin DSL)**
- UI Framework: **Compose Multiplatform (Compose for Web)**
- 상태관리: Compose State + ViewModel 패턴
- 라우팅: Simple SPA Router (커스텀 구현 예정)

---

## 📁 패키지 구조 (Frontend)

```
frontend/
 ├── build.gradle.kts          # 프론트엔드 루트(멀티 모듈 루트)
 └── composeApp/
     ├── build.gradle.kts      # JS/Compose Web 모듈
     └── src/
         └── jsMain/
             ├── kotlin/
             │   └── com/
             │       └── splitmate/
             │           ├── App.kt          # 루트 컴포저블
             │           ├── main.kt        # 엔트리 포인트
             │           ├── components/    # 공통 UI 컴포넌트
             │           ├── screens/       # 화면(Home, SoloSplit, MenuSplit...)
             │           ├── api/           # REST API 클라이언트
             │           ├── websocket/     # WebSocket/STOMP 클라이언트
             │           ├── state/         # ViewModel / 상태
             │           └── util/          # 포맷/매핑 헬퍼
             └── resources/
                 └── index.html                 # SPA 엔트리 HTML
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

- [ ]  금액 입력
- [ ]  잘못된 값 입력 시 안내

### 2-2. 세금 입력 화면

- [ ]  숫자 또는 “없음”
- [ ]  UI에서 선택형 옵션도 제공

### 2-3. 팁 입력 모드 선택

- [ ]  % / $ / 없음

### 2-4. 팁 값 입력

- [ ]  %일 때 0~100
- [ ]  $일 때 0보다 큰 실수

### 2-5. 분배 방식 선택

- [ ]  현재는 “N분의 1”만 허용

### 2-6. 인원 수 입력

- [ ]  자연수만 허용

### 2-7. 환율 모드 선택

- [ ]  자동 조회(오토)
- [ ]  수동 입력
- [ ]  환율 생략(CAD만 보기)

### 2-8. 환율 값 입력(수동)

- [ ]  0보다 큰 숫자만

### 2-9. 결과 화면

- [ ]  총합 / 1인당 CAD
- [ ]  KRW 변환 값(옵션)

---

## 3. 메뉴별 계산 (REST 기반)

### 3-1. 메뉴 등록 화면

- [ ]  항목 추가/삭제
- [ ]  메뉴 가격 검증

### 3-2. 참가자 등록 화면

- [ ]  사람 추가/삭제

### 3-3. 메뉴별 참여 인원 선택

- [ ]  체크박스로 참가자 선택

### 3-4. 결과 화면

- [ ]  각자 부담 subtotal + 세금/팁 비례 분배 + 총합
- [ ]  KRW 변환 옵션 제공

---

## 4. GROUP 모드 (WebSocket 기반)

### 4-1. 방 생성 화면

- [ ]  방 ID 입력 or 자동생성
- [ ]  생성 시 초기 메시지 출력

### 4-2. 방 입장 화면

- [ ]  멤버 ID 입력
- [ ]  입장 성공 → 메시지 흐름 공유

### 4-3. 실시간 계산 입력 화면

- [ ]  콘솔 엔진과 동일한 메시지 흐름에 따라 입력
- [ ]  WebSocket으로 서버와 주고받기

### 4-4. 실시간 브로드캐스트

- [ ]  서버 메시지 수신 시 UI 갱신
- [ ]  같은 방 모든 멤버가 동일한 상태 표시

### 4-5. 에러 메시지 처리

- [ ]  /topic/group/{roomId}.errors 구독
- [ ]  INVALID_INPUT, ROOM_NOT_FOUND 등의 에러 표시

---

## 5. 상태 관리

- [ ]  각 화면 단위 `ViewModel` 제공
- [ ]  Compose State로 UI 자동 업데이트
- [ ]  RoomState, ConversationOutput 모델을 백엔드 DTO와 동일하게 유지

---

## 6. API/WSS 연동

### 6-1. REST

- [ ]  `/api/split/even`
- [ ]  `/api/split/by-menu`
- [ ]  `/api/group/*`

### 6-2. WebSocket

- [ ]  connect to `/ws`
- [ ]  subscribe `/topic/group/{roomId}`
- [ ]  subscribe `/topic/group/{roomId}.errors`
- [ ]  send `/app/group/{roomId}/messages`