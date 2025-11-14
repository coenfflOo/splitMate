package application.conversation

import domain.conversation.ConversationStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationEngineRestartTest {

    @Test
    fun `같은 단계에서 3번 틀리면 RESTART_CONFIRM 단계로 전환된다`() {
        val engine = ConversationEngine()
        var out = engine.start()

        // ASK_TOTAL_AMOUNT 단계에서 일부러 3번 잘못 입력
        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertTrue(out.message.contains("처음부터 다시 시작하시겠습니까"))
    }

    @Test
    fun `RESTART_CONFIRM에서 Y를 입력하면 완전히 처음 상태로 돌아간다`() {
        val engine = ConversationEngine()
        var out = engine.start()

        // 일부 입력 후, RESTART_CONFIRM까지 유도
        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        // 이제 RESTART_CONFIRM 단계에서 Y
        out = engine.handle(out.nextStep, "Y", out.context)

        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, out.nextStep)
        assertTrue(out.message.contains("총 결제 금액을 입력해주세요"))

        // 새 컨텍스트라 baseAmount 등은 초기 상태여야 함
        kotlin.test.assertEquals(null, out.context.baseAmount)
    }

    @Test
    fun `RESTART_CONFIRM에서 N을 입력하면 직전 단계로 돌아간다`() {
        val engine = ConversationEngine()
        var out = engine.start()

        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        // N 입력
        out = engine.handle(out.nextStep, "N", out.context)

        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, out.nextStep)
        assertTrue(out.message.contains("총 결제 금액을 입력해주세요"))
    }
}
