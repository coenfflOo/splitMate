package domain.split

import domain.money.Money

data class MenuSplitResult(
    val total: Money,
    val shares: List<PerPersonShare>
)
