package domain.split

import domain.money.Money

data class SplitResult(
    val total: Money,
    val peopleCount: Int,
    val perPerson: Money
)
