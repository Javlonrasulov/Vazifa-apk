package uz.vazifa.app.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uz.vazifa.app.BuildConfig
import uz.vazifa.app.data.repository.AuthRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor() {
    var accessToken: String? = null
    var refreshToken: String? = null
}

@Singleton
class ApiClient @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepositoryProvider: Provider<AuthRepository>,
) {
    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
        tokenStore.accessToken?.let { req.addHeader("Authorization", "Bearer $it") }
        chain.proceed(req.build())
    }

    private val authenticator = Authenticator { _: Route?, response: Response ->
        if (responseCount(response) >= 2) return@Authenticator null
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) return@Authenticator null

        val refreshed = runBlocking { authRepositoryProvider.get().refreshAndPersist() }
        if (!refreshed) return@Authenticator null

        response.request.newBuilder()
            .header("Authorization", "Bearer ${tokenStore.accessToken}")
            .build()
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val httpClient: OkHttpClient get() = client

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(ApiService::class.java)

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
