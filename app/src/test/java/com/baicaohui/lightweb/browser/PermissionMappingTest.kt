package com.baicaohui.lightweb.browser

import android.Manifest
import android.webkit.PermissionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionMappingTest {

    @Test
    fun `video capture maps to camera`() {
        assertEquals(
            listOf(Manifest.permission.CAMERA),
            PermissionMapping.androidPermissions(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)),
        )
    }

    @Test
    fun `audio capture maps to record audio`() {
        assertEquals(
            listOf(Manifest.permission.RECORD_AUDIO),
            PermissionMapping.androidPermissions(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)),
        )
    }

    @Test
    fun `unknown resources map to empty list`() {
        assertEquals(
            emptyList<String>(),
            PermissionMapping.androidPermissions(arrayOf("android.webkit.resource.UNKNOWN")),
        )
    }

    @Test
    fun `describe lists resources in chinese`() {
        assertEquals(
            "摄像头、麦克风",
            PermissionMapping.describe(
                arrayOf(
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                ),
            ),
        )
    }
}
