package com.example.imagecaptureapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY id DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Delete
    suspend fun deleteImage(image: ImageEntity)

    @Query("DELETE FROM images")
    suspend fun deleteAllImages()
} 