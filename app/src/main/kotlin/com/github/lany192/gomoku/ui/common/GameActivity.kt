package com.github.lany192.gomoku.ui.common

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

/**
 * 对局页基类：统一处理「终局结果浮层」与页面触摸的关系。
 *
 * 浮层是独立的 Dialog 窗口，并带 `FLAG_NOT_TOUCH_MODAL` —— 窗口之外的触摸**不再**被浮层拦截，
 * 而是直接落到本页面。所以"点浮层外"时底下的按钮能照常收到 DOWN/UP 并触发点击，
 * 浮层则在这一次触摸结束后由 [dispatchTouchEvent] 顺手收起。
 */
abstract class GameActivity : AppCompatActivity() {

    /** 终局结果浮层：浮动在棋盘中部偏上，不占对局页布局，棋盘不会位移 */
    protected var resultOverlay: GameResultOverlay? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 先让子控件处理（按钮的 OnClick 在 UP 时才触发），再收浮层
        val handled = super.dispatchTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP) {
            resultOverlay?.dismiss()
        }
        return handled
    }
}
