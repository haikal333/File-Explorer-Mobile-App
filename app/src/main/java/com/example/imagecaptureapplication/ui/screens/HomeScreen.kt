package com.example.imagecaptureapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { navController.navigate("scan") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { navController.navigate("history") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("History")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }
    }
} 