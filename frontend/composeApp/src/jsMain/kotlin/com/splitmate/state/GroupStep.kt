package com.splitmate.state

enum class GroupStep {
    TOTAL_AMOUNT,
    TAX,
    TIP_MODE,
    TIP_VALUE,
    SPLIT_MODE,
    PEOPLE_COUNT,

    MENU_ITEMS,
    PARTICIPANTS,
    MENU_ASSIGNMENTS,

    EXCHANGE_MODE,
    EXCHANGE_VALUE,

    RESULT,
    RESTART_CONFIRM,

    UNKNOWN;

    companion object {
        fun fromServer(step: String?): GroupStep {
            val s = step?.uppercase() ?: return UNKNOWN
            return when (s) {
                "ASK_TOTAL_AMOUNT"        -> TOTAL_AMOUNT
                "ASK_TAX"                 -> TAX
                "ASK_TIP_MODE"            -> TIP_MODE
                "ASK_TIP_VALUE"           -> TIP_VALUE
                "ASK_SPLIT_MODE"          -> SPLIT_MODE
                "ASK_PEOPLE_COUNT"        -> PEOPLE_COUNT

                "ASK_MENU_ITEMS"          -> MENU_ITEMS
                "ASK_MENU_PARTICIPANTS"   -> PARTICIPANTS
                "ASK_MENU_ASSIGNMENTS"    -> MENU_ASSIGNMENTS

                "ASK_EXCHANGE_RATE_MODE"  -> EXCHANGE_MODE
                "ASK_EXCHANGE_RATE_VALUE" -> EXCHANGE_VALUE
                "SHOW_RESULT"             -> RESULT
                "RESTART_CONFIRM"         -> RESTART_CONFIRM
                else -> UNKNOWN
            }
        }

        fun from(raw: String?): GroupStep =
            raw?.let {
                runCatching { valueOf(it) }.getOrNull()
            } ?: UNKNOWN
    }
}

private fun String.toGroupStep(): GroupStep =
    runCatching { GroupStep.valueOf(this) }
        .getOrDefault(GroupStep.UNKNOWN)
