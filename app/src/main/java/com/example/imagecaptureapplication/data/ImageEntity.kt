package com.example.imagecaptureapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filename: String,
    val date: String,
    val time: String,
    val fileSize: Long,
    val apiReturnCode: String,
    val location: String = ""  // Default empty string
) 