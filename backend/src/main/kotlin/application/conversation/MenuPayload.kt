package application.conversation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private data class MenuPayload(
    val items: List<MenuPayloadItem>,
    val participants: List<MenuPayloadParticipant>,
    val assignments: List<MenuPayloadAssignment>
)

private data class MenuPayloadItem(val id: String, val name: String, val price: String)
private data class MenuPayloadParticipant(val id: String, val name: String)
private data class MenuPayloadAssignment(val menuId: String, val participantIds: List<String>)

public fun parseMenuPayload(input: String): Triple<List<MenuItemInput>, List<MenuParticipantInput>, Map<String, List<String>>>? {
    return runCatching {
        val json = input.removePrefix("MENU_PAYLOAD:").trim()

        val mapper = jacksonObjectMapper()
        val payload: MenuPayload = mapper.readValue(json)

        val items = payload.items.map {
            MenuItemInput(
                id = it.id,
                name = it.name,
                priceCad = it.price.replace(",", "").toBigDecimal()
            )
        }

        val participants = payload.participants.map {
            MenuParticipantInput(
                id = it.id,
                name = it.name
            )
        }

        val assignments = payload.assignments.associate { it.menuId to it.participantIds }

        Triple(items, participants, assignments)
    }.getOrNull()
}
