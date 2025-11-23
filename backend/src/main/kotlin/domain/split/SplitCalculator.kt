package domain.split

import domain.menu.MenuAssignment
import domain.menu.Participant
import domain.money.Currency
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

        val base = receipt.baseAmount
        val taxMoney = receipt.tax.amount
        val basePlusTax = base + taxMoney
        val tipMoney = receipt.tip?.amountOn(basePlusTax) ?: Money.zero(currency)

        val totalAmount = base.amount
            .add(taxMoney.amount)
            .add(tipMoney.amount)

        val total = Money.of(totalAmount, currency)

        val totalBaseAmount = base.amount
        require(totalBaseAmount >= BigDecimal.ZERO) {
            "base amount must be >= 0"
        }

        val rawShares = subtotals.entries.map { (participant, subtotalMoney) ->
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

        val adjustedShares = adjustSharesForRounding(
            total = total,
            shares = rawShares,
            currency = currency
        )

        return MenuSplitResult(
            total = total,
            shares = adjustedShares
        )
    }

    private fun validatePeopleCount(peopleCount: Int) {
        require(peopleCount > 0) { "peopleCount must be > 0" }
    }

    private fun adjustSharesForRounding(
        total: Money,
        shares: List<PerPersonShare>,
        currency: Currency
    ): List<PerPersonShare> {
        if (shares.isEmpty()) return shares

        val totalExpected = total.amount.setScale(2, RoundingMode.HALF_UP)

        val totalActual = shares
            .map { it.total.amount }
            .fold(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP)

        var diff = totalExpected.subtract(totalActual)

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return shares
        }

        var centDiff = diff
            .multiply(BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

        if (centDiff == 0) {
            return shares
        }

        val adjusted = shares.toMutableList()

        val sortedIndexes = shares
            .indices
            .sortedByDescending { shares[it].total.amount }

        var idx = 0
        val sign = if (centDiff > 0) 1 else -1

        while (centDiff != 0) {
            val targetIndex = sortedIndexes[idx % sortedIndexes.size]
            val target = adjusted[targetIndex]

            val delta = BigDecimal(sign).divide(BigDecimal("100"))

            val deltaMoney = Money.of(delta, currency)

            val newTipShare = target.tipShare + deltaMoney
            val newTotal = target.total + deltaMoney

            adjusted[targetIndex] = target.copy(
                tipShare = newTipShare,
                total = newTotal
            )

            centDiff -= sign
            idx += 1
        }

        return adjusted
    }
}
