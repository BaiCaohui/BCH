package com.baicaohui.lightweb.browser

/**
 * Default-browser support decisions that are pure and unit-testable.
 * Android 11 (API 30) introduced the browser role via [android.app.role.RoleManager];
 * older versions can only point the user to the system default-apps settings.
 */
object DefaultBrowser {
    private const val ROLE_API_LEVEL = 30

    fun actionFor(apiLevel: Int): DefaultBrowserAction =
        if (apiLevel >= ROLE_API_LEVEL) DefaultBrowserAction.REQUEST_ROLE
        else DefaultBrowserAction.OPEN_SETTINGS

    fun isDefault(
        apiLevel: Int,
        ownPackage: String,
        isRoleHeld: () -> Boolean,
        resolvedWebPackage: () -> String?,
    ): Boolean = if (apiLevel >= ROLE_API_LEVEL) {
        isRoleHeld()
    } else {
        ownPackage == resolvedWebPackage()
    }
}

enum class DefaultBrowserAction {
    REQUEST_ROLE,
    OPEN_SETTINGS,
}
