//package domain.receipt
//
//import domain.money.Money
//
//class Receipt(
//    val baseAmount: Money,
//    val tax: Tax,
//    val tip: Tip?
//) {
//
//    fun subtotal(): Money =
//        baseAmount + tax.amount
//
//    fun totalWithTip(): Money {
//        val subtotal = subtotal()
//        val tipAmount = tip?.calculate(subtotal)
//            ?: Money.zero(baseAmount.currency)
//
//        return subtotal + tipAmount
//    }
//}
package domain.receipt

import domain.money.Money
import kotlin.plus

data class Receipt(
    val baseAmount: Money,
    val tax: Tax,
    val tip: Tip? = null,
) {
    fun subtotal(): Money = baseAmount + tax.amount
    fun totalWithTip(): Money {
        val subtotal = subtotal()
        val tipAmount = tip?.amountOn(subtotal)
            ?: Money.zero(baseAmount.currency)

        return subtotal + tipAmount
    }
}
