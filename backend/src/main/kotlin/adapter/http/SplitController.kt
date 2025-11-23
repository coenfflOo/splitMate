package adapter.http

import adapter.http.dto.*
import domain.fx.ExchangeService
import domain.menu.MenuAssignment
import domain.menu.MenuItem
import domain.menu.Participant
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
        val currency = parseCurrency(request.currency)

        val baseAmount = Money.of(request.totalAmount, currency)
        val taxAmount = Money.of(request.taxAmount, currency)
        val tax = Tax(taxAmount)
        val tip = toDomainTip(request.tip, currency)

        val receipt = Receipt(baseAmount = baseAmount, tax = tax, tip = tip)
        val result = SplitCalculator.splitEvenly(receipt = receipt, peopleCount = request.peopleCount)

        val totalCad = result.total
        val perPersonCad = result.perPerson

        val mode = request.exchange.mode.uppercase()
        val (exchangeResponse, perPersonKrwStr) = when (mode) {
            "NONE" -> null to null

            "MANUAL" -> {
                val (rate, manual) = parseManualRate(request.exchange)
                val krwAmount = perPersonCad.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                val exch = ExchangeOptionResponse(
                    mode = "MANUAL",
                    rate = manual,
                    targetCurrency = "KRW"
                )
                val perKrw = krwAmount.toPlainString()
                exch to perKrw
            }

            "AUTO" -> {
                try {
                    val svc = this.exchangeService ?: throw IllegalStateException("AUTO exchange mode is unavailable")
                    val rate = svc.getCadToKrwRate().rate
                    val krwAmount = perPersonCad.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                    val exch = ExchangeOptionResponse(
                        mode = "AUTO",
                        rate = rate.stripTrailingZeros().toPlainString(),
                        targetCurrency = "KRW"
                    )
                    exch to krwAmount.toPlainString()
                } catch (e: Exception) {
                    val body = ErrorResponse(
                        error = ErrorBody(
                            code = "EXCHANGE_UNAVAILABLE",
                            message = "Failed to fetch exchange rate"
                        )
                    )
                    return ResponseEntity.status(502).body(body)
                }
            }

            else -> null to null
        }

        val response = SplitEvenResponse(
            currency = Currency.CAD.name,
            totalAmountCad = formatMoneyPlain(totalCad),
            peopleCount = request.peopleCount,
            perPersonCad = formatMoneyPlain(perPersonCad),
            exchange = exchangeResponse,
            perPersonKrw = perPersonKrwStr
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/by-menu")
    fun splitByMenu(@RequestBody request: MenuSplitRequest): ResponseEntity<Any> {
        val currency = parseCurrency(request.currency)

        val itemsById: Map<String, MenuItem> = request.items.associate { mi ->
            mi.id to MenuItem(
                id = mi.id,
                name = mi.name,
                price = Money.of(mi.price, currency)
            )
        }

        val participantsById: Map<String, Participant> = request.participants.associate { p ->
            p.id to Participant(id = p.id, displayName = p.name)
        }

        val assignments: List<MenuAssignment> = request.assignments.map { a ->
            val menu = itemsById[a.menuId]
                ?: throw IllegalArgumentException("unknown menuId: ${a.menuId}")
            val ps = a.participantIds.map { pid ->
                participantsById[pid] ?: throw IllegalArgumentException("unknown participantId: $pid")
            }
            MenuAssignment(menuItem = menu, participants = ps)
        }

        val baseAmount = request.items
            .map { Money.of(it.price, currency) }
            .fold(Money.zero(currency)) { acc, m -> acc + m }

        val tax = Tax(Money.of(request.taxAmount, currency))
        val tip = toDomainTip(request.tip, currency)

        val receipt = Receipt(
            baseAmount = baseAmount,
            tax = tax,
            tip = tip
        )

        val result = SplitCalculator.splitByMenu(
            receipt = receipt,
            assignments = assignments
        )

        val mode = request.exchange.mode.uppercase()
        val (exchangeResponse, perPersonKrwList) = when (mode) {
            "NONE" -> null to List(result.shares.size) { null }

            "MANUAL" -> {
                val (rate, manual) = parseManualRate(request.exchange)
                val exch = ExchangeOptionResponse(
                    mode = "MANUAL",
                    rate = manual,
                    targetCurrency = "KRW"
                )
                val krwList = result.shares.map { share ->
                    val krwMoney = convert(share.total, rate)
                    krwMoney.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                }
                exch to krwList
            }

            "AUTO" -> {
                try {
                    val svc = this.exchangeService ?: throw IllegalStateException("AUTO exchange mode is unavailable")
                    val rate = svc.getCadToKrwRate().rate
                    val exch = ExchangeOptionResponse(
                        mode = "AUTO",
                        rate = rate.stripTrailingZeros().toPlainString(),
                        targetCurrency = "KRW"
                    )
                    val krwList = result.shares.map { share ->
                        val krwMoney = convert(share.total, rate)
                        krwMoney.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    }
                    exch to krwList
                } catch (e: Exception) {
                    val body = ErrorResponse(
                        error = ErrorBody(
                            code = "EXCHANGE_UNAVAILABLE",
                            message = "Failed to fetch exchange rate"
                        )
                    )
                    return ResponseEntity.status(502).body(body)
                }
            }

            else -> null to List(result.shares.size) { null }
        }

        val participantsResp = result.shares.mapIndexed { index, share ->
            ParticipantShareResponse(
                id = share.participant.id,
                name = share.participant.displayName,
                subtotalCad = formatMoneyPlain(share.subtotal),
                taxShareCad = formatMoneyPlain(share.taxShare),
                tipShareCad = formatMoneyPlain(share.tipShare),
                totalCad = formatMoneyPlain(share.total),
                totalKrw = perPersonKrwList[index]
            )
        }

        val resp = MenuSplitResponse(
            currency = Currency.CAD.name,
            totalAmountCad = formatMoneyPlain(result.total),
            exchange = exchangeResponse,
            participants = participantsResp
        )

        return ResponseEntity.ok(resp)
    }

    @GetMapping("/health")
    fun health(): String = "OK"

    private fun parseCurrency(code: String): Currency =
        when (code.uppercase()) {
            "CAD" -> Currency.CAD
            "KRW" -> Currency.KRW
            else -> Currency.CAD
        }

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

    private fun parseManualRate(exchange: ExchangeOptionRequest): Pair<BigDecimal, String> {
        val manual = exchange.manualRate
            ?: throw IllegalArgumentException("manualRate is required when exchange.mode=MANUAL")
        val rate = manual.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("manualRate must be a number")
        require(rate > BigDecimal.ZERO) { "manualRate must be > 0" }
        return rate to manual
    }

    private fun convert(cad: Money, rate: BigDecimal): Money {
        val v = cad.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        return Money.of(v, Currency.KRW)
    }

    private fun formatMoneyPlain(money: Money): String =
        money.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
}