package application.group

@JvmInline
value class MemberId(val value: String) {
    init {
        require(value.isNotBlank()) { "MemberId must not be blank" }
    }

    override fun toString(): String = value
}