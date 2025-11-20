package com.splitmate.screens

import androidx.compose.runtime.*
import com.splitmate.AppStyles
import com.splitmate.state.SoloSplitViewModel
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.dom.*

@Composable
fun SoloSplitScreen(
    goHome: () -> Unit
) {
    val viewModel = remember { SoloSplitViewModel() }
    val uiState = viewModel.uiState

    // 카드 안의 내용만 담당 (레이아웃은 App.kt에서)
    Div {
        // 상단 뒤로가기
        Div({ classes(AppStyles.backButtonRow) }) {
            Button(attrs = {
                onClick { goHome() }
            }) { Text("← 홈으로") }
        }

        H2 { Text("SOLO N분의 1 계산") }
        P {
            Text("첫 단계: 총 결제 금액(CAD)을 입력해주세요. 세금과 팁은 다음 단계에서 입력합니다.")
        }

        Div({ classes(AppStyles.formColumn) }) {
            Label(forId = "totalAmount") {
                Text("총 결제 금액 (CAD)")
            }

            Input(
                type = InputType.Text,
                attrs = {
                    id("totalAmount")
                    placeholder("예: 27.40")
                    value(uiState.amountInput)
                    onInput { event ->
                        viewModel.onAmountChange(event.value)
                    }
                    classes(AppStyles.textField)
                }
            )

            if (uiState.amountError != null) {
                P({ classes(AppStyles.errorText) }) {
                    Text(uiState.amountError!!)
                }
            }

            Button(attrs = {
                if (!uiState.canProceed) {
                    disabled()
                }
                onClick {
                    val ok = viewModel.onSubmit()
                    if (ok) {
                        // TODO: 다음 단계(세금 입력 화면)로 네비게이션 추가 예정
                        // 지금은 단순히 폼 검증만 수행
                    }
                }
            }) {
                Text("다음 단계로")
            }
        }
    }
}
