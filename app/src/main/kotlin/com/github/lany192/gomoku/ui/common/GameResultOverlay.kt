package com.github.lany192.gomoku.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
 *
 * 内容只保留**一句结果文案**（如「电脑获胜」）：轻量简洁，不要副标题、比分与按钮。
 * 卡片宽度随文字自适应并居中，不占满屏。窗口遮罩透明度置 0，棋盘不会被压暗。
 *
 * 交互：浮层窗口带 `FLAG_NOT_TOUCH_MODAL`，**不拦截**窗口之外的触摸 —— 那股触摸直接落到对局页，
 * 底下的按钮照常触发；浮层则由对局页在这次触摸结束后收起（见 [GameActivity]）。
 * 于是"点浮层外"既是关掉浮层，也是把这次点击交给下面的控件，一下一次到位。
 */
class GameResultOverlay(
    private val activity: AppCompatActivity,
    private val boardView: View,
) {
    private var dialog: AppCompatDialog? = null
    private var titleView: TextView? = null

    /**
     * 展示终局结果（幂等：已展示时只更新文案，不重复弹出）
     *
     * @param headline 结果文案，如「电脑获胜」
     */
    fun show(headline: String) {
        val current = dialog
        if (current != null && current.isShowing) {
            titleView?.text = headline
            return
        }
        val overlay = AppCompatDialog(activity)
        overlay.setContentView(R.layout.dialog_game_result)
        overlay.window?.apply {
            // 卡片自带圆角与配色，窗口本身透明
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // 遮罩透明：浮层弹出时棋盘不被压暗
            setDimAmount(0f)
            // NOT_TOUCH_MODAL：窗口之外的触摸不发给浮层，而是直接落到对局页，
            // 于是点按钮一次就生效（浮层由对局页在这次触摸结束后收起，见 GameActivity）
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }
        // 窗口外的触摸不发给浮层，而是直接落到对局页（NOT_TOUCH_MODAL），
        // 由对局页在这次触摸结束后收起浮层 —— 于是"点浮层外"既能关掉浮层，
        // 底下的按钮也照常触发点击（见 GameActivity.dispatchTouchEvent）
        overlay.window?.decorView?.setOnTouchListener { _, event ->
            // 卡片自己占着的那条横带：点卡片也算"点浮层外"，同样把浮层收掉
            if (event.action == MotionEvent.ACTION_UP) {
                overlay.dismiss()
            }
            // 一律吃掉手势：避免关闭后残余事件穿透到底下的棋盘造成误落子
            true
        }

        titleView = overlay.findViewById(R.id.result_title)
        overlay.setOnDismissListener {
            // 只清理自己那一份，避免旧浮层的回调把新浮层的引用清空
            if (dialog === overlay) {
                clearViews()
            }
        }
        dialog = overlay
        titleView?.text = headline
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
     * 再用 y 偏移相对屏幕中心微调。棋盘与活动窗口都已布局完毕，读到的坐标是准的，
     * 不用等窗口回调，也就不存在"第一帧位置不对"的时序问题。
     */
    private fun placeOverBoard(overlay: AppCompatDialog) {
        val window = overlay.window ?: return
        // 窗口铺满宽度、只占卡片那么高的一条横带：横带之外的触摸归对局页（NOT_TOUCH_MODAL），
        // 横带之内由装饰层的触摸监听兜底；卡片自身宽度随文字自适应并居中
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val boardLoc = IntArray(2)
        boardView.getLocationOnScreen(boardLoc)
        val rootLoc = IntArray(2)
        activity.window.decorView.getLocationOnScreen(rootLoc)
        val params = window.attributes
        params.gravity = Gravity.CENTER
        // x/y 是相对 gravity 的偏移量（老的 yOffset 字段在 API 37 的 SDK 里已移除）
        // 卡片中心对齐到棋盘往下 35% 处
        val targetCenter = boardLoc[1] + (boardView.height * BOARD_ANCHOR_RATIO).toInt()
        val screenCenter = rootLoc[1] + activity.window.decorView.height / 2
        params.y = targetCenter - screenCenter
        window.attributes = params
    }

    private fun clearViews() {
        dialog = null
        titleView = null
    }

    companion object {
        /** 卡片中心对齐到棋盘往下 35% 处：浮在棋盘中部偏上，不压住下方战局 */
        private const val BOARD_ANCHOR_RATIO = 0.35f
    }
}
