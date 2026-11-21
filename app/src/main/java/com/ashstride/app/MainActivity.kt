package com.ashstride.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ashstride.app.ui.home.HomeScreen
import com.ashstride.app.ui.theme.AshstrideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AshstrideApp
        setContent {
            AshstrideTheme {
                HomeScreen(app.db)
            }
        }
    }
}
