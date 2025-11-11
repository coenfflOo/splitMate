package config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["adapter", "domain", "application", "config"])
class SplitMateApplication

fun main(args: Array<String>) {
    runApplication<SplitMateApplication>(*args)
}
