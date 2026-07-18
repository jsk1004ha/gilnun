package com.gilnun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.gilnun.app.ui.GilnunApp
import com.gilnun.app.ui.GilnunTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<GilnunViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GilnunTheme {
                GilnunApp(viewModel)
            }
        }
    }
}
