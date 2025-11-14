package application.group

@JvmInline
value class RoomId(val value: String) {
    init {
        require(value.isNotBlank()) { "RoomId must not be blank" }
    }

    override fun toString(): String = value
}