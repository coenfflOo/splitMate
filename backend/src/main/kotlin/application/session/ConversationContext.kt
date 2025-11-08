package application.session

import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.split.SplitResult

data class ConversationContext(
    val baseAmount: Money? = null,
    val tax: Tax? = null,
    val tip: Tip? = null,
    val receipt: Receipt? = null,
    val splitResult: SplitResult? = null,
    val peopleCount: Int? = null
)
