package domain.menu

/**
 * 한 메뉴(MenuItem)를 어떤 참가자들이 함께 먹었는지 표현.
 * participants 가 1명이면 혼자 먹은 메뉴,
 * 여러 명이면 균등 분배한다.
 */
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
