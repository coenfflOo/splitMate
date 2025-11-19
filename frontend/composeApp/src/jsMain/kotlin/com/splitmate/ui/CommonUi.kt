package com.splitmate.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*


// 백엔드의 에러 응답을 프론트에서 공통으로 다루기 위한 모델.
data class UiError(
    val code: String? = null,
    val message: String
)

// 상단 혹은 섹션 내에 띄우는 공통 에러 배너.
@Composable
fun ErrorBanner(
    error: UiError?,
    onDismiss: (() -> Unit)? = null
) {
    if (error == null) return

    Div({
        // 간단히 인라인 스타일로 처리
        attr(
            "style",
            """
            padding: 12px 16px;
            border-radius: 8px;
            background: #f87171; 
            color: #ffffff;
            font-size: 14px;
            margin-bottom: 12px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        """.trimIndent()
        )
    }) {
        Div {
            if (!error.code.isNullOrBlank()) {
                B {
                    Text("[${error.code}] ")
                }
            }
            Span { Text(error.message) }
        }

        if (onDismiss != null) {
            Button(attrs = {
                attr(
                    "style",
                    """
                    margin-left: 12px;
                    padding: 4px 8px;
                    border-radius: 6px;
                    border: none;
                    background: #f8fafc;
                    color: #ef4444;
                    font-size: 12px;
                    cursor: pointer;
                """.trimIndent()
                )
                onClick { onDismiss() }
            }) {
                Text("닫기")
            }
        }
    }
}

@Composable
fun LoadingOverlay(
    isVisible: Boolean
) {
    if (!isVisible) return

    // 반투명 배경
    Div({
        attr(
            "style",
            """
            position: fixed;
            inset: 0;
            width: 100vw;
            height: 100vh;
            background: rgba(15, 23, 42, 0.35);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 999;
        """.trimIndent()
        )
    }) {
        // 중앙 로딩 카드
        Div({
            attr(
                "style",
                """
                padding: 16px 24px;
                border-radius: 12px;
                background: #ffffff;
                box-shadow: 0 10px 25px rgba(15, 23, 42, 0.25);
                font-size: 14px;
            """.trimIndent()
            )
        }) {
            Text("계산 중입니다...")
        }
    }
}

@Composable
fun ValidatedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    errorText: String? = null
) {
    Div({
        attr(
            "style",
            """
            display: flex;
            flex-direction: column;
            margin-bottom: 12px;
        """.trimIndent()
        )
    }) {
        Label(attrs = {
            attr(
                "style",
                """
                font-size: 14px;
                margin-bottom: 4px;
            """.trimIndent()
            )
        }) {
            Text(label)
        }

        Input(type = InputType.Text) {
            value(value)
            onInput { event -> onValueChange(event.value) }

            val borderColor = if (errorText == null) "#cbd5f5" else "#f87171"

            attr(
                "style",
                """
                padding: 8px 10px;
                border-radius: 8px;
                font-size: 14px;
                border: 1px solid $borderColor;
                outline: none;
            """.trimIndent()
            )

            if (placeholder.isNotBlank()) {
                attr("placeholder", placeholder)
            }
        }

        if (!errorText.isNullOrBlank()) {
            P({
                attr(
                    "style",
                    """
                    font-size: 12px;
                    color: #f87171;
                    margin-top: 4px;
                    margin-bottom: 0;
                """.trimIndent()
                )
            }) {
                Text(errorText)
            }
        }
    }
}
