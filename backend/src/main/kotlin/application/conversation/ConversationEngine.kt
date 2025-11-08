package application.conversation

import application.session.ConversationContext
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator

class ConversationEngine {

    fun start(): ConversationOutput {
        val context = ConversationContext()
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = "총 결제 금액을 입력해주세요 (예: 27.40)",
            context = context
        )
    }

    fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (step) {
            ConversationStep.ASK_TOTAL_AMOUNT ->
                handleTotalAmount(input, context)

            ConversationStep.ASK_TAX ->
                handleTax(input, context)

            ConversationStep.ASK_TIP_MODE ->
                handleTipMode(input, context)

            ConversationStep.ASK_TIP_VALUE ->
                handleTipValue(input, context)

            ConversationStep.ASK_SPLIT_MODE ->
                handleSplitMode(input, context)

            ConversationStep.ASK_PEOPLE_COUNT ->
                handlePeopleCount(input, context)

            ConversationStep.SHOW_RESULT,
            ConversationStep.FINISHED ->
                ConversationOutput(
                    nextStep = ConversationStep.FINISHED,
                    message = "이미 계산이 완료되었습니다.",
                    context = context,
                    isFinished = true
                )
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

        if (amount <= java.math.BigDecimal.ZERO) {
            return invalidNumber(
                step = ConversationStep.ASK_TOTAL_AMOUNT,
                message = "0보다 큰 금액을 입력해주세요.",
                context = context
            )
        }

        val baseMoney = Money.of(amount, Currency.CAD)
        val newContext = context.copy(baseAmount = baseMoney)

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TAX,
            message = "세금 금액을 입력해주세요. 없으면 0을 입력하세요.",
            context = newContext
        )
    }

    private fun handleTax(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val value = input.toBigDecimalOrNull()
            ?: return invalidNumber(
                step = ConversationStep.ASK_TAX,
                message = "세금 금액은 숫자로 입력해주세요.",
                context = context
            )

        if (value < java.math.BigDecimal.ZERO) {
            return invalidNumber(
                step = ConversationStep.ASK_TAX,
                message = "세금 금액은 0 이상이어야 합니다.",
                context = context
            )
        }

        val taxMoney = Money.of(value, Currency.CAD)
        val tax = Tax(taxMoney)

        val newContext = context.copy(tax = tax)

        return ConversationOutput(
            nextStep = ConversationStep.ASK_TIP_MODE,
            message = "팁 입력 방식을 선택해주세요. 1) 퍼센트(%)  2) 금액($)  3) 없음",
            context = newContext
        )
    }

    private fun handleTipMode(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (input.trim()) {
            "1" -> ConversationOutput(
                nextStep = ConversationStep.ASK_TIP_VALUE,
                message = "팁 퍼센트를 입력해주세요. (예: 15)",
                context = context.copy()
            )

            "2" -> ConversationOutput(
                nextStep = ConversationStep.ASK_TIP_VALUE,
                message = "팁 금액을 입력해주세요. (예: 5.25)",
                context = context
            )

            "3" -> {
                val base = requireNotNull(context.baseAmount) { "baseAmount is required" }
                val tax = requireNotNull(context.tax) { "tax is required" }
                val receipt = Receipt(base, tax, null)
                val newContext = context.copy(receipt = receipt)

                ConversationOutput(
                    nextStep = ConversationStep.ASK_SPLIT_MODE,
                    message = "분배 방식을 선택해주세요. 1) N분의 1",
                    context = newContext
                )
            }

            else -> ConversationOutput(
                nextStep = ConversationStep.ASK_TIP_MODE,
                message = "1, 2, 3 중에서 선택해주세요.",
                context = context
            )
        }
    }

    private fun handleTipValue(
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        val value = input.toBigDecimalOrNull()
            ?: return ConversationOutput(
                nextStep = ConversationStep.ASK_TIP_VALUE,
                message = "팁 값은 숫자로 입력해주세요.",
                context = context
            )

        val base = requireNotNull(context.baseAmount) { "baseAmount is required" }
        val tax = requireNotNull(context.tax) { "tax is required" }

        val tip = Tip(TipMode.PERCENT, value)

        val receipt = Receipt(base, tax, tip)
        val newContext = context.copy(
            tip = tip,
            receipt = receipt
        )

        return ConversationOutput(
            nextStep = ConversationStep.ASK_SPLIT_MODE,
            message = "분배 방식을 선택해주세요. 1) N분의 1",
            context = newContext
        )
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
        val count = input.toIntOrNull()
            ?: return invalidNumber(
                step = ConversationStep.ASK_PEOPLE_COUNT,
                message = "인원 수는 정수로 입력해주세요.",
                context = context
            )

        if (count <= 0) {
            return invalidNumber(
                step = ConversationStep.ASK_PEOPLE_COUNT,
                message = "1명 이상이어야 합니다.",
                context = context
            )
        }

        val receipt = requireNotNull(context.receipt) { "receipt is required" }
        val splitResult = SplitCalculator.splitEvenly(receipt, count)

        val newContext = context.copy(
            peopleCount = count,
            splitResult = splitResult
        )

        val msg = buildString {
            appendLine("계산이 완료되었습니다.")
            appendLine("총 금액: ${splitResult.total.amount} ${splitResult.total.currency}")
            appendLine("인원 수: ${splitResult.peopleCount}명")
            appendLine("1인당: ${splitResult.perPerson.amount} ${splitResult.perPerson.currency}")
        }

        return ConversationOutput(
            nextStep = ConversationStep.SHOW_RESULT,
            message = msg,
            context = newContext,
            isFinished = true
        )
    }

    private fun invalidNumber(
        step: ConversationStep,
        message: String,
        context: ConversationContext
    ): ConversationOutput {
        return ConversationOutput(
            nextStep = step,
            message = message,
            context = context
        )
    }
}
