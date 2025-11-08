package domain.receipt

import domain.money.Money

class Receipt(
    val baseAmount: Money,
    val tax: Tax,
    val tip: Tip?
) {

    fun subtotal(): Money =
        baseAmount + tax.amount

    fun totalWithTip(): Money {
        val subtotal = subtotal()
        val tipAmount = tip?.calculate(subtotal)
            ?: Money.zero(baseAmount.currency)

        return subtotal + tipAmount
    }
}
