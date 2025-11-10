package domain.split

import domain.menu.MenuAssignment
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

    fun splitByMenu(
        receipt: Receipt,
        assignments: List<MenuAssignment>
    ): MenuSplitResult {
        throw NotImplementedError("splitByMenu is not implemented yet")
    }

    private fun validatePeopleCount(peopleCount: Int) {
        require(peopleCount > 0) { "peopleCount must be > 0" }
    }
}
