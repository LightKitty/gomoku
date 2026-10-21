package com.github.lany192.gomoku.domain.ai

/**
 * AI 思考速度：机器落子前刻意等待的时间，用来控制"人下完机器秒回"的节奏。
 *
 * 只影响观感，不影响棋力：[delayMillis] 是落子**之前**的等待，
 * 与引擎自身的时间预算（[AiTunings] 里的 timeBudgetMillis）互不干涉：
 * 等待结束后引擎照常用自己的预算搜索，总耗时 = 思考延迟 + 搜索耗时。
 */
enum class AiThinkingSpeed(val delayMillis: Long) {
    /** 立即落子，给追求对局节奏的玩家 */
    FAST(0L),

    /** 默认档：略作停顿，观感接近真人思考 */
    NORMAL(700L),

    /** 慢速：能明显看出机器在"想" */
    SLOW(1800L);

    companion object {
        /** 未设置过思考速度时的默认档 */
        val DEFAULT = NORMAL
    }
}
