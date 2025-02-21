package com.example.imagecaptureapplication.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.imagecaptureapplication.data.AppDatabase
import com.example.imagecaptureapplication.data.ImageEntity
import com.example.imagecaptureapplication.network.ApiClient
import com.example.imagecaptureapplication.network.VisionRequest
import com.example.imagecaptureapplication.network.AnnotateImageRequest
import com.example.imagecaptureapplication.network.ImageContent
import com.example.imagecaptureapplication.network.Feature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.provider.MediaStore
import android.util.Base64

class ScanViewModel(private val database: AppDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Initial)
    val uiState: StateFlow<ScanUiState> = _uiState

    fun processImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = ScanUiState.Loading

                // Convert image to base64
                val imageBytes = context.contentResolver.openInputStream(uri)?.use { 
                    it.readBytes() 
                } ?: throw Exception("Could not read image file")
                
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Create Vision API request
                val request = VisionRequest(
                    requests = listOf(
                        AnnotateImageRequest(
                            image = ImageContent(content = base64Image),
                            features = listOf(
                                Feature(type = "TEXT_DETECTION", maxResults = 1)
                            )
                        )
                    )
                )

                // Make API call
                val response = ApiClient.imageProcessingApi.processImage(request)
                
                if (!response.isSuccessful) {
                    throw Exception("API call failed with code: ${response.code()}")
                }

                val visionResponse = response.body() ?: throw Exception("Empty response from API")
                val extractedText = visionResponse.responses?.firstOrNull()
                    ?.textAnnotations?.firstOrNull()?.description ?: "No text found"

                // Save to database
                val imageEntity = ImageEntity(
                    filename = uri.lastPathSegment ?: "unknown",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                    fileSize = imageBytes.size.toLong(),
                    apiReturnCode = extractedText,
                    location = ""
                )

                database.imageDao().insertImage(imageEntity)
                _uiState.value = ScanUiState.Success
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getRealPathFromUri(context: Context, uri: Uri): String {
        // Implementation to get real file path from Uri
        // This is a simplified version, you might need to handle different URI schemes
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        
        return cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            it.getString(columnIndex)
        } ?: uri.path ?: throw Exception("Cannot get file path from URI")
    }
}

sealed class ScanUiState {
    object Initial : ScanUiState()
    object Loading : ScanUiState()
    object Success : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class ScanViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 