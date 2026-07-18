package com.gilnun.app.guidance

import com.gilnun.app.data.ActionReceipt
import com.gilnun.app.data.ReceiptOutcome

/**
 * Reversible demo assistance levels. These are UI states, not an assessment of a person's age,
 * cognition, competence, or long-term learning.
 */
enum class HelpLevel(val numericValue: Int) {
    LEVEL_0(0),
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    ;

    companion object {
        fun from(value: Int): HelpLevel =
            when (value.coerceIn(MIN_HELP_LEVEL, MAX_HELP_LEVEL)) {
                0 -> LEVEL_0
                1 -> LEVEL_1
                2 -> LEVEL_2
                else -> LEVEL_3
            }
    }
}

enum class HelpPolicyEvent {
    HELP_REQUESTED,
    STRUGGLE_CANDIDATE,
    VERIFIED_SUCCESS,
    UNVERIFIED_RESULT,
    FAILED_RESULT,
}

/**
 * Moves at most one level per observation and always remains inside 0..3.
 *
 * A truthful verified result reduces help by one. A request, a new friction candidate, or any
 * result that is not truthfully verified restores one level immediately.
 */
class HelpPolicy {
    fun transition(
        currentLevel: Int,
        event: HelpPolicyEvent,
    ): Int {
        val boundedCurrent = currentLevel.coerceIn(MIN_HELP_LEVEL, MAX_HELP_LEVEL)
        val delta =
            when (event) {
                HelpPolicyEvent.VERIFIED_SUCCESS -> -1
                HelpPolicyEvent.HELP_REQUESTED,
                HelpPolicyEvent.STRUGGLE_CANDIDATE,
                HelpPolicyEvent.UNVERIFIED_RESULT,
                HelpPolicyEvent.FAILED_RESULT,
                -> 1
            }
        return (boundedCurrent + delta).coerceIn(MIN_HELP_LEVEL, MAX_HELP_LEVEL)
    }

    fun transition(
        currentLevel: HelpLevel,
        event: HelpPolicyEvent,
    ): HelpLevel = HelpLevel.from(transition(currentLevel.numericValue, event))

    fun onHelpRequested(currentLevel: Int): Int =
        transition(currentLevel, HelpPolicyEvent.HELP_REQUESTED)

    fun onStruggleCandidate(currentLevel: Int): Int =
        transition(currentLevel, HelpPolicyEvent.STRUGGLE_CANDIDATE)

    fun onReceipt(
        currentLevel: Int,
        receipt: ActionReceipt,
    ): Int {
        val event =
            when {
                receipt.outcome == ReceiptOutcome.VERIFIED &&
                    receipt.guidanceShown &&
                    receipt.userActionObserved &&
                    receipt.postconditionVerified -> HelpPolicyEvent.VERIFIED_SUCCESS
                receipt.outcome == ReceiptOutcome.FAILED -> HelpPolicyEvent.FAILED_RESULT
                else -> HelpPolicyEvent.UNVERIFIED_RESULT
            }
        return transition(currentLevel, event)
    }

    companion object {
        const val MIN_HELP_LEVEL = 0
        const val MAX_HELP_LEVEL = 3
    }
}

private const val MIN_HELP_LEVEL = HelpPolicy.MIN_HELP_LEVEL
private const val MAX_HELP_LEVEL = HelpPolicy.MAX_HELP_LEVEL
