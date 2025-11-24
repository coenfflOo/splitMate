package com.splitmate.styles

import org.jetbrains.compose.web.css.*

object AppStyles : StyleSheet() {

    val page by style {
        property("min-height", "100vh")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        backgroundColor(rgb(10, 13, 24))
        color(Color.white)
        fontFamily("system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif")
    }

    val card by style {
        padding(32.px)
        borderRadius(16.px)
        backgroundColor(rgb(20, 24, 40))
        property("box-shadow", "0 18px 45px rgba(0,0,0,0.45)")
        maxWidth(480.px)
        textAlign("center")
    }

    val buttonRow by style {
        marginTop(24.px)
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        property("gap", "12px")
    }

    val formColumn by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Stretch)
        property("gap", "12px")
        marginTop(16.px)
        textAlign("left")
    }

    val textField by style {
        padding(8.px, 12.px)
        borderRadius(8.px)
        border {
            style(LineStyle.Solid)
            width(1.px)
            color(rgb(70, 80, 120))
        }
        backgroundColor(rgb(15, 18, 30))
        color(Color.white)
        fontSize(14.px)
    }

    val errorText by style {
        color(Color.red)
        fontSize(13.px)
    }

    val backButtonRow by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.FlexStart)
        marginBottom(16.px)
    }

    val primaryButton by style {
        padding(10.px, 16.px)
        borderRadius(999.px)
        border {
            style(LineStyle.None)
        }
        backgroundColor(rgb(88, 101, 242))
        color(Color.white)
        fontSize(14.px)
        fontWeight("600")
        cursor("pointer")
        property("transition", "background-color 0.15s ease, transform 0.08s ease")

        hover {
            backgroundColor(rgb(100, 115, 255))
            property("transform", "translateY(-1px)")
        }

        active {
            property("transform", "translateY(0)")
            backgroundColor(rgb(70, 85, 220))
        }
    }

    val secondaryButton by style {
        padding(8.px, 16.px)
        borderRadius(999.px)
        backgroundColor(Color.transparent)
        color(Color.white)
        border {
            style(LineStyle.Solid)
            width(1.px)
            color(Color.white)
        }
        property("cursor", "pointer")
    }
}