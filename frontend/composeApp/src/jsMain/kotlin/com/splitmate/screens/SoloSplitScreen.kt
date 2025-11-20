package com.splitmate.screens

import androidx.compose.runtime.*
import com.splitmate.AppStyles
import com.splitmate.state.SoloSplitUiState
import com.splitmate.state.SoloSplitViewModel
import com.splitmate.state.SoloStep
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*

@Composable
fun SoloSplitScreen(
    goHome: () -> Unit
) {
    val viewModel = remember { SoloSplitViewModel() }
    val uiState = viewModel.uiState

    Div {
        // 상단 뒤로가기
        Div({ classes(AppStyles.backButtonRow) }) {
            Button(attrs = {
                onClick { goHome() }
            }) {
                Text("← 홈으로")
            }
        }

        when (uiState.step) {
            SoloStep.TOTAL_AMOUNT -> TotalAmountStep(uiState, viewModel)
            SoloStep.TAX -> TaxStep(uiState, viewModel)
        }
    }
}

@Composable
private fun TotalAmountStep(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 1단계") }
    P {
        Text("총 결제 금액(CAD)을 입력해주세요. 세금과 팁은 다음 단계에서 입력합니다.")
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
                Text(uiState.amountError)
            }
        }

        Button(attrs = {
            if (!uiState.canProceedFromTotal) {
                disabled()
            }
            onClick {
                viewModel.onTotalSubmit()
            }
        }) {
            Text("다음 단계로 (세금 입력)")
        }
    }
}

@Composable
private fun TaxStep(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 2단계") }
    P {
        Text("세금 금액을 입력해주세요. 세금이 없으면 '없음'을 선택하거나 0을 입력할 수 있습니다.")
    }

    Div({ classes(AppStyles.formColumn) }) {
        Label(forId = "taxAmount") {
            Text("세금 금액 (CAD)")
        }

        Input(
            type = InputType.Text,
            attrs = {
                id("taxAmount")
                placeholder("예: 2.40 또는 없음")
                value(uiState.taxInput)
                onInput { event ->
                    viewModel.onTaxChange(event.value)
                }
                classes(AppStyles.textField)
            }
        )

        // '없음' 선택 버튼 (선택형 옵션 제공)
        Button(attrs = {
            onClick { viewModel.onTaxNoneClick() }
        }) {
            Text("세금 없음")
        }

        if (uiState.taxError != null) {
            P({ classes(AppStyles.errorText) }) {
                Text(uiState.taxError)
            }
        }

        Button(attrs = {
            if (!uiState.canProceedFromTax) {
                disabled()
            }
            onClick {
                val ok = viewModel.onTaxSubmit()
                if (ok) {
                    // TODO: 다음 단계(팁 입력 화면)로 진행 예정
                }
            }
        }) {
            Text("다음 단계로 (팁 입력 예정)")
        }
    }
}
