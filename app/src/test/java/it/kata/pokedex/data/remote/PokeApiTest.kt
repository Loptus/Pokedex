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
import kotlin.test.assertNull

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
    fun `reads a page and asks for the right window`() = runTest {
        server.enqueue(MockResponse(body = PAGE_JSON))

        val page = api.getPokemonPage(limit = 20, offset = 40)

        assertEquals(1302, page.count)
        assertEquals(2, page.results?.size)
        assertEquals("bulbasaur", page.results?.first()?.name)
        assertEquals("/pokemon?limit=20&offset=40", server.takeRequest().target)
    }

    @Test
    fun `reads the nested sprites and types of a detail`() = runTest {
        server.enqueue(MockResponse(body = DETAIL_JSON))

        val detail = api.getPokemonDetail("bulbasaur")

        assertEquals(1, detail.id)
        assertEquals("grass", detail.types?.first()?.type?.name)
        assertEquals("artwork.png", detail.sprites?.other?.officialArtwork?.frontDefault)
        assertEquals("front.png", detail.sprites?.frontDefault)
    }

    /** The last page of the API has `next: null`, which is how paging knows it is over. */
    @Test
    fun `a null next survives the parsing`() = runTest {
        server.enqueue(MockResponse(body = """{"count":2,"next":null,"results":[]}"""))

        assertNull(api.getPokemonPage(limit = 20, offset = 0).next)
    }

    @Test
    fun `reads the flavour text entries of a species`() = runTest {
        server.enqueue(MockResponse(body = SPECIES_JSON))

        val species = api.getPokemonSpecies(1)

        assertEquals(2, species.flavorTextEntries?.size)
        assertEquals("en", species.flavorTextEntries?.first()?.language?.name)
    }

    private companion object {
        const val PAGE_JSON = """
            {
              "count": 1302,
              "next": "https://pokeapi.co/api/v2/pokemon?offset=60&limit=20",
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
