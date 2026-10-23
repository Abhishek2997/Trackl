package com.trackl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trackl.app.ui.navigation.TracklApp
import com.trackl.app.ui.theme.TracklTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TracklTheme {
                TracklApp()
            }
        }
    }
}
