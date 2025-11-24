package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationContext
import application.conversation.model.ConversationStep
import domain.money.Currency
import domain.money.Money
import domain.receipt.TipMode
import domain.split.SplitMode
import kotlin.test.Test
import kotlin.test.assertTrue
import java.math.BigDecimal

class ConversationEngineTipAbsoluteTest {

    @Test
    fun `팁 금액 모드로 10달러 입력 - KRW 자동 환산 포함 요약`() {
        val engine = ConsoleConversationFlow()

        val ctx = ConversationContext(
            baseAmount = Money.of(BigDecimal("27.40"), Currency.CAD),
            taxAmount = Money.of(BigDecimal("2.60"), Currency.CAD),
            tipMode = TipMode.ABSOLUTE,
            tipAbsolute = Money.of(BigDecimal("10.00"), Currency.CAD),
            peopleCount = 2,
            splitMode = SplitMode.N_DIVIDE,
            wantKrw = true,
            manualRate = BigDecimal("1000")
        )

        val out = engine.handle(ConversationStep.SHOW_RESULT, "", ctx)
        val msg = out.message

        assertTrue(msg.contains("총 금액: 40.00 CAD"))
        assertTrue(msg.contains("1인당: 20.00 CAD"))
        assertTrue(msg.contains("1 CAD = 1,000 KRW"))
        assertTrue(msg.contains("20,000.00 KRW"))
    }

    @Test
    fun `팁 없음 모드(3) 선택 시 팁 0원 처리`() {
        val engine = ConsoleConversationFlow()

        val ctx = ConversationContext(
            baseAmount = Money.of(BigDecimal("10.00"), Currency.CAD),
            taxAmount = Money.zero(Currency.CAD),
            tipMode = TipMode.NONE,
            peopleCount = 1,
            splitMode = SplitMode.N_DIVIDE,
            wantKrw = true,
            manualRate = BigDecimal("1000")
        )

        val out = engine.handle(ConversationStep.SHOW_RESULT, "", ctx)
        val msg = out.message

        assertTrue(msg.contains("총 금액: 10.00 CAD"))
        assertTrue(msg.contains("1인당: 10.00 CAD"))
        assertTrue(msg.contains("10,000.00 KRW"))
    }
}