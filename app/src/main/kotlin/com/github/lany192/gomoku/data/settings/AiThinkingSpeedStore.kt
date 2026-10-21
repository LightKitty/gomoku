package com.github.lany192.gomoku.data.settings

import android.content.Context
import com.github.lany192.gomoku.domain.ai.AiThinkingSpeed

/** AI 思考速度持久化 */
interface AiThinkingSpeedStore {
    /** 未存过思考速度时返回 null，由调用方决定默认档 */
    fun read(): AiThinkingSpeed?

    fun write(speed: AiThinkingSpeed)
}

/** 按枚举名存（与难度/算法同一份 prefs）；名字不认识时视为未存过 */
class SharedPrefsAiThinkingSpeedStore(context: Context) : AiThinkingSpeedStore {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun read(): AiThinkingSpeed? =
        AiThinkingSpeed.entries.firstOrNull { it.name == prefs.getString(KEY_AI_THINKING_SPEED, null) }

    override fun write(speed: AiThinkingSpeed) {
        prefs.edit().putString(KEY_AI_THINKING_SPEED, speed.name).apply()
    }

    private companion object {
        const val PREF_NAME = "fivechess"
        const val KEY_AI_THINKING_SPEED = "ai_thinking_speed_v1"
    }
}
