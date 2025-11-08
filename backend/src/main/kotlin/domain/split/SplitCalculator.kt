package domain.split

import domain.money.Money
import domain.receipt.Receipt

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

    private fun validatePeopleCount(peopleCount: Int) {
        require(peopleCount > 0) { "peopleCount must be > 0" }
    }
}
