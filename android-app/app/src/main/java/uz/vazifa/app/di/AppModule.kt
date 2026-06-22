package uz.vazifa.app.di

import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.ApiService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideApiService(client: ApiClient): ApiService = client.api

    @Provides @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        apiClient: ApiClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(apiClient.httpClient)
        .crossfade(true)
        .build()
}
