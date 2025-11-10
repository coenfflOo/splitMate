package domain.menu

/**
 * 메뉴별 계산에 참여하는 사람
 */
data class Participant(
    val id: String,
    val displayName: String = id
)
