package application.conversation

import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
import domain.fx.ExchangeService
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator
import java.math.BigDecimal
import java.math.RoundingMode


class ConversationEngine(
    private val exchangeService: ExchangeService? = null
) {

    fun start(): ConversationOutput {
        val message = "총 결제 금액을 입력해주세요 (예: 27.40)"
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = message,
            context = ConversationContext()
        )
    }

    fun handle(
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

            ConversationStep.ASK_MENU_ITEMS,
            ConversationStep.ASK_MENU_PARTICIPANTS,
            ConversationStep.ASK_MENU_ASSIGNMENTS -> ConversationOutput(
                nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                message = """
                이 콘솔 엔진에서는 메뉴별 계산 단계는 지원하지 않습니다.
                총 결제 금액부터 다시 입력해주세요. (예: 27.40)
            """.trimIndent(),
                context = context.copy(
                    failureCount = 0,
                    lastStep = ConversationStep.ASK_TOTAL_AMOUNT
                )
            )

            ConversationStep.ASK_EXCHANGE_RATE_MODE -> handleExchangeMode(input, context)
            ConversationStep.ASK_EXCHANGE_RATE_VALUE -> handleExchangeValue(input, context)

            ConversationStep.SHOW_RESULT ->
                ConversationOutput(
                    nextStep = ConversationStep.SHOW_RESULT,
                    message = "이미 계산이 완료되었습니다.",
                    context = context,
                    isFinished = true
                )

            ConversationStep.RESTART_CONFIRM -> handleRestartConfirm(input, context)
        }
    }

    private fun handleTotalAmount(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val amount = input.toBigDecimalOrNull()
            ?: return invalidNumber(
                step = ConversationStep.ASK_TOTAL_AMOUNT,
                message = "총 결제 금액은 숫자로 입력해주세요. (예: 27.40)",
                context = context
            )

        if (amount <= BigDecimal.ZERO) {
            return invalidNumber(
                step = ConversationStep.ASK_TOTAL_AMOUNT,
                message = "0보다 큰 금액을 입력해주세요.",
                context = context
            )
        }

        val baseMoney = Money.of(amount, Currency.CAD)
        val newContext = context.copy(
            baseAmount = baseMoney,
            failureCount = 0
        )

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TAX,
            message = "세금 금액을 입력해주세요. 없으면 0 또는 '없음'을 입력하세요.",
            context = newContext
        )
    }

    private fun handleTax(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val normalized = input.trim().lowercase()

        val value = when (normalized) {
            "없음", "none", "no" -> BigDecimal.ZERO
            else -> normalized.toBigDecimalOrNull()
                ?: return invalidNumber(
                    step = ConversationStep.ASK_TAX,
                    message = "세금 금액은 숫자 또는 '없음'으로 입력해주세요.",
                    context = context
                )
        }

        if (value < BigDecimal.ZERO) {
            return invalidNumber(
                step = ConversationStep.ASK_TAX,
                message = "세금 금액은 0 이상이어야 합니다.",
                context = context
            )
        }

        val taxMoney = Money.of(value, Currency.CAD)

        val newContext = context.copy(
            taxAmount = taxMoney,
            failureCount = 0
        )

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TIP_MODE,
            message = "팁 입력 방식을 선택해주세요. 1) 퍼센트(%)  2) 금액($)  3) 없음",
            context = newContext
        )
    }

    private fun handleTipMode(input: String, context: ConversationContext): ConversationOutput {
        return when (input.trim()) {
            "1" -> {
                ConversationOutput(
                    message = "팁 퍼센트를 입력해주세요. (예: 15)",
                    nextStep = ConversationStep.ASK_TIP_VALUE,
                    context = context.copy(tipMode = TipMode.PERCENT)
                )
            }

            "2" -> {
                ConversationOutput(
                    message = "팁 금액($)을 입력해주세요. (예: 10.00)",
                    nextStep = ConversationStep.ASK_TIP_VALUE,
                    context = context.copy(tipMode = TipMode.ABSOLUTE)
                )
            }

            "3" -> {
                ConversationOutput(
                    message = "분배 방식을 선택해주세요. 1) N분의 1",
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(tipMode = TipMode.NONE, tipPercent = 0, tipAbsolute = null)
                )
            }

            else -> retry(ConversationStep.ASK_TIP_MODE, "1) 퍼센트 2) 금액 3) 없음 중에서 선택해주세요.", context)
        }
    }

    private fun handleTipValue(input: String, context: ConversationContext): ConversationOutput {
        return when (context.tipMode) {
            TipMode.PERCENT -> {
                val p = input.toIntOrNull()
                    ?: return retry(ConversationStep.ASK_TIP_VALUE, "정수 퍼센트로 입력해주세요. (예: 15)", context)
                if (p < 0 || p > 100) {
                    return retry(ConversationStep.ASK_TIP_VALUE, "0~100 사이의 퍼센트를 입력해주세요.", context)
                }
                ConversationOutput(
                    message = "분배 방식을 선택해주세요. 1) N분의 1",
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(tipPercent = p, tipAbsolute = null)
                )
            }

            TipMode.ABSOLUTE -> {
                val v = input.toBigDecimalOrNull()
                    ?: return retry(ConversationStep.ASK_TIP_VALUE, "숫자 금액으로 입력해주세요. (예: 10.00)", context)
                if (v <= BigDecimal.ZERO) {
                    return retry(ConversationStep.ASK_TIP_VALUE, "0보다 큰 값을 입력해주세요.", context)
                }
                ConversationOutput(
                    message = "분배 방식을 선택해주세요. 1) N분의 1",
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(tipAbsolute = Money.of(v, Currency.CAD))
                )
            }

            TipMode.NONE, null -> {
                ConversationOutput(
                    message = "분배 방식을 선택해주세요. 1) N분의 1",
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    context = context.copy(tipMode = TipMode.NONE, tipPercent = 0, tipAbsolute = null)
                )
            }
        }
    }

    private fun handleSplitMode(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (input.trim()) {
            "1" -> ConversationOutput(
                nextStep = ConversationStep.ASK_PEOPLE_COUNT,
                message = "몇 명이서 나누시나요?",
                context = context
            )

            else -> ConversationOutput(
                nextStep = ConversationStep.ASK_SPLIT_MODE,
                message = "현재는 1) N분의 1 방식만 지원합니다.",
                context = context
            )
        }
    }

    private fun handlePeopleCount(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val n = input.toIntOrNull()
            ?: return retry(
                ConversationStep.ASK_PEOPLE_COUNT,
                "인원 수는 1 이상의 정수로 입력해주세요.",
                context
            )

        if (n < 1) {
            return retry(
                ConversationStep.ASK_PEOPLE_COUNT,
                "인원 수는 1 이상의 정수로 입력해주세요.",
                context
            )
        }

        val nextCtx = context.copy(
            peopleCount = n,
            failureCount = 0
        )

        val message = buildString {
            appendLine("환율 및 통화 선택:")
            appendLine("1) 오늘 환율 자동 조회 (CAD → KRW)")
            appendLine("2) 환율 직접 입력 (예: 1000)")
            appendLine("3) KRW 변환 없이 CAD만 보기")
            append("번호를 선택해주세요: ")
        }
        return ConversationOutput(
            message = message,
            nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
            context = nextCtx
        )
    }

    private fun handleExchangeMode(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (input.trim()) {

            "1" -> {
                val svc = exchangeService
                if (svc == null) {
                    return ConversationOutput(
                        message = "자동 환율 조회를 사용할 수 없습니다(키 미설정). 환율을 직접 입력해주세요 (예: 1000).",
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(wantKrw = true)
                    )
                }

                return try {
                    val rate = svc.getCadToKrwRate()

                    summarize(
                        context.copy(
                            wantKrw = true,
                            manualRate = rate.rate
                        )
                    )
                } catch (e: Exception) {
                    val msg = "환율 조회에 실패했습니다. 환율을 직접 입력해주세요 (예: 1000)."
                    ConversationOutput(
                        message = msg,
                        nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                        context = context.copy(wantKrw = true)
                    )
                }
            }

            "2" -> {
                ConversationOutput(
                    message = "환율을 숫자로 입력해주세요. 예) 1 CAD = 1000 KRW → 1000 입력",
                    nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                    context = context.copy(wantKrw = true)
                )
            }

            "3" -> {
                summarize(
                    context.copy(
                        wantKrw = false,
                        manualRate = null
                    )
                )
            }

            else -> {
                retry(
                    step = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                    reason = "1, 2, 3 중에서 선택해주세요.",
                    context = context
                )
            }
        }
    }


    private fun handleExchangeValue(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val rate = input.toBigDecimalOrNull()
            ?: return retry(
                ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                "숫자로 입력해주세요. 예: 1000",
                context
            )

        if (rate <= BigDecimal.ZERO) {
            return retry(
                ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                "0보다 큰 값을 입력해주세요.",
                context
            )
        }

        return summarize(context.copy(wantKrw = true, manualRate = rate))
    }

    private fun handleRestartConfirm(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (input.trim().lowercase()) {
            "y", "yes", "예", "네" -> {
                start()
            }

            "n", "no", "아니오" -> {
                val step = context.lastStep ?: ConversationStep.ASK_TOTAL_AMOUNT

                val msg = when (step) {
                    ConversationStep.ASK_TOTAL_AMOUNT ->
                        "그럼 다시 총 결제 금액부터 입력해볼게요.\n총 결제 금액을 입력해주세요 (예: 27.40)"

                    ConversationStep.ASK_TAX ->
                        "그럼 다시 세금 금액부터 입력해볼게요.\n세금 금액을 입력해주세요. (없으면 0 또는 '없음')"

                    ConversationStep.ASK_TIP_MODE ->
                        "그럼 팁 입력 방식부터 다시 선택해주세요.\n1) 퍼센트 2) 금액 3) 없음"

                    ConversationStep.ASK_TIP_VALUE ->
                        "그럼 팁 값을 다시 입력해주세요."

                    ConversationStep.ASK_SPLIT_MODE ->
                        "분배 방식을 다시 선택해주세요. 1) N분의 1"

                    ConversationStep.ASK_PEOPLE_COUNT ->
                        "인원 수를 다시 입력해주세요. (예: 3)"

                    ConversationStep.ASK_EXCHANGE_RATE_MODE ->
                        "환율 및 통화 선택:\n1) 오늘 환율 자동 조회 (CAD → KRW)\n2) 환율 직접 입력 (예: 1000)\n3) KRW 변환 없이 CAD만 보기\n번호를 선택해주세요: "

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

                return ConversationOutput(
                    nextStep = step,
                    message = msg,
                    context = context.copy(failureCount = 0)
                )
            }

            else -> {
                ConversationOutput(
                    nextStep = ConversationStep.RESTART_CONFIRM,
                    message = "Y 또는 N으로 입력해주세요. 처음부터 다시 시작하시겠습니까? (Y/N)",
                    context = context
                )
            }
        }
    }

    private fun summarize(context: ConversationContext): ConversationOutput {
        val base = requireNotNull(context.baseAmount) { "baseAmount is required" }
        val taxMoney = context.taxAmount ?: Money.zero(Currency.CAD)
        val people = requireNotNull(context.peopleCount) { "peopleCount is required" }

        val tip = when (context.tipMode) {
            TipMode.PERCENT -> {
                val percent = context.tipPercent ?: 0
                Tip(mode = TipMode.PERCENT, percent = percent)
            }

            TipMode.ABSOLUTE -> {
                val abs = context.tipAbsolute ?: Money.zero(Currency.CAD)
                Tip(mode = TipMode.ABSOLUTE, absolute = abs)
            }

            TipMode.NONE, null -> {
                Tip(mode = TipMode.NONE)
            }
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


    private fun convertWithRate(cad: Money, rate: BigDecimal): Money {
        val krwAmount = cad.amount.multiply(rate)
            .setScale(2, RoundingMode.HALF_UP)
        return Money.of(krwAmount, Currency.KRW)
    }

    private fun invalidNumber(
        step: ConversationStep,
        message: String,
        context: ConversationContext
    ): ConversationOutput {
        return retry(step, message, context)
    }

    private fun retry(
        step: ConversationStep,
        reason: String,
        context: ConversationContext
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
                context = context.copy(
                    failureCount = 0,
                    lastStep = step
                )
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
                "$reason\n분배 방식을 입력해주세요. 1) N분의 1"

            ConversationStep.ASK_PEOPLE_COUNT ->
                "$reason\n인원 수를 입력해주세요. (예: 3)"

            ConversationStep.ASK_EXCHANGE_RATE_MODE ->
                "$reason\n환율 및 통화 선택:\n1) 오늘 환율 자동 조회 (CAD → KRW)\n2) 환율 직접 입력 (예: 1000)\n3) KRW 변환 없이 CAD만 보기\n번호를 선택해주세요: "

            ConversationStep.ASK_EXCHANGE_RATE_VALUE ->
                "$reason\n환율을 숫자로 입력해주세요. 예) 1000"

            // 🔽 새로 추가
            ConversationStep.ASK_MENU_ITEMS ->
                "$reason\n메뉴를 입력해주세요.\n예) 파스타 18.9; 피자 22; 콜라 3"

            ConversationStep.ASK_MENU_PARTICIPANTS ->
                "$reason\n참가자 이름을 쉼표로 구분해 입력해주세요.\n예) 민지, 철수, 영희"

            ConversationStep.ASK_MENU_ASSIGNMENTS ->
                "$reason\n메뉴별로 누가 먹었는지 지정해주세요.\n예) m1:p1,p2; m2:p2; m3:p1,p3"

            ConversationStep.SHOW_RESULT ->
                reason

            ConversationStep.RESTART_CONFIRM ->
                reason
        }

        return ConversationOutput(
            nextStep = step,
            message = msg,
            context = context.copy(failureCount = newCount)
        )
    }

    private fun formatMoney(m: Money): String {
        val plain = m.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

        return when (m.currency) {
            Currency.CAD -> {
                "$plain CAD"
            }

            Currency.KRW -> {
                val withComma = formatWithComma(plain)
                "$withComma KRW"
            }
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

    private fun formatRate(rate: BigDecimal): String {
        val scaled = rate.setScale(0, RoundingMode.HALF_UP).toPlainString()
        val withComma = formatWithComma("$scaled.00")
        return withComma.substringBefore(".")
    }
}
