package it.kata.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** An abstract class because Room needs a `@Transaction` method to have a body. */
@Dao
abstract class FavoriteDao {

    @Query("SELECT id FROM favorite_pokemon")
    abstract fun observeIds(): Flow<List<Int>>

    /**
     * By id, which is the Pokedex number: an order that does not reshuffle when an entry comes back,
     * and the reason the table records no date.
     */
    @Query("SELECT * FROM favorite_pokemon ORDER BY id")
    abstract fun observeAll(): Flow<List<FavoritePokemonEntity>>

    /**
     * The delete doubles as the question: it reports how many rows it removed, so a zero means the
     * entry has to become a favorite. One transaction, so two quick taps cannot interleave.
     */
    @Transaction
    open suspend fun toggle(favorite: FavoritePokemonEntity) {
        if (deleteById(favorite.id) == 0) insert(favorite)
    }

    @Query("DELETE FROM favorite_pokemon WHERE id = :id")
    abstract suspend fun deleteById(id: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(favorite: FavoritePokemonEntity)
}
