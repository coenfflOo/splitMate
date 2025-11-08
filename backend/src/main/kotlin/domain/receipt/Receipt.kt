package domain.receipt

import domain.money.Money

class Receipt(
    val baseAmount: Money,
    val tax: Tax,
    val tip: Tip?
) {
    fun totalWithTip(): Money {
        throw NotImplementedError("Receipt.totalWithTip not implemented yet")
    }
}
