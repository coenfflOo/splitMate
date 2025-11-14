package application.conversation

import domain.conversation.ConversationStep
import domain.money.Money
import domain.receipt.TipMode
import java.math.BigDecimal

data class ConversationContext(
    val baseAmount: Money? = null,          // 총 결제 금액 (CAD)
    val taxAmount: Money? = null,           // 세금 금액 (CAD)

    val tipMode: TipMode? = null,           // PERCENT / ABSOLUTE / NONE
    val tipPercent: Int? = null,            // 퍼센트 모드일 때 값 (예: 15)
    val tipAbsolute: Money? = null,         // 금액 모드일 때 값 (예: $10.00)

    val peopleCount: Int? = null,           // 인원 수

    val wantKrw: Boolean = false,           // KRW로도 보고 싶은지
    val manualRate: BigDecimal? = null,      // 1 CAD = ? KRW (수동 또는 자동 조회 값)

    val failureCount: Int = 0,
    val lastStep: ConversationStep? = null
)