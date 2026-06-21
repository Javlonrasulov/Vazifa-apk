package uz.vazifa.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.ApiService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideApiService(client: ApiClient): ApiService = client.api
}
