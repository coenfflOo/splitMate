package com.splitmate.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.splitmate.AppStyles
import com.splitmate.state.GroupStep
import com.splitmate.state.GroupViewModel
import com.splitmate.state.MenuSplitUiState
import com.splitmate.state.MenuSplitViewModel
import com.splitmate.state.MenuStep
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*

@Composable
fun GroupScreen(
    goHome: () -> Unit,
    viewModel: GroupViewModel = remember { GroupViewModel() }
) {
    val state = viewModel.uiState

    if (state.isJoined) {
        Hr()

        Div({
            classes(AppStyles.formColumn)
            style { marginTop(12.px) }
        }) {
            H3 { Text("현재 방 정보") }

            P { Text("Room ID: ${state.joinedRoomId ?: "-"}") }

            if (state.members.isNotEmpty()) {
                P { Text("참여자: ${state.members.joinToString(", ")}") }
            }

            if (state.currentPrompt.isNotBlank()) {
                P { Text("서버 안내 메시지: ${state.currentPrompt}") }
            }
        }

        Div({
            classes(AppStyles.formColumn)
            style { marginTop(12.px) }
        }) {
            H3 { Text("메시지") }

            if (state.messages.isEmpty()) {
                P { Text("아직 메시지가 없습니다. 아래 입력창에 값을 넣고 보내보세요.") }
            } else {
                Ul {
                    state.messages.forEach { msg -> Li { Text(msg) } }
                }
            }

            when (state.currentStep) {
                GroupStep.SPLIT_MODE -> {
                    Div({ classes(AppStyles.buttonRow) }) {

                        Button(attrs = {
                            onClick {
                                viewModel.sendMessage("N_DIVIDE")
                            }
                        }) { Text("N분의 1") }

                        Button(attrs = {
                            onClick {
                                // 로컬 메뉴 플로우 시작
                                viewModel.startMenuFlow()
                                viewModel.sendMessage("MENU_BASED")
                            }
                        }) { Text("메뉴별 계산") }
                    }
                }

                GroupStep.TIP_MODE -> {
                    P { Text("팁 입력 방식을 선택해주세요.") }
                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.sendMessage("PERCENT") } }) { Text("퍼센트(%)") }
                        Button(attrs = { onClick { viewModel.sendMessage("ABSOLUTE") } }) { Text("금액") }
                        Button(attrs = { onClick { viewModel.sendMessage("NONE") } }) { Text("없음") }
                    }
                }

                GroupStep.EXCHANGE_MODE -> {
                    P { Text("환율 모드를 선택해주세요.") }
                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.sendMessage("AUTO") } }) { Text("자동(오늘 환율)") }
                        Button(attrs = { onClick { viewModel.sendMessage("MANUAL") } }) { Text("수동 입력") }
                        Button(attrs = { onClick { viewModel.sendMessage("NONE") } }) { Text("KRW 생략") }
                    }
                }

                GroupStep.RESULT -> {
                    Hr()
                    Div({ classes(AppStyles.formColumn) }) {
                        H3 { Text("✅ 최종 결과") }

                        // 서버가 내려준 최종 message 그대로 강조 표시
                        Div({
                            style {
                                marginTop(8.px)
                                padding(12.px)
                                property("border", "1px solid #ddd")
                                property("border-radius", "8px")
                            }
                        }) {
                            Text(state.currentPrompt)
                        }

                        P {
                            Text("아래는 전체 대화 로그입니다.")
                        }
                    }
                }

                GroupStep.RESTART_CONFIRM -> {
                    Hr()
                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.sendMessage("YES") } }) {
                            Text("다시 계산하기")
                        }
                        Button(attrs = {
                            onClick {
                                viewModel.sendMessage("NO")
                                viewModel.disconnect()
                                goHome()
                            }
                        }) {
                            Text("종료")
                        }
                    }
                }

                else -> {
                    Label { Text("입력") }
                    Input(type = InputType.Text, attrs = {
                        classes(AppStyles.textField)
                        value(state.inputText)
                        attr("placeholder", "예: 27.40")
                        onInput { ev -> viewModel.onInputTextChange(ev.value) }
                    })

                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.sendMessage() } }) {
                            Text("보내기")
                        }
                    }
                }
            }
            if (state.isMenuFlowActive) {
                Hr()
                H3 { Text("🍽 메뉴별 입력 (로컬 플로우)") }

                val menuVm = remember { MenuSplitViewModel() }
                MenuSplitScreen(
                    goHome = {},
                )

                // 로컬 플로우가 RESULT에 도달하면 서버로 payload 전송
                val menuState = menuVm.uiState
                if (menuState.step == MenuStep.RESULT && menuState.result != null) {
                    val payload = buildMenuPayload(menuState)
                    Button(attrs = {
                        onClick {
                            viewModel.sendMessage(payload)
                        }
                    }) {
                        Text("이 결과를 서버에 전송")
                    }
                }
            }
        }
    }

}

private fun buildMenuPayload(state: MenuSplitUiState): String {
    // 서버가 기대하는 포맷에 맞춰 바꿀 수 있도록
    // 임시로 JSON 문자열을 보냄
    val itemsJson = state.menuItems.joinToString(
        prefix = "[", postfix = "]"
    ) {
        val price = it.priceInput.replace(",", "")
        """{"id":"${it.id}","name":"${it.name}","price":"$price"}"""
    }

    val participantsJson = state.participants.joinToString(
        prefix = "[", postfix = "]"
    ) {
        """{"id":"${it.id}","name":"${it.name}"}"""
    }

    val assignmentsJson = state.assignments.entries.joinToString(
        prefix = "[", postfix = "]"
    ) { (menuId, pids) ->
        val pidList = pids.joinToString(prefix = "[", postfix = "]") { """"$it"""" }
        """{"menuId":"$menuId","participantIds":$pidList}"""
    }

    return """MENU_PAYLOAD:{"items":$itemsJson,"participants":$participantsJson,"assignments":$assignmentsJson}"""
}
