// src/test/kotlin/application/group/GroupConversationServiceTest.kt
package application.group

import application.conversation.ConversationContext
import application.conversation.ConversationFlow
import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private class FakeConversationFlow : ConversationFlow {
    var startCalled = 0
    var handleCalled = 0

    override fun start(): ConversationOutput {
        startCalled++
        val ctx = ConversationContext() // 실제 타입대로 맞춰주세요
        return ConversationOutput(
            nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
            message = "총 결제 금액을 입력해주세요",
            context = ctx
        )
    }

    override fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        handleCalled++
        // 아주 단순한 fake: 항상 같은 step으로 넘어간다고 가정
        return ConversationOutput(
            nextStep = step,
            message = "echo: $input",
            context = context
        )
    }
}

class GroupConversationServiceTest {

    @Test
    fun `createRoom은 새 Room을 만들고 첫 멤버를 추가하며 start()를 호출한다`() {
        val fakeFlow = FakeConversationFlow()
        val service = GroupConversationService(fakeFlow)

        val roomId = RoomId("room-1")
        val memberId = MemberId("alice")

        val room = service.createRoom(roomId, memberId)

        assertEquals(roomId, room.id)
        assertEquals(setOf(memberId), room.members)
        assertEquals(1, fakeFlow.startCalled)
        assertEquals(
            "총 결제 금액을 입력해주세요",
            room.lastOutput.message
        )

        val loaded = service.getRoom(roomId)
        assertNotNull(loaded)
        assertEquals(room, loaded)
    }

    @Test
    fun `handleMessage는 해당 Room의 lastOutput을 기반으로 handle를 호출하고 lastOutput를 갱신한다`() {
        val fakeFlow = FakeConversationFlow()
        val service = GroupConversationService(fakeFlow)

        val roomId = RoomId("room-1")
        val alice = MemberId("alice")

        // 먼저 방 생성
        val created = service.createRoom(roomId, alice)

        val before = created.lastOutput
        val updated = service.handleMessage(roomId, alice, "27.40")

        // handle가 1번 호출되었는지
        assertEquals(1, fakeFlow.handleCalled)

        // lastOutput가 바뀌었는지
        assertEquals("echo: 27.40", updated.lastOutput.message)

        // rooms 맵에도 반영되었는지
        val loaded = service.getRoom(roomId)
        assertEquals(updated, loaded)

        // 멤버 구성은 그대로
        assertEquals(setOf(alice), updated.members)
        // step은 FakeConversationFlow 구현대로 동일하게 유지 (혹은 상황에 맞게 비교)
        assertEquals(before.nextStep, updated.lastOutput.nextStep)
    }

    @Test
    fun `joinRoom은 기존 방에 새 멤버만 추가하고 대화 상태는 그대로 유지한다`() {
        val fakeFlow = FakeConversationFlow()
        val service = GroupConversationService(fakeFlow)

        val roomId = RoomId("room-1")
        val alice = MemberId("alice")
        val bob = MemberId("bob")

        val created = service.createRoom(roomId, alice)
        val joined = service.joinRoom(roomId, bob)

        assertEquals(setOf(alice, bob), joined.members)
        assertEquals(created.lastOutput, joined.lastOutput)
    }
}
