package adapter.console

interface ConsoleIO {
    fun readLine(): String?
    fun println(message: String)
}
