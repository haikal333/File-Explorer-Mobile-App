package com.example.imagecaptureapplication.ui.screens

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import com.example.imagecaptureapplication.ui.components.TopBar
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imagecaptureapplication.data.AppDatabase
import com.example.imagecaptureapplication.data.ImageEntity
import com.example.imagecaptureapplication.network.ApiClient
import com.example.imagecaptureapplication.ui.viewmodels.ScanViewModel
import com.example.imagecaptureapplication.ui.viewmodels.ScanViewModelFactory
import com.example.imagecaptureapplication.ui.viewmodels.ScanUiState
import kotlinx.coroutines.launch
import java.io.File
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.compose.material3.MaterialTheme
import com.example.imagecaptureapplication.network.VisionRequest
import com.example.imagecaptureapplication.network.AnnotateImageRequest
import com.example.imagecaptureapplication.network.ImageContent
import com.example.imagecaptureapplication.network.Feature

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: ScanViewModel = viewModel(
        factory = ScanViewModelFactory(database)
    )
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingError by remember { mutableStateOf<String?>(null) }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            scope.launch {
                isProcessing = true
                processingError = null
                try {
                    // Convert URI to Base64
                    val base64Image = withContext(Dispatchers.IO) {
                        val contentResolver: ContentResolver = context.contentResolver
                        val inputStream = contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        Base64.encodeToString(bytes, Base64.DEFAULT)
                    }

                    // Get file details
                    val fileName = getFileName(context.contentResolver, uri)
                    val fileSize = getFileSize(context.contentResolver, uri)
                    
                    // Process image with API
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

                    val response = ApiClient.imageProcessingApi.processImage(request)
                    if (!response.isSuccessful) {
                        throw Exception("API call failed with code: ${response.code()}")
                    }

                    val visionResponse = response.body() ?: throw Exception("Empty response from API")
                    val extractedText = visionResponse.responses?.firstOrNull()
                        ?.textAnnotations?.firstOrNull()?.description ?: "No text found"

                    // Save to database
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    
                    val imageEntity = ImageEntity(
                        id = 0, // Room will auto-generate this
                        filename = fileName,
                        date = currentDate,
                        time = currentTime,
                        fileSize = fileSize,
                        apiReturnCode = extractedText,
                        location = ""  // Empty string for location
                    )
                    
                    database.imageDao().insertImage(imageEntity)
                    
                    // Navigate back
                    navController.navigateUp()
                } catch (e: Exception) {
                    processingError = e.message
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(title = "Scan Image", navController = navController)
        
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        setupCamera(context, this, lifecycleOwner) { imageCaptureInstance ->
                            imageCapture = imageCaptureInstance
                        }
                    }
                }
            )
            
            // Button Row at the bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Gallery Button
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    enabled = !isProcessing
                ) {
                    Text(if (selectedImageUri == null) "Select Image" else "Change Image")
                }

                // Capture Button
                Button(
                    onClick = {
                        captureImage(
                            imageCapture = imageCapture,
                            executor = executor,
                            context = context,
                            onImageCaptured = { uri ->
                                viewModel.processImage(uri, context)
                            },
                            onError = { exception ->
                                Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Capture")
                }
            }
        }

        // Add a loading indicator and error handling
        viewModel.uiState.collectAsState().value.let { state ->
            when (state) {
                is ScanUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ScanUiState.Error -> {
                    LaunchedEffect(state) {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is ScanUiState.Success -> {
                    LaunchedEffect(state) {
                        Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> { /* Initial state, do nothing */ }
            }
        }

        if (selectedImageUri != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(selectedImageUri),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        if (isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        
        if (processingError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error: $processingError",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun setupCamera(
    context: android.content.Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun captureImage(
    imageCapture: ImageCapture?,
    executor: Executor,
    context: android.content.Context,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture?.let {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        it.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        onImageCaptured(uri)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}

private fun getFileName(contentResolver: ContentResolver, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result ?: "unknown_file"
}

private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
    var size: Long = 0
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }
    return size
} 