package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ConversationEngineTest {

    private val engine = ConsoleConversationFlow()

    @Test
    fun `대화 시작시 총 금액을 물어본다`() {
        val output = engine.start()

        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, output.nextStep)
        assertTrue(output.message.contains("총 결제 금액"))
    }

    @Test
    fun `행복 경로 - 총액부터 1인당 금액 계산까지 흐름이 이어진다`() {
        val engine = ConsoleConversationFlow()

        var out = engine.start()

        out = engine.handle(out.nextStep, "27.40", out.context) // 총액
        assertEquals(ConversationStep.ASK_TAX, out.nextStep)

        out = engine.handle(out.nextStep, "2.60", out.context)  // 세금
        assertEquals(ConversationStep.ASK_TIP_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "1", out.context)     // 팁 모드: 퍼센트
        assertEquals(ConversationStep.ASK_TIP_VALUE, out.nextStep)

        out = engine.handle(out.nextStep, "10", out.context)    // 팁 값: 10%
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)     // 인원: 3
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)

        assertEquals(ConversationStep.ASK_SPLIT_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "1", out.context)
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_MODE, out.nextStep)

        out = engine.handle(out.nextStep, "3", out.context)

        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertFalse(out.isFinished)

        val msg = out.message
        assertTrue(msg.contains("1인당"))
        assertTrue(msg.contains("11.00"))
    }

    @Test
    fun `총액 입력 단계에서 숫자가 아닌 값을 넣으면 같은 질문을 반복한다`() {
        val start = engine.start()
        val ctx0 = start.context

        val result = engine.handle(
            step = ConversationStep.ASK_TOTAL_AMOUNT,
            input = "abc",
            context = ctx0
        )

        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, result.nextStep)
        assertTrue(result.message.contains("숫자"))
        assertFalse(result.isFinished)
    }
}