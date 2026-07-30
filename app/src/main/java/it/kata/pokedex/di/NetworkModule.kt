package it.kata.pokedex.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.kata.pokedex.BuildConfig
import it.kata.pokedex.data.remote.PokeApi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.io.File
import javax.inject.Singleton

private const val BASE_URL = "https://pokeapi.co/api/v2/"
private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

/**
 * Building a page costs one list call plus two per entry, so the client is tuned for that shape
 * rather than for the occasional single request.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * The API sends cache friendly headers and its data barely ever changes, so a disk cache turns
     * a page the user scrolls back to into no network work at all.
     */
    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http"), CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(cache: Cache): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }
        }
        .build()
        .apply {
            // Default is five requests per host, which would drip feed the forty calls a page
            // needs. Eight keeps the first screen responsive without hammering a free public API.
            dispatcher.maxRequestsPerHost = 8
        }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun providePokeApi(retrofit: Retrofit): PokeApi = retrofit.create()
}
