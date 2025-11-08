package adapter.console

class StdConsoleIO : ConsoleIO {
    override fun readLine(): String? = kotlin.io.readLine()
    override fun println(message: String) {
        kotlin.io.println(message)
    }
}