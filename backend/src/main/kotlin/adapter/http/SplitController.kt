package adapter.http

import adapter.http.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/split")
class SplitController {

    //N분의 1 더치페이 API
    @PostMapping("/even")
    fun splitEven(@RequestBody request: SplitEvenRequest): ResponseEntity<SplitEvenResponse> {
        // TODO: request → 도메인(Receipt, Tip, etc) 변환
        // TODO: SplitCalculator.splitEvenly 호출
        // TODO: 결과 → SplitEvenResponse 매핑
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(
                SplitEvenResponse(
                    currency = request.currency,
                    totalAmountCad = "0.00",
                    peopleCount = request.peopleCount,
                    perPersonCad = "0.00",
                    exchange = null,
                    perPersonKrw = null
                )
            )
    }

    //메뉴별 더치페이 API
    @PostMapping("/by-menu")
    fun splitByMenu(@RequestBody request: MenuSplitRequest): ResponseEntity<MenuSplitResponse> {
        // TODO: request.items → MenuItem
        // TODO: request.participants → Participant
        // TODO: request.assignments → MenuAssignment
        // TODO: SplitCalculator.splitByMenu 호출
        // TODO: 결과 → MenuSplitResponse 매핑

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
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
}
