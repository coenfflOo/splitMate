# 🧮 SplitMate

**SplitMate**는 유학생·여행자·일상 사용자들이 해외에서 겪는 “복잡한 더치페이 + 환율 계산” 문제를 해결하기 위해 만든 **실시간 환율 & 더치페이 계산 도우미**입니다.

영수증의 **총액/세금/팁/메뉴** 정보를 입력하면, 상황에 맞는 분배 방식으로 금액을 자동 계산하고 **CAD → KRW 환산**까지 한 번에 제공합니다.

또한 **GROUP 모드**에서는 WebSocket 기반의 실시간 방/채팅 기능으로 여러 명이 같은 흐름을 공유하며 함께 계산할 수 있습니다.

---

## ✨ What SplitMate Does

- 영수증 입력(총액, 세금, 팁) → **정확한 더치페이 자동 계산**
- **N분의 1 / 메뉴별 분배** 두 모드 지원
- CAD → KRW **실시간 환율 변환**
    - 자동 환율(외부 API) 또는 수동 입력 fallback
- WebSocket 기반 **GROUP 모드**
    - 방 생성/입장
    - 실시간 단계 동기화 & 결과 브로드캐스트
    - 참여자 채팅

---

## 🧩 Project Modules

이 프로젝트는 **Kotlin Full-Stack** 구조로, 프론트/백엔드가 각각 독립 모듈로 존재합니다.

```
root
 ├── backend/        # Kotlin + Spring Boot (REST + WebSocket)
 └── frontend/       # Kotlin/JS + Compose Multiplatform Web (SPA)
```

---

## 🎯 Background & Problem

해외 식당/카페에서는

- 카드 분할 결제 제한
- 세금/팁 포함 구조
- 결제 통화(CAD)와 송금 통화(KRW)의 불일치

  때문에 “누가 얼마를 보내야 하는지” 계산이 번거롭고 실수도 잦습니다.


SplitMate는 실제 유학생 생활에서 출발한 문제의식으로

**복잡한 더치페이를 ‘대화/방 기반 UI’로 자동화**해, 스트레스와 시간을 줄이는 것을 목표로 합니다.

---

## ✅ Functional Requirements

1. **방 생성 및 모드 선택**
    - SOLO(개인 계산) / GROUP(실시간 협업) 선택
2. **단계별 챗봇 흐름**
    - 영수증 입력 → 분배 방식 → CAD 계산 → 환율 설정 → 결과
    - 입력 검증/재질문/재시작 흐름 제공
3. **영수증 정보 입력**
    - 총액, 세금(없음=0), 팁 모드(% / 금액 / 없음)
4. **분배 방식**
    - N분의 1: 인원 수 입력 후 균등 분배
    - 메뉴별: 메뉴/참여자/배정 입력 후 비례 분배
5. **환율 및 통화 변환**
    - 자동(오늘 환율) / 수동 입력 / KRW 생략
6. **결과 출력**
    - CAD + KRW(옵션) 기준 금액 제공
7. **GROUP 실시간 통신**
    - 모든 사용자 동일한 단계/메시지 공유
    - 채팅 & 계산 결과 브로드캐스트
8. **예외 처리**
    - 잘못된 입력 3회 이상 시 재시작 확인
    - RESET 등 시스템 입력 지원

---

## 🖥 Tech Stack

### Backend [[백엔드 README]](https://github.com/coenfflOo/splitMate/tree/main/backend)

- Kotlin
- Spring Boot (Web, WebSocket/STOMP)
- Gradle (Kotlin DSL)
- JUnit5 / MockMvc

### Frontend [[프론트엔드 README]](https://github.com/coenfflOo/splitMate/tree/main/frontend)

- Kotlin/JS (IR)
- Compose Multiplatform (Compose for Web / HTML)
- STOMP over SockJS
- Compose State + ViewModel Pattern
- Custom Simple SPA Router

---

## 🚀 Quick Start

### Requirements

- JDK 17+
- Gradle 8+
- (선택) 환율 API Key

---

### 1) Backend 실행

### (선택) 환율 API 키 설정

수출입은행 환율 API를 사용합니다.

키가 없으면 자동 환율 대신 수동 입력 fallback으로 동작합니다.

```bash
export EXCHANGE_API_KEY=발급받은_키
```

`application.yml`

```yaml
exchange:
  api-key: ${EXCHANGE_API_KEY:}
```

### 서버 실행

```bash
cd backend
./gradlew bootRun
```

### 콘솔 모드 실행(옵션)

```bash
cd backend
./gradlew run
```

---

### 2) Frontend 실행 (Compose Web Dev Server)

```bash
cd frontend/composeApp
./gradlew jsBrowserDevelopmentRun
```

Gradle 로그에 출력된 주소로 접속하면 됩니다.

(`http://localhost:8081`)

---

## 🏁 Outcome

SplitMate는 **Kotlin만으로 Web + Backend + Real-Time 통신까지 완성하는 Full-Stack 프로젝트**이며, 캐나다에서 지내며 겪은 실제 생활 문제(해외 더치페이)를 서비스로 해결하는 경험을 담았습니다.

[블로그 회고/정리 글 링크](https://hi-seo-log.tistory.com/65)