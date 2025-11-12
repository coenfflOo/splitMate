package domain.menu

class Participant(
    val id: String,
    val displayName: String = id
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Participant) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Participant(id=$id, displayName=$displayName)"
}
