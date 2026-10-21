package com.github.lany192.gomoku.domain.ai

/**
 * AI 思考速度：控制"人下完机器多久回手"的节奏。
 *
 * 只影响观感，不影响棋力：[delayMillis] 是**落子时刻的下限**，
 * 与引擎自身的时间预算（[AiTunings] 里的 timeBudgetMillis）互不干涉：
 * 对外的观感是 **落子时刻 = max(思考间隔, 算法耗时)** ——
 * 算法本身就慢过间隔时直接落子，不再叠加等待；算法很快时补足到间隔。
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

        /**
         * 落子前还需要再等多久（毫秒）。
         *
         * [elapsedMillis] 是从对手落子到算法算完的耗时；已经超过 [waitMillis] 就返回 0，
         * 让机器直接落子，而不是把间隔再叠一遍。
         */
        fun remainingDelay(waitMillis: Long, elapsedMillis: Long): Long =
            (waitMillis - elapsedMillis).coerceAtLeast(0L)
    }
}
