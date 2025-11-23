package com.splitmate.screens

import androidx.compose.runtime.*
import com.splitmate.AppStyles
import com.splitmate.state.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr

@Composable
fun MenuSplitScreen(
    goHome: () -> Unit,
    viewModel: MenuSplitViewModel = remember { MenuSplitViewModel()}
) {
    val state = viewModel.uiState

    Div({
        classes(AppStyles.backButtonRow)
    }) {
        Button(attrs = { onClick { goHome() } }) {
            Text("← 홈으로")
        }
    }

    H2 { Text("메뉴별 계산") }

    when (state.step) {
        MenuStep.MENU_ITEMS -> MenuItemsStep(
            state = state,
            onAddItem = { viewModel.addMenuItem() },
            onRemoveItem = { id -> viewModel.removeMenuItem(id) },
            onNameChange = { id, name -> viewModel.onMenuNameChange(id, name) },
            onPriceChange = { id, price -> viewModel.onMenuPriceChange(id, price) },
            onNext = { viewModel.goToParticipantsStep() }
        )

        MenuStep.PARTICIPANTS -> ParticipantsStep(
            state = state,
            onAddParticipant = { viewModel.addParticipant() },
            onRemoveParticipant = { id -> viewModel.removeParticipant(id) },
            onNameChange = { id, name -> viewModel.onParticipantNameChange(id, name) },
            onPrev = { viewModel.backToMenuStep() },
            onNext = { viewModel.goToAssignmentsStep() }
        )

        MenuStep.ASSIGNMENTS -> AssignmentsStep(
            state = state,
            onToggle = { menuId, participantId -> viewModel.toggleAssignment(menuId, participantId) },
            onPrev = { viewModel.backToParticipantsStep() },
            onNext = { viewModel.goToTaxStep() }
        )

        MenuStep.TAX -> MenuTaxStep(
            state = state,
            viewModel = viewModel
        )

        MenuStep.TIP_MODE -> MenuTipModeStep(
            state = state,
            viewModel = viewModel
        )

        MenuStep.TIP_VALUE -> MenuTipValueStep(
            state = state,
            viewModel = viewModel
        )

        MenuStep.EXCHANGE_MODE -> MenuExchangeModeStep(
            state = state,
            viewModel = viewModel
        )

        MenuStep.EXCHANGE_RATE_VALUE -> MenuExchangeRateValueStep(
            state = state,
            viewModel = viewModel
        )

        MenuStep.RESULT -> MenuResultStep(
            state = state,
            viewModel = viewModel,
            onRestart = { viewModel.backToMenuStep() }
        )
    }
}

@Composable
private fun MenuItemsStep(
    state: MenuSplitUiState,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onNameChange: (Int, String) -> Unit,
    onPriceChange: (Int, String) -> Unit,
    onNext: () -> Unit
) {
    H3 { Text("1단계: 메뉴 등록") }

    Div({ classes(AppStyles.formColumn) }) {
        if (state.menuItems.isEmpty()) {
            P {
                Text("먼저 '메뉴 추가' 버튼을 눌러 메뉴를 등록해주세요.")
            }
        }

        state.menuItems.forEach { item ->
            Div {
                Label {
                    Text("메뉴 이름")
                }
                Input(type = InputType.Text, attrs = {
                    classes(AppStyles.textField)
                    value(item.name)
                    onInput { ev -> onNameChange(item.id, ev.value) }
                    attr("placeholder", "예: 파스타")
                })
                item.nameError?.let {
                    P({ classes(AppStyles.errorText) }) { Text(it) }
                }

                Label {
                    Text("가격 (CAD)")
                }
                Input(type = InputType.Text, attrs = {
                    classes(AppStyles.textField)
                    value(item.priceInput)
                    onInput { ev -> onPriceChange(item.id, ev.value) }
                    attr("placeholder", "예: 18.90")
                })
                item.priceError?.let {
                    P({ classes(AppStyles.errorText) }) { Text(it) }
                }

                Button(attrs = {
                    onClick { onRemoveItem(item.id) }
                    style {
                        marginTop(8.px)
                        fontSize(12.px)
                        padding(4.px, 8.px)
                    }
                }) {
                    Text("메뉴 삭제")
                }

                // 구분선
                Div({
                    style {
                        marginTop(12.px)
                        padding(1.px)
                    }
                })
            }
        }

        Button(attrs = {
            onClick { onAddItem() }
        }) {
            Text("＋ 메뉴 추가")
        }

        Div({
            classes(AppStyles.buttonRow)
            style {
                justifyContent(JustifyContent.FlexEnd)
            }
        }) {
            Button(attrs = { onClick { onNext() } }) {
                Text("다음 단계 → 참가자")
            }
        }
    }
}


@Composable
private fun ParticipantsStep(
    state: MenuSplitUiState,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (Int) -> Unit,
    onNameChange: (Int, String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    H3 { Text("2단계: 참가자 등록") }

    Div({ classes(AppStyles.formColumn) }) {
        if (state.participants.isEmpty()) {
            P {
                Text("함께 계산할 사람들을 추가해주세요.")
            }
        }

        state.participants.forEach { p ->
            Div {
                Label {
                    Text("이름")
                }
                Input(type = InputType.Text, attrs = {
                    classes(AppStyles.textField)
                    value(p.name)
                    onInput { ev -> onNameChange(p.id, ev.value) }
                    attr("placeholder", "예: 민지")
                })
                p.nameError?.let {
                    P({ classes(AppStyles.errorText) }) { Text(it) }
                }

                Button(attrs = {
                    onClick { onRemoveParticipant(p.id) }
                    style {
                        marginTop(8.px)
                        fontSize(12.px)
                        padding(4.px, 8.px)
                    }
                }) {
                    Text("참가자 삭제")
                }

                Div({
                    style {
                        marginTop(12.px)
                        padding(1.px)
                    }
                })
            }
        }

        Button(attrs = {
            onClick { onAddParticipant() }
        }) {
            Text("＋ 참가자 추가")
        }

        Div({
            classes(AppStyles.buttonRow)
            style {
                justifyContent(JustifyContent.SpaceBetween)
            }
        }) {
            Button(attrs = { onClick { onPrev() } }) {
                Text("← 메뉴로 돌아가기")
            }
            Button(attrs = { onClick { onNext() } }) {
                Text("다음 단계 → 메뉴별 선택")
            }
        }
    }
}


@Composable
private fun AssignmentsStep(
    state: MenuSplitUiState,
    onToggle: (Int, Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    H3 { Text("3단계: 메뉴별로 누가 먹었는지 선택") }

    if (state.menuItems.isEmpty() || state.participants.isEmpty()) {
        P {
            Text("메뉴와 참가자가 모두 있어야 합니다. 이전 단계에서 다시 확인해주세요.")
        }
        Button(attrs = { onClick { onPrev() } }) {
            Text("← 이전 단계로")
        }
        return
    }

    Div({
        style {
            marginTop(16.px)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(org.jetbrains.compose.web.css.AlignItems.Stretch)
        }
    }) {
        Table({
            style {
                property("border-collapse", "collapse")
                fontSize(14.px)
            }
        }) {
            Thead {
                Tr {
                    Th {
                        Text("메뉴")
                    }
                    state.participants.forEach { p ->
                        Th {
                            Text(p.name.ifBlank { "참가자 ${p.id}" })
                        }
                    }
                }
            }
            Tbody {
                state.menuItems.forEach { item ->
                    Tr {
                        Td {
                            Text(item.name.ifBlank { "메뉴 ${item.id}" })
                        }
                        state.participants.forEach { p ->
                            val selected = state.assignments[item.id]?.contains(p.id) == true
                            Td {
                                Input(type = InputType.Checkbox, attrs = {
                                    checked(selected)
                                    onInput {
                                        onToggle(item.id, p.id)
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }

        P {
            Text("각 메뉴에 최소 1명 이상 선택되어 있어야 합니다.")
        }

        Div({
            classes(AppStyles.buttonRow)
            style {
                justifyContent(JustifyContent.SpaceBetween)
            }
        }) {
            Button(attrs = { onClick { onPrev() } }) {
                Text("← 참가자 단계로")
            }
            Button(attrs = { onClick { onNext() } }) {
                Text("결과 보기")
            }
        }
    }
}

@Composable
private fun MenuTaxStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel
) {
    H3 { Text("4단계: 세금 입력") }
    P { Text("세금 금액을 입력해주세요. 없으면 '없음'을 눌러주세요.") }

    Div({ classes(AppStyles.formColumn) }) {
        Input(type = InputType.Text, attrs = {
            classes(AppStyles.textField)
            value(state.taxInput)
            onInput { ev -> viewModel.onTaxChange(ev.value) }
            attr("placeholder", "예: 2.40 또는 없음")
        })

        Button(attrs = { onClick { viewModel.onTaxNoneClick() } }) {
            Text("세금 없음")
        }

        state.taxError?.let { P({ classes(AppStyles.errorText) }) { Text(it) } }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.backToAssignmentsStep() } }) {
                Text("← 이전")
            }
            Button(attrs = { onClick { viewModel.onTaxSubmit() } }) {
                Text("다음 (팁 모드)")
            }
        }
    }
}

@Composable
private fun MenuTipModeStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel
) {
    H3 { Text("5단계: 팁 입력 방식") }

    Div({ classes(AppStyles.formColumn) }) {
        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.onTipModeSelected(SoloTipMode.PERCENT) } }) {
                Text("% 퍼센트")
            }
            Button(attrs = { onClick { viewModel.onTipModeSelected(SoloTipMode.ABSOLUTE) } }) {
                Text("$ 금액")
            }
            Button(attrs = { onClick { viewModel.onTipModeSelected(SoloTipMode.NONE) } }) {
                Text("팁 없음")
            }
        }

        state.tipModeError?.let { P({ classes(AppStyles.errorText) }) { Text(it) } }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.backToTaxStep() } }) {
                Text("← 이전")
            }
            Button(attrs = { onClick { viewModel.onTipModeProceed() } }) {
                Text("다음")
            }
        }
    }
}

@Composable
private fun MenuTipValueStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel
) {
    H3 { Text("6단계: 팁 값 입력") }

    val mode = state.tipMode

    val placeholderText = when (mode) {
        SoloTipMode.PERCENT -> "예: 15"
        SoloTipMode.ABSOLUTE -> "예: 10.00"
        else -> ""
    }

    Div({ classes(AppStyles.formColumn) }) {
        Input(type = InputType.Text, attrs = {
            classes(AppStyles.textField)
            value(state.tipValueInput)
            onInput { ev -> viewModel.onTipValueChange(ev.value) }
            if (placeholderText.isNotBlank()) attr("placeholder", placeholderText)
        })

        state.tipValueError?.let { P({ classes(AppStyles.errorText) }) { Text(it) } }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.backToTipModeStep() } }) {
                Text("← 이전")
            }
            Button(attrs = { onClick { viewModel.onTipValueSubmit() } }) {
                Text("다음 (환율)")
            }
        }
    }
}

@Composable
private fun MenuExchangeModeStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel
) {
    H3 { Text("7단계: 환율 모드 선택") }

    Div({ classes(AppStyles.formColumn) }) {
        Button(attrs = { onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.AUTO) } }) {
            Text("1) 오늘 환율 자동 조회")
        }
        Button(attrs = { onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.MANUAL) } }) {
            Text("2) 환율 직접 입력")
        }
        Button(attrs = { onClick { viewModel.onExchangeModeSelected(SoloExchangeMode.NONE) } }) {
            Text("3) KRW 변환 없이 보기")
        }

        state.exchangeModeError?.let { P({ classes(AppStyles.errorText) }) { Text(it) } }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.backToTipValueOrModeStep() } }) {
                Text("← 이전")
            }
            Button(attrs = { onClick { viewModel.onExchangeModeSubmit() } }) {
                Text("다음")
            }
        }
    }
}

@Composable
private fun MenuExchangeRateValueStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel
) {
    H3 { Text("8단계: 환율 직접 입력") }

    Div({ classes(AppStyles.formColumn) }) {
        Input(type = InputType.Text, attrs = {
            classes(AppStyles.textField)
            value(state.exchangeRateInput)
            onInput { ev -> viewModel.onExchangeRateValueChange(ev.value) }
            attr("placeholder", "예: 1000")
        })

        state.exchangeRateError?.let { P({ classes(AppStyles.errorText) }) { Text(it) } }

        Div({ classes(AppStyles.buttonRow) }) {
            Button(attrs = { onClick { viewModel.backToExchangeModeStep() } }) {
                Text("← 이전")
            }
            Button(attrs = { onClick { viewModel.onExchangeRateValueSubmit() } }) {
                Text("결과 보기")
            }
        }
    }
}

@Composable
private fun MenuResultStep(
    state: MenuSplitUiState,
    viewModel: MenuSplitViewModel,
    onRestart: () -> Unit
) {
    H3 { Text("9단계: 결과") }

    LaunchedEffect(Unit) {
        viewModel.fetchBackendResult()   // ✅ RESULT 들어오면 호출
    }

    if (state.isLoading) {
        P { Text("계산 중입니다...") }
        return
    }

    state.apiError?.let {
        P({ classes(AppStyles.errorText) }) { Text(it) }
        Button(attrs = { onClick { viewModel.fetchBackendResult() } }) {
            Text("다시 시도")
        }
        return
    }

    val result = state.result ?: run {
        P { Text("아직 결과가 없습니다.") }
        return
    }

    // ✅ 여기부터는 result를 백엔드 기준으로 표시
    P { Text("총 결제 금액: ${result.totalAmountCad} CAD") }
    result.exchangeMode?.let { mode ->
        P { Text("환율 모드: $mode / 환율: ${result.exchangeRate ?: "-"}") }
    }

    Table {
        Thead {
            Tr {
                Th { Text("참가자") }
                Th { Text("Subtotal") }
                Th { Text("Tax") }
                Th { Text("Tip") }
                Th { Text("Total") }
                Th { Text("KRW") }
            }
        }
        Tbody {
            result.perPersonTotals.forEach { row ->
                Tr {
                    Td { Text(row.participantName) }
                    Td { Text(row.subtotalCad) }
                    Td { Text(row.taxShareCad) }
                    Td { Text(row.tipShareCad) }
                    Td { Text(row.totalCad) }
                    Td { Text(row.totalKrw ?: "-") }
                }
            }
        }
    }

    Button(attrs = { onClick { onRestart() } }) {
        Text("다시 계산하기")
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun Double.format2(): String =
    (this.asDynamic().toFixed(2) as String)
