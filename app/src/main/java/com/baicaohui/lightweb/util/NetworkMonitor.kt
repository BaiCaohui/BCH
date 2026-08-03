package com.baicaohui.lightweb.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _online = MutableStateFlow(isOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _online.value = true
        }

        override fun onLost(network: Network) {
            _online.value = isOnline()
        }
    }

    fun start() {
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun isOnline(): Boolean = runCatching {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)
}
