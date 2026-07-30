package it.kata.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * An abstract class rather than an interface because of [toggle]: Room needs a `@Transaction` method
 * to have a body, and a body needs a class.
 */
@Dao
abstract class FavoriteDao {

    @Query("SELECT id FROM favorite_pokemon")
    abstract fun observeIds(): Flow<List<Int>>

    /**
     * Ordered by id, which is the Pokedex number: an order the user already knows, and one that does
     * not reshuffle the page when an entry is removed and saved again. It is also why the table has
     * no column recording when a favorite was added.
     */
    @Query("SELECT * FROM favorite_pokemon ORDER BY id")
    abstract fun observeAll(): Flow<List<FavoritePokemonEntity>>

    /**
     * Reads and writes in one transaction, so two quick taps on the same heart cannot interleave
     * into a delete that decides against an insert that already happened.
     *
     * The delete doubles as the question: it reports how many rows it removed, so a zero means the
     * entry was not a favorite and has to become one. Asking first and then deleting would be the
     * same work in two statements.
     */
    @Transaction
    open suspend fun toggle(favorite: FavoritePokemonEntity) {
        if (deleteById(favorite.id) == 0) insert(favorite)
    }

    /** Returns how many rows it removed, which is what makes it double as a question in [toggle]. */
    @Query("DELETE FROM favorite_pokemon WHERE id = :id")
    abstract suspend fun deleteById(id: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(favorite: FavoritePokemonEntity)
}
