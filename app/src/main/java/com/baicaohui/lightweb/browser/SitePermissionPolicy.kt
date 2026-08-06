package com.baicaohui.lightweb.browser

import android.webkit.PermissionRequest
import com.baicaohui.lightweb.data.db.SiteSettingEntity

enum class PermissionKind { LOCATION, CAMERA, MICROPHONE, NOTIFICATIONS, POPUPS, AUTOPLAY }

enum class PermissionDecision { ASK, ALLOW, BLOCK }

/**
 * 站点权限策略：每类权限先看站点覆盖（true=允许/false=禁止/null=跟随全局），
 * 再看全局默认；权限请求主开关关闭时一律拒绝。
 */
object SitePermissionPolicy {

    fun kindsForResources(resources: Array<String>): Set<PermissionKind> = buildSet {
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) add(PermissionKind.CAMERA)
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) add(PermissionKind.MICROPHONE)
    }

    fun resolve(
        kind: PermissionKind,
        site: SiteSettingEntity?,
        permissionPromptEnabled: Boolean,
        autoplayAllowed: Boolean,
    ): PermissionDecision {
        if (!permissionPromptEnabled) return PermissionDecision.BLOCK
        val override = when (kind) {
            PermissionKind.LOCATION -> site?.location
            PermissionKind.CAMERA -> site?.camera
            PermissionKind.MICROPHONE -> site?.microphone
            PermissionKind.NOTIFICATIONS -> site?.notifications
            PermissionKind.POPUPS -> site?.popups
            PermissionKind.AUTOPLAY -> site?.autoplay
        }
        if (override != null) {
            return if (override) PermissionDecision.ALLOW else PermissionDecision.BLOCK
        }
        return when (kind) {
            PermissionKind.AUTOPLAY ->
                if (autoplayAllowed) PermissionDecision.ALLOW else PermissionDecision.BLOCK
            PermissionKind.NOTIFICATIONS -> PermissionDecision.BLOCK
            else -> PermissionDecision.ASK
        }
    }
}
