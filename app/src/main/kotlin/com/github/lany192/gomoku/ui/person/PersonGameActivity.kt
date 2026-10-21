package com.github.lany192.gomoku.ui.person

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.repeatOnLifecycle
import com.github.lany192.gomoku.R
import com.github.lany192.gomoku.databinding.GameFightBinding
import com.github.lany192.gomoku.domain.model.Side
import com.github.lany192.gomoku.ui.common.GameResultSheet
import com.github.lany192.gomoku.ui.common.TurnCountdown
import com.github.lany192.gomoku.ui.common.setupEdgeToEdge
import kotlinx.coroutines.launch

class PersonGameActivity : AppCompatActivity() {
    private lateinit var binding: GameFightBinding
    private val viewModel: PersonGameViewModel by viewModels()

    /** 终局结果面板：贴底弹出，不占对局页布局，棋盘不会位移 */
    private var resultSheet: GameResultSheet? = null

    /** 当前已展示的终局结果标识，避免状态重复渲染时反复弹面板 */
    private var resultKey: String? = null

    /** 记分卡下方的倒计时常态色，用于从警告色复位 */
    private var countdownNormalColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GameFightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)
        countdownNormalColor = binding.countdown.currentTextColor
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.gameView.configure(BOARD_SIZE, BOARD_SIZE)
        binding.gameView.onCellTapped = { x, y ->
            viewModel.dispatch(PersonGameIntent.BoardTap(x, y))
        }
        binding.restart.setOnClickListener {
            viewModel.dispatch(PersonGameIntent.RestartClicked)
        }
        binding.rollback.setOnClickListener {
            viewModel.dispatch(PersonGameIntent.RollbackClicked)
        }
        resultSheet = GameResultSheet(this) {
            viewModel.dispatch(PersonGameIntent.RestartClicked)
        }
        observeViewModel()
    }

    override fun onDestroy() {
        // 面板持有 Activity 上下文，必须先收起再销毁，避免窗口泄漏
        resultSheet?.dismiss()
        resultSheet = null
        super.onDestroy()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
            }
        }
    }

    private fun render(state: PersonGameState) {
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
        renderCountdown(state.remainingMillis)
        renderResult(state)
    }

    /**
     * 终局结果：贴底弹出面板，**不占用棋盘位置**，棋盘不会因结果出现而位移。
     * 非终局（悔棋/重开解除终局）时收起面板。
     */
    private fun renderResult(state: PersonGameState) {
        val headline = when {
            state.drawn -> getString(R.string.msg_draw_end)
            state.timedOut && state.winner == Side.BLACK -> getString(R.string.msg_black_timeout)
            state.timedOut -> getString(R.string.msg_white_timeout)
            state.winner == Side.BLACK -> getString(R.string.msg_black_win)
            state.winner == Side.WHITE -> getString(R.string.msg_white_win)
            else -> null
        }
        if (headline == null) {
            resultKey = null
            resultSheet?.dismiss()
            return
        }
        val key = "$headline#${state.blackWins}#${state.whiteWins}"
        if (key == resultKey) return
        resultKey = key
        resultSheet?.show(
            headline = headline,
            subtitle = fiveLineHint(state),
            blackWins = state.blackWins,
            whiteWins = state.whiteWins,
        )
    }

    /** 只有五连胜才有高亮棋型可指，其余终局（超时/和棋）不给这句提示 */
    private fun fiveLineHint(state: PersonGameState): String? =
        if (state.board.winLine.isNotEmpty()) getString(R.string.game_result_hint_five) else null

    private fun renderCountdown(remainingMillis: Long) {
        binding.countdown.text = TurnCountdown.format(remainingMillis)
        val warning = remainingMillis in 1 until WARN_MILLIS
        // 主题的 colorError 就是 @color/error（values 与 values-night 各有一份）
        binding.countdown.setTextColor(
            if (warning) ContextCompat.getColor(this, R.color.error) else countdownNormalColor
        )
    }

    private companion object {
        const val BOARD_SIZE = 15
        const val WARN_MILLIS = 30_000L
    }
}
