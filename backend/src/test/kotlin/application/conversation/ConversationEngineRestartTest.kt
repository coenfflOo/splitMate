package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationEngineRestartTest {

    @Test
    fun `같은 단계에서 3번 틀리면 RESTART_CONFIRM 단계로 전환된다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()

        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertTrue(out.message.contains("처음부터 다시 시작하시겠습니까"))
    }

    @Test
    fun `RESTART_CONFIRM에서 Y를 입력하면 완전히 처음 상태로 돌아간다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()

        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        out = engine.handle(out.nextStep, "Y", out.context)

        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, out.nextStep)
        assertTrue(out.message.contains("총 결제 금액을 입력해주세요"))

        kotlin.test.assertEquals(null, out.context.baseAmount)
    }

    @Test
    fun `RESTART_CONFIRM에서 N을 입력하면 분배 방식 선택으로 돌아간다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()

        out = engine.handle(out.nextStep, "abc", out.context)
        out = engine.handle(out.nextStep, "def", out.context)
        out = engine.handle(out.nextStep, "ghi", out.context)

        out = engine.handle(out.nextStep, "N", out.context)

        assertEquals(ConversationStep.ASK_SPLIT_MODE, out.nextStep)
        assertTrue(out.message.contains("분배 방식을 선택해주세요"))
    }
}
