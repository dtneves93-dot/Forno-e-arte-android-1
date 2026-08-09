package com.fornoearte.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fornoearte.app.ui.FornoApp
import com.fornoearte.app.ui.OrderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        val app = application as FornoApplication
        FornoApp(viewModel(factory = OrderViewModel.Factory(app.repository)))
    } }
}
