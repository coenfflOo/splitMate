plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        // 번들 실행 파일 생성 (npm run jsBrowserProductionWebpack 등)
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // 👉 Compose Web
                implementation(compose.runtime)
                implementation(compose.web.core)
                implementation(compose.html.svg)

                // 👉 HTTP API 호출용 Ktor 클라이언트
                implementation("io.ktor:ktor-client-core:3.0.0")
                implementation("io.ktor:ktor-client-js:3.0.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")

                // 👉 WebSocket 클라이언트
                implementation("io.ktor:ktor-client-websockets:3.0.0")

                // 👉 코루틴
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // 👉 JSON 직렬화
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}