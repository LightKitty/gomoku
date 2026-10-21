package com.github.lany192.gomoku.ui.robot

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.lany192.gomoku.R
import com.github.lany192.gomoku.data.db.AiWeightsDatabase
import com.github.lany192.gomoku.data.db.RoomAiWeightStore
import com.github.lany192.gomoku.data.settings.SharedPrefsAiAlgorithmStore
import com.github.lany192.gomoku.data.settings.SharedPrefsAiLevelStore
import com.github.lany192.gomoku.data.settings.SharedPrefsAiThinkingSpeedStore
import com.github.lany192.gomoku.databinding.GameSingleBinding
import com.github.lany192.gomoku.domain.ai.AiEnginePool
import com.github.lany192.gomoku.domain.model.Side
import com.github.lany192.gomoku.ui.common.GameResultOverlay
import com.github.lany192.gomoku.ui.common.setupEdgeToEdge
import kotlinx.coroutines.launch

/**
 * 人机对战
 */
class RobotGameActivity : AppCompatActivity() {
    private lateinit var binding: GameSingleBinding

    /** 终局结果浮层：浮动在棋盘中部偏上，不占对局页布局，棋盘不会位移 */
    private var resultOverlay: GameResultOverlay? = null

    /** 当前已展示的终局结果标识，避免状态重复渲染时反复弹面板 */
    private var resultKey: String? = null
    private val viewModel: RobotGameViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                RobotGameViewModel(
                    enginePool = AiEnginePool(
                        width = BOARD_SIZE,
                        height = BOARD_SIZE,
                        weightStore = RoomAiWeightStore(AiWeightsDatabase.get(app).aiWeightsDao()),
                    ),
                    levelStore = SharedPrefsAiLevelStore(app),
                    algorithmStore = SharedPrefsAiAlgorithmStore(app),
                    thinkingSpeedStore = SharedPrefsAiThinkingSpeedStore(app),
                )
            }
        }
    }

    /** 选择页返回即生效：同时带回落库后的难度、算法与思考速度 */
    private val selectAi = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
        val selection = AiSelectActivity.parseResult(
            data.getStringExtra(AiSelectActivity.EXTRA_ALGORITHM),
            data.getIntExtra(AiSelectActivity.EXTRA_LEVEL, -1),
            data.getIntExtra(AiSelectActivity.EXTRA_THINKING_SPEED, -1),
        ) ?: return@registerForActivityResult
        viewModel.dispatch(RobotGameIntent.AlgorithmSelected(selection.algorithm))
        viewModel.dispatch(RobotGameIntent.LevelSelected(selection.level))
        viewModel.dispatch(RobotGameIntent.ThinkingSpeedSelected(selection.thinkingSpeed))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GameSingleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.gameView.configure(BOARD_SIZE, BOARD_SIZE)
        binding.gameView.onCellTapped = { x, y ->
            viewModel.dispatch(RobotGameIntent.BoardTap(x, y))
        }
        binding.restart.setOnClickListener {
            viewModel.dispatch(RobotGameIntent.RestartClicked)
        }
        binding.rollback.setOnClickListener {
            viewModel.dispatch(RobotGameIntent.RollbackClicked)
        }
        binding.opponentSettings.setOnClickListener {
            val state = viewModel.state.value
            selectAi.launch(
                AiSelectActivity.intent(this, state.aiAlgorithm, state.aiLevel, state.thinkingSpeed)
            )
        }
        resultOverlay = GameResultOverlay(this, binding.gameView)
        observeViewModel()
    }

    override fun onDestroy() {
        // 浮层持有 Activity 上下文，必须先收起再销毁，避免窗口泄漏
        resultOverlay?.dismiss()
        resultOverlay = null
        super.onDestroy()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
            }
        }
    }

    private fun render(state: RobotGameState) {
        binding.gameView.render(state.board)
        if (state.active == Side.BLACK) {
            binding.blackActive.visibility = View.VISIBLE
            binding.whiteActive.visibility = View.INVISIBLE
        } else {
            binding.blackActive.visibility = View.INVISIBLE
            binding.whiteActive.visibility = View.VISIBLE
        }
        binding.blackWin.text = state.blackWins.toString()
        binding.whiteWin.text = state.whiteWins.toString()
        // 默认不展示算法名：算法是进阶选项，默认用经典棋型评分就够
        binding.opponentSummary.text = getString(
            R.string.level_speed_format,
            getString(levelNameRes(state.aiLevel)),
            getString(thinkingSpeedNameRes(state.thinkingSpeed)),
        )
        renderResult(state)
    }

    /**
     * 终局结果：**浮动结果浮层**，只显示一句结果文案，**不占用棋盘位置**，棋盘不会因结果出现而位移。
     * 非终局（悔棋/重开解除终局）时收起浮层。
     *
     * 文案取玩家视角：人机对局里 AI 永远执白，所以黑方胜即玩家获胜。
     */
    private fun renderResult(state: RobotGameState) {
        val headline = when {
            state.drawn -> getString(R.string.msg_draw_end)
            state.winner == Side.BLACK -> getString(R.string.game_result_player_win)
            state.winner == Side.WHITE -> getString(R.string.game_result_ai_win)
            else -> null
        }
        if (headline == null) {
            resultKey = null
            resultOverlay?.dismiss()
            return
        }
        if (headline == resultKey) return
        resultKey = headline
        resultOverlay?.show(headline)
    }

    private companion object {
        const val BOARD_SIZE = 15
    }
}
