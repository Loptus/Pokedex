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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(favorite: FavoritePokemonEntity)

    @Query("DELETE FROM favorite_pokemon WHERE id = :id")
    protected abstract suspend fun deleteById(id: Int): Int
}
