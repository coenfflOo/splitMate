package application.conversation

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

        // start
        val start = engine.start()
        val ctx0 = start.context

        val step1 = engine.handle(
            step = ConversationStep.ASK_TOTAL_AMOUNT,
            input = "27.40",
            context = ctx0
        )
        val ctx1 = step1.context
        assertEquals(ConversationStep.ASK_TAX, step1.nextStep)

        val step2 = engine.handle(
            step = ConversationStep.ASK_TAX,
            input = "2.60",
            context = ctx1
        )
        val ctx2 = step2.context
        assertEquals(ConversationStep.ASK_TIP_MODE, step2.nextStep)

        val step3 = engine.handle(
            step = ConversationStep.ASK_TIP_MODE,
            input = "1",
            context = ctx2
        )
        val ctx3 = step3.context
        assertEquals(ConversationStep.ASK_TIP_VALUE, step3.nextStep)

        val step4 = engine.handle(
            step = ConversationStep.ASK_TIP_VALUE,
            input = "10",
            context = ctx3
        )
        val ctx4 = step4.context
        assertEquals(ConversationStep.ASK_SPLIT_MODE, step4.nextStep)

        val step5 = engine.handle(
            step = ConversationStep.ASK_SPLIT_MODE,
            input = "1",
            context = ctx4
        )
        val ctx5 = step5.context
        assertEquals(ConversationStep.ASK_PEOPLE_COUNT, step5.nextStep)

        val step6 = engine.handle(
            step = ConversationStep.ASK_PEOPLE_COUNT,
            input = "3",
            context = ctx5
        )
        val ctx6 = step6.context
        // ✅ 여기서는 아직 환율 모드 선택 단계여야 한다
        assertEquals(ConversationStep.ASK_EXCHANGE_RATE_MODE, step6.nextStep)

        // 환율은 신경 안 쓰고 CAD만 보기 → 3번 선택
        val step7 = engine.handle(
            step = step6.nextStep,
            input = "3",
            context = ctx6
        )

        assertEquals(ConversationStep.SHOW_RESULT, step7.nextStep)
        assertTrue(step7.isFinished)

        val msg = step7.message
        assertTrue(msg.contains("1인당"))
        assertTrue(msg.contains("11.00"))  // 33 / 3 = 11.00
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
