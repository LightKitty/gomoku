package com.github.lany192.gomoku.ui.robot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.lany192.gomoku.R
import com.github.lany192.gomoku.data.settings.algorithmByName
import com.github.lany192.gomoku.databinding.AiSelectBinding
import com.github.lany192.gomoku.domain.ai.AiAlgorithm
import com.github.lany192.gomoku.domain.ai.AiThinkingSpeed
import com.github.lany192.gomoku.domain.ai.Difficulty
import com.github.lany192.gomoku.ui.common.setupEdgeToEdge

/**
 * 对手设置：难度单选 + 思考速度单选 + 算法分组列表。
 *
 * 任何返回路径（返回键/返回箭头）都带结果 finish，由对局页负责落库与生效；
 * 本页不写持久化，避免"看着改了其实没应用"的两份真相。
 */
class AiSelectActivity : AppCompatActivity() {

    private lateinit var binding: AiSelectBinding
    private var level = Difficulty.MEDIUM
    private var algorithm = AiAlgorithm.DEFAULT
    private var thinkingSpeed = AiThinkingSpeed.DEFAULT
    private lateinit var adapter: AiSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AiSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)

        level = Difficulty.entries.getOrNull(intent.getIntExtra(EXTRA_LEVEL, -1)) ?: Difficulty.MEDIUM
        algorithm = algorithmByName(intent.getStringExtra(EXTRA_ALGORITHM)) ?: AiAlgorithm.DEFAULT
        thinkingSpeed = AiThinkingSpeed.entries.getOrNull(intent.getIntExtra(EXTRA_THINKING_SPEED, -1))
            ?: AiThinkingSpeed.DEFAULT

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupLevelChips()
        setupThinkingSpeedChips()
        adapter = AiSelectAdapter(onSelected = { algorithm = it }, selected = algorithm)
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
    }

    override fun finish() {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_ALGORITHM, algorithm.name)
                .putExtra(EXTRA_LEVEL, level.ordinal)
                .putExtra(EXTRA_THINKING_SPEED, thinkingSpeed.ordinal),
        )
        super.finish()
    }

    private fun setupLevelChips() {
        binding.difficultyGroup.check(chipId(level))
        binding.difficultyGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val chosen = levelOfChip(checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            if (chosen != null) level = chosen
        }
    }

    /** 思考速度：落子前等多久，只影响观感不影响棋力 */
    private fun setupThinkingSpeedChips() {
        binding.thinkingSpeedGroup.check(speedChipId(thinkingSpeed))
        binding.thinkingSpeedGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val chosen = speedOfChip(checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            if (chosen != null) thinkingSpeed = chosen
        }
    }

    private fun chipId(level: Difficulty): Int = when (level) {
        Difficulty.NOVICE -> R.id.level_novice
        Difficulty.EASY -> R.id.level_easy
        Difficulty.MEDIUM -> R.id.level_medium
        Difficulty.HARD -> R.id.level_hard
        Difficulty.MASTER -> R.id.level_master
    }

    private fun levelOfChip(id: Int): Difficulty? = when (id) {
        R.id.level_novice -> Difficulty.NOVICE
        R.id.level_easy -> Difficulty.EASY
        R.id.level_medium -> Difficulty.MEDIUM
        R.id.level_hard -> Difficulty.HARD
        R.id.level_master -> Difficulty.MASTER
        else -> null
    }

    private fun speedChipId(speed: AiThinkingSpeed): Int = when (speed) {
        AiThinkingSpeed.FAST -> R.id.speed_fast
        AiThinkingSpeed.NORMAL -> R.id.speed_normal
        AiThinkingSpeed.SLOW -> R.id.speed_slow
    }

    private fun speedOfChip(id: Int): AiThinkingSpeed? = when (id) {
        R.id.speed_fast -> AiThinkingSpeed.FAST
        R.id.speed_normal -> AiThinkingSpeed.NORMAL
        R.id.speed_slow -> AiThinkingSpeed.SLOW
        else -> null
    }

    companion object {
        const val EXTRA_ALGORITHM = "ai_algorithm"
        const val EXTRA_LEVEL = "ai_level"
        const val EXTRA_THINKING_SPEED = "ai_thinking_speed"

        fun intent(
            context: Context,
            algorithm: AiAlgorithm,
            level: Difficulty,
            thinkingSpeed: AiThinkingSpeed,
        ): Intent = Intent(context, AiSelectActivity::class.java)
            .putExtra(EXTRA_ALGORITHM, algorithm.name)
            .putExtra(EXTRA_LEVEL, level.ordinal)
            .putExtra(EXTRA_THINKING_SPEED, thinkingSpeed.ordinal)

        /** 解析返回结果；数据缺失或不认识时返回 null（调用方保持原选择） */
        fun parseResult(algorithmName: String?, levelOrdinal: Int, thinkingSpeedOrdinal: Int): AiSelection? {
            val algorithm = algorithmByName(algorithmName) ?: return null
            val level = Difficulty.entries.getOrNull(levelOrdinal) ?: return null
            val speed = AiThinkingSpeed.entries.getOrNull(thinkingSpeedOrdinal) ?: return null
            return AiSelection(algorithm, level, speed)
        }
    }
}

/** 对手设置页的一次选择结果：算法 + 难度 + 思考速度 */
data class AiSelection(
    val algorithm: AiAlgorithm,
    val level: Difficulty,
    val thinkingSpeed: AiThinkingSpeed,
)
