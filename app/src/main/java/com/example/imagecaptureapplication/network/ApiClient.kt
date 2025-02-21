package com.example.imagecaptureapplication.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://vision.googleapis.com/v1/"
    private const val API_KEY = "024b184b2978bfec2b9527a42953c9024601a0ed"  // Using your API key

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("key", API_KEY)
                .build()
            val request = original.newBuilder().url(url).build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val imageProcessingApi: ImageProcessingApi = retrofit.create(ImageProcessingApi::class.java)
} 