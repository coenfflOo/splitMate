package com.splitmate.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.splitmate.AppStyles
import com.splitmate.state.GroupViewModel
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*

@Composable
fun GroupScreen(
    goHome: () -> Unit,
    viewModel: GroupViewModel = remember { GroupViewModel() }
) {
    val state = viewModel.uiState

    // 상단 뒤로가기
    Div({ classes(AppStyles.backButtonRow) }) {
        Button(attrs = {
            onClick { goHome() }
        }) {
            Text("← 홈으로")
        }
    }

    H2 { Text("GROUP 모드 (WebSocket)") }
    P {
        Text("여러 명이 한 방에 들어와서 계산 과정을 함께 진행하는 모드입니다.")
    }

    // 🔹 방 생성 / 입장 폼
    Div({ classes(AppStyles.formColumn) }) {
        Label {
            Text("Room ID")
        }
        Input(type = InputType.Text, attrs = {
            classes(AppStyles.textField)
            value(state.roomIdInput)
            attr("placeholder", "예: room-1")
            onInput { ev -> viewModel.onRoomIdChange(ev.value) }
        })

        Label {
            Text("Member ID")
        }
        Input(type = InputType.Text, attrs = {
            classes(AppStyles.textField)
            value(state.memberIdInput)
            attr("placeholder", "예: member-1")
            onInput { ev -> viewModel.onMemberIdChange(ev.value) }
        })

        Div({
            classes(AppStyles.buttonRow)
            style { marginTop(8.px) }
        }) {
            Button(attrs = {
                onClick { viewModel.createAndJoinRoom() }
            }) {
                Text("방 생성 후 입장")
            }

            Button(attrs = {
                onClick { viewModel.joinExistingRoom() }
            }) {
                Text("기존 방 입장")
            }
        }

        // 에러 / 정보 메시지
        if (state.error != null) {
            P({ classes(AppStyles.errorText) }) {
                Text(state.error!!)
            }
        }

        if (state.info != null) {
            P {
                Text(state.info!!)
            }
        }
    }

    // 🔹 방에 입장한 이후 UI (메시지 / 멤버 / 계산 흐름)
    if (state.isJoined) {
        Hr()

        // 방 정보
        Div({
            classes(AppStyles.formColumn)
            style { marginTop(12.px) }
        }) {
            H3 { Text("현재 방 정보") }

            P {
                Text("Room ID: ${state.joinedRoomId ?: "-"}")
            }

            if (state.members.isNotEmpty()) {
                P {
                    Text("참여자: ${state.members.joinToString(", ")}")
                }
            }

            if (state.currentPrompt.isNotBlank()) {
                P {
                    Text("서버 안내 메시지 (Mock): ${state.currentPrompt}")
                }
            }
        }

        // 메시지 리스트 + 입력
        Div({
            classes(AppStyles.formColumn)
            style { marginTop(12.px) }
        }) {
            H3 { Text("메시지") }

            if (state.messages.isEmpty()) {
                P {
                    Text("아직 메시지가 없습니다. 아래 입력창에 값을 넣고 보내보세요. (현재는 클라이언트 Mock)")
                }
            } else {
                Ul {
                    state.messages.forEach { msg ->
                        Li { Text(msg) }
                    }
                }
            }

            Label {
                Text("입력")
            }
            Input(type = InputType.Text, attrs = {
                classes(AppStyles.textField)
                value(state.inputText)
                attr("placeholder", "예: 27.40")
                onInput { ev -> viewModel.onInputTextChange(ev.value) }
            })

            Div({ classes(AppStyles.buttonRow) }) {
                Button(attrs = {
                    onClick { viewModel.sendMessageMock() }
                }) {
                    Text("보내기 (Mock)")
                }
            }
        }
    }
}
