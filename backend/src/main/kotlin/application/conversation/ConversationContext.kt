package application.conversation

import domain.conversation.ConversationStep
import domain.money.Money
import domain.receipt.TipMode
import domain.split.SplitMode
import java.math.BigDecimal

data class ConversationContext(
    val baseAmount: Money? = null,
    val taxAmount: Money? = null,

    val tipMode: TipMode? = null,
    val tipPercent: Int? = null,
    val tipAbsolute: Money? = null,

    val peopleCount: Int? = null,

    val splitMode: SplitMode? = null,

    val menuItems: List<MenuItemInput> = emptyList(),
    val menuParticipants: List<MenuParticipantInput> = emptyList(),
    val menuAssignments: Map<String, List<String>> = emptyMap(),

    val wantKrw: Boolean = false,
    val manualRate: BigDecimal? = null,

    val failureCount: Int = 0,
    val lastStep: ConversationStep? = null
)

data class MenuItemInput(
    val id: String,
    val name: String,
    val priceCad: BigDecimal
)

data class MenuParticipantInput(
    val id: String,
    val name: String
)
