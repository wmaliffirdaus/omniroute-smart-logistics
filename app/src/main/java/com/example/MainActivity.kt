package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge drawing
        enableEdgeToEdge()
        
        // Instantiate ViewModel
        val viewModel = ViewModelProvider(this)[DeliveryViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                MainDashboard(viewModel = viewModel)
            }
        }
    }
}
