package application.conversation

import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
import org.springframework.stereotype.Component

@Component
class SimpleConversationFlow : ConversationFlow {

    override fun start(): ConversationOutput {
        // 너희 도메인 ConversationOutput 생성 방식에 맞춰 최소 값으로 시작
        return ConversationOutput(
            message = "GROUP 대화를 시작합니다.",
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT, // 프로젝트에 있는 첫 스텝으로
            context = ConversationContext()
        )
    }

    override fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        // 일단은 입력을 그대로 echo 하는 더미 로직
        return ConversationOutput(
            message = "입력받음: $input",
            nextStep = step,   // 임시: 같은 단계 유지
            context = context
        )
    }
}
