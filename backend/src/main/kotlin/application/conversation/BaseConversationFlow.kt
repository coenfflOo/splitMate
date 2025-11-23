package application.conversation

import domain.fx.ExchangeService
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator
import domain.split.SplitMode
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BaseConversationFlow(
    private val exchangeService: ExchangeService? = null,
    private val askRestartAfterResult: Boolean = false
) : ConversationFlow {

    override fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (step) {
            ConversationStep.ASK_TOTAL_AMOUNT -> handleTotalAmount(input, context)
            ConversationStep.ASK_TAX -> handleTax(input, context)
            ConversationStep.ASK_TIP_MODE -> handleTipMode(input, context)
            ConversationStep.ASK_TIP_VALUE -> handleTipValue(input, context)
            ConversationStep.ASK_SPLIT_MODE -> handleSplitMode(input, context)
            ConversationStep.ASK_PEOPLE_COUNT -> handlePeopleCount(input, context)

            ConversationStep.ASK_MENU_ITEMS -> handleMenuItems(input, context)
            ConversationStep.ASK_MENU_PARTICIPANTS -> handleMenuParticipants(input, context)
            ConversationStep.ASK_MENU_ASSIGNMENTS -> handleMenuAssignments(input, context)

            ConversationStep.ASK_EXCHANGE_RATE_MODE -> handleExchangeMode(input, context)
            ConversationStep.ASK_EXCHANGE_RATE_VALUE -> handleExchangeValue(input, context)

            ConversationStep.SHOW_RESULT -> showResult(context)
            ConversationStep.RESTART_CONFIRM -> handleRestartConfirm(input, context)
        }
    }

    protected open fun handleSplitMode(input: String, context: ConversationContext): ConversationOutput {
        val mode = parseSplitMode(input)
            ?: return retry(context, ConversationStep.ASK_SPLIT_MODE, splitModeGuideMessage())

        val newCtx = context.copy(
            splitMode = mode,
            lastStep = ConversationStep.ASK_SPLIT_MODE,
            failureCount = 0
        )

        return when (mode) {
            SplitMode.N_DIVIDE -> {
                if (newCtx.baseAmount == null) {
                    ConversationOutput(
                        nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                        message = "총 결제 금액을 입력해주세요 (예: 27.40)",
                        context = newCtx
                    )
                } else {
                    ConversationOutput(
                        nextStep = ConversationStep.ASK_PEOPLE_COUNT,
                        message = "몇 명이서 나누시나요?",
                        context = newCtx
                    )
                }
            }

            SplitMode.MENU_BASED -> unsupportedMenuStep(newCtx)
        }
    }

    protected open fun handleMenuItems(input: String, context: ConversationContext): ConversationOutput =
        unsupportedMenuStep(context)

    protected open fun handleMenuParticipants(input: String, context: ConversationContext): ConversationOutput =
        unsupportedMenuStep(context)

    protected open fun handleMenuAssignments(input: String, context: ConversationContext): ConversationOutput =
        unsupportedMenuStep(context)

    protected open fun showResult(context: ConversationContext): ConversationOutput {
        return when (context.splitMode) {
            SplitMode.N_DIVIDE -> showEvenResult(context)
            SplitMode.MENU_BASED -> unsupportedMenuStep(context)
            null -> retry(context, ConversationStep.ASK_SPLIT_MODE, splitModeGuideMessage())
        }
    }

    private fun unsupportedMenuStep(context: ConversationContext): ConversationOutput {
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = """
                이 플로우에서는 메뉴별 계산을 지원하지 않습니다.
                총 결제 금액부터 다시 입력해주세요. (예: 27.40)
            """.trimIndent(),
            context = context.copy(
                failureCount = 0,
                lastStep = ConversationStep.ASK_TOTAL_AMOUNT
            )
        )
    }

    private fun handleTotalAmount(input: String, context: ConversationContext): ConversationOutput {
        val amount = input.toBigDecimalOrNull()
            ?: return retry(context, ConversationStep.ASK_TOTAL_AMOUNT, "총 결제 금액은 숫자로 입력해주세요. (예: 27.40)")

        if (amount <= BigDecimal.ZERO) {
            return retry(context, ConversationStep.ASK_TOTAL_AMOUNT, "0보다 큰 금액을 입력해주세요.")
        }

        val baseMoney = Money.of(amount, Currency.CAD)
        val newContext = context.copy(
            baseAmount = baseMoney,
            failureCount = 0,
            lastStep = ConversationStep.ASK_TOTAL_AMOUNT
        )

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TAX,
            message = "세금 금액을 입력해주세요. 없으면 0 또는 '없음'을 입력하세요.",
            context = newContext
        )
    }

    private fun handleTax(input: String, context: ConversationContext): ConversationOutput {
        val normalized = input.trim().lowercase()

        val value = when (normalized) {
            "없음", "none", "no" -> BigDecimal.ZERO
            else -> normalized.toBigDecimalOrNull()
                ?: return retry(context, ConversationStep.ASK_TAX, "세금 금액은 숫자 또는 '없음'으로 입력해주세요.")
        }

        if (value < BigDecimal.ZERO) {
            return retry(context, ConversationStep.ASK_TAX, "세금 금액은 0 이상이어야 합니다.")
        }

        val taxMoney = Money.of(value, Currency.CAD)
        val newContext = context.copy(
            taxAmount = taxMoney,
            failureCount = 0,
            lastStep = ConversationStep.ASK_TAX
        )

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TIP_MODE,
            message = "팁 입력 방식을 선택해주세요. 1) 퍼센트(%)  2) 금액($)  3) 없음",
            context = newContext
        )
    }

    private fun handleTipMode(input: String, context: ConversationContext): ConversationOutput {
        val mode = parseTipMode(input)
            ?: return retry(context, ConversationStep.ASK_TIP_MODE, "1) 퍼센트 2) 금액 3) 없음 중에서 선택해주세요.")

        val newCtx = context.copy(
            tipMode = mode,
            tipPercent = null,
            tipAbsolute = null,
            failureCount = 0,
            lastStep = ConversationStep.ASK_TIP_MODE
        )

        return when (mode) {
            TipMode.PERCENT -> ConversationOutput(
                message = "팁 퍼센트를 입력해주세요. (예: 15)",
                nextStep = ConversationStep.ASK_TIP_VALUE,
                context = newCtx
            )

            TipMode.ABSOLUTE -> ConversationOutput(
                message = "팁 금액($)을 입력해주세요. (예: 10.00)",
                nextStep = ConversationStep.ASK_TIP_VALUE,
                context = newCtx
            )

            TipMode.NONE -> ConversationOutput(
                message = splitModePromptMessage(),
                nextStep = ConversationStep.ASK_SPLIT_MODE,
                context = newCtx.copy(tipPercent = 0)
            )
        }
    }

    private fun handleTipValue(input: String, context: ConversationContext): ConversationOutput {
        return when (context.tipMode) {
            TipMode.PERCENT -> {
                val p = input.toIntOrNull()
                    ?: return retry(context, ConversationStep.ASK_TIP_VALUE, "정수 퍼센트로 입력해주세요. (예: 15)")
                if (p !in 0..100) {
                    return retry(context, ConversationStep.ASK_TIP_VALUE, "0~100 사이의 퍼센트를 입력해주세요.")
                }

                ConversationOutput(
                    message = splitModePromptMessage(),
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(
                        tipPercent = p,
                        tipAbsolute = null,
                        failureCount = 0,
                        lastStep = ConversationStep.ASK_TIP_VALUE
                    )
                )
            }

            TipMode.ABSOLUTE -> {
                val v = input.toBigDecimalOrNull()
                    ?: return retry(context, ConversationStep.ASK_TIP_VALUE, "숫자 금액으로 입력해주세요. (예: 10.00)")
                if (v <= BigDecimal.ZERO) {
                    return retry(context, ConversationStep.ASK_TIP_VALUE, "0보다 큰 값을 입력해주세요.")
                }

                ConversationOutput(
                    message = splitModePromptMessage(),
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(
                        tipAbsolute = Money.of(v, Currency.CAD),
                        failureCount = 0,
                        lastStep = ConversationStep.ASK_TIP_VALUE
                    )
                )
            }

            TipMode.NONE, null -> ConversationOutput(
                message = splitModePromptMessage(),
                nextStep = ConversationStep.ASK_SPLIT_MODE,
                context = context.copy(
                    tipMode = TipMode.NONE,
                    tipPercent = 0,
                    tipAbsolute = null,
                    failureCount = 0,
                    lastStep = ConversationStep.ASK_TIP_VALUE
                )
            )
        }
    }

    private fun handlePeopleCount(input: String, context: ConversationContext): ConversationOutput {
        val n = input.toIntOrNull()
            ?: return retry(context, ConversationStep.ASK_PEOPLE_COUNT, "인원 수는 1 이상의 정수로 입력해주세요.")

        if (n < 1) {
            return retry(context, ConversationStep.ASK_PEOPLE_COUNT, "인원 수는 1 이상의 정수로 입력해주세요.")
        }

        val nextCtx = context.copy(
            peopleCount = n,
            failureCount = 0,
            lastStep = ConversationStep.ASK_PEOPLE_COUNT
        )

        return ConversationOutput(
            message = exchangeModePromptMessage(),
            nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
            context = nextCtx
        )
    }

    private fun handleExchangeMode(input: String, context: ConversationContext): ConversationOutput {
        return when (input.trim()) {
            "1" -> {
                val svc = exchangeService
                    ?: return ConversationOutput(
                        message = "자동 환율 조회를 사용할 수 없습니다(키 미설정). 환율을 직접 입력해주세요 (예: 1000).",
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(wantKrw = true, lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE)
                    )

                val rate = runCatching { svc.getCadToKrwRate().rate }.getOrNull()
                    ?: return ConversationOutput(
                        message = "환율 조회에 실패했습니다. 환율을 직접 입력해주세요 (예: 1000).",
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(wantKrw = true, lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE)
                    )

                showResult(context.copy(wantKrw = true, manualRate = rate, lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE))
            }

            "2" -> ConversationOutput(
                message = "환율을 숫자로 입력해주세요. 예) 1 CAD = 1000 KRW → 1000 입력",
                nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                context = context.copy(wantKrw = true, lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE)
            )

            "3" -> showResult(context.copy(wantKrw = false, manualRate = null, lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE))

            else -> retry(context, ConversationStep.ASK_EXCHANGE_RATE_MODE, "1, 2, 3 중에서 선택해주세요.")
        }
    }

    private fun handleExchangeValue(input: String, context: ConversationContext): ConversationOutput {
        val rate = input.toBigDecimalOrNull()
            ?: return retry(context, ConversationStep.ASK_EXCHANGE_RATE_VALUE, "숫자로 입력해주세요. 예: 1000")

        if (rate <= BigDecimal.ZERO) {
            return retry(context, ConversationStep.ASK_EXCHANGE_RATE_VALUE, "0보다 큰 값을 입력해주세요.")
        }

        return showResult(context.copy(wantKrw = true, manualRate = rate, lastStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE))
    }

    private fun handleRestartConfirm(input: String, context: ConversationContext): ConversationOutput {
        return when (input.trim().lowercase()) {
            "y", "yes", "예", "네", "1" -> start()
            "n", "no", "아니오", "2" -> {
                val step = context.lastStep ?: ConversationStep.ASK_TOTAL_AMOUNT
                ConversationOutput(
                    nextStep = step,
                    message = restartBackMessage(step),
                    context = context.copy(failureCount = 0)
                )
            }
            else -> ConversationOutput(
                nextStep = ConversationStep.RESTART_CONFIRM,
                message = "Y 또는 N으로 입력해주세요. 처음부터 다시 시작하시겠습니까? (Y/N)",
                context = context
            )
        }
    }

    protected fun retry(
        context: ConversationContext,
        step: ConversationStep,
        reason: String
    ): ConversationOutput {
        val newCount = context.failureCount + 1

        if (newCount >= 3) {
            val msg = buildString {
                appendLine(reason)
                appendLine()
                append("입력을 여러 번 잘못하셨어요. 처음부터 다시 시작하시겠습니까? (Y/N)")
            }
            return ConversationOutput(
                nextStep = ConversationStep.RESTART_CONFIRM,
                message = msg,
                context = context.copy(failureCount = 0, lastStep = step)
            )
        }

        val msg = when (step) {
            ConversationStep.ASK_TOTAL_AMOUNT ->
                "$reason\n총 결제 금액을 입력해주세요 (예: 27.40)"

            ConversationStep.ASK_TAX ->
                "$reason\n세금 금액을 입력해주세요. (없으면 0 또는 '없음')"

            ConversationStep.ASK_TIP_MODE ->
                "$reason\n팁 입력 방식을 선택해주세요. 1) 퍼센트 2) 금액 3) 없음"

            ConversationStep.ASK_TIP_VALUE ->
                "$reason\n팁 값을 입력해주세요."

            ConversationStep.ASK_SPLIT_MODE ->
                "$reason\n${splitModePromptMessage()}"

            ConversationStep.ASK_PEOPLE_COUNT ->
                "$reason\n인원 수를 입력해주세요. (예: 3)"

            ConversationStep.ASK_EXCHANGE_RATE_MODE ->
                "$reason\n${exchangeModePromptMessage()}"

            ConversationStep.ASK_EXCHANGE_RATE_VALUE ->
                "$reason\n환율을 숫자로 입력해주세요. 예) 1000"

            ConversationStep.ASK_MENU_ITEMS ->
                "$reason\n메뉴를 입력해주세요.\n예) 파스타 18.9; 피자 22; 콜라 3"

            ConversationStep.ASK_MENU_PARTICIPANTS ->
                "$reason\n참가자 이름을 쉼표로 구분해 입력해주세요.\n예) 민지, 철수, 영희"

            ConversationStep.ASK_MENU_ASSIGNMENTS ->
                "$reason\n메뉴별로 누가 먹었는지 지정해주세요.\n예) m1:p1,p2; m2:p2; m3:p1,p3"

            ConversationStep.SHOW_RESULT,
            ConversationStep.RESTART_CONFIRM ->
                reason
        }

        return ConversationOutput(
            nextStep = step,
            message = msg,
            context = context.copy(failureCount = newCount, lastStep = step)
        )
    }

    protected fun parsePositiveMoney(input: String): Money? {
        val v = parsePositiveDecimal(input) ?: return null
        return Money.of(v, Currency.CAD)
    }

    protected fun parseTaxMoney(input: String): Money? {
        val norm = input.trim().lowercase()
        if (norm in listOf("없음", "none", "no", "0")) {
            return Money.zero(Currency.CAD)
        }
        val v = parsePositiveDecimal(norm) ?: return null
        return Money.of(v, Currency.CAD)
    }

    protected fun parsePositiveDecimal(input: String): BigDecimal? {
        val s = input.replace(",", "").trim()
        val v = s.toBigDecimalOrNull() ?: return null
        if (v < BigDecimal.ZERO) return null
        return v
    }

    protected fun parseTipMode(input: String): TipMode? {
        return when (input.trim().lowercase()) {
            "1", "percent", "퍼센트" -> TipMode.PERCENT
            "2", "absolute", "금액" -> TipMode.ABSOLUTE
            "3", "none", "없음" -> TipMode.NONE
            else -> null
        }
    }

    protected fun parseSplitMode(input: String): SplitMode? {
        return when (input.trim().lowercase()) {
            "1", "n", "n_divide", "n분의1" -> SplitMode.N_DIVIDE
            "2", "menu", "menu_based", "메뉴", "메뉴별" -> SplitMode.MENU_BASED
            else -> null
        }
    }

    protected fun showEvenResult(context: ConversationContext): ConversationOutput {
        val base = requireNotNull(context.baseAmount) { "baseAmount is required" }
        val taxMoney = context.taxAmount ?: Money.zero(Currency.CAD)
        val people = requireNotNull(context.peopleCount) { "peopleCount is required" }

        val tip = when (context.tipMode) {
            TipMode.PERCENT -> Tip(mode = TipMode.PERCENT, percent = context.tipPercent ?: 0)
            TipMode.ABSOLUTE -> Tip(mode = TipMode.ABSOLUTE, absolute = context.tipAbsolute ?: Money.zero(Currency.CAD))
            TipMode.NONE, null -> Tip(mode = TipMode.NONE)
        }

        val receipt = Receipt(
            baseAmount = base,
            tax = Tax(taxMoney),
            tip = tip
        )

        val splitResult = SplitCalculator.splitEvenly(receipt, people)

        val totalCad = splitResult.total
        val perPersonCad = splitResult.perPerson

        val sb = StringBuilder()
        sb.appendLine("=== 계산 결과 ===")
        sb.appendLine("총 금액: ${formatMoney(totalCad)}")
        sb.appendLine("인원 수: $people")
        sb.appendLine("1인당: ${formatMoney(perPersonCad)}")

        if (context.wantKrw && context.manualRate != null) {
            val krw = convertWithRate(perPersonCad, context.manualRate)
            sb.appendLine("환율: 1 CAD = ${formatRate(context.manualRate)} KRW")
            sb.appendLine("1인당(원화): ${formatMoney(krw)}")
        }

        return ConversationOutput(
            message = sb.toString().trimEnd(),
            nextStep = ConversationStep.SHOW_RESULT,
            context = context,
            isFinished = true
        )
    }

    protected fun convertWithRate(cad: Money, rate: BigDecimal): Money {
        val krwAmount = cad.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return Money.of(krwAmount, Currency.KRW)
    }

    protected fun formatMoney(m: Money): String {
        val plain = m.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        return when (m.currency) {
            Currency.CAD -> "$plain CAD"
            Currency.KRW -> "${formatWithComma(plain)} KRW"
        }
    }

    private fun formatWithComma(number: String): String {
        val parts = number.split(".")
        val intPart = parts[0]
        val fracPart = if (parts.size > 1) parts[1] else "00"
        val reversed = intPart.reversed()
        val chunked = reversed.chunked(3).joinToString(",")
        val withComma = chunked.reversed()
        return "$withComma.$fracPart"
    }

    protected fun formatRate(rate: BigDecimal): String {
        val scaled = rate.setScale(0, RoundingMode.HALF_UP).toPlainString()
        val withComma = formatWithComma("$scaled.00")
        return withComma.substringBefore(".")
    }

    protected open fun splitModePromptMessage(): String =
        "분배 방식을 선택해주세요. 1) N분의 1"

    protected open fun splitModeGuideMessage(): String =
        "현재는 1) N분의 1 방식만 지원합니다."

    protected open fun exchangeModePromptMessage(): String =
        buildString {
            appendLine("환율 및 통화 선택:")
            appendLine("1) 오늘 환율 자동 조회 (CAD → KRW)")
            appendLine("2) 환율 직접 입력 (예: 1000)")
            appendLine("3) KRW 변환 없이 CAD만 보기")
            append("번호를 선택해주세요: ")
        }

    private fun restartBackMessage(step: ConversationStep): String {
        return when (step) {
            ConversationStep.ASK_TOTAL_AMOUNT ->
                "그럼 다시 총 결제 금액부터 입력해볼게요.\n총 결제 금액을 입력해주세요 (예: 27.40)"
            ConversationStep.ASK_TAX ->
                "그럼 다시 세금 금액부터 입력해볼게요.\n세금 금액을 입력해주세요. (없으면 0 또는 '없음')"
            ConversationStep.ASK_TIP_MODE ->
                "그럼 팁 입력 방식부터 다시 선택해주세요.\n1) 퍼센트 2) 금액 3) 없음"
            ConversationStep.ASK_TIP_VALUE ->
                "그럼 팁 값을 다시 입력해주세요."
            ConversationStep.ASK_SPLIT_MODE ->
                splitModePromptMessage()
            ConversationStep.ASK_PEOPLE_COUNT ->
                "인원 수를 다시 입력해주세요. (예: 3)"
            ConversationStep.ASK_EXCHANGE_RATE_MODE ->
                exchangeModePromptMessage()
            ConversationStep.ASK_EXCHANGE_RATE_VALUE ->
                "환율을 숫자로 입력해주세요. 예) 1000"
            ConversationStep.ASK_MENU_ITEMS ->
                "메뉴를 다시 입력해주세요.\n예) 파스타 18.9; 피자 22; 콜라 3"
            ConversationStep.ASK_MENU_PARTICIPANTS ->
                "참가자 이름을 다시 입력해주세요.\n예) 민지, 철수, 영희"
            ConversationStep.ASK_MENU_ASSIGNMENTS ->
                "메뉴별로 누가 먹었는지 다시 지정해주세요.\n예) m1:p1,p2; m2:p2; m3:p1,p3"
            ConversationStep.SHOW_RESULT ->
                "이미 계산이 완료되었습니다."
            ConversationStep.RESTART_CONFIRM ->
                "처음부터 다시 시작하시겠습니까? (Y/N)"
        }
    }
}
