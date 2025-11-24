package application.conversation.flow

import application.conversation.model.ConversationContext
import application.conversation.model.ConversationOutput
import application.conversation.model.ConversationStep
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
    private val exchangeService: ExchangeService? = null
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
                message = "인원 수를 입력해주세요. (예: 3)",
                nextStep = ConversationStep.ASK_PEOPLE_COUNT,
                context = newCtx.copy(tipPercent = 0)
            )
        }
    }

    private fun handleTipValue(input: String, context: ConversationContext): ConversationOutput {
        val trimmed = input.trim().replace(",", "")

        val newCtx = when (context.tipMode) {
            TipMode.PERCENT -> {
                val percentBd = trimmed.toBigDecimalOrNull()
                    ?: return retry(context, ConversationStep.ASK_TIP_VALUE,
                        "퍼센트는 숫자로 입력해주세요. 예) 10 또는 10.5")

                val percent = percentBd.setScale(0, RoundingMode.HALF_UP).toInt()
                if (percent !in 0..100) {
                    return retry(context, ConversationStep.ASK_TIP_VALUE,
                        "퍼센트는 0~100 사이로 입력해주세요.")
                }

                context.copy(
                    tipPercent = percent,
                    lastStep = ConversationStep.ASK_TIP_VALUE,
                    failureCount = 0
                )
            }

            TipMode.ABSOLUTE -> {
                val value = trimmed.toBigDecimalOrNull()
                    ?: return retry(context, ConversationStep.ASK_TIP_VALUE,
                        "팁 금액은 숫자로 입력해주세요. 예) 10.00")

                if (value < BigDecimal.ZERO) {
                    return retry(context, ConversationStep.ASK_TIP_VALUE,
                        "팁 금액은 0 이상이어야 합니다.")
                }

                context.copy(
                    tipAbsolute = Money.of(value, Currency.CAD),
                    lastStep = ConversationStep.ASK_TIP_VALUE,
                    failureCount = 0
                )
            }

            TipMode.NONE -> context.copy(
                lastStep = ConversationStep.ASK_TIP_VALUE,
                failureCount = 0
            )

            null -> return retry(context, ConversationStep.ASK_TIP_MODE, tipModeGuideMessage())
        }

        return ConversationOutput(
            message = "인원 수를 입력해주세요. (예: 3)",
            nextStep = ConversationStep.ASK_PEOPLE_COUNT,
            context = newCtx
        )
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
        return when (input.trim().uppercase()) {
            "1", "AUTO" -> {
                val svc = exchangeService
                    ?: return ConversationOutput(
                        message = "자동 환율 조회를 사용할 수 없습니다(키 미설정). 환율을 직접 입력해주세요 (예: 1000).",
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(
                            wantKrw = true,
                            lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE
                        )
                    )

                val rate = runCatching { svc.getCadToKrwRate().rate }.getOrNull()
                    ?: return ConversationOutput(
                        message = "환율 조회에 실패했습니다. 환율을 직접 입력해주세요 (예: 1000).",
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(
                            wantKrw = true,
                            lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE
                        )
                    )

                showResult(
                    context.copy(
                        wantKrw = true,
                        manualRate = rate,
                        lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE
                    )
                )
            }

            "2", "MANUAL" -> ConversationOutput(
                message = "환율을 숫자로 입력해주세요. 예) 1 CAD = 1000 KRW → 1000 입력",
                nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                context = context.copy(
                    wantKrw = true,
                    lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE
                )
            )

            "3", "NONE" -> showResult(
                context.copy(
                    wantKrw = false,
                    manualRate = null,
                    lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE
                )
            )

            else -> retry(context, ConversationStep.ASK_EXCHANGE_RATE_MODE, "1, 2, 3(AUTO/MANUAL/NONE) 중에서 선택해주세요.")
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
        val normalized = input.trim().lowercase()

        return when (normalized) {
            "y", "yes", "예", "네", "1" -> {
                // ✅ 다시 계산하기: splitMode 유지한 채 첫 단계로 리셋
                when (context.splitMode) {
                    SplitMode.N_DIVIDE -> {
                        val resetCtx = context.copy(
                            baseAmount = null,
                            taxAmount = null,
                            tipMode = null,
                            tipPercent = null,
                            tipAbsolute = null,
                            peopleCount = null,
                            wantKrw = false,
                            manualRate = null,
                            failureCount = 0,
                            lastStep = ConversationStep.ASK_TOTAL_AMOUNT
                        )

                        ConversationOutput(
                            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                            message = "총 결제 금액을 입력해주세요. (예: 27.40)",
                            context = resetCtx
                        )
                    }

                    SplitMode.MENU_BASED -> {
                        val resetCtx = context.copy(
                            baseAmount = null,
                            taxAmount = null,
                            tipMode = null,
                            tipPercent = null,
                            tipAbsolute = null,
                            peopleCount = null,
                            wantKrw = false,
                            manualRate = null,
                            menuItems = emptyList(),
                            menuParticipants = emptyList(),
                            menuAssignments = emptyMap(),
                            failureCount = 0,
                            lastStep = ConversationStep.ASK_MENU_ITEMS
                        )

                        ConversationOutput(
                            nextStep = ConversationStep.ASK_MENU_ITEMS,
                            message = """
                            메뉴를 입력해주세요.
                            형식: "이름 가격; 이름 가격; ..."
                            예) 파스타 18.9; 피자 22; 콜라 3
                        """.trimIndent(),
                            context = resetCtx
                        )
                    }

                    null -> start()
                }
            }

            "n", "no", "아니오", "2" -> {
                // ✅ 종료: 분배 방식 선택으로 돌아가기
                ConversationOutput(
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    message = splitModePromptMessage(),
                    context = ConversationContext()
                )
            }

            else -> ConversationOutput(
                nextStep = ConversationStep.RESTART_CONFIRM,
                message = "1(YES) 또는 2(NO)로 입력해주세요.",
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

    protected fun parseTipMode(input: String): TipMode? {
        return when (input.trim().lowercase()) {
            "1", "percent", "퍼센트", "PERCENT", "PERCENTAGE" -> TipMode.PERCENT
            "2", "absolute", "금액", "ABSOLUTE", "AMOUNT" -> TipMode.ABSOLUTE
            "3", "none", "없음", "NONE", "NO" -> TipMode.NONE
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
            TipMode.ABSOLUTE -> Tip(
                mode = TipMode.ABSOLUTE,
                absolute = context.tipAbsolute ?: Money.zero(Currency.CAD)
            )
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

        val msg = """
        ${sb.toString().trimEnd()}
        
        다시 시작할까요?
        1) YES
        2) NO
    """.trimIndent()

        return ConversationOutput(
            message = msg,
            nextStep = ConversationStep.RESTART_CONFIRM,
            context = context.copy(lastStep = ConversationStep.SHOW_RESULT),
            isFinished = false
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

    protected open fun tipModeGuideMessage(): String =
        "Tip을 입력해주세요"

    protected open fun exchangeModePromptMessage(): String =
        buildString {
            appendLine("환율 및 통화 선택:")
            appendLine("1) 오늘 환율 자동 조회 (CAD → KRW)")
            appendLine("2) 환율 직접 입력 (예: 1000)")
            appendLine("3) KRW 변환 없이 CAD만 보기")
            append("번호를 선택해주세요: ")
        }
}