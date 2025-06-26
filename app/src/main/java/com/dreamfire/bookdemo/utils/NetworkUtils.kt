package com.dreamfire.bookdemo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkUtils {
    fun hasNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}