package domain.menu

data class MenuAssignment(
    val menuItem: MenuItem,
    val participants: List<Participant>
) {
    init {
        require(participants.isNotEmpty()) {
            "MenuAssignment must have at least one participant"
        }
    }
}
