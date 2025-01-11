package com.codewithkael.groupcallyt.utils

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.util.UUID

@HiltAndroidApp
class MyApplication : Application() {
    companion object {
        val STREAM_ID = UUID.randomUUID().toString().substring(0,3)
    }
}