package it.kata.pokedex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** `exportSchema` goes back on the day the app ships: today there is no version to migrate from. */
@Database(entities = [FavoritePokemonEntity::class], version = 1, exportSchema = false)
abstract class PokedexDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
}
