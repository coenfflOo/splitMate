package com.splitmate.screens

import androidx.compose.runtime.*
import com.splitmate.styles.AppStyles
import com.splitmate.state.uistate.SoloSplitUiState
import com.splitmate.state.viewmodel.SoloSplitViewModel
import com.splitmate.state.steps.SoloStep
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.dom.Input
import androidx.compose.runtime.LaunchedEffect
import com.splitmate.state.model.solo.SoloExchangeMode
import com.splitmate.state.model.solo.SoloTipMode
import com.splitmate.ui.SelectableButton
import com.splitmate.ui.ToastError


@Composable
fun SoloSplitScreen(
    goHome: () -> Unit
) {
    val viewModel = remember { SoloSplitViewModel() }
    val uiState = viewModel.uiState

    ToastError(
        message = uiState.apiError,
        onDismiss = { viewModel.clearApiError() }
    )

    Div {
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
            SoloStep.TIP_MODE -> TipModeStep(uiState, viewModel)
            SoloStep.TIP_VALUE -> TipValueStep(uiState, viewModel)
            SoloStep.SPLIT_MODE -> SplitModeStep(viewModel)
            SoloStep.PEOPLE_COUNT -> PeopleCountStep(uiState, viewModel)
            SoloStep.EXCHANGE_RATE_MODE -> ExchangeRateModePlaceholder(uiState, viewModel)
            SoloStep.EXCHANGE_RATE_VALUE -> ExchangeRateValuePlaceholder(uiState, viewModel)
            SoloStep.RESULT -> ResultPlaceholder(uiState, viewModel, goHome)
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
            SelectableButton(
                text = "% 퍼센트",
                isSelected = uiState.tipMode == SoloTipMode.PERCENT
            ) { viewModel.onTipModeSelected(SoloTipMode.PERCENT) }

            SelectableButton(
                text = "$ 금액",
                isSelected = uiState.tipMode == SoloTipMode.ABSOLUTE
            ) { viewModel.onTipModeSelected(SoloTipMode.ABSOLUTE) }

            SelectableButton(
                text = "팁 없음",
                isSelected = uiState.tipMode == SoloTipMode.NONE
            ) { viewModel.onTipModeSelected(SoloTipMode.NONE) }
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
            Button(attrs = {
                onClick { viewModel.onSplitModeNDivideSelected() }
            }) {
                Text("1) N분의 1")
            }
        }

        P {
            Text("지금은 N분의 1 방식만 사용할 수 있습니다. 다른 옵션들은 추후 지원 예정입니다.")
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

        SelectableButton(
            "1) 오늘 환율 자동 조회",
            isSelected = uiState.exchangeMode == SoloExchangeMode.AUTO
        ) { viewModel.onExchangeModeSelected(SoloExchangeMode.AUTO) }

        SelectableButton(
            "2) 환율 직접 입력",
            isSelected = uiState.exchangeMode == SoloExchangeMode.MANUAL
        ) { viewModel.onExchangeModeSelected(SoloExchangeMode.MANUAL) }

        SelectableButton(
            "3) KRW 변환 없이 보기",
            isSelected = uiState.exchangeMode == SoloExchangeMode.NONE
        ) { viewModel.onExchangeModeSelected(SoloExchangeMode.NONE) }

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
private fun ExchangeRateValuePlaceholder(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel
) {
    H2 { Text("8. 환율 값 입력 (수동)") }
    P {
        Text("1 CAD가 몇 KRW인지 입력해주세요. 예: 1000")
    }

    Div({ classes(AppStyles.formColumn) }) {
        Input(
            type = InputType.Text,
            attrs = {
                classes(AppStyles.textField)
                value(uiState.exchangeRateInput)
                onInput { ev -> viewModel.onExchangeRateValueChange(ev.value) }
                attr("placeholder", "예: 1000")
            }
        )

        uiState.exchangeRateError?.let {
            P({ classes(AppStyles.errorText) }) { Text(it) }
        }

        Button(attrs = {
            classes(AppStyles.primaryButton)
            onClick { viewModel.onExchangeRateValueSubmit() }
        }) {
            Text("결과 보기")
        }
    }
}

@Composable
private fun ResultPlaceholder(
    uiState: SoloSplitUiState,
    viewModel: SoloSplitViewModel,
    goHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.requestBackendResult()
    }

    H2 { Text("9. 계산 결과") }

    when {
        uiState.isLoading -> {
            P {
                Text("계산 중입니다... 잠시만 기다려주세요.")
            }
        }

        uiState.apiError != null -> {
            P({ classes(AppStyles.errorText) }) {
                Text(uiState.apiError)
            }
        }

        uiState.result == null -> {
            P {
                Text("결과가 아직 준비되지 않았습니다. 입력 값을 다시 확인해주세요.")
            }
        }

        else -> {
            val result = uiState.result!!
            Div({ classes(AppStyles.formColumn) }) {
                P {
                    Text("총 금액 (CAD): ${result.totalCad}")
                }
                P {
                    Text("1인당 (CAD): ${result.perPersonCad}")
                }

                result.perPersonKrw?.let { krw ->
                    P {
                        Text("1인당 (KRW): $krw")
                    }
                }
            }
        }
    }

    Div({ classes(AppStyles.buttonRow) }) {
        Button(attrs = {
            classes(AppStyles.secondaryButton)
            onClick { goHome() }
        }) {
            Text("← 홈으로")
        }
    }
}
