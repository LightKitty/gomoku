package com.github.lany192.gomoku.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 思考速度的落子时刻换算。
 *
 * 口径是「落子时刻 = max(思考间隔, 算法耗时)」：
 * 算法本来就慢过间隔时直接落子，不再把间隔叠一遍。
 */
class AiThinkingSpeedTest {

    @Test
    fun `算法很快时补足到思考间隔`() {
        val wait = AiThinkingSpeed.NORMAL.delayMillis
        assertEquals(wait, AiThinkingSpeed.remainingDelay(wait, 0L))
        assertEquals(wait - 50L, AiThinkingSpeed.remainingDelay(wait, 50L))
        assertEquals(1L, AiThinkingSpeed.remainingDelay(wait, wait - 1L))
    }

    @Test
    fun `算法耗时超过间隔时直接落子`() {
        val wait = AiThinkingSpeed.NORMAL.delayMillis
        assertEquals(0L, AiThinkingSpeed.remainingDelay(wait, wait))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(wait, wait + 500L))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(wait, 5000L))
    }

    @Test
    fun `快速档不等待`() {
        assertEquals(0L, AiThinkingSpeed.remainingDelay(AiThinkingSpeed.FAST.delayMillis, 0L))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(AiThinkingSpeed.FAST.delayMillis, 30L))
    }

    @Test
    fun `默认档是标准`() {
        assertEquals(AiThinkingSpeed.NORMAL, AiThinkingSpeed.DEFAULT)
        assertEquals(1000L, AiThinkingSpeed.DEFAULT.delayMillis)
        assertEquals(3000L, AiThinkingSpeed.SLOW.delayMillis)
    }
}
