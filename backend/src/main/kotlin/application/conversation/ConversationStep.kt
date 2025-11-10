package application.conversation

enum class ConversationStep {
    ASK_TOTAL_AMOUNT,
    ASK_TAX,
    ASK_TIP_MODE,
    ASK_TIP_VALUE,
    ASK_SPLIT_MODE,
    ASK_PEOPLE_COUNT,

    // 환율 관련
    ASK_EXCHANGE_RATE_MODE,   // 1: 자동(오늘 환율), 2: 수동 입력, 3: KRW 생략
    ASK_EXCHANGE_RATE_VALUE,  // 수동 입력일 때 환율 값 입력

    SHOW_RESULT               // 최종 요약 출력 단계
}
