package com.github.lany192.gomoku.ui.common

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.lany192.gomoku.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 终局结果底部面板（本地双人 / 人机 / 联机共用）。
 *
 * 为什么不再用原来的横幅：
 * - 横幅是对局页布局流里的兄弟节点，`gone → visible` 会把下方的棋盘整块顶下去，棋盘位移；
 * - 本面板挂在 Dialog 窗口上、贴底停住，**不参与对局页布局**，棋盘纹丝不动；
 * - 窗口遮罩透明度置 0，面板弹出时棋盘不会被压暗；收起后棋局完全裸露，可慢慢复盘。
 *
 * 面板内容：结果标题 + 提示 + 比分 + 「再来一局 / 收起」两个按钮。
 *
 * 注意：`BottomSheetDialog.setContentView(View)` 会直接抛 `UnsupportedOperationException`
 * （Material 明确禁用了带 View 的重载），只能走 `setContentView(Int)` 由框架把布局
 * 塞进 `design_bottom_sheet` 容器，因此这里用 `findViewById` 取控件而非 ViewBinding。
 */
class GameResultSheet(
    private val activity: AppCompatActivity,
    private val onRestart: () -> Unit,
) {
    private var dialog: BottomSheetDialog? = null
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
        val sheet = BottomSheetDialog(activity)
        sheet.setContentView(R.layout.dialog_game_result)
        // 遮罩透明：面板弹出时棋盘既不被压暗也不被覆盖
        sheet.window?.setDimAmount(0f)
        // 点棋盘区域即收起面板，方便直接看当前棋局
        sheet.setCanceledOnTouchOutside(true)

        titleView = sheet.findViewById<TextView>(R.id.result_title)
        subtitleView = sheet.findViewById<TextView>(R.id.result_subtitle)
        scoreView = sheet.findViewById<TextView>(R.id.result_score)
        sheet.findViewById<View>(R.id.result_collapse)?.setOnClickListener { dismiss() }
        sheet.findViewById<View>(R.id.result_again)?.setOnClickListener {
            dismiss()
            onRestart()
        }

        sheet.setOnShowListener {
            // 内容多高就占多高、贴底停住，棋盘所在区域完全空出来。
            // 不允许拖拽/隐藏：收起只走「收起」按钮、点棋盘区域或返回键，
            // 避免出现「滑下去就没了、又想看结果却拉不回来」的坑。
            behaviorOf(sheet)?.apply {
                isFitToContents = true
                isDraggable = false
                isHideable = false
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        sheet.setOnDismissListener {
            // 只清理自己那一份，避免旧面板的回调把新面板的引用清空
            if (dialog === sheet) {
                clearViews()
            }
        }
        dialog = sheet
        applyContent(headline, subtitle, blackWins, whiteWins)
        sheet.show()
    }

    /** 收起面板（棋局保持终局状态，棋盘继续可见） */
    fun dismiss() {
        dialog?.dismiss()
        clearViews()
    }

    private fun behaviorOf(sheet: BottomSheetDialog): BottomSheetBehavior<*>? {
        val bottomSheet = sheet.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet,
        ) ?: return null
        return BottomSheetBehavior.from(bottomSheet)
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
}
