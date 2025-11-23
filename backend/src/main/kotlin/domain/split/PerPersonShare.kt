package domain.split

import domain.menu.Participant
import domain.money.Money

data class PerPersonShare(
    val participant: Participant,
    val subtotal: Money,
    val taxShare: Money,
    val tipShare: Money,
    val total: Money
)
