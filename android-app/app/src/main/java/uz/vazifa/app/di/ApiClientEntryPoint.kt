package uz.vazifa.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.TokenStore

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApiClientEntryPoint {
    fun apiClient(): ApiClient
    fun tokenStore(): TokenStore
}
