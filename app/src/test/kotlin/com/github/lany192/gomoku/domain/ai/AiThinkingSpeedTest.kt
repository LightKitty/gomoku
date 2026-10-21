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
        assertEquals(700L, AiThinkingSpeed.remainingDelay(700L, 0L))
        assertEquals(650L, AiThinkingSpeed.remainingDelay(700L, 50L))
        assertEquals(1L, AiThinkingSpeed.remainingDelay(700L, 699L))
    }

    @Test
    fun `算法耗时超过间隔时直接落子`() {
        assertEquals(0L, AiThinkingSpeed.remainingDelay(700L, 700L))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(700L, 1200L))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(700L, 5000L))
    }

    @Test
    fun `快速档不等待`() {
        assertEquals(0L, AiThinkingSpeed.remainingDelay(AiThinkingSpeed.FAST.delayMillis, 0L))
        assertEquals(0L, AiThinkingSpeed.remainingDelay(AiThinkingSpeed.FAST.delayMillis, 30L))
    }

    @Test
    fun `默认档是标准`() {
        assertEquals(AiThinkingSpeed.NORMAL, AiThinkingSpeed.DEFAULT)
        assertEquals(700L, AiThinkingSpeed.DEFAULT.delayMillis)
    }
}
