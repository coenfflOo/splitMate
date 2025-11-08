package application.conversation

enum class ConversationStep {
    ASK_TOTAL_AMOUNT,
    ASK_TAX,
    ASK_TIP_MODE,
    ASK_TIP_VALUE,
    ASK_SPLIT_MODE,
    ASK_PEOPLE_COUNT,
    SHOW_RESULT,
    FINISHED
}
