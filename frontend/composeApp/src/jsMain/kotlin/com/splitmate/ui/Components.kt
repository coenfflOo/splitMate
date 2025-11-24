package com.splitmate.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Div

data class UiError(
    val code: String? = null,
    val message: String
)

@Composable
fun ErrorBanner(
    error: UiError?,
    onDismiss: (() -> Unit)? = null
) {
    if (error == null) return

    Div({
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

@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(attrs = {
        style {
            padding(10.px, 14.px)
            borderRadius(999.px)
            fontSize(14.px)
            cursor("pointer")

            border {
                style(LineStyle.Solid)
                width(1.px)
                color(if (isSelected) rgb(88, 101, 242) else rgb(70, 80, 120))
            }

            backgroundColor(if (isSelected) rgb(88, 101, 242) else rgb(15, 18, 30))
            color(Color.white)

            property("transition", "all 0.12s ease")
        }
        onClick { onClick() }
    }) { Text(text) }
}


@Composable
fun ToastError(
    message: String?,
    onDismiss: () -> Unit = {}
) {
    if (message.isNullOrBlank()) return

    var visible by remember(message) { mutableStateOf(true) }

    LaunchedEffect(message) {
        visible = true
        delay(2800)
        visible = false
        delay(200)
        onDismiss()
    }

    if (!visible) return

    Div({
        style {
            position(Position.Fixed)
            top(16.px)
            right(16.px)
            padding(12.px, 16.px)
            borderRadius(10.px)
            backgroundColor(rgb(239, 68, 68))
            color(Color.white)
            fontSize(14.px)
            property("box-shadow", "0 8px 20px rgba(0,0,0,0.35)")
            property("z-index", "9999")
        }
    }) {
        Text(message)
    }
}
