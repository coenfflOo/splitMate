package application.conversation

import application.conversation.flow.ConsoleConversationFlow
import application.conversation.model.ConversationContext
import application.conversation.model.ConversationStep
import domain.money.Currency
import domain.money.Money
import domain.receipt.TipMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TipModeStoreTest {

    @Test
    fun `% 모드 선택 후 퍼센트 값이 context에 저장된다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()
        var ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "27.40", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "2.60", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "1", ctx)
        ctx = out.context as ConversationContext

        assertThat(ctx.tipMode).isEqualTo(TipMode.PERCENT)

        out = engine.handle(ConversationStep.ASK_TIP_VALUE, "15", ctx)
        ctx = out.context as ConversationContext

        assertThat(ctx.tipMode).isEqualTo(TipMode.PERCENT)
        assertThat(ctx.tipPercent).isEqualTo(15)
        assertThat(ctx.tipAbsolute).isNull()
    }

    @Test
    fun `ABSOLUTE 모드 선택 후 금액이 context에 저장된다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()
        var ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "27.40", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "2.60", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "2", ctx)
        ctx = out.context as ConversationContext

        assertThat(ctx.tipMode).isEqualTo(TipMode.ABSOLUTE)

        out = engine.handle(ConversationStep.ASK_TIP_VALUE, "10.00", ctx)
        ctx = out.context as ConversationContext

        assertThat(ctx.tipMode).isEqualTo(TipMode.ABSOLUTE)
        assertThat(ctx.tipAbsolute).isEqualTo(
            Money.of(BigDecimal("10.00"), Currency.CAD)
        )
        assertThat(ctx.tipPercent).isNull()
    }

    @Test
    fun `팁 없음 선택 시 NONE 모드로 저장된다`() {
        val engine = ConsoleConversationFlow()
        var out = engine.start()
        var ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "27.40", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "0", ctx)
        ctx = out.context as ConversationContext

        out = engine.handle(out.nextStep, "3", ctx)
        ctx = out.context as ConversationContext

        assertThat(out.nextStep).isEqualTo(ConversationStep.ASK_PEOPLE_COUNT)
        assertThat(ctx.tipMode).isEqualTo(TipMode.NONE)
        assertThat(ctx.tipPercent).isEqualTo(0)
        assertThat(ctx.tipAbsolute).isNull()
    }
}
