package adapter.console

import application.conversation.ConversationEngine
import application.session.ConversationContext

class ConsoleApp(
    private val engine: ConversationEngine,
    private val io: ConsoleIO
) {

    fun run() {
        printWelcome()

        var output = engine.start()
        var currentStep = output.nextStep
        var context: ConversationContext = output.context

        io.println(output.message)

        while (!output.isFinished) {
            val input = io.readLine()

            if (input == null) {
                io.println("입력이 감지되지 않아 프로그램을 종료합니다.")
                return
            }

            output = engine.handle(
                step = currentStep,
                input = input,
                context = context
            )

            currentStep = output.nextStep
            context = output.context

            io.println(output.message)
        }

        io.println("SplitMate를 이용해주셔서 감사합니다. 프로그램을 종료합니다.")
    }

    private fun printWelcome() {
        io.println("==== SplitMate - 유학생 더치페이 계산 도우미 (콘솔 버전) ====")
        io.println("총 금액, 세금, 팁, 인원수를 입력하면 1인당 부담 금액을 계산해드립니다.")
        io.println("금액은 CAD 기준으로 입력해주세요.")
        io.println("")
    }
}

fun main() {
    val engine = ConversationEngine()
    val io = StdConsoleIO()
    val app = ConsoleApp(engine, io)

    app.run()
}
