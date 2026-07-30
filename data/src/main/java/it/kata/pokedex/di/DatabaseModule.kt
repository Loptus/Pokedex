package it.kata.pokedex.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.kata.pokedex.data.local.FavoriteDao
import it.kata.pokedex.data.local.PokedexDatabase
import javax.inject.Singleton

private const val DATABASE_NAME = "pokedex.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PokedexDatabase =
        Room.databaseBuilder(context, PokedexDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideFavoriteDao(database: PokedexDatabase): FavoriteDao = database.favoriteDao()
}
