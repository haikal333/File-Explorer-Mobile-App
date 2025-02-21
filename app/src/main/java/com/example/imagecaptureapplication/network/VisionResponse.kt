package com.example.imagecaptureapplication.network

data class VisionResponse(
    val responses: List<AnnotateImageResponse>? = null
)

data class AnnotateImageResponse(
    val textAnnotations: List<TextAnnotation>? = null
)

data class TextAnnotation(
    val description: String? = null
) 