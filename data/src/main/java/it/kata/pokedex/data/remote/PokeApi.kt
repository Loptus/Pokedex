package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonIndexDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {

    /**
     * The full list of names. There is no offset because the app always asks for all of it: see
     * [PokemonNameIndexDataSource] for why one big request beats one small request per page.
     */
    @GET("pokemon")
    suspend fun getPokemonIndex(
        @Query("limit") limit: Int,
    ): PokemonIndexDto

    @GET("pokemon/{name}")
    suspend fun getPokemonDetail(
        @Path("name") name: String,
    ): PokemonDetailDto

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(
        @Path("id") id: Int,
    ): PokemonSpeciesDto
}
