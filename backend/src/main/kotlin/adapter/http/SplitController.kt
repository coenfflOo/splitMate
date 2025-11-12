package adapter.http

import adapter.http.dto.*
import domain.money.Currency
import domain.money.Money
import domain.receipt.Receipt
import domain.receipt.Tax
import domain.receipt.Tip
import domain.receipt.TipMode
import domain.split.SplitCalculator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.RoundingMode

@RestController
@RequestMapping("/api/split")
class SplitController {

    /**
     * N분의 1 더치페이 API
     *
     * 예: POST /api/split/even
     */
    @PostMapping("/even")
    fun splitEven(@RequestBody request: SplitEvenRequest): ResponseEntity<SplitEvenResponse> {
        // 1) 통화 파싱 (현재는 CAD만 지원)
        val currency = when (request.currency.uppercase()) {
            "CAD" -> Currency.CAD
            "KRW" -> Currency.KRW   // 혹시나 대비 (실제로는 CAD → KRW 시나리오가 기본)
            else -> Currency.CAD
        }

        // 2) Money/Tax/Tip/Receipt 도메인 객체로 변환
        val baseAmount = Money.of(request.totalAmount, currency)
        val taxAmount = Money.of(request.taxAmount, currency)
        val tax = Tax(taxAmount)

        val tip: Tip? = toDomainTip(request.tip, currency)

        val receipt = Receipt(
            baseAmount = baseAmount,
            tax = tax,
            tip = tip
        )

        // 3) N분의 1 분배 계산
        val result = SplitCalculator.splitEvenly(
            receipt = receipt,
            peopleCount = request.peopleCount
        )

        val totalCad = result.total
        val perPersonCad = result.perPerson

        // 4) 환율 모드 처리 (지금 GREEN 단계에서는 mode=NONE만 신경 쓰면 됨)
        val (exchangeResponse, perPersonKrwStr) = when (request.exchange.mode.uppercase()) {
            "NONE" -> null to null
            else   -> null to null   // 이후 AUTO/MANUAL 모드에서 확장 예정
        }

        // 5) 응답 DTO 작성
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

    /**
     * 메뉴별 더치페이 API
     *
     * 예: POST /api/split/by-menu
     * (아직 구현 전이므로 501 유지)
     */
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

    /**
     * 간단한 헬스 체크용 엔드포인트 (선택)
     */
    @GetMapping("/health")
    fun health(): String = "OK"

    // ----------------------------------------------------------------------
    // 내부 helper 메서드들
    // ----------------------------------------------------------------------

    private fun toDomainTip(tipRequest: TipRequest, currency: Currency): Tip? {
        return when (tipRequest.mode.uppercase()) {
            "PERCENT" -> {
                val percent = tipRequest.percent
                    ?: throw IllegalArgumentException("percent is required when tip mode is PERCENT")
                Tip(
                    mode = TipMode.PERCENT,
                    percent = percent,
                    absolute = null
                )
            }

            "ABSOLUTE" -> {
                val absStr = tipRequest.absolute
                    ?: throw IllegalArgumentException("absolute is required when tip mode is ABSOLUTE")
                Tip(
                    mode = TipMode.ABSOLUTE,
                    percent = null,
                    absolute = Money.of(absStr, currency)
                )
            }

            "NONE" -> null

            else -> null
        }
    }

    /**
     * Money를 "33.00" 같은 문자열로 변환
     */
    private fun formatMoneyPlain(money: Money): String {
        return money.amount
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }
}
