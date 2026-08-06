package com.baicaohui.lightweb.browser

/** 网页通知策略：非 ALLOW 一律注入抑制脚本，避免页面请求通知权限。 */
object NotificationPolicy {

    fun shouldSuppress(decision: PermissionDecision): Boolean =
        decision != PermissionDecision.ALLOW

    fun suppressionScript(): String = """
        (function() {
          try {
            var denied = {
              permission: 'denied',
              requestPermission: function() { return Promise.resolve('denied'); }
            };
            Object.defineProperty(window, 'Notification', {
              value: denied,
              writable: true,
              configurable: true
            });
          } catch (e) {}
        })();
    """.trimIndent()
}
