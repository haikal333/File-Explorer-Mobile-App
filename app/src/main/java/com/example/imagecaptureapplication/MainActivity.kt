package com.example.imagecaptureapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.imagecaptureapplication.ui.screens.HistoryScreen
import com.example.imagecaptureapplication.ui.screens.HomeScreen
import com.example.imagecaptureapplication.ui.screens.ScanScreen
import com.example.imagecaptureapplication.ui.screens.SettingsScreen
import com.example.imagecaptureapplication.ui.theme.ImageCaptureApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCaptureApplicationTheme {
                val navController = rememberNavController()
                
                // Handle system back button
                BackHandler(enabled = navController.currentBackStackEntry?.destination?.route != "home") {
                    navController.navigateUp()
                }
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { 
                        HomeScreen(navController = navController)
                    }
                    composable("scan") {
                        ScanScreen(navController = navController)
                    }
                    composable("history") {
                        HistoryScreen(navController = navController)
                    }
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}