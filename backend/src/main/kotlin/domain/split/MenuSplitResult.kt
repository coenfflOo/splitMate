package domain.split

import domain.money.Money

/**
 * 메뉴별 분배 전체 결과
 */
data class MenuSplitResult(
    val total: Money,                 // 전체 금액 (세금 + 팁 포함)
    val shares: List<PerPersonShare>  // 사람별 부담 금액
)
