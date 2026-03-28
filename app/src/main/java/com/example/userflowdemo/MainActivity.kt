package com.example.userflowdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.userflowdemo.navigation.EntryApp
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UserFlowDemoTheme {
                EntryApp()
            }
        }
    }
}
