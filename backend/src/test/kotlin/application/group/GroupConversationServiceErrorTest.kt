package application.group

import application.conversation.model.ConversationContext
import application.conversation.flow.ConversationFlow
import application.conversation.model.ConversationOutput
import application.conversation.model.ConversationStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupConversationServiceErrorTest {

    private class StubConversationFlow : ConversationFlow {
        override fun start(): ConversationOutput =
            ConversationOutput(
                message = "총 결제 금액을 입력해주세요.",
                nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                context = ConversationContext()
            )

        override fun handle(
            step: ConversationStep,
            input: String,
            context: ConversationContext
        ): ConversationOutput =
            ConversationOutput(
                message = "stub",
                nextStep = step,
                context = context
            )
    }

    private fun service(): GroupConversationService =
        GroupConversationService(StubConversationFlow())

    @Test
    fun `존재하지 않는 방에 joinRoom을 호출하면 예외가 발생한다`() {
        val svc = service()

        val ex = assertFailsWith<IllegalArgumentException> {
            svc.joinRoom(RoomId("room-404"), MemberId("user-1"))
        }

        assertEquals("Room not found: room-404", ex.message)
    }

    @Test
    fun `존재하지 않는 방에 handleMessage를 호출하면 예외가 발생한다`() {
        val svc = service()

        val ex = assertFailsWith<IllegalArgumentException> {
            svc.handleMessage(
                roomId = RoomId("room-404"),
                memberId = MemberId("user-1"),
                input = "hello"
            )
        }

        assertEquals("Room not found: room-404", ex.message)
    }

    @Test
    fun `방에 속하지 않은 멤버가 handleMessage를 호출하면 예외가 발생한다`() {
        val svc = service()

        // given: room-1 을 생성하고 member-1 을 참가자로 둔다
        val roomId = RoomId("room-1")
        svc.createRoom(roomId, MemberId("member-1"))

        // when: member-2 가 메시지를 보내면
        val ex = assertFailsWith<IllegalArgumentException> {
            svc.handleMessage(
                roomId = roomId,
                memberId = MemberId("intruder"),
                input = "hi"
            )
        }

        assertEquals(
            "Member intruder is not in room room-1",
            ex.message
        )
    }
}
