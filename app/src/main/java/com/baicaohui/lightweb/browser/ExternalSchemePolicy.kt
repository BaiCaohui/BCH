package com.baicaohui.lightweb.browser

/**
 * 外部 scheme 处理策略（与 Chrome 行为对齐）：
 * - 自动触发的深链（无用户手势）一律静默取消，避免弹框打断网页渲染；
 * - 用户主动点击、且系统存在可处理该链接的应用时，才弹外部应用确认框。
 */
object ExternalSchemePolicy {

    fun shouldPrompt(url: String, hasGesture: Boolean, canOpenExternally: Boolean): Boolean =
        !UrlSecurity.isSafeUrl(url) && hasGesture && canOpenExternally
}
