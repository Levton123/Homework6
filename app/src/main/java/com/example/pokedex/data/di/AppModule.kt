package com.example.pokedex.data.di

import android.content.Context
import androidx.room.Room
import com.example.pokedex.data.api.PokemonApiService
import com.example.pokedex.data.db.FavouriteDao
import com.example.pokedex.data.db.PokedexDatabase
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.FavouriteRepositoryImpl
import com.example.pokedex.data.repository.PokemonRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindFavouriteRepository(impl: FavouriteRepositoryImpl): FavouriteRepository

    companion object {

        @Provides
        @Singleton
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
            Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        @Provides
        @Singleton
        fun providePokemonApiService(retrofit: Retrofit): PokemonApiService =
            retrofit.create(PokemonApiService::class.java)

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): PokedexDatabase =
            Room.databaseBuilder(
                context,
                PokedexDatabase::class.java,
                "pokedex.db"
            ).build()

        @Provides
        @Singleton
        fun provideFavouriteDao(database: PokedexDatabase): FavouriteDao =
            database.favouriteDao()

        @Provides
        @Singleton
        fun providePokemonRepository(apiService: PokemonApiService): PokemonRepository =
            PokemonRepository(apiService)
    }
}