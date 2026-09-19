package com.shanganba.examcountdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shanganba.examcountdown.ui.AppViewModel
import com.shanganba.examcountdown.ui.SgApp
import com.shanganba.examcountdown.util.CrashLogger

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashLogger.install(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SgApp(vm)
        }
    }
}
