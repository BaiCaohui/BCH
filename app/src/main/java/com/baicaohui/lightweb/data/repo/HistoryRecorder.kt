package com.baicaohui.lightweb.data.repo

interface HistoryRecorder {
    suspend fun record(url: String, title: String)
}
