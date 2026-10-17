package com.loftitan.eoltestfixture

import android.app.Application
import android.util.Log

class EolApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("EolApplication", "LOF Titan S3 EOL PCB Test Fixture initialized.")
    }
}
