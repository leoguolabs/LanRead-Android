package com.lanread.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lanread.app.ui.LanReadRoot
import com.lanread.app.ui.theme.LanReadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanReadTheme {
                LanReadRoot()
            }
        }
    }
}
