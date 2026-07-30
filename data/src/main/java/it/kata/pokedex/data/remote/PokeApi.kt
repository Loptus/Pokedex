package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonIndexDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Only the index has a path of its own. Everything else is reached through a url the API handed
 * back, which is why those calls take a [Url] instead of an id: building the address ourselves means
 * guessing, and the ids of alternate forms do not line up with the ids of their species.
 */
interface PokeApi {

    @GET("pokemon")
    suspend fun getPokemonIndex(
        @Query("limit") limit: Int,
    ): PokemonIndexDto

    /** The url comes from an index entry. */
    @GET
    suspend fun getPokemonDetail(
        @Url url: String,
    ): PokemonDetailDto

    /** The url comes from the `species` object of a detail. */
    @GET
    suspend fun getPokemonSpecies(
        @Url url: String,
    ): PokemonSpeciesDto
}
