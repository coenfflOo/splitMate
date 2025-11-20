package com.splitmate.screens

import androidx.compose.runtime.*
import com.splitmate.AppStyles
import com.splitmate.state.*
import com.splitmate.state.SoloSplitUiState
import com.splitmate.state.SoloSplitViewModel
import com.splitmate.state.SoloStep
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.dom.Input


@Composable
fun SoloSplitScreen(
    goHome: () -> Unit
) {
    val viewModel = remember { SoloSplitViewModel() }
    val uiState = viewModel.uiState

    Div {
        Div({ classes(AppStyles.backButtonRow) }) {
            Button(attrs = {
                onClick { goHome() }
            }) {
                Text("← 홈으로")
            }
        }

        when (uiState.step) {
            SoloStep.TOTAL_AMOUNT        -> TotalAmountStep(uiState, viewModel)
            SoloStep.TAX                 -> TaxStep(uiState, viewModel)
            SoloStep.TIP_MODE            -> TipModeStep(uiState, viewModel)
            SoloStep.TIP_VALUE           -> TipValueStep(uiState, viewModel)
            SoloStep.SPLIT_MODE          -> SplitModeStep(viewModel)
            SoloStep.PEOPLE_COUNT        -> PeopleCountStep(uiState, viewModel)
            SoloStep.EXCHANGE_RATE_MODE  -> ExchangeRateModePlaceholder(uiState, viewModel)
            SoloStep.EXCHANGE_RATE_VALUE -> ExchangeRateValuePlaceholder()
            SoloStep.RESULT              -> ResultPlaceholder()
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

@Composable
private fun TipModeStep(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 3단계") }
    P {
        Text("팁 입력 방식을 선택해주세요. 퍼센트 / 금액 / 없음 중에서 고를 수 있습니다.")
    }

    Div({ classes(AppStyles.formColumn) }) {
        P { Text("팁 입력 방식") }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = {
                onClick { viewModel.onTipModeSelected(SoloTipMode.PERCENT) }
            }) { Text("% 퍼센트") }

            Button(attrs = {
                onClick { viewModel.onTipModeSelected(SoloTipMode.ABSOLUTE) }
            }) { Text("$ 금액") }

            Button(attrs = {
                onClick { viewModel.onTipModeSelected(SoloTipMode.NONE) }
            }) { Text("팁 없음") }
        }

        if (uiState.tipMode != null) {
            P {
                val label = when (uiState.tipMode) {
                    SoloTipMode.PERCENT -> "퍼센트(%)로 팁을 입력합니다."
                    SoloTipMode.ABSOLUTE -> "금액($)으로 팁을 입력합니다."
                    SoloTipMode.NONE -> "팁 없이 계산을 진행합니다."
                }
                Text(label)
            }
        }

        if (uiState.tipModeError != null) {
            P({ classes(AppStyles.errorText) }) {
                Text(uiState.tipModeError)
            }
        }

        Button(attrs = {
            onClick { viewModel.onTipModeProceed() }
        }) {
            val label = when (uiState.tipMode) {
                SoloTipMode.PERCENT,
                SoloTipMode.ABSOLUTE -> "다음 단계로 (팁 값 입력)"
                SoloTipMode.NONE -> "다음 단계로 (분배 방식 선택)"
                null -> "다음 단계로"
            }
            Text(label)
        }
    }
}

@Composable
private fun SplitModeStep(
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 5단계") }
    P {
        Text("분배 방식을 선택해주세요. 현재는 N분의 1만 지원합니다.")
    }

    Div({ classes(AppStyles.formColumn) }) {
        P { Text("분배 방식") }

        Div({ classes(AppStyles.buttonRow) }) {
            // ✅ N분의 1만 실제로 동작
            Button(attrs = {
                onClick { viewModel.onSplitModeNDivideSelected() }
            }) {
                Text("1) N분의 1")
            }

            // 메뉴별은 아직 준비 중
            Button(attrs = {
                // 아직 미구현이므로 비활성화
                disabled()
            }) {
                Text("2) 메뉴별 계산 (준비 중)")
            }
        }

        P {
            Text("지금은 N분의 1 방식만 사용할 수 있습니다. 메뉴별 계산은 추후 지원 예정입니다.")
        }
    }
}

@Composable
private fun PeopleCountStep(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 6단계") }
    P {
        Text("몇 명이서 나누시나요? 인원 수는 1 이상의 정수로 입력해주세요.")
    }

    Div({ classes(AppStyles.formColumn) }) {
        Label(forId = "peopleCount") {
            Text("인원 수")
        }

        // 🔽 type을 Number → Text로, min(...) 제거
        Input(
            type = InputType.Text,
            attrs = {
                id("peopleCount")
                value(uiState.peopleCountInput)
                onInput { ev -> viewModel.onPeopleCountChange(ev.value) }
                placeholder("예: 3")
                classes(AppStyles.textField)
            }
        )

        uiState.peopleCountError?.let { msg ->
            P({ classes(AppStyles.errorText) }) {
                Text(msg)
            }
        }

        Button(attrs = {
            classes(AppStyles.primaryButton)
            onClick {
                viewModel.onPeopleCountSubmit()
            }
        }) {
            Text("다음 (환율 선택)")
        }
    }
}

@Composable
private fun TipValueStep(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("SOLO N분의 1 계산 – 4단계") }

    val mode = uiState.tipMode

    val description = when (mode) {
        SoloTipMode.PERCENT -> "퍼센트(%) 기준으로 팁을 입력해주세요. 예: 15"
        SoloTipMode.ABSOLUTE -> "금액($) 기준으로 팁을 입력해주세요. 예: 10.00"
        SoloTipMode.NONE, null -> "팁 없이 진행하는 모드입니다."
    }

    P { Text(description) }

    if (mode == SoloTipMode.NONE || mode == null) {
        P {
            Text("팁 없음 모드이므로, 다음 단계에서 분배 방식을 선택하게 됩니다.")
        }
        Button(attrs = {
            onClick {
                viewModel.onTipValueSubmit()
            }
        }) {
            Text("분배 방식 선택으로 이동")
        }
        return
    }

    Div({ classes(AppStyles.formColumn) }) {
        Label(forId = "tipValue") {
            val label = when (mode) {
                SoloTipMode.PERCENT -> "팁 퍼센트 (%)"
                SoloTipMode.ABSOLUTE -> "팁 금액 (CAD)"
                SoloTipMode.NONE -> "팁"
            }
            Text(label)
        }

        val placeholderText = when (mode) {
            SoloTipMode.PERCENT -> "예: 15"
            SoloTipMode.ABSOLUTE -> "예: 10.00"
            SoloTipMode.NONE -> ""
        }

        Input(
            type = InputType.Text,
            attrs = {
                id("tipValue")
                if (placeholderText.isNotBlank()) {
                    placeholder(placeholderText)
                }
                value(uiState.tipValueInput)
                onInput { event ->
                    viewModel.onTipValueChange(event.value)
                }
                classes(AppStyles.textField)
            }
        )

        if (uiState.tipValueError != null) {
            P({ classes(AppStyles.errorText) }) {
                Text(uiState.tipValueError)
            }
        }

        Button(attrs = {
            if (!uiState.canProceedFromTipValue) {
                disabled()
            }
            onClick {
                viewModel.onTipValueSubmit()
            }
        }) {
            Text("다음 단계로 (분배 방식 선택)")
        }
    }
}

@Composable
private fun ExchangeRateModePlaceholder(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("환율 모드 선택 (준비 중)") }
    H2 { Text("7. 환율 모드 선택") }
    P { Text("환율을 자동으로 불러올지, 직접 입력할지 선택해주세요.") }

    Div({ classes(AppStyles.formColumn) }) {

        Button(attrs = {
            onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.AUTO) }
        }) {
            Text("1) 오늘 환율 자동 조회 (CAD → KRW)")
        }

        Button(attrs = {
            onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.MANUAL) }
        }) {
            Text("2) 환율 직접 입력")
        }

        Button(attrs = {
            onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.NONE) }
        }) {
            Text("3) KRW 변환 없이 CAD만 보기")
        }

        uiState.exchangeModeError?.let {
            P({ classes(AppStyles.errorText) }) { Text(it) }
        }

        Button(attrs = {
            classes(AppStyles.primaryButton)
            onClick { viewModel.onExchangeModeSubmit() }
        }) {
            Text("다음")
        }
    }
}

@Composable
private fun ExchangeRateValuePlaceholder() {
    H2 { Text("환율 값 입력 (준비 중)") }
    P { Text("수동 입력 모드에서 환율 숫자를 입력할 수 있도록 구현할 예정입니다.") }
}

@Composable
private fun ResultPlaceholder() {
    H2 { Text("결과 화면 (준비 중)") }
    P { Text("REST API 결과를 받아 총합 및 1인당 부담금, KRW 변환 값을 보여줄 예정입니다.") }
}
