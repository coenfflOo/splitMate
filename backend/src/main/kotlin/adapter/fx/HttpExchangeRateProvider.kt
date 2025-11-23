package adapter.fx

import domain.fx.ExchangeRate
import domain.fx.ExchangeRateProvider
import domain.money.Currency
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HttpExchangeRateProvider(
    private val authKey: String,
    private val baseUrl: String = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
) : ExchangeRateProvider {

    override fun getRate(base: Currency, target: Currency): ExchangeRate {
        require(base == Currency.CAD && target == Currency.KRW) {
            "지원하지 않는 통화 조합: $base -> $target"
        }

        val json = callApiWithFallback()
        val dealBasR = extractDealBasR(json, "CAD")
        val rate = BigDecimal(dealBasR)

        return ExchangeRate(
            base = Currency.CAD,
            target = Currency.KRW,
            rate = rate
        )
    }

    private fun callApiWithFallback(): String {
        var date = lastBusinessDayInKorea(LocalDate.now(ZoneId.of("Asia/Seoul")))
        repeat(5) { // 최대 5영업일 전까지 시도
            val json = callApi(date)
            if (json.trim().startsWith("[") && json.trim() != "[]") {
                return json
            }
            date = date.minusDays(1)
        }
        throw IllegalStateException("최근 영업일 환율 데이터를 찾지 못했습니다.")
    }

    private fun callApi(date: LocalDate): String {
        val searchDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val urlString = "$baseUrl?authkey=$authKey&searchdate=$searchDate&data=AP01"

        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SplitMate/1.0")
        }

        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("환율 API 호출 실패: HTTP $code ${err ?: ""}".trim())
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun lastBusinessDayInKorea(todayKst: LocalDate): LocalDate {
        return when (todayKst.dayOfWeek) {
            DayOfWeek.SATURDAY -> todayKst.minusDays(1)
            DayOfWeek.SUNDAY -> todayKst.minusDays(2)
            else -> todayKst
        }
    }

    private fun extractDealBasR(json: String, curUnit: String): String {
        val objectRegex = Regex("""\{[^}]*"cur_unit"\s*:\s*"$curUnit"[^}]*}""")
        val objectMatch = objectRegex.find(json)
            ?: throw IllegalStateException("응답에서 $curUnit 환율 정보를 찾을 수 없습니다.")

        val obj = objectMatch.value

        val dealRegex = Regex(""""deal_bas_r"\s*:\s*"([^"]+)"""")
        val dealMatch = dealRegex.find(obj)
            ?: throw IllegalStateException("응답에서 deal_bas_r 값을 찾을 수 없습니다.")

        return dealMatch.groupValues[1].replace(",", "")
    }
}