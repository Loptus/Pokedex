package it.kata.pokedex.data.repository

import androidx.room.Room
import app.cash.turbine.test
import it.kata.pokedex.data.local.PokedexDatabase
import it.kata.pokedex.domain.model.PokemonRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The repository and the DAO are tested together against a real in-memory database, because taken
 * apart there is almost nothing left: the toggle is one SQL transaction, and a mocked DAO would only
 * confirm that the repository calls the method the test told it to call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
// Robolectric has no image for SDK 37 yet, and the modules target it.
@Config(sdk = [36])
class FavoriteRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var database: PokedexDatabase
    private lateinit var repository: FavoriteRepositoryImpl

    private val bulbasaur = PokemonRef(id = 1, name = "bulbasaur", detailUrl = "url/1")
    private val charmander = PokemonRef(id = 4, name = "charmander", detailUrl = "url/4")

    @Before
    fun setUp() {
        // Room's own executors are replaced by the test dispatcher, so a write and the emission it
        // causes land in this test's timeline instead of on a background thread of their own.
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            PokedexDatabase::class.java,
        )
            .setQueryExecutor(dispatcher.asExecutor())
            .setTransactionExecutor(dispatcher.asExecutor())
            .build()

        repository = FavoriteRepositoryImpl(database.favoriteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `starts with nothing saved`() = runTest(dispatcher) {
        repository.favoriteIds().test {
            assertEquals(emptySet(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling saves, and toggling again removes`() = runTest(dispatcher) {
        repository.favoriteIds().test {
            assertEquals(emptySet(), awaitItem())

            repository.toggle(bulbasaur)
            assertEquals(setOf(1), awaitItem())

            repository.toggle(bulbasaur)
            assertEquals(emptySet(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing one favorite leaves the others alone`() = runTest(dispatcher) {
        repository.toggle(bulbasaur)
        repository.toggle(charmander)

        repository.toggle(bulbasaur)

        repository.favoriteIds().test {
            assertEquals(setOf(4), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Pokedex order, not the order they were saved in, which is why the table has no column
     * recording when a favorite was added.
     */
    @Test
    fun `lists the favorites by pokedex number`() = runTest(dispatcher) {
        val pikachu = PokemonRef(id = 25, name = "pikachu", detailUrl = "url/25")

        repository.toggle(pikachu)
        repository.toggle(charmander)
        repository.toggle(bulbasaur)

        assertEquals(listOf(1, 4, 25), repository.favorites().first().map { it.id })
    }

    @Test
    fun `removing takes one out and leaves the rest`() = runTest(dispatcher) {
        repository.toggle(bulbasaur)
        repository.toggle(charmander)

        repository.remove(bulbasaur.id)

        assertEquals(listOf(charmander), repository.favorites().first())
    }

    /** The page can only ever remove what it is showing, but a double tap must not be an error. */
    @Test
    fun `removing something that was never saved does nothing`() = runTest(dispatcher) {
        repository.toggle(bulbasaur)

        repository.remove(charmander.id)

        assertEquals(listOf(bulbasaur), repository.favorites().first())
    }

    /** Saving the same entry twice must not leave two rows behind. */
    @Test
    fun `saving again after removing keeps a single entry`() = runTest(dispatcher) {
        repository.toggle(bulbasaur)
        repository.toggle(bulbasaur)
        repository.toggle(bulbasaur)

        assertEquals(listOf(1), database.favoriteDao().observeIds().first())
    }

    /**
     * The pointer is stored whole, address included, and this reads the columns rather than the
     * ids because the address is the part that would go missing quietly.
     *
     * Rebuilding it from the id is the mistake this project already paid for: past 10000 the ids of
     * the entries and of their species do not line up, so a favorite alternate form saved without
     * its address would come back unloadable.
     */
    @Test
    fun `keeps the address it was given`() = runTest(dispatcher) {
        val deoxysAttack = PokemonRef(
            id = 10001,
            name = "deoxys-attack",
            detailUrl = "https://pokeapi.co/api/v2/pokemon/10001/",
        )

        repository.toggle(deoxysAttack)

        database.openHelper.readableDatabase
            .query("SELECT id, name, detailUrl FROM favorite_pokemon")
            .use { stored ->
                assertTrue(stored.moveToFirst())
                assertEquals(deoxysAttack.id, stored.getInt(0))
                assertEquals(deoxysAttack.name, stored.getString(1))
                assertEquals(deoxysAttack.detailUrl, stored.getString(2))
            }
    }
}
