package com.baicaohui.lightweb.browser

import kotlinx.serialization.Serializable

@Serializable
data class TabSnapshot(
    val id: Long,
    val url: String = "",
    val title: String = "",
    val status: String = TabStatus.EMPTY.name,
    val createdAt: Long = System.currentTimeMillis(),
)
