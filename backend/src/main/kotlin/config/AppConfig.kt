package config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "adapter",
        "application",
        "domain",
        "config"
    ]
)
class AppConfig

fun main(args: Array<String>) {
    runApplication<AppConfig>(*args)
}
