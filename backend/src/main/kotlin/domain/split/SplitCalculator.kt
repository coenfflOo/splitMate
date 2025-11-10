package domain.split

import domain.menu.MenuAssignment
import domain.menu.Participant
import domain.money.Money
import domain.receipt.Receipt
import java.math.BigDecimal
import java.math.RoundingMode

object SplitCalculator {

    fun splitEvenly(receipt: Receipt, peopleCount: Int): SplitResult {
        validatePeopleCount(peopleCount)

        val total = receipt.totalWithTip()
        val perPerson = total.divideBy(peopleCount)

        return SplitResult(
            total = total,
            peopleCount = peopleCount,
            perPerson = perPerson
        )
    }

    fun splitByMenu(
        receipt: Receipt,
        assignments: List<MenuAssignment>
    ): MenuSplitResult {
        require(assignments.isNotEmpty()) {
            "assignments must not be empty"
        }

        val currency = receipt.baseAmount.currency

        // 1. 사람별 메뉴 소계 계산
        val subtotals = mutableMapOf<Participant, Money>()

        assignments.forEach { assignment ->
            val participants = assignment.participants
            require(participants.isNotEmpty()) {
                "MenuAssignment must have at least one participant"
            }

            val shareCount = participants.size
            val perPersonPrice = assignment.menuItem.price.divideBy(shareCount)

            participants.forEach { participant ->
                val current = subtotals[participant] ?: Money.zero(currency)
                subtotals[participant] = current + perPersonPrice
            }
        }

        // 2. 총액 구성 요소 계산
        val base = receipt.baseAmount
        val taxMoney = receipt.tax.amount
        val basePlusTax = base + taxMoney
        val tipMoney = receipt.tip?.amountOn(basePlusTax) ?: Money.zero(currency)

        val totalAmount = base.amount
            .add(taxMoney.amount)
            .add(tipMoney.amount)

        val total = Money.of(totalAmount, currency)

        // baseAmount가 0이면 세금/팁은 모두 0이어야 함
        val totalBaseAmount = base.amount
        require(totalBaseAmount >= BigDecimal.ZERO) {
            "base amount must be >= 0"
        }

        // 3. 사람별 비례 분배
        val shares = subtotals.entries.map { (participant, subtotalMoney) ->
            val subtotalAmount = subtotalMoney.amount

            val ratio =
                if (totalBaseAmount == BigDecimal.ZERO) {
                    BigDecimal.ZERO
                } else {
                    subtotalAmount
                        .divide(totalBaseAmount, 4, RoundingMode.HALF_UP)
                }

            val taxShareAmount = taxMoney.amount
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP)

            val tipShareAmount = tipMoney.amount
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP)

            val taxShare = Money.of(taxShareAmount, currency)
            val tipShare = Money.of(tipShareAmount, currency)
            val totalShare = subtotalMoney + taxShare + tipShare

            PerPersonShare(
                participant = participant,
                subtotal = subtotalMoney,
                taxShare = taxShare,
                tipShare = tipShare,
                total = totalShare
            )
        }

        return MenuSplitResult(
            total = total,
            shares = shares
        )
    }

    private fun validatePeopleCount(peopleCount: Int) {
        require(peopleCount > 0) { "peopleCount must be > 0" }
    }
}
