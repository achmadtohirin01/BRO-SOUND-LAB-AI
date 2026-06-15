package com.example

import android.app.Application
import android.os.Build

class BroSoundLabApplication : Application() {

    override fun getAttributionTag(): String? {
        return "audio"
    }
}
