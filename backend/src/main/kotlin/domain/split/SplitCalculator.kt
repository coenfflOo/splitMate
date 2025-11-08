package domain.split

import domain.receipt.Receipt

object SplitCalculator {

    fun splitEvenly(receipt: Receipt, peopleCount: Int): SplitResult {
        require(peopleCount > 0) { "peopleCount must be > 0" }

        val total = receipt.totalWithTip()
        val perPerson = total.divideBy(peopleCount)

        return SplitResult(
            total = total,
            peopleCount = peopleCount,
            perPerson = perPerson
        )
    }
}

