package com.example.imagecaptureapplication.network

data class VisionRequest(
    val requests: List<AnnotateImageRequest>
)

data class AnnotateImageRequest(
    val image: ImageContent,
    val features: List<Feature>
)

data class ImageContent(
    val content: String
)

data class Feature(
    val type: String = "TEXT_DETECTION",
    val maxResults: Int = 1
) 