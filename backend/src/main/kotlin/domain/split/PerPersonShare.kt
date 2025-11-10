package domain.split

import domain.menu.Participant
import domain.money.Money

/**
 * 메뉴별 분배 결과에서 한 사람의 최종 부담 요약
 */
data class PerPersonShare(
    val participant: Participant,
    val subtotal: Money,   // 메뉴 금액 합 (세금/팁 전)
    val taxShare: Money,   // 세금 배분
    val tipShare: Money,   // 팁 배분
    val total: Money       // subtotal + taxShare + tipShare
)
