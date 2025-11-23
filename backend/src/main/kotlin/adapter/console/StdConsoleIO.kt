package adapter.console

class StdConsoleIO : ConsoleIO {
    override fun readLine(): String? = readlnOrNull()
    override fun println(message: String) {
        kotlin.io.println(message)
    }
}