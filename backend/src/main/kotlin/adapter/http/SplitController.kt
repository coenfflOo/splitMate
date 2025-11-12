package adapter.http

import adapter.http.dto.*
import domain.fx.ExchangeService
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode

@RestController
@RequestMapping("/api/split")
class SplitController(
    private val exchangeService: ExchangeService? = null
) {

    @PostMapping("/even")
    fun splitEven(@RequestBody request: SplitEvenRequest): ResponseEntity<Any> {
        val currency = when (request.currency.uppercase()) {
            "CAD" -> Currency.CAD
            "KRW" -> Currency.KRW
            else -> Currency.CAD
        }

        val baseAmount = Money.of(request.totalAmount, currency)
        val taxAmount = Money.of(request.taxAmount, currency)
        val tax = Tax(taxAmount)
        val tip: Tip? = toDomainTip(request.tip, currency)

        val receipt = Receipt(baseAmount = baseAmount, tax = tax, tip = tip)
        val result = SplitCalculator.splitEvenly(receipt = receipt, peopleCount = request.peopleCount)

        val totalCad = result.total
        val perPersonCad = result.perPerson

        val mode = request.exchange.mode.uppercase()
        val (exchangeResponse, perPersonKrwStr) = when (mode) {
            "NONE" -> null to null

            "MANUAL" -> {
                val manual = request.exchange.manualRate
                    ?: throw IllegalArgumentException("manualRate is required when exchange.mode=MANUAL")

                val rate = manual.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("manualRate must be a number")

                if (rate <= java.math.BigDecimal.ZERO) {
                    throw IllegalArgumentException("manualRate must be > 0")
                }

                val krwAmount = perPersonCad.amount.multiply(rate)
                    .setScale(2, java.math.RoundingMode.HALF_UP)

                val exch = ExchangeOptionResponse(
                    mode = "MANUAL",
                    rate = manual,
                    targetCurrency = "KRW"
                )
                val perKrw = krwAmount.toPlainString()
                exch to perKrw
            }

            // AUTO는 다음 GREEN에서 구현
            else -> null to null
        }

        val response = SplitEvenResponse(
            currency = "CAD",
            totalAmountCad = formatMoneyPlain(totalCad),
            peopleCount = request.peopleCount,
            perPersonCad = formatMoneyPlain(perPersonCad),
            exchange = exchangeResponse,
            perPersonKrw = perPersonKrwStr
        )
        return ResponseEntity.ok(response)
    }



    @PostMapping("/by-menu")
    fun splitByMenu(@RequestBody request: MenuSplitRequest): ResponseEntity<MenuSplitResponse> {
        return ResponseEntity.status(501)
            .body(
                MenuSplitResponse(
                    currency = request.currency,
                    totalAmountCad = "0.00",
                    exchange = null,
                    participants = emptyList()
                )
            )
    }

    @GetMapping("/health")
    fun health(): String = "OK"

    // ---------------- helpers ----------------

    private fun toDomainTip(tipRequest: TipRequest, currency: Currency): Tip? =
        when (tipRequest.mode.uppercase()) {
            "PERCENT" -> {
                val percent = tipRequest.percent
                    ?: throw IllegalArgumentException("percent is required when tip mode is PERCENT")
                Tip(mode = TipMode.PERCENT, percent = percent, absolute = null)
            }
            "ABSOLUTE" -> {
                val absStr = tipRequest.absolute
                    ?: throw IllegalArgumentException("absolute is required when tip mode is ABSOLUTE")
                Tip(mode = TipMode.ABSOLUTE, percent = null, absolute = Money.of(absStr, currency))
            }
            "NONE" -> null
            else -> null
        }

    private fun convert(cad: Money, rate: BigDecimal): Money {
        val v = cad.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return Money.of(v, Currency.KRW)
    }

    private fun formatMoneyPlain(money: Money): String =
        money.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun formatRateForResponse(rate: BigDecimal): String {
        val rounded = rate.setScale(0, RoundingMode.HALF_UP).toPlainString()
        return addComma(rounded)
    }

    private fun addComma(intStr: String): String {
        val s = intStr.reversed().chunked(3).joinToString(",").reversed()
        return s
    }
}
