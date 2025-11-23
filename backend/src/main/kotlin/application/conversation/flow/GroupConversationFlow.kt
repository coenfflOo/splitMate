package application.conversation.flow

import application.conversation.model.ConversationContext
import application.conversation.model.ConversationOutput
import application.conversation.model.ConversationStep
import application.conversation.model.MenuItemInput
import application.conversation.model.MenuParticipantInput
import application.conversation.payload.parseMenuPayload
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
class GroupConversationFlow(
    private val exchangeService: ExchangeService? = null
) : BaseConversationFlow(exchangeService) {

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

    override fun splitModePromptMessage(): String =
        """
            분배 방식을 선택해주세요:
            1) N분의1
            2) 메뉴별 분배
        """.trimIndent()

    override fun splitModeGuideMessage(): String =
        "1(N분의1) / 2(메뉴별) 중 선택해주세요."

    override fun handleSplitMode(input: String, context: ConversationContext): ConversationOutput {
        val mode = parseSplitMode(input)
            ?: return retry(context, ConversationStep.ASK_SPLIT_MODE, splitModeGuideMessage())

        val newCtx = context.copy(
            splitMode = mode,
            lastStep = ConversationStep.ASK_SPLIT_MODE,
            failureCount = 0
        )

        return when (mode) {
            SplitMode.N_DIVIDE -> ConversationOutput(
                message = "총 결제 금액을 입력해주세요. (예: 27.40)",
                nextStep = ConversationStep.ASK_TOTAL_AMOUNT,
                context = newCtx
            )

            SplitMode.MENU_BASED -> ConversationOutput(
                message = """
                    메뉴를 입력해주세요.
                    형식: "이름 가격; 이름 가격; ..."
                    예) 파스타 18.9; 피자 22; 콜라 3
                """.trimIndent(),
                nextStep = ConversationStep.ASK_MENU_ITEMS,
                context = newCtx
            )
        }
    }

    override fun handleMenuItems(input: String, context: ConversationContext): ConversationOutput {
        if (input.startsWith("MENU_PAYLOAD:")) {
            val parsed = parseMenuPayload(input)
                ?: return retry(context, ConversationStep.ASK_MENU_ITEMS, "메뉴 payload 파싱에 실패했습니다. 다시 시도해주세요.")

            val (items, participants, assignments) = parsed
            val derivedCtx = applyMenuDerived(context, items, participants)

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
            ?: return retry(context, ConversationStep.ASK_MENU_ITEMS, "메뉴 형식이 올바르지 않습니다. 예시 형태로 입력해주세요.")

        val newCtx = context.copy(
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

    override fun handleMenuParticipants(input: String, context: ConversationContext): ConversationOutput {
        val participants = parseParticipants(input)
            ?: return retry(context, ConversationStep.ASK_MENU_PARTICIPANTS, "참가자 이름을 쉼표로 구분해 입력해주세요.")

        val newCtx = context.copy(
            menuParticipants = participants,
            lastStep = ConversationStep.ASK_MENU_PARTICIPANTS,
            failureCount = 0
        )

        val menuGuide = participants.joinToString("\n") { "${it.id}) ${it.name}" }
        val itemGuide = context.menuItems.joinToString("\n") { "${it.id}) ${it.name}(${it.priceCad})" }

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

    override fun handleMenuAssignments(input: String, context: ConversationContext): ConversationOutput {
        if (input.startsWith("MENU_PAYLOAD:")) {
            val parsed = parseMenuPayload(input)
                ?: return retry(context, ConversationStep.ASK_MENU_ITEMS, "메뉴 payload 파싱에 실패했습니다. 다시 시도해주세요.")

            val (items, participants, assignments) = parsed
            val derivedCtx = applyMenuDerived(context, items, participants)

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

        val assignments = parseAssignments(input, context)
            ?: return retry(context, ConversationStep.ASK_MENU_ASSIGNMENTS, "지정 형식이 올바르지 않습니다. 예시대로 입력해주세요.")

        val derivedCtx = applyMenuDerived(context, context.menuItems, context.menuParticipants)

        val newCtx = derivedCtx.copy(
            menuAssignments = assignments,
            lastStep = ConversationStep.ASK_MENU_ASSIGNMENTS,
            failureCount = 0
        )

        return ConversationOutput(
            message = exchangeModePromptMessage(),
            nextStep = ConversationStep.ASK_EXCHANGE_RATE_MODE,
            context = newCtx
        )
    }

    override fun showResult(context: ConversationContext): ConversationOutput {
        return when (context.splitMode) {
            SplitMode.N_DIVIDE -> showEvenResult(context)

            SplitMode.MENU_BASED -> showMenuResult(context)

            null -> retry(context, ConversationStep.ASK_SPLIT_MODE, splitModeGuideMessage())
        }
    }

    private fun showMenuResult(ctx: ConversationContext): ConversationOutput {
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

        val base = ctx.baseAmount ?: run {
            val sum = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.priceCad }
            Money.of(sum, Currency.CAD)
        }
        val tax = ctx.taxAmount ?: Money.zero(Currency.CAD)

        val tip = when (ctx.tipMode) {
            TipMode.PERCENT -> ctx.tipPercent?.let { Tip(TipMode.PERCENT, percent = it, absolute = null) }
            TipMode.ABSOLUTE -> ctx.tipAbsolute?.let { Tip(TipMode.ABSOLUTE, percent = null, absolute = it) }
            else -> Tip(TipMode.NONE)
        }

        val receipt = Receipt(baseAmount = base, tax = Tax(tax), tip = tip)

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

        val resultMessage = buildString {
            appendLine("✅ 총액: ${formatMoney(r.total)} CAD")
            appendLine("✅ 메뉴별 결과")
            lines.forEach { appendLine(it) }
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
            MenuParticipantInput(id = "p${i + 1}", name = n)
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
}