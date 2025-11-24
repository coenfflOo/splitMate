package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationContext
import application.conversation.model.ConversationStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConversationNumericInputTest {

    @Test
    fun `팁 퍼센트 단계에서 숫자가 아닌 값 입력 시 재질문한다`() {
        val engine = ConsoleConversationFlow()
        var output = engine.start()
        var ctx = output.context as ConversationContext

        output = engine.handle(output.nextStep, "27.40", ctx)
        ctx = output.context as ConversationContext

        output = engine.handle(output.nextStep, "2.60", ctx)
        ctx = output.context as ConversationContext

        output = engine.handle(output.nextStep, "1", ctx)
        ctx = output.context as ConversationContext
        assertThat(output.nextStep).isEqualTo(ConversationStep.ASK_TIP_VALUE)
        assertThat(ctx.tipMode).isNotNull

        output = engine.handle(ConversationStep.ASK_TIP_VALUE, "abc", ctx)

        assertThat(output.nextStep).isEqualTo(ConversationStep.ASK_TIP_VALUE)
        assertThat(output.message).containsAnyOf("퍼센트", "숫자", "다시 입력")
        val newCtx = output.context as ConversationContext
        assertThat(newCtx.failureCount).isEqualTo(1)
    }

    @Test
    fun `인원 수 단계에서 0이나 음수 입력 시 재질문한다`() {
        val engine = ConsoleConversationFlow()
        val ctx = ConversationContext()

        val output = engine.handle(
            ConversationStep.ASK_PEOPLE_COUNT,
            "0",
            ctx
        )

        assertThat(output.nextStep).isEqualTo(ConversationStep.ASK_PEOPLE_COUNT)
        assertThat(output.message).contains("인원 수는 1 이상의 정수로 입력해주세요")
        val newCtx = output.context as ConversationContext
        assertThat(newCtx.failureCount).isEqualTo(1)
    }

    @Test
    fun `환율 수동 입력 단계에서 숫자가 아니면 재질문한다`() {
        val engine = ConsoleConversationFlow()
        val ctx = ConversationContext(wantKrw = true)

        val output = engine.handle(
            ConversationStep.ASK_EXCHANGE_RATE_VALUE,
            "aaa",
            ctx
        )

        assertThat(output.nextStep).isEqualTo(ConversationStep.ASK_EXCHANGE_RATE_VALUE)
        assertThat(output.message).contains("숫자로 입력해주세요")
        val newCtx = output.context as ConversationContext
        assertThat(newCtx.failureCount).isEqualTo(1)
    }
}
