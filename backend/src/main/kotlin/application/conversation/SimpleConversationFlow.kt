package application.conversation

import domain.conversation.ConversationOutput
import domain.conversation.ConversationStep
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
import domain.split.SplitMode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class SimpleConversationFlow(
    private val exchangeService: ExchangeService? = null
) : ConversationFlow {

    override fun start(): ConversationOutput {
        return ConversationOutput(
            message = """
            GROUP 대화를 시작합니다!
            분배 방식을 선택해주세요:
            1) N분의1
            2) 메뉴별 분배
        """.trimIndent(),
            nextStep = ConversationStep.ASK_SPLIT_MODE,
            context = ConversationContext()
        )
    }

    override fun handle(
        step: ConversationStep,
        input: String,
        context: ConversationContext
    ): ConversationOutput {
        return when (step) {
            ConversationStep.ASK_TOTAL_AMOUNT -> handleTotal(input, context)
            ConversationStep.ASK_TAX -> handleTax(input, context)
            ConversationStep.ASK_TIP_MODE -> handleTipMode(input, context)
            ConversationStep.ASK_TIP_VALUE -> handleTipValue(input, context)
            ConversationStep.ASK_SPLIT_MODE -> handleSplitMode(input, context)
            ConversationStep.ASK_PEOPLE_COUNT -> handlePeopleCount(input, context)

            ConversationStep.ASK_MENU_ITEMS -> handleMenuItems(input, context)
            ConversationStep.ASK_MENU_PARTICIPANTS -> handleMenuParticipants(input, context)
            ConversationStep.ASK_MENU_ASSIGNMENTS -> handleMenuAssignments(input, context)

            ConversationStep.ASK_EXCHANGE_RATE_MODE -> handleExchangeMode(input, context)
            ConversationStep.ASK_EXCHANGE_RATE_VALUE -> handleExchangeValue(input, context)

            ConversationStep.SHOW_RESULT -> showResult(context)
            ConversationStep.RESTART_CONFIRM -> handleRestart(input, context)

            else -> ConversationOutput(
                message = "알 수 없는 단계입니다. 처음부터 다시 시작해주세요.",
                nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                context = ConversationContext()
            )
        }
    }

    private fun handleTotal(input: String, ctx: ConversationContext): ConversationOutput {
        val amount = parsePositiveMoney(input) ?: return retry(
            ctx, ConversationStep.ASK_TOTAL_AMOUNT,
            "총 금액을 숫자로 입력해주세요. (예: 27.40)"
        )

        val newCtx = ctx.copy(
            baseAmount = amount,
            lastStep = ConversationStep.ASK_TOTAL_AMOUNT,
            failureCount = 0
        )

        return ConversationOutput(
            message = "세금 금액을 입력해주세요. 없으면 '없음' 또는 0",
            nextStep = ConversationStep.ASK_TAX,
            context = newCtx
        )
    }

    private fun handleTax(input: String, ctx: ConversationContext): ConversationOutput {
        val tax = parseTaxMoney(input) ?: return retry(
            ctx, ConversationStep.ASK_TAX,
            "세금은 숫자 또는 '없음'으로 입력해주세요."
        )

        val newCtx = ctx.copy(
            taxAmount = tax,
            lastStep = ConversationStep.ASK_TAX,
            failureCount = 0
        )

        return ConversationOutput(
            message = """
                팁 방식을 선택해주세요:
                1) 퍼센트
                2) 금액
                3) 없음
            """.trimIndent(),
            nextStep = ConversationStep.ASK_TIP_MODE,
            context = newCtx
        )
    }

    private fun handleTipMode(input: String, ctx: ConversationContext): ConversationOutput {
        val mode = parseTipMode(input) ?: return retry(
            ctx, ConversationStep.ASK_TIP_MODE,
            "1(퍼센트) / 2(금액) / 3(없음) 중 선택해주세요."
        )

        val newCtx = ctx.copy(
            tipMode = mode,
            tipPercent = null,
            tipAbsolute = null,
            lastStep = ConversationStep.ASK_TIP_MODE,
            failureCount = 0
        )

        return when (mode) {
            TipMode.NONE -> ConversationOutput(
                message = "인원 수를 입력해주세요. (예: 3)",
                nextStep = ConversationStep.ASK_PEOPLE_COUNT,
                context = newCtx
            )

            TipMode.PERCENT, TipMode.ABSOLUTE -> ConversationOutput(
                message = if (mode == TipMode.PERCENT)
                    "팁 퍼센트를 입력해주세요. (예: 15)"
                else
                    "팁 금액을 입력해주세요. (예: 10.00)",
                nextStep = ConversationStep.ASK_TIP_VALUE,
                context = newCtx
            )
        }
    }

    // ---------------------------
    // 4) TIP VALUE
    // ---------------------------
    private fun handleTipValue(input: String, ctx: ConversationContext): ConversationOutput {
        val mode = ctx.tipMode ?: TipMode.NONE

        val newCtx = when (mode) {
            TipMode.PERCENT -> {
                val p = input.trim().toIntOrNull()
                if (p == null || p < 0 || p > 100) {
                    return retry(ctx, ConversationStep.ASK_TIP_VALUE, "0~100 사이 퍼센트를 입력해주세요.")
                }
                ctx.copy(tipPercent = p)
            }

            TipMode.ABSOLUTE -> {
                val abs = parsePositiveMoney(input)
                    ?: return retry(ctx, ConversationStep.ASK_TIP_VALUE, "팁 금액을 숫자로 입력해주세요.")
                ctx.copy(tipAbsolute = abs)
            }

            TipMode.NONE -> ctx
        }.copy(
            lastStep = ConversationStep.ASK_TIP_VALUE,
            failureCount = 0
        )

        // Tip이 끝나면 splitMode가 N_DIVIDE인 경우 사람수로 바로
        return ConversationOutput(
            message = "인원 수를 입력해주세요. (예: 3)",
            nextStep = ConversationStep.ASK_PEOPLE_COUNT,
            context = newCtx
        )
    }

    // ---------------------------
    // 5) SPLIT MODE
    // ---------------------------
    private fun handleSplitMode(input: String, ctx: ConversationContext): ConversationOutput {
        val mode = parseSplitMode(input) ?: return retry(
            ctx, ConversationStep.ASK_SPLIT_MODE,
            "1(N분의1) / 2(메뉴별) 중 선택해주세요."
        )

        val newCtx = ctx.copy(
            splitMode = mode,
            lastStep = ConversationStep.ASK_SPLIT_MODE,
            failureCount = 0
        )

        return when (mode) {
            SplitMode.N_DIVIDE -> ConversationOutput(
                message = "총 결제 금액을 입력해주세요. (예: 27.40)",
                nextStep = ConversationStep.ASK_TOTAL_AMOUNT,   // ✅ 이제 여기서부터 총액 시작
                context = newCtx
            )

            SplitMode.MENU_BASED -> ConversationOutput(
                message = """
                메뉴를 입력해주세요.
                형식: "이름 가격; 이름 가격; ..."
                예) 파스타 18.9; 피자 22; 콜라 3
            """.trimIndent(),
                nextStep = ConversationStep.ASK_MENU_ITEMS,      // ✅ 메뉴 플로우 바로 진입
                context = newCtx
            )
        }
    }

    // ---------------------------
    // 6) PEOPLE COUNT
    // ---------------------------
    private fun handlePeopleCount(input: String, ctx: ConversationContext): ConversationOutput {
        val n = input.trim().toIntOrNull()
        if (n == null || n < 1) {
            return retry(ctx, ConversationStep.ASK_PEOPLE_COUNT, "인원 수는 1 이상의 정수로 입력해주세요.")
        }

        val newCtx = ctx.copy(
            peopleCount = n,
            lastStep = ConversationStep.ASK_PEOPLE_COUNT,
            failureCount = 0
        )

        return ConversationOutput(
            message = """
                환율 모드를 선택해주세요:
                1) 자동(오늘 환율)
                2) 수동 입력
                3) KRW 생략
            """.trimIndent(),
            nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
            context = newCtx
        )
    }

    private fun handleMenuItems(input: String, ctx: ConversationContext): ConversationOutput {
        if (input.startsWith("MENU_PAYLOAD:")) {
            val parsed = parseMenuPayload(input)
                ?: return retry(ctx, ConversationStep.ASK_MENU_ITEMS, "메뉴 payload 파싱에 실패했습니다. 다시 시도해주세요.")

            val (items, participants, assignments) = parsed

            val derivedCtx = applyMenuDerived(ctx, items, participants)

            val newCtx = derivedCtx.copy(
                menuItems = items,
                menuParticipants = participants,
                menuAssignments = assignments,
                lastStep = ConversationStep.ASK_MENU_ITEMS,
                failureCount = 0
            )

            return ConversationOutput(
                message = """
            메뉴별 입력이 완료되었습니다.
            환율 모드를 선택해주세요:
            1) 자동(오늘 환율)
            2) 수동 입력
            3) KRW 생략
        """.trimIndent(),
                nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                context = newCtx
            )
        }

        val items = parseMenuItems(input)
            ?: return retry(ctx, ConversationStep.ASK_MENU_ITEMS, "메뉴 형식이 올바르지 않습니다. 예시 형태로 입력해주세요.")

        val newCtx = ctx.copy(
            menuItems = items,
            lastStep = ConversationStep.ASK_MENU_ITEMS,
            failureCount = 0
        )

        return ConversationOutput(
            message = """
            참가자를 입력해주세요.
            형식: "이름, 이름, 이름"
            예) 민지, 철수, 영희
        """.trimIndent(),
            nextStep = ConversationStep.ASK_MENU_PARTICIPANTS,
            context = newCtx
        )
    }

    // ---------------------------
    // 8) MENU PARTICIPANTS
    // ---------------------------
    private fun handleMenuParticipants(input: String, ctx: ConversationContext): ConversationOutput {
        val participants = parseParticipants(input)
            ?: return retry(ctx, ConversationStep.ASK_MENU_PARTICIPANTS, "참가자 이름을 쉼표로 구분해 입력해주세요.")

        val newCtx = ctx.copy(
            menuParticipants = participants,
            lastStep = ConversationStep.ASK_MENU_PARTICIPANTS,
            failureCount = 0
        )

        val menuGuide = participants.joinToString("\n") { "${it.id}) ${it.name}" }
        val itemGuide = ctx.menuItems.joinToString("\n") { "${it.id}) ${it.name}(${it.priceCad})" }

        return ConversationOutput(
            message = """
                메뉴별로 누가 먹었는지 지정해주세요.
                형식: "m1:p1,p2; m2:p2; ..."
                
                메뉴:
                $itemGuide
                
                참가자:
                $menuGuide
                
                예) m1:p1,p2; m2:p2; m3:p1,p3
            """.trimIndent(),
            nextStep = ConversationStep.ASK_MENU_ASSIGNMENTS,
            context = newCtx
        )
    }

    // ---------------------------
    // 9) MENU ASSIGNMENTS
    // ---------------------------
    private fun handleMenuAssignments(input: String, ctx: ConversationContext): ConversationOutput {
        if (input.startsWith("MENU_PAYLOAD:")) {
            val parsed = parseMenuPayload(input)
                ?: return retry(ctx, ConversationStep.ASK_MENU_ITEMS, "메뉴 payload 파싱에 실패했습니다. 다시 시도해주세요.")

            val (items, participants, assignments) = parsed

            val derivedCtx = applyMenuDerived(ctx, items, participants)

            val newCtx = derivedCtx.copy(
                menuItems = items,
                menuParticipants = participants,
                menuAssignments = assignments,
                lastStep = ConversationStep.ASK_MENU_ITEMS,
                failureCount = 0
            )

            return ConversationOutput(
                message = """
            메뉴별 입력이 완료되었습니다.
            환율 모드를 선택해주세요:
            1) 자동(오늘 환율)
            2) 수동 입력
            3) KRW 생략
        """.trimIndent(),
                nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                context = newCtx
            )
        }

        val assignments = parseAssignments(input, ctx)
            ?: return retry(ctx, ConversationStep.ASK_MENU_ASSIGNMENTS, "지정 형식이 올바르지 않습니다. 예시대로 입력해주세요.")

        val derivedCtx = applyMenuDerived(ctx, ctx.menuItems, ctx.menuParticipants)

        val newCtx = derivedCtx.copy(
            menuAssignments = assignments,
            lastStep = ConversationStep.ASK_MENU_ASSIGNMENTS,
            failureCount = 0
        )

        return ConversationOutput(
            message = """
        환율 모드를 선택해주세요:
        1) 자동(오늘 환율)
        2) 수동 입력
        3) KRW 생략
    """.trimIndent(),
            nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
            context = newCtx
        )
    }


    // ---------------------------
    // 10) EXCHANGE MODE
    // ---------------------------
    private fun handleExchangeMode(input: String, ctx: ConversationContext): ConversationOutput {
        val mode = input.trim().lowercase()

        return when (mode) {
            "1", "auto", "자동" -> {
                val svc = exchangeService
                    ?: return retry(ctx, ConversationStep.ASK_EXCHANGE_RATE_MODE,
                        "자동 환율 기능이 아직 준비되지 않았습니다. 2(수동) 또는 3(생략)을 선택해주세요.")

                val rate = runCatching { svc.getCadToKrwRate().rate }.getOrNull()
                    ?: return retry(ctx, ConversationStep.ASK_EXCHANGE_RATE_MODE,
                        "자동 환율 조회에 실패했습니다. 2(수동) 또는 3(생략)을 선택해주세요.")

                val newCtx = ctx.copy(
                    wantKrw = true,
                    manualRate = rate,
                    lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                    failureCount = 0
                )

                showResult(newCtx)
            }

            "2", "manual", "수동" -> {
                ConversationOutput(
                    message = "환율 값을 입력해주세요. (예: 980.5)",
                    nextStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
                    context = ctx.copy(
                        wantKrw = true,
                        lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                        failureCount = 0
                    )
                )
            }

            "3", "none", "생략", "krw 생략" -> {
                val newCtx = ctx.copy(
                    wantKrw = false,
                    manualRate = null,
                    lastStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
                    failureCount = 0
                )

                // ✅ "CAD만 계산합니다" 안내는 굳이 별도 단계로 안 보내고
                //    결과 메시지 안에서 자연스럽게 CAD만 출력
                showResult(newCtx)
            }

            else -> retry(ctx, ConversationStep.ASK_EXCHANGE_RATE_MODE, "1(자동)/2(수동)/3(생략) 중 선택해주세요.")
        }
    }

    // ---------------------------
    // 11) EXCHANGE VALUE
    // ---------------------------
    private fun handleExchangeValue(input: String, ctx: ConversationContext): ConversationOutput {
        val rate = parsePositiveDecimal(input)
            ?: return retry(ctx, ConversationStep.ASK_EXCHANGE_RATE_VALUE, "환율은 0보다 큰 숫자로 입력해주세요.")

        val newCtx = ctx.copy(
            manualRate = rate,
            wantKrw = true,
            lastStep = ConversationStep.ASK_EXCHANGE_RATE_VALUE,
            failureCount = 0
        )

        return ConversationOutput(
            message = "입력한 환율 $rate KRW/CAD 를 적용해 계산합니다.",
            nextStep = ConversationStep.SHOW_RESULT,
            context = newCtx
        )
    }

    // ---------------------------
    // 12) SHOW RESULT
    // ---------------------------
    private fun showResult(ctx: ConversationContext): ConversationOutput {
        val base = ctx.baseAmount ?: return retry(ctx, ConversationStep.ASK_TOTAL_AMOUNT, "총 금액부터 다시 입력해주세요.")
        val tax = ctx.taxAmount ?: Money.zero(base.currency)

        val tip = when (ctx.tipMode) {
            TipMode.PERCENT -> ctx.tipPercent?.let { Tip(TipMode.PERCENT, percent = it, absolute = null) }
            TipMode.ABSOLUTE -> ctx.tipAbsolute?.let { Tip(TipMode.ABSOLUTE, percent = null, absolute = it) }
            else -> null
        }

        val receipt = Receipt(baseAmount = base, tax = Tax(tax), tip = tip)

        val resultMessage = when (ctx.splitMode) {
            SplitMode.N_DIVIDE -> {
                val people = ctx.peopleCount ?: 1
                val r = SplitCalculator.splitEvenly(receipt, people)

                val totalCad = formatMoney(r.total)
                val perCad = formatMoney(r.perPerson)

                val perKrw = if (ctx.wantKrw && ctx.manualRate != null) {
                    val krw = r.perPerson.amount.multiply(ctx.manualRate)
                        .setScale(0, RoundingMode.HALF_UP)
                    "${krw.toPlainString()} KRW"
                } else null

                buildString {
                    appendLine("✅ 총액: $totalCad CAD")
                    appendLine("✅ 1인당: $perCad CAD")
                    if (perKrw != null) appendLine("✅ 1인당(KRW): $perKrw")
                }
            }

            SplitMode.MENU_BASED -> {
                val items = ctx.menuItems
                val persons = ctx.menuParticipants
                val assigns = ctx.menuAssignments

                if (items.isEmpty() || persons.isEmpty() || assigns.isEmpty()) {
                    return retry(ctx, ConversationStep.ASK_MENU_ITEMS, "메뉴 입력이 부족합니다. 메뉴부터 다시 입력해주세요.")
                }

                val itemsById = items.associate { it.id to
                        MenuItem(
                            id = it.id,
                            name = it.name,
                            price = Money.of(it.priceCad, Currency.CAD)
                        )
                }

                val personsById = persons.associate { it.id to Participant(it.id, it.name) }

                val assignmentList = assigns.map { (menuId, pids) ->
                    val menu = itemsById[menuId]
                        ?: throw IllegalArgumentException("unknown menuId: $menuId")
                    val ps = pids.map { pid ->
                        personsById[pid] ?: throw IllegalArgumentException("unknown participantId: $pid")
                    }
                    MenuAssignment(menu, ps)
                }

                val r = SplitCalculator.splitByMenu(receipt, assignmentList)

                val lines = r.shares.map { share ->
                    val cad = formatMoney(share.total)
                    val krw = if (ctx.wantKrw && ctx.manualRate != null) {
                        val v = share.total.amount.multiply(ctx.manualRate)
                            .setScale(0, RoundingMode.HALF_UP)
                        " (${v.toPlainString()} KRW)"
                    } else ""
                    "- ${share.participant.displayName}: $cad CAD$krw"
                }

                buildString {
                    appendLine("✅ 총액: ${formatMoney(r.total)} CAD")
                    appendLine("✅ 메뉴별 결과")
                    lines.forEach { appendLine(it) }
                }
            }

            else -> "분배 방식이 선택되지 않았습니다."
        }

        return ConversationOutput(
            message = """
                $resultMessage
                
                다시 시작할까요?
                1) YES
                2) NO
            """.trimIndent(),
            nextStep = ConversationStep.RESTART_CONFIRM,
            context = ctx.copy(lastStep = ConversationStep.SHOW_RESULT),
            isFinished = false
        )
    }

    // ---------------------------
    // 13) RESTART
    // ---------------------------
    private fun handleRestart(input: String, ctx: ConversationContext): ConversationOutput {
        return when (input.trim().lowercase()) {
            "1", "y", "yes", "네", "응" -> start()
            "2", "n", "no", "아니오", "아니" -> ConversationOutput(
                message = "대화를 종료합니다. 이용해주셔서 감사합니다!",
                nextStep = ConversationStep.RESTART_CONFIRM,
                context = ctx,
                isFinished = true
            )
            else -> retry(ctx, ConversationStep.RESTART_CONFIRM, "1(YES) 또는 2(NO)로 입력해주세요.")
        }
    }

    // ---------------------------
    // helpers
    // ---------------------------
    private fun retry(ctx: ConversationContext, step: ConversationStep, msg: String): ConversationOutput {
        val newCtx = ctx.copy(
            failureCount = ctx.failureCount + 1,
            lastStep = step
        )
        return ConversationOutput(
            message = msg,
            nextStep = step,
            context = newCtx
        )
    }

    private fun parsePositiveMoney(input: String): Money? {
        val v = parsePositiveDecimal(input) ?: return null
        return Money.of(v, Currency.CAD)
    }

    private fun parseTaxMoney(input: String): Money? {
        val norm = input.trim().lowercase()
        if (norm in listOf("없음", "none", "no", "0")) {
            return Money.zero(Currency.CAD)
        }
        val v = parsePositiveDecimal(norm) ?: return null
        return Money.of(v, Currency.CAD)
    }

    private fun parsePositiveDecimal(input: String): BigDecimal? {
        val s = input.replace(",", "").trim()
        val v = s.toBigDecimalOrNull() ?: return null
        if (v < BigDecimal.ZERO) return null
        return v
    }

    private fun parseTipMode(input: String): TipMode? {
        return when (input.trim().lowercase()) {
            "1", "percent", "퍼센트" -> TipMode.PERCENT
            "2", "absolute", "금액" -> TipMode.ABSOLUTE
            "3", "none", "없음" -> TipMode.NONE
            else -> null
        }
    }

    private fun parseSplitMode(input: String): SplitMode? {
        return when (input.trim().lowercase()) {
            "1", "n", "n_divide", "n분의1" -> SplitMode.N_DIVIDE
            "2", "menu", "menu_based", "메뉴", "메뉴별" -> SplitMode.MENU_BASED
            else -> null
        }
    }

    private fun parseMenuItems(input: String): List<MenuItemInput>? {
        val parts = input.split(";").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return null

        val items = mutableListOf<MenuItemInput>()
        var idx = 1

        for (p in parts) {
            val tokens = p.split(" ").filter { it.isNotBlank() }
            if (tokens.size < 2) return null
            val name = tokens.dropLast(1).joinToString(" ")
            val price = tokens.last().replace(",", "").toBigDecimalOrNull() ?: return null
            if (price <= BigDecimal.ZERO) return null

            items += MenuItemInput(
                id = "m$idx",
                name = name,
                priceCad = price
            )
            idx++
        }
        return items
    }

    private fun parseParticipants(input: String): List<MenuParticipantInput>? {
        val names = input.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (names.isEmpty()) return null

        return names.mapIndexed { i, n ->
            MenuParticipantInput(id = "p${i+1}", name = n)
        }
    }

    private fun parseAssignments(input: String, ctx: ConversationContext): Map<String, List<String>>? {
        val menus = ctx.menuItems.map { it.id }.toSet()
        val persons = ctx.menuParticipants.map { it.id }.toSet()

        val pairs = input.split(";").map { it.trim() }.filter { it.isNotBlank() }
        if (pairs.isEmpty()) return null

        val map = mutableMapOf<String, List<String>>()

        for (pair in pairs) {
            val idx = pair.indexOf(":")
            if (idx == -1) return null

            val menuId = pair.substring(0, idx).trim()
            val pids = pair.substring(idx + 1).split(",").map { it.trim() }.filter { it.isNotBlank() }

            if (menuId !in menus) return null
            if (pids.isEmpty() || pids.any { it !in persons }) return null

            map[menuId] = pids
        }

        // 모든 메뉴가 최소 1명 배정되었는지 체크
        if (!menus.all { map[it]?.isNotEmpty() == true }) return null

        return map
    }

    private fun applyMenuDerived(
        ctx: ConversationContext,
        items: List<MenuItemInput>,
        participants: List<MenuParticipantInput>
    ): ConversationContext {
        val base = ctx.baseAmount ?: run {
            val sum = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.priceCad }
            Money.of(sum, Currency.CAD)
        }

        val people = ctx.peopleCount ?: participants.size

        return ctx.copy(
            baseAmount = base,
            peopleCount = people
        )
    }

    private fun formatMoney(m: Money): String =
        m.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
}