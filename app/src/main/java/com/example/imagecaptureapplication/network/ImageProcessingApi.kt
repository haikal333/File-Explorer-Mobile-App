package com.example.imagecaptureapplication.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface ImageProcessingApi {
    @POST("images:annotate")
    suspend fun processImage(@Body request: VisionRequest): Response<VisionResponse>
}

data class VisionRequest(
    val requests: List<AnnotateImageRequest>
)

data class AnnotateImageRequest(
    val image: Image,
    val features: List<Feature>
)

data class Image(
    val content: String  // Base64 encoded image
)

data class Feature(
    val type: String = "TEXT_DETECTION",
    val maxResults: Int = 1
) 