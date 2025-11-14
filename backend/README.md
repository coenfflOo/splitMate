# SplitMate Backend (Kotlin + Spring)

유학생을 위한 **실시간 환율 & 더치페이 계산 도우미 – SplitMate** 의 백엔드 모듈입니다.

현재 1차 목표는 **콘솔 기반 SOLO 모드 + 외부 환율 API 연동** 까지 구현하는 것입니다.

---

## 💡 프로젝트 목표 (Backend 관점)

- **복잡한 더치페이 계산을 자동화**하는 도메인 로직 구현
    - 총액 + 세금 + 팁(% / 금액) + 인원수(N분의 1) 계산
    - CAD → KRW 환율 반영
- **외부 환율 API 연동**
    - CAD → KRW 환율을 조회하는 `ExchangeRateProvider` 구현
    - 실패 시 수동 입력으로 fallback
- **도메인 주도 설계 & TDD**
    - Money, Receipt, SplitCalculator 등을 중심으로 도메인 모델링
    - 콘솔 기반 대화형 인터페이스를 통해 도메인을 검증
- 이후 확장을 고려한 구조
    - HTTP API / WebSocket / GROUP 모드로 확장 가능하도록 설계

---

## 🖥️ 기술 스택

- 언어: **Kotlin**
- 빌드: **Gradle (Kotlin DSL)**
- (추가 예정) Spring Boot
    - Spring Web (REST)
    - Spring WebSocket (GROUP 모드 구현 시)
- 테스트: JUnit5 / Kotest (예정)

---

## 📂 패키지 구조 (Backend)

> 레이어드 아키텍처 + 헥사고날(포트/어댑터) 마인드
>

```csharp
com.splitmate
 ├─ application        # 유스케이스 / 흐름 조립 (도메인 + adapter 연결)
 │   ├─ conversation
 │   │    └─ ConversationEngine.kt
 │   ├─ session
 │   │    └─ ConversationContext.kt
 │   └─ group
 │        ├─ RoomService.kt
 │        ├─ RoomRepository.kt
 │        └─ GroupConversationService.kt
 │
 ├─ domain            # 순수 도메인 (비즈니스 규칙, 값/엔티티/서비스)
 │   ├─ money
 │   │    ├─ Money.kt
 │   │    └─ Currency.kt
 │   │
 │   ├─ receipt
 │   │    ├─ Receipt.kt
 │   │    ├─ Tax.kt
 │   │    └─ Tip.kt
 │   │
 │   ├─ menu              
 │   │    ├─ MenuItem.kt          
 │   │    ├─ Participant.kt       
 │   │    └─ MenuAssignment.kt    
 │   │
 │   ├─ split             
 │   │    ├─ SplitMode.kt         
 │   │    ├─ SplitResult.kt       
 │   │    ├─ PerPersonShare.kt    
 │   │    ├─ MenuSplitResult.kt   
 │   │    └─ SplitCalculator.kt   
 │   │
 │   ├─ fx                # foreign exchange (환율)
 │   │    ├─ ExchangeRate.kt
 │   │    ├─ ExchangeRateProvider.kt
 │   │    └─ ExchangeService.kt
 │   │
 │   └─ conversation      # 대화 단계 정의 (입출력은 없음)
 │        ├─ ConversationStep.kt
 │        └─ ConversationOutput.kt
 │
 ├─ adapter           # 헥사고날의 adapter 영역 (입출력, 외부 시스템)
 │   ├─ console
 │   │    ├─ ConsoleApp.kt
 │   │    └─ ConsoleIO.kt
 │   │
 │   ├─ fx
 │   │    └─ HttpExchangeRateProvider.kt
 │   └─ http        
 │       ├─ dto
 │       │    ├─ SplitEvenRequest.kt
 │       │    ├─ SplitEvenResponse.kt
 │       │    ├─ MenuSplitRequest.kt
 │       │    └─ MenuSplitResponse.kt
 │       ├─ SplitHttpHandler.kt   
 │       └─ GroupController.kt
 └─ config            
      └─ AppConfig.kt   
```

- **domain**: 순수 계산/비즈니스 규칙만 포함 (I/O 없음)
- **application**: 대화 흐름(단계 전환), 도메인 서비스 조합
- **adapter**: 콘솔 I/O, 외부 환율 API 연동
- **config**: 의존성 조립, 앱 시작점

---

# 🗒️ Feature List 1차

### 1. 세션/대화 흐름 (콘솔 기준)

### 1-1. 대화 시작 & 안내

**구현**

- [ ]  앱 시작 시 환영 메시지와 간단한 사용 설명을 출력한다.
- [x]  첫 질문은 항상 “총 결제 금액을 입력해주세요”로 시작한다.

**테스트**

- [x]  `ConversationEngine.start()` 호출 시, 첫 출력이 환영/안내 + “총 결제 금액” 질문인지 검증한다.


---

### 1-2. 잘못된 입력 처리 (공통)

**구현**

- [x]  숫자 입력이 필요한 단계에서 숫자가 아닌 문자열이 들어오면 에러 메시지를 출력하고 **같은 단계로 재질문**한다.
- [x]  0 이하, 음수 등 유효 범위를 벗어난 숫자는 에러로 처리하고 재질문한다.

**테스트**

- [x]  `총 금액` 단계에서 `"abc"` 입력 시, 다시 같은 질문이 나온다.
- [x]  `총 금액` 단계에서 `10`, `0` 입력 시, “0보다 큰 값으로 입력해 주세요” 등의 메시지 후 재질문.

- [ ]  팁 %, 인원 수, 환율 등 숫자 입력이 필요한 모든 단계에 대해 위 패턴이 동작하는지 각각 테스트한다.


---

### 2. 영수증 정보 입력

### 2-1. 총 결제 금액 입력

**구현**

- [x] 사용자가 총 결제 금액을 입력하면 `ConversationContext.baseAmount` 에 `Money` 값으로 저장한다.
- [x] 금액은 `Money` + `BigDecimal` 기반으로 관리한다.

**테스트**

- [x] `27.40` 입력 → 상태에 `27.40`이 저장되고 다음 질문(세금)으로 넘어가는지 확인.
- [x] 소수점 둘째 자리까지 허용, 셋째 자리 이상 입력 시 반올림 또는 에러 정책이 일관적인지 확인.


---

### 2-2. 세금 입력

**구현**

- [x]  세금 금액을 별도 입력받을 수 있다. (예: `2.40`)
- [x]  “없음” 입력 시 세금 = 0으로 처리한다.   ← 아직 문자열 "없음"은 처리 안 함

**테스트**

- [x]  세금 `2.40` 입력 시 `taxAmount = 2.40`으로 저장되는지 테스트.
- [x]  “없음” 입력 시 `taxAmount = 0`이 되고, 다음 단계로 정상 진행되는지 테스트.
- [x]  잘못된 입력(문자, 음수) 시 재질문이 되는지 테스트.

---

### 2-3. 팁 입력 (모드 + 값)

**구현 (도메인)**

- [x]  팁 입력 모드를 `%` 또는 `$` 중에서 표현할 수 있다. (`TipMode.PERCENT`, `TipMode.ABSOLUTE`)
- [x]  `% 모드`: 팁 퍼센트(예: 15)를 입력하면 나중에 기준 금액의 퍼센트를 계산한다.
- [x]  `$ 모드`: 팁 금액(예: 10.00)을 그대로 사용한다.  (도메인 기준)

**구현 (현재 콘솔 대화)**

- [x]  콘솔 대화에서 실제로 `%`와 `$` 두 모드 모두에 대해 입력/계산 흐름을 제공한다.
- [x]  “없음” 입력 시 (3번 선택) 팁 없이 진행한다.

**테스트**

- [ ]  `% 모드` 선택 후 `15` 입력 → `tipMode = PERCENT`, `tipValue = 15` 저장.
- [ ]  `$ 모드` 선택 후 `10` 입력 → `tipMode = ABSOLUTE`, `tipAmount = 10` 저장.
- [x]  팁 없음 → 0으로 처리되고 흐름이 끊기지 않는지 테스트.
- [x]  팁 % 100 초과, 음수, 문자열 등 잘못된 입력 시 재질문.

---

### 3. 분배 방식 (N분의 1)

### 3-1. 분배 방식 선택

**구현**

**구현**

- [x]  “1) N분의 1 2) 메뉴별 계산” 중 하나를 고르게 한다. (현재는 1만 지원)
- [x]  콘솔 1단계에서는 **N분의 1만** 구현, 2번은 “아직 지원하지 않습니다” 안내.

**테스트**

- [x]  `1` 입력 시 `splitMode = N_DIVIDE`로 설정되고, 인원 수 입력 단계로 넘어가는지 확인.
- [x]  `2` 입력 시 “미구현” 안내 후 다시 1을 고르게 하는지 (혹은 프로그램 종료) 정책에 따라 테스트.


---

### 3-2. 인원 수 입력 & 유효성 검사

**구현**

- [x]  사용자에게 실제 인원 수 `N`을 입력받는다.
- [x]  1 이상 정수만 허용한다.
- [x]  0, 음수, 소수, 문자 입력 시 에러 + 재질문.

**테스트**

- [x]  `3` 입력 시 `peopleCount = 3`으로 저장되고 계산 단계로 넘어가는지 테스트.
- [x]  `0`, `-1`, `1.5`, `abc` 입력 시 각각 에러 메시지 후 재질문되는지 테스트.  ← 일부만 작성했다면 나중에 보완

---

### 3-3. N분의 1 계산 로직

**구현**

- [x]  `총액 + 세금 + 팁` 을 합산한 **최종 CAD 금액**을 구한다.
- [x]  그 금액을 `N`으로 나눈다.
- [x]  소수점 처리 정책(반올림)을 일관되게 적용한다.

**테스트 (단위 테스트 위주)**

- [ ]  총액 27.40, 세금 2.60, 팁 10%(총액+세금 기준) / 인원 2명 → 기대 CAD 1인당 금액이 맞는지 계산식 검증.
- [x]  팁이 0인 경우, 세금만 포함한 금액이 정상적으로 나뉘는지.
- [x]  세금/팁 모두 0일 때, 단순히 `총액 / N`이 되는지.
- [ ]  소수점이 긴 결과에서 반올림이 의도한 대로 되는지.

---

### 4. 환율 및 통화 변환

### 4-1. 환율 입력 및 조회

**구현**

- [x]  사용자는 “오늘 환율 불러오기” 옵션을 선택할 수 있다.
- [x]  시스템은 **외부 환율 API 클라이언트**를 통해 CAD→KRW 환율을 조회한다.
- [x]  조회 실패 시:
    - 에러 메시지를 보여주고,
    - 수동 입력으로 fallback 하도록 한다.
- [x]  도메인 레벨에서는 `ExchangeRateProvider` 인터페이스만 알고,

  실제 HTTP 호출은 구현체에서 수행한다.

   ```kotlin
   interface ExchangeRateProvider {
       fun getRate(base: String, target: String): Double
   }
   ```


**테스트**

- [x]  `FakeExchangeRateProvider` 나 Mock을 사용해, `getRate("CAD", "KRW")` 호출 시 1000.0을 반환하도록 설정하고, 전체 계산 흐름이 이 값을 사용해 제대로 결과를 산출하는지 테스트.

- [ ] provider가 예외를 던지는 상황(실패 케이스)은 아직 테스트 없음 → [ ] 유지 
- [ ] 실제 API 엔드포인트 향한 통합 테스트는 아직 없음 


---

### 4-2. CAD → KRW 변환

**구현**

- [x]  `1인당 CAD 금액 * 환율`로 KRW 금액을 계산한다.
- [x]  소수점 처리 정책을 반올림 기준으로 통일한다.

**테스트**

- [x]  1인당 금액 $10.50, 환율 1000 → 10,500 KRW인지 테스트.
- [ ]  소수점이 많은 CAD 값 × 환율에서 KRW 반올림이 제대로 되는지.

---

### 5. 최종 결과 출력

### 5-1. SOLO 모드 결과

**구현**

- [x] 최종 결과를 사람이 읽기 좋은 형태로 반환한다. (현재는 CAD 기준)
- [x] KRW 변환까지 포함해 1인당 금액(CAD, KRW)을 모두 보여준다.
- [x] 콘솔/웹과 같은 출력 형식은 도메인이 아닌 어댑터에서 담당한다.

**테스트**

- [x] 행복 경로(ABSOLUTE 팁 + 자동 환율) 시나리오 테스트로 출력 형식까지 검증
- [ ]  잘못된 입력 여러 번 후에도 결국 올바른 결과까지 도달하는 시나리오 테스트.


---

### 6. 에러/재시작 흐름

### 6-1. 여러 번 잘못 입력한 경우

**구현**

- [x] 같은 단계에서 3번 틀리면 RESTART_CONFIRM 단계로 이동 
- [x] Y 입력 시 start() 로 완전 초기화 
- [x] N 입력 시 lastStep 으로 되돌아가서 다시 입력 받기

**테스트**

- [x] 3번 연속 잘못 입력 시 RESTART_CONFIRM으로 가는지 테스트 
- [x]“예(Y)” 선택 시 처음 상태로 돌아가는지 테스트 
- [x] “아니오(N)” 선택 시 직전 단계(예: ASK_TOTAL_AMOUNT)로 돌아가는지 테스트


---
# 🗒️ Feature List 2차
### 1. 메뉴 & 참가자 모델링

**구현**

- [x]  `MenuItem` 값 객체를 정의한다. (id, name, price: Money)
- [x]  `Participant` 값/엔티티를 정의한다. (id, displayName)

**테스트**

- [ ]  `MenuItem` 생성 시 음수/0 가격을 허용하지 않는지 검증한다.
- [x]  `Participant`가 equals/hashCode 기준으로 잘 비교되는지 검증한다. (id 기반)

---

### 2. 메뉴별 선택 & 공유 표현 (`MenuAssignment`)

**구현**

- [x]  `MenuAssignment(menuItem, participants)`로 “이 메뉴를 누가 함께 먹었는지” 표현한다.
- [x]  `participants` 리스트가 비어 있으면 예외로 처리한다.

**테스트**

- [ ]  `participants`가 1명일 때 → 전액이 그 사람 subtotal에 더해지는지 테스트.
- [ ]  `participants`가 N명일 때 → 메뉴 가격이 `N`으로 나뉘어 각 사람 subtotal에 더해지는지 테스트.
- [ ]  `participants`가 빈 리스트일 때 예외가 나는지 테스트.

---

### 3. 메뉴별 세금/팁 비례 분배 (`SplitCalculator.splitByMenu`)

**구현**

- [x]  `assignments`로부터 사람별 메뉴 소계(subtotal)를 계산한다.
- [x]  `Receipt`의 `baseAmount + tax + tip`으로 전체 금액을 계산한다.
- [x]  각 사람의 `subtotal / baseAmount` 비율에 따라 세금/팁을 비례 분배한다.
- [x]  반올림 정책은 `Money`의 SCALE(2), HALF_UP을 따른다.
- [x]  세금/팁 분배로 인해 생기는 1~2 cent 오차를 한 사람에게 몰아서 보정하는 정책을 정한다.

**테스트**

- [x]  “각자 메뉴만 있는 경우” 사람별 총액이 기대값과 일치하는지 테스트.
- [x]  “모두가 공유한 메뉴 1개”인 경우, 완전히 균등하게 나누어지는지 테스트.
- [x]  “혼자 먹은 메뉴 + 공유 메뉴 혼합” 케이스에서 비율과 금액이 맞는지 테스트.
- [ ]  subtotal 합과 baseAmount가 크게 다를 때(입력 실수)는 예외 or 로그로 처리하는 정책을 테스트. (선택)

---

### 4. `SplitResult` & `PerPersonShare`

**구현**

- [x]  `PerPersonShare`에 subtotal, taxShare, tipShare, total 필드를 가진다.
- [x]  메뉴별 계산 결과로 MenuSplitResult(total, shares)를 정의한다.
- [ ]  N분의 1 결과도 List<PerPersonShare> 구조로 통합할지 여부는 추후 리팩터링 단계에서 고려.

**테스트**

- [ ]  `SplitResult.total`가 사람별 total 합과 일치하는지 테스트.
- [ ]  `PerPersonShare.total == subtotal + taxShare + tipShare`를 보장하는지 테스트.

---

### 5. KRW 변환과의 연결 (기존 도메인과 합치기)

**구현**

- [ ]  메뉴별 분배 결과(PerPersonShare)를 입력으로 받아 CAD → KRW로 변환하는 helper를 만든다. (`ExchangeService` 재사용)
- [ ]  환율 없이 CAD만 보고 싶을 때는 KRW 계산을 생략한다.

**테스트**

- [ ]  rate = 1000일 때, perPerson.total(CAD)가 올바르게 KRW로 변환되는지 테스트.
- [ ]  메뉴별 분배 + 자동 환율 모드가 함께 동작하는 엔드투엔드 시나리오 테스트.

---

### 6. HTTP API (Spring 기반 REST)
**구현**

- [x]  `adapter.http` 패키지 생성
- [x]  `SplitController` 구현
    - `POST /api/split/even` (N분의 1)
    - `POST /api/split/by-menu` (메뉴별)
- [x]  요청/응답 DTO 정의 (`adapter.http.dto`)
    - `SplitEvenRequest`, `SplitEvenResponse`
    - `MenuSplitRequest`, `MenuSplitResponse`
- [x]  DTO → 도메인 (`Receipt`, `MenuItem`, `Participant`, `MenuAssignment`) 변환 로직
- [x]  도메인 결과(`SplitResult`, `MenuSplitResult`) → 응답 DTO 변환

**테스트**

- [x]  컨트롤러 단위 테스트
    - 유효한 요청 → 기대한 JSON 응답이 나오는지
- [ ]  잘못된 입력(음수 금액, 잘못된 통화, 빈 참가자 등)에 대한 에러 응답 형식 테스트
- [x]  메뉴별 요청에서 세금/팁/환율이 반영된 금액이 정확한지 통합 테스트 1~2개

---
### 7. Room / Member / 상태 모델링

**구현**

- [ ]  `RoomId`, `MemberId` 값 타입 정의
    - `@JvmInline value class RoomId(val value: String)`
    - `@JvmInline value class MemberId(val value: String)`
- [ ]  방 상태를 표현하는 `RoomStatus` enum 정의 (`OPEN`, `CLOSED` 등)
- [ ]  한 방 전체를 표현하는 `RoomState` 정의
    - 필드 예시:
        - `id: RoomId`
        - `status: RoomStatus`
        - `members: Set<MemberId>`
        - `splitMode: SplitMode?`
        - `conversationContext: ConversationContext?`
        - `createdAt`, `updatedAt`

**테스트**

- [ ]  `RoomState` 기본 생성 시 `status = OPEN`, `members` 비어 있는지 확인
- [ ]  `CLOSED` 상태에서 추가 입력을 막을 수 있도록 하는 정책을 테스트 (ex. 나중에 Service 레벨에서 사용)

---

### 8. 인메모리 Room 저장소 (`RoomRepository`)

**구현**

- [ ]  `RoomRepository` 인터페이스 정의
    - `create(room: RoomState): RoomState`
    - `findById(id: RoomId): RoomState?`
    - `save(room: RoomState): RoomState`
    - `delete(id: RoomId)`
- [ ]  `InMemoryRoomRepository` 구현
    - 내부에 `ConcurrentHashMap<String, RoomState>` 사용
    - `save()` 시 `updatedAt` 갱신

**테스트**

- [ ]  `create()` 호출 후 `findById()` 로 같은 방을 조회할 수 있는지 테스트
- [ ]  `save()` 호출 시 필드 변경 내용이 반영되는지 테스트
- [ ]  `delete()` 후 `findById()` 가 `null` 을 반환하는지 테스트

---

### 9. RoomService – 방 생성/참가/종료 유스케이스

**구현**

- [ ]  `RoomService` 생성 (`RoomRepository` 의존)
- [ ]  `createRoom()`
    - 랜덤 `RoomId` 생성 (UUID 등)
    - 빈 멤버 / `OPEN` 상태의 `RoomState` 생성 후 저장
- [ ]  `joinRoom(roomId, memberId)`
    - 방이 존재하는지 확인
    - `CLOSED` 방이면 예외
    - `members` set 에 `memberId` 추가 후 저장
- [ ]  `leaveRoom(roomId, memberId)`
    - `members` set 에서 제거 후 저장
- [ ]  `closeRoom(roomId)`
    - `status` 를 `CLOSED` 로 바꾸고 저장
- [ ]  `getRoom(roomId)` 로 현재 상태 조회

**테스트**

- [ ]  `createRoom()` → `getRoom()` 으로 조회 시 `OPEN` 상태인지 테스트
- [ ]  `joinRoom()` 여러 번 호출 시 `members` 에 중복 없이 쌓이는지 테스트
- [ ]  `closeRoom()` 후 `joinRoom()` 을 호출하면 예외가 나는지 테스트
- [ ]  `leaveRoom()` 호출 시 멤버가 제거되는지 테스트

---

### 10. GroupConversationService – Room + ConversationEngine 연결

**구현**

- [ ]  `GroupConversationService` 정의
    - 의존성: `RoomRepository`, `ConversationEngine`
- [ ]  `startConversation(roomId)`
    - `RoomState` 가 `OPEN` 인지 확인
    - `ConversationEngine.start()` 호출
    - 반환된 `ConversationOutput.context` 를 `RoomState.conversationContext` 에 저장
    - 업데이트한 Room 저장
- [ ]  `handleInput(roomId, memberId, step, userInput)`
    - 방이 존재하고 `OPEN` 인지 확인
    - `RoomState.conversationContext` 가 존재하는지 검증
    - `ConversationEngine.handle(step, input, context)` 호출
    - 결과의 `context` 를 다시 `RoomState` 에 저장
    - `ConversationOutput` 반환

**테스트**

- [ ]  `startConversation()` 호출 후 해당 방의 `conversationContext` 가 초기화되는지 테스트

  (테스트용 fake/spy `ConversationEngine` 사용 or 단순 필드 체크)

- [ ]  `handleInput()` 호출 시 `ConversationEngine.handle()` 이 호출되고, 업데이트된 컨텍스트가 Room에 저장되는지 테스트
- [ ]  `conversationContext` 가 없는 방에서 `handleInput()` 호출 시 예외가 나는지 테스트
- [ ]  `CLOSED` 방에서 `startConversation`/`handleInput` 호출 시 예외 나는지 테스트

---

### 11. GROUP HTTP API – Room 관리 & 메시지 전달

**구현**

- [ ]  `adapter.http.GroupController` 생성
- [ ]  `POST /api/group/rooms`
    - `RoomService.createRoom()` 호출
    - `{"roomId": "<id>"}` 형태로 응답
- [ ]  `POST /api/group/rooms/{roomId}/join?memberId=...`
    - `RoomService.joinRoom()` 호출
    - 200 OK 반환
- [ ]  `POST /api/group/rooms/{roomId}/start`
    - `GroupConversationService.startConversation()` 호출
    - `ConversationOutput` 를 HTTP-friendly DTO로 변환하여 반환
- [ ]  `POST /api/group/rooms/{roomId}/message`
    - 요청 DTO: `{ memberId, step, input }`
    - `GroupConversationService.handleInput()` 호출
    - 응답: 다음 질문/메시지/nextStep 등을 포함한 DTO
- [ ]  (선택) `GET /api/group/rooms/{roomId}`
    - 방 상태, 멤버 목록, 현재 단계 등을 조회

**테스트**

- [ ]  `POST /api/group/rooms` 호출 → 200 OK와 `roomId` 필드가 내려오는지 MockMvc 로 테스트
- [ ]  `join → start → message` 까지 이어지는 happy-path 시나리오 1개 작성
- [ ]  존재하지 않는 `roomId` 로 호출 시 4xx 에러/메시지 형식이 일관적인지 테스트
- [ ]  `CLOSED` 상태에서 message 를 보내면 적절한 에러가 나는지 테스트

---

### 12. 에러 처리 & 만료 정책

**구현**

- [ ]  `RoomService` / `GroupConversationService` 에서 던지는 예외를 HTTP 에러로 매핑할 `@ControllerAdvice` 추가
    - 예: `IllegalArgumentException` → 400, `IllegalStateException` → 409 등
- [ ]  오래된 방 자동 정리 정책 설계 (예: `updatedAt` 기준 N분 이상 지난 방 삭제 – 이후 구현)

**테스트**

- [ ]  잘못된 `roomId`, `memberId` 등에서 에러 응답 JSON 형식 (`ErrorResponse`) 이 유지되는지 테스트
- [ ]  만료 정책을 구현한다면, “가짜 시간” 사용 or 작은 TTL로 Room 정리 테스트

---
## 🚦 구현 우선순위 (MVP 기준)

**1차 (콘솔 기준, 반드시 구현)**

- SOLO 모드
- 영수증 정보 입력 (총액, 세금, 팁 %/$)
- N분의 1 분배 (인원 수 입력 포함)
- KRW 변환 (외부 환율 API + fallback)
- 계산 결과 반환 (요약 정보)
- 잘못된 입력 재입력 처리

**2차 (가능하면 도전 꼭 하기)**

- 메뉴별 계산
- HTTP 기반 단순 웹/채팅 API
- GROUP 모드(한 서버에서 여러 세션/방 관리)

**3차 (완전 도전 과제)**

- WebSocket 기반 그룹 채팅
- 메뉴별 세금/팁 비례 분배 완전체