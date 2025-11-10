package application.session

import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.split.SplitResult
import java.math.BigDecimal

data class ConversationContext(
    val baseAmount: Money? = null,
    val tax: Tax? = null,
    val tip: Tip? = null,
    val receipt: Receipt? = null,
    val splitResult: SplitResult? = null,
    val peopleCount: Int? = null,

    // 새로 추가된 환율 관련 상태
    val wantKrw: Boolean = false,          // KRW 금액을 보고 싶은지 여부
    val manualRate: BigDecimal? = null     // 수동 입력 환율 (1 CAD -> KRW)
)
