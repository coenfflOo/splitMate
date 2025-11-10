package application.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationEngineValidationTest {

    @Test
    fun `총 금액이 0이나 음수면 다시 입력을 요구한다`() {
        val engine = ConversationEngine()
        var out = engine.start()

        out = engine.handle(out.nextStep, "0", out.context)
        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, out.nextStep)
        assertTrue(out.message.contains("0보다 큰 금액을 입력해주세요"))

        out = engine.handle(out.nextStep, "-10", out.context)
        assertEquals(ConversationStep.ASK_TOTAL_AMOUNT, out.nextStep)
    }

    @Test
    fun `인원 수가 0이나 음수거나 정수가 아니면 다시 입력을 요구하고 여러 번 틀리면 재시작 여부를 묻는다`() {
        val engine = ConversationEngine()
        var out = engine.start()

        out = engine.handle(out.nextStep, "10.00", out.context)
        out = engine.handle(out.nextStep, "0", out.context)
        out = engine.handle(out.nextStep, "3", out.context)
        out = engine.handle(out.nextStep, "1", out.context)

        out = engine.handle(out.nextStep, "0", out.context)
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)
        assertTrue(out.message.contains("인원 수는 1 이상의 정수로 입력해주세요"))

        out = engine.handle(out.nextStep, "-1", out.context)
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, out.nextStep)

        // 3번째 잘못된 인원 수 입력 → 이제 RESTART_CONFIRM 로 가야 함
        out = engine.handle(out.nextStep, "1.5", out.context)
        assertEquals(ConversationStep.RESTART_CONFIRM, out.nextStep)
        assertTrue(out.message.contains("처음부터 다시 시작하시겠습니까"))
    }
}
