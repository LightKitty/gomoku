package com.github.lany192.gomoku.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import com.github.lany192.gomoku.R

/**
 * 终局结果浮层（本地双人 / 人机 / 联机共用）。
 *
 * 为什么必须是独立窗口，而不是对局页里的横幅：
 * - 横幅是对局页布局流里的兄弟节点，`gone → visible` 会把下方棋盘整块顶下去（棋盘位移）；
 * - 棋盘是 SurfaceView（setZOrderOnTop），普通 View 盖不住它，想浮在棋盘上只能用独立窗口。
 *
 * 所以浮层挂在 Dialog 窗口上、**浮动在棋盘中部偏上**，不参与对局页布局，棋盘纹丝不动。
 * 样式沿用原本的横幅卡片（主色容器 + 12dp 圆角）；交互改为点浮层外任意区域即关闭，
 * 棋局保持终局态、棋盘完全裸露可复盘。窗口遮罩透明度置 0，棋盘不会被压暗。
 */
class GameResultOverlay(
    private val activity: AppCompatActivity,
    private val boardView: View,
    private val onRestart: () -> Unit,
) {
    private var dialog: AppCompatDialog? = null
    private var titleView: TextView? = null
    private var subtitleView: TextView? = null
    private var scoreView: TextView? = null

    /**
     * 展示终局结果（幂等：已展示时只更新文案，不重复弹出）
     *
     * @param headline 主标题，如「黑方胜！」
     * @param subtitle 副标题，可空
     * @param blackWins 黑方胜场
     * @param whiteWins 白方胜场
     */
    fun show(
        headline: String,
        subtitle: String? = null,
        blackWins: Int = 0,
        whiteWins: Int = 0,
    ) {
        val current = dialog
        if (current != null && current.isShowing) {
            applyContent(headline, subtitle, blackWins, whiteWins)
            return
        }
        val overlay = AppCompatDialog(activity)
        overlay.setContentView(R.layout.dialog_game_result)
        overlay.window?.apply {
            // 卡片自带圆角与配色，窗口本身透明
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // 遮罩透明：浮层弹出时棋盘不被压暗
            setDimAmount(0f)
        }
        // 点浮层外任意区域即关闭，方便直接看当前棋局
        overlay.setCanceledOnTouchOutside(true)

        titleView = overlay.findViewById(R.id.result_title)
        subtitleView = overlay.findViewById(R.id.result_subtitle)
        scoreView = overlay.findViewById(R.id.result_score)
        overlay.findViewById<View>(R.id.result_collapse)?.setOnClickListener { dismiss() }
        overlay.findViewById<View>(R.id.result_again)?.setOnClickListener {
            dismiss()
            onRestart()
        }
        overlay.setOnDismissListener {
            // 只清理自己那一份，避免旧浮层的回调把新浮层的引用清空
            if (dialog === overlay) {
                clearViews()
            }
        }
        dialog = overlay
        applyContent(headline, subtitle, blackWins, whiteWins)
        overlay.show()
        placeOverBoard(overlay)
    }

    /** 关闭浮层（棋局保持终局状态，棋盘继续可见） */
    fun dismiss() {
        dialog?.dismiss()
        clearViews()
    }

    /**
     * 把浮层挪到棋盘中部偏上。
     *
     * 卡片以窗口中心为基准定位：取棋盘在屏幕上的位置，算出卡片中心该落在棋盘往下 35% 处，
     * 再用 yOffset 相对屏幕中心微调。棋盘与活动窗口都已布局完毕，读到的坐标是准的，
     * 不用等窗口回调，也就不存在"第一帧位置不对"的时序问题。
     */
    private fun placeOverBoard(overlay: AppCompatDialog) {
        val window = overlay.window ?: return
        // 宽度铺满，卡片自带左右边距，观感与原本的横幅一致
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val boardLoc = IntArray(2)
        boardView.getLocationOnScreen(boardLoc)
        val rootLoc = IntArray(2)
        activity.window.decorView.getLocationOnScreen(rootLoc)
        val params = window.attributes
        params.gravity = Gravity.CENTER
        // yOffset 是 Int 像素值：卡片中心对齐到棋盘往下 35% 处
        val targetCenter = boardLoc[1] + (boardView.height * BOARD_ANCHOR_RATIO).toInt()
        val screenCenter = rootLoc[1] + activity.window.decorView.height / 2
        params.yOffset = targetCenter - screenCenter
        window.attributes = params
    }

    private fun applyContent(
        headline: String,
        subtitle: String?,
        blackWins: Int,
        whiteWins: Int,
    ) {
        titleView?.text = headline
        subtitleView?.let {
            it.text = subtitle.orEmpty()
            it.visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        scoreView?.text = activity.getString(R.string.game_result_score, blackWins, whiteWins)
    }

    private fun clearViews() {
        dialog = null
        titleView = null
        subtitleView = null
        scoreView = null
    }

    companion object {
        /** 卡片中心对齐到棋盘往下 35% 处：浮在棋盘中部偏上，不压住下方战局 */
        private const val BOARD_ANCHOR_RATIO = 0.35f
    }
}
