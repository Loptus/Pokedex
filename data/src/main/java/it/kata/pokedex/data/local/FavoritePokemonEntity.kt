package it.kata.pokedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A favorite on disk: the same three fields as a pointer, and nothing else.
 *
 * [detailUrl] is stored rather than rebuilt from [id] for the reason it exists at all: the API's
 * ids do not line up past 10000, so an address assembled from an id is wrong for every alternate
 * form. The address the API handed us keeps working, and storing it costs a column.
 */
@Entity(tableName = "favorite_pokemon")
data class FavoritePokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val detailUrl: String,
)
