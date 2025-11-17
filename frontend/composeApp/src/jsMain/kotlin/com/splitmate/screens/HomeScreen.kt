package com.splitmate.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun HomeScreen(
    goSolo: () -> Unit,
    goMenu: () -> Unit,
    goGroup: () -> Unit
) {
    Div({
        style {
            minHeight(100.vh)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            justifyContent(JustifyContent.Center)
            alignItems(AlignItems.Center)
            fontFamily("system-ui", "Inter", "sans-serif")
            backgroundColor(rgb(248, 249, 252))
        }
    }) {
        // 헤더 영역
        Div({
            style {
                textAlign("center")
                marginBottom(32.px)
            }
        }) {
            H1({
                style {
                    fontSize(32.px)
                    marginBottom(8.px)
                }
            }) { Text("SplitMate") }

            P({
                style {
                    fontSize(16.px)
                    color(rgb(90, 90, 90))
                    marginTop(0.px)
                }
            }) {
                Text("영수증을 기반으로 N분의 1 / 메뉴별 / 그룹 실시간 계산을 돕는 Kotlin 풀스택 앱입니다.")
            }
        }

        // 모드 선택 카드 영역
        Div({
            style {
                display(DisplayStyle.Flex)
                flexWrap(FlexWrap.Wrap)
                justifyContent(JustifyContent.Center)
                gap(16.px)
                maxWidth(900.px)
            }
        }) {
            ModeCard(
                title = "SOLO 계산",
                description = "혼자서 영수증 정보를 입력하고 N분의 1 + 환율 변환까지 한 번에 계산합니다.",
                buttonLabel = "SOLO 모드 시작하기",
                onClick = goSolo
            )

            ModeCard(
                title = "메뉴별 계산",
                description = "메뉴 항목과 참가자를 입력하고, 세금/팁을 비례 분배하여 각자 부담 금액을 계산합니다.",
                buttonLabel = "메뉴별 계산 시작하기",
                onClick = goMenu
            )

            ModeCard(
                title = "GROUP 모드",
                description = "여러 명이 한 방에 접속해 WebSocket으로 실시간 대화를 공유하며 함께 계산합니다.",
                buttonLabel = "GROUP 모드 입장",
                onClick = goGroup
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Div({
        style {
            width(260.px)
            padding(20.px)
            borderRadius(12.px)
            backgroundColor(rgb(255, 255, 255))

            property(
                "box-shadow",
                "0 4px 12px rgba(15, 23, 42, 0.08)"
            )

            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            justifyContent(JustifyContent.SpaceBetween)
        }
    }) {
        Div {
            H2({
                style {
                    fontSize(20.px)
                    marginBottom(8.px)
                }
            }) { Text(title) }

            P({
                style {
                    fontSize(14.px)
                    color(rgb(100, 100, 100))
                    marginTop(0.px)
                    marginBottom(16.px)
                }
            }) { Text(description) }
        }

        Button(attrs = {
            style {
                padding(8.px, 12.px)
                borderRadius(8.px)
                border { style(LineStyle.None) }
                backgroundColor(rgb(59, 130, 246))
                color(rgb(255, 255, 255))
                fontSize(14.px)
                cursor("pointer")
            }
            onClick { onClick() }
        }) {
            Text(buttonLabel)
        }
    }
}