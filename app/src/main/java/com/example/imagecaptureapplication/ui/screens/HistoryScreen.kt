package com.example.imagecaptureapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.imagecaptureapplication.ui.components.TopBar
import com.example.imagecaptureapplication.data.ImageEntity
import com.example.imagecaptureapplication.data.AppDatabase
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HistoryScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val images by database.imageDao().getAllImages().collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(title = "History", navController = navController)
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(images) { image ->
                ImageHistoryCard(image)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageHistoryCard(image: ImageEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filename: ${image.filename}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Date: ${image.date}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Time: ${image.time}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Size: ${formatFileSize(image.fileSize)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Extracted Text:",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = image.apiReturnCode,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
} 