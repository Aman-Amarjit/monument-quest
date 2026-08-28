package com.monumentquest.core.di

import com.monumentquest.data.remote.GroqApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GroqModule {

    @Provides
    @Singleton
    @Named("groq_api_key")
    fun provideGroqApiKey(): String = System.getenv("GROQ_API_KEY") ?: ""

    @Provides
    @Singleton
    fun provideGroqApi(okHttpClient: OkHttpClient): GroqApi {
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }
}
