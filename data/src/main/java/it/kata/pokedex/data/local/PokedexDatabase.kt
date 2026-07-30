package it.kata.pokedex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * `exportSchema` is off because there is no shipped version to migrate from: the schema files would
 * be a record of a history that does not exist yet. It goes back on the day the app ships.
 */
@Database(entities = [FavoritePokemonEntity::class], version = 1, exportSchema = false)
abstract class PokedexDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
}
