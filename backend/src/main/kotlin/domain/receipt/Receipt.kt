package domain.receipt

import domain.money.Money

class Receipt(
    val baseAmount: Money,
    val tax: Tax,
    val tip: Tip?
) {

    /**
     * base + tax + tip 을 모두 포함한 최종 금액을 반환한다.
     * 퍼센트 팁일 경우 (base + tax)를 기준으로 계산한다.
     */
    fun totalWithTip(): Money {
        val subtotal = baseAmount + tax.amount
        val tipAmount = tip?.calculate(subtotal)
            ?: Money.zero(baseAmount.currency)

        return subtotal + tipAmount
    }
}
