package adapter.fx

import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.money.Currency
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HttpExchangeRateProvider(
    private val authKey: String,
    private val baseUrl: String = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
) : ExchangeRateProvider {

    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        if (base != Currency.CAD || target != Currency.KRW) {
            throw IllegalArgumentException("지원하지 않는 통화 조합: $base -> $target")
        }

        val json = callApiForToday()
        val dealBasR = extractDealBasR(json, "CAD")
        val rate = BigDecimal(dealBasR)

        return ExchangeRate(
            base = Currency.CAD,
            target = Currency.KRW,
            rate = rate
        )
    }

    /**
     * 오늘 날짜 기준으로 AP01(환율) 데이터를 조회한다.
     */
    private fun callApiForToday(): String {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val urlString = "$baseUrl?authkey=$authKey&searchdate=$today&data=AP01"

        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            throw IllegalStateException("환율 API 호출 실패: HTTP ${conn.responseCode}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * JSON 문자열에서 특정 통화(cur_unit)의 객체를 찾아 deal_bas_r 값을 추출한다.
     * 아주 러프한 파서지만, 이번 과제 범위에서는 충분.
     */
    private fun extractDealBasR(json: String, curUnit: String): String {
        val objectRegex = Regex("""\{[^}]*"cur_unit"\s*:\s*"$curUnit"[^}]*}""")
        val objectMatch = objectRegex.find(json)
            ?: throw IllegalStateException("응답에서 $curUnit 환율 정보를 찾을 수 없습니다.")

        val obj = objectMatch.value

        val dealRegex = Regex(""""deal_bas_r"\s*:\s*"([^"]+)"""")
        val dealMatch = dealRegex.find(obj)
            ?: throw IllegalStateException("응답에서 deal_bas_r 값을 찾을 수 없습니다.")

        val raw = dealMatch.groupValues[1]

        return raw.replace(",", "")
    }
}
