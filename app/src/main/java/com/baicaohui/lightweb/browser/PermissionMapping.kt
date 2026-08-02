package com.baicaohui.lightweb.browser

import android.Manifest
import android.webkit.PermissionRequest

object PermissionMapping {

    fun androidPermissions(resources: Array<String>): List<String> = buildList {
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) {
            add(Manifest.permission.CAMERA)
        }
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun describe(resources: Array<String>): String = resources.joinToString("、") { resource ->
        when (resource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "摄像头"
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "麦克风"
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "受保护媒体"
            PermissionRequest.RESOURCE_MIDI_SYSEX -> "MIDI"
            else -> resource
        }
    }
}
