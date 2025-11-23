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
    val menuVm = remember { MenuSplitViewModel() }
    val menuState = menuVm.uiState

    Div({ classes(AppStyles.backButtonRow) }) {
        Button(attrs = {
            onClick {
                viewModel.disconnect()
                goHome()
            }
        }) { Text("← 홈으로") }
    }

    H2 { Text("GROUP 모드") }
    P { Text("여러 명이 한 방에 들어와서 계산 과정을 함께 진행하는 모드입니다.") }

    // ✅ 입장 전 UI는 isJoined=false 일 때만 보여준다
    if (!state.isJoined) {
        Div({ classes(AppStyles.formColumn) }) {
            Label { Text("Room ID") }
            Input(type = InputType.Text, attrs = {
                classes(AppStyles.textField)
                value(state.roomIdInput)
                attr("placeholder", "예: room-1")
                onInput { ev -> viewModel.onRoomIdChange(ev.value) }
            })

            Label { Text("Member ID") }
            Input(type = InputType.Text, attrs = {
                classes(AppStyles.textField)
                value(state.memberIdInput)
                attr("placeholder", "예: member-1")
                onInput { ev -> viewModel.onMemberIdChange(ev.value) }
            })

            Div({ classes(AppStyles.buttonRow) }) {
                Button(attrs = {
                    if (state.isLoading) attr("disabled", "true")
                    onClick { viewModel.createAndJoinRoom() }
                }) { Text(if (state.isLoading) "처리중..." else "방 생성 후 입장") }

                Button(attrs = {
                    if (state.isLoading) attr("disabled", "true")
                    onClick { viewModel.joinExistingRoom() }
                }) { Text(if (state.isLoading) "처리중..." else "기존 방 입장") }
            }

            if (state.error != null) {
                P({ classes(AppStyles.errorText) }) { Text(state.error!!) }
            }
            if (state.info != null) {
                P { Text(state.info!!) }
            }
        }
    }

    // ✅ 입장 후 UI
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

//            if (state.currentPrompt.isNotBlank()) {
//                P { Text("서버 안내 메시지: ${state.currentPrompt}") }
//            }
        }

//    Div({ classes(AppStyles.backButtonRow) }) {
//        Button(attrs = {
//            onClick {
//                viewModel.disconnect()
//                goHome()
//            }
//        }) { Text("← 홈으로") }
//    }
//
//    H2 { Text("GROUP 모드") }
//    P { Text("여러 명이 한 방에 들어와서 계산 과정을 함께 진행하는 모드입니다.") }
//
//    Div({ classes(AppStyles.formColumn) }) {
//        Label { Text("Room ID") }
//        Input(type = InputType.Text, attrs = {
//            classes(AppStyles.textField)
//            value(state.roomIdInput)
//            attr("placeholder", "예: room-1")
//            onInput { ev -> viewModel.onRoomIdChange(ev.value) }
//        })
//
//        Label { Text("Member ID") }
//        Input(type = InputType.Text, attrs = {
//            classes(AppStyles.textField)
//            value(state.memberIdInput)
//            attr("placeholder", "예: member-1")
//            onInput { ev -> viewModel.onMemberIdChange(ev.value) }
//        })
//
//        Div({ classes(AppStyles.buttonRow) }) {
//            Button(attrs = {
//                if (state.isLoading) attr("disabled", "true")
//                onClick { viewModel.createAndJoinRoom() }
//            }) { Text(if (state.isLoading) "처리중..." else "방 생성 후 입장") }
//
//            Button(attrs = {
//                if (state.isLoading) attr("disabled", "true")
//                onClick { viewModel.joinExistingRoom() }
//            }) { Text(if (state.isLoading) "처리중..." else "기존 방 입장") }
//        }
//
//        if (state.error != null) {
//            P({ classes(AppStyles.errorText) }) { Text(state.error!!) }
//        }
//        if (state.info != null) {
//            P { Text(state.info!!) }
//        }
//    }
//
//    if (state.isJoined) {
//        Hr()
//
//        Div({ classes(AppStyles.buttonRow) }) {
//            Button(attrs = {
//                if (state.isLoading) {
//                    attr("disabled", "true")
//                }
//                onClick { viewModel.createAndJoinRoom() }
//            }) {
//                Text(if (state.isLoading) "처리중..." else "방 생성 후 입장")
//            }
//
//            Button(attrs = {
//                if (state.isLoading) {
//                    attr("disabled", "true")
//                }
//                onClick { viewModel.joinExistingRoom() }
//            }) {
//                Text(if (state.isLoading) "처리중..." else "기존 방 입장")
//            }
//        }
//
//
//
//        Div({
//            classes(AppStyles.formColumn)
//            style { marginTop(12.px) }
//        }) {
//            H3 { Text("현재 방 정보") }
//
//            P { Text("Room ID: ${state.joinedRoomId ?: "-"}") }
//
//            if (state.members.isNotEmpty()) {
//                P { Text("참여자: ${state.members.joinToString(", ")}") }
//            }
//
//            if (state.currentPrompt.isNotBlank()) {
//                P { Text("서버 안내 메시지: ${state.currentPrompt}") }
//            }
//        }

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
                    H3 { Text("분배 방식을 선택하세요") }
                    P { Text(state.currentPrompt) }

                    Div({ classes(AppStyles.buttonRow) }) {

                        Button(attrs = {
                            onClick { viewModel.onSplitModeSelected("N_DIVIDE") }
                        }) { Text("N분의 1") }

                        Button(attrs = {
                            onClick { viewModel.onSplitModeSelected("MENU_BASED") }
                        }) { Text("메뉴별 분배") }
                    }
                }

                GroupStep.MENU_ITEMS,
                GroupStep.PARTICIPANTS,
                GroupStep.MENU_ASSIGNMENTS -> {
                    H3 { Text("🍽 메뉴별 분배 입력") }
                    P { Text("아래 UI로 메뉴/참가자/배정을 선택하세요.") }

                    MenuSplitScreen(
                        goHome = {},
                        viewModel = menuVm
                    )

                    if (menuState.step == MenuStep.RESULT && menuState.result != null) {
                        val payload = buildMenuPayload(menuState)

                        Div({ classes(AppStyles.buttonRow) }) {
                            Button(attrs = {
                                onClick {
                                    viewModel.sendSystemInput(payload)
                                    menuVm.backToMenuStep()
                                }
                            }) {
                                Text("이 결과를 서버에 전송")
                            }
                        }
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
                    H3 { Text("✅ 최종 결과") }
                    P { Text(state.currentPrompt) }

//                    // ✅ MENU_BASED 결과면 메뉴 섹션 같이 보여줌
//                    if (state.isMenuFlowActive) {
//                        Hr()
//                        H3 { Text("🍽 메뉴별 분배 결과") }
//
//                        val menuVm = remember { MenuSplitViewModel() }
//
//                        MenuSplitScreen(
//                            goHome = {},
//                            viewModel = menuVm
//                        )
//
//                        val menuState = menuVm.uiState
//                        if (menuState.step == MenuStep.RESULT && menuState.result != null) {
//                            val payload = buildMenuPayload(menuState)
//                            Button(attrs = {
//                                onClick { viewModel.sendMessage(payload) }
//                            }) {
//                                Text("이 결과를 서버에 전송")
//                            }
//                        }
//                    }

                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.onRestartAnswer("Y") } }) {
                            Text("다시 계산하기")
                        }
                        Button(attrs = { onClick { viewModel.onRestartAnswer("N") } }) {
                            Text("종료")
                        }
                    }
                }

                GroupStep.RESTART_CONFIRM -> {
                    H3 { Text("다시 시작할까요?") }
                    P { Text(state.currentPrompt) }

                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.onRestartAnswer("Y") } }) {
                            Text("예, 다시 시작")
                        }
                        Button(attrs = { onClick { viewModel.onRestartAnswer("N") } }) {
                            Text("아니오")
                        }
                    }
                }

                else -> {
                    // TOTAL_AMOUNT / TAX / TIP_VALUE / PEOPLE_COUNT / EXCHANGE_VALUE 등
                    Label { Text("계산 입력") }
                    Input(type = InputType.Text, attrs = {
                        classes(AppStyles.textField)
                        value(state.inputText)
                        attr("placeholder", placeholderFor(state.currentStep))
                        onInput { ev -> viewModel.onInputTextChange(ev.value) }
                    })

                    Div({ classes(AppStyles.buttonRow) }) {
                        Button(attrs = { onClick { viewModel.sendMessage() } }) {
                            Text("계산 보내기")
                        }
                    }
                }
            }

            Hr()

            Label { Text("채팅") }
            Input(type = InputType.Text, attrs = {
                classes(AppStyles.textField)
                value(state.chatText)
                attr("placeholder", "친구에게 메시지 보내기")
                onInput { ev -> viewModel.onChatTextChange(ev.value) }
            })

            Div({ classes(AppStyles.buttonRow) }) {
                Button(attrs = { onClick { viewModel.sendChat() } }) {
                    Text("채팅 보내기")
                }
            }
//            if (state.isMenuFlowActive) {
//                Hr()
//                H3 { Text("🍽 메뉴별 입력 (로컬 플로우)") }
//
//                val menuVm = remember { MenuSplitViewModel() }
//                MenuSplitScreen(
//                    goHome = {},
//                )
//
//                // 로컬 플로우가 RESULT에 도달하면 서버로 payload 전송
//                val menuState = menuVm.uiState
//                if (menuState.step == MenuStep.RESULT && menuState.result != null) {
//                    val payload = buildMenuPayload(menuState)
//                    Button(attrs = {
//                        onClick {
//                            viewModel.sendMessage(payload)
//                        }
//                    }) {
//                        Text("이 결과를 서버에 전송")
//                    }
//                }
//            }
        }
    }
}
private fun placeholderFor(step: GroupStep): String =
    when (step) {
        GroupStep.TOTAL_AMOUNT -> "예: 27.40"
        GroupStep.TAX -> "예: 2.60 또는 없음"
        GroupStep.TIP_VALUE -> "예: 15 또는 10.00"
        GroupStep.PEOPLE_COUNT -> "예: 3"
        GroupStep.EXCHANGE_VALUE -> "예: 980.5"
        GroupStep.MENU_ITEMS -> "예: 파스타 18.9; 피자 22; 콜라 3"
        GroupStep.PARTICIPANTS -> "예: 민지, 철수, 영희"
        GroupStep.MENU_ASSIGNMENTS -> "예: m1:p1,p2; m2:p2"
        else -> "입력해주세요"
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