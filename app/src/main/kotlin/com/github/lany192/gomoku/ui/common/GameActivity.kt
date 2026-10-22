package com.github.lany192.gomoku.ui.common

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

/**
 * 对局页基类：统一处理「终局结果浮层」与页面触摸的关系。
 *
 * 浮层是独立的 Dialog 窗口，并带 `FLAG_NOT_TOUCH_MODAL` —— 窗口之外的触摸**不再**被浮层拦截，
 * 而是直接落到本页面。所以"点浮层外"时底下的按钮能照常收到 DOWN/UP 并触发点击，
 * 浮层则在这次触摸结束后由 [dispatchTouchEvent] 顺手收起。
 */
abstract class GameActivity : AppCompatActivity() {

    /** 终局结果浮层：浮动在棋盘中部偏上，不占对局页布局，棋盘不会位移 */
    protected var resultOverlay: GameResultOverlay? = null

    /**
     * 本次触摸**按下时**浮层是否已经显示。
     *
     * 只有"按下时就亮着"的浮层才在这一次触摸结束时收起。反之会把**这一手棋刚弹出来的**
     * 终局提示当成旧浮层顺手关掉：赢棋那一下同样是 UP，落子→判胜→渲染→弹出提示，
     * 回到这里提示就被关了（表现为闪一下就没了）。悔棋后再赢一次同理，所以看着像"第二次不弹"。
     */
    private var overlayShownBeforeTouch = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 先让子控件处理（按钮的 OnClick 在 UP 时才触发），再收浮层
        val handled = super.dispatchTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> overlayShownBeforeTouch = resultOverlay?.isShowing == true
            MotionEvent.ACTION_UP -> if (overlayShownBeforeTouch) resultOverlay?.dismiss()
            // 触摸被打断（父容器拦截等）：不当作一次点击，也不收浮层
            MotionEvent.ACTION_CANCEL -> overlayShownBeforeTouch = false
        }
        return handled
    }
}
