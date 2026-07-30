package it.kata.pokedex.data.remote

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the wiring between the real JSON shape and the DTOs, which is the part no unit test on
 * the mappers alone can cover. The payloads are trimmed copies of the real responses.
 */
class PokeApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PokeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `reads the index and asks for all of it in one request`() = runTest {
        server.enqueue(MockResponse(body = INDEX_JSON))

        val index = api.getPokemonIndex(limit = 100_000)

        assertEquals(2, index.results?.size)
        assertEquals("bulbasaur", index.results?.first()?.name)
        assertEquals("/pokemon?limit=100000", server.takeRequest().target)
    }

    @Test
    fun `reads the nested sprites, types and species link of a detail`() = runTest {
        server.enqueue(MockResponse(body = DETAIL_JSON))

        val detail = api.getPokemonDetail(server.url("/pokemon/1/").toString())

        assertEquals(1, detail.id)
        assertEquals("grass", detail.types?.first()?.type?.name)
        assertEquals("artwork.png", detail.sprites?.other?.officialArtwork?.frontDefault)
        assertEquals("front.png", detail.sprites?.frontDefault)
        assertEquals("https://pokeapi.co/api/v2/pokemon-species/1/", detail.species?.url)
        assertEquals("/pokemon/1/", server.takeRequest().target)
    }

    /**
     * The addresses of the detail and of the species are taken from the API, never built, so the
     * call has to go exactly where it was pointed.
     */
    @Test
    fun `calls the url it is given rather than one of its own`() = runTest {
        server.enqueue(MockResponse(body = SPECIES_JSON))

        api.getPokemonSpecies(server.url("/pokemon-species/386/").toString())

        assertEquals("/pokemon-species/386/", server.takeRequest().target)
    }

    /** The fields the DTO leaves out must not upset the parsing of the ones it keeps. */
    @Test
    fun `ignores the paging fields it does not declare`() = runTest {
        server.enqueue(
            MockResponse(
                body = """{"count":2,"next":null,"previous":null,"results":[{"name":"mew"}]}""",
            ),
        )

        assertEquals(listOf("mew"), api.getPokemonIndex(limit = 100_000).results?.map { it.name })
    }

    @Test
    fun `reads the flavour text entries of a species`() = runTest {
        server.enqueue(MockResponse(body = SPECIES_JSON))

        val species = api.getPokemonSpecies(server.url("/pokemon-species/1/").toString())

        assertEquals(2, species.flavorTextEntries?.size)
        assertEquals("en", species.flavorTextEntries?.first()?.language?.name)
    }

    private companion object {
        const val INDEX_JSON = """
            {
              "count": 1351,
              "next": null,
              "previous": null,
              "results": [
                { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/" },
                { "name": "ivysaur", "url": "https://pokeapi.co/api/v2/pokemon/2/" }
              ]
            }
        """

        const val DETAIL_JSON = """
            {
              "id": 1,
              "name": "bulbasaur",
              "height": 7,
              "types": [
                { "slot": 1, "type": { "name": "grass", "url": "https://pokeapi.co/api/v2/type/12/" } },
                { "slot": 2, "type": { "name": "poison", "url": "https://pokeapi.co/api/v2/type/4/" } }
              ],
              "species": {
                "name": "bulbasaur",
                "url": "https://pokeapi.co/api/v2/pokemon-species/1/"
              },
              "sprites": {
                "front_default": "front.png",
                "back_default": "back.png",
                "other": {
                  "dream_world": { "front_default": "dream.svg" },
                  "official-artwork": { "front_default": "artwork.png" }
                }
              }
            }
        """

        const val SPECIES_JSON = """
            {
              "id": 1,
              "flavor_text_entries": [
                {
                  "flavor_text": "A strange seed was\nplanted on its back.",
                  "language": { "name": "en", "url": "https://pokeapi.co/api/v2/language/9/" }
                },
                {
                  "flavor_text": "Un seme misterioso.",
                  "language": { "name": "it", "url": "https://pokeapi.co/api/v2/language/8/" }
                }
              ]
            }
        """
    }
}
