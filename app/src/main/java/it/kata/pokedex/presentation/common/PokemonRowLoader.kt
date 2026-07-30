package it.kata.pokedex.presentation.common

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.usecase.GetPokemonUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Fetches the contents of list rows, one visible row at a time, and remembers what came back.
 *
 * Shared by the two screens that draw Pokemon rows, because the way a row is paid for is the same on
 * both: the list has its pointers long before it has anything to show for them.
 *
 * One instance per ViewModel, deliberately not a singleton. A shared cache would save the second
 * screen a round trip, but it would also grow for as long as the app is alive with nothing deciding
 * what to drop.
 */
class PokemonRowLoader @Inject constructor(
    private val getPokemon: GetPokemonUseCase,
) {

    /**
     * One flow per Pokemon rather than one map for all of them: a row collects only its own, so a
     * row arriving recomposes that row and leaves the others alone.
     *
     * Plain collections with no lock because every access happens on the main thread, from
     * composition or from a coroutine started by it. It doubles as the cache, and it outlives the
     * paging invalidation caused by a new query, so going back to a previous search does not refetch
     * rows that are already here.
     */
    private val rows = mutableMapOf<Int, MutableStateFlow<PokemonRowState>>()

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowFlow(id)

    /**
     * Fetches one row, and is deliberately a suspend function rather than something launched in a
     * scope of its own.
     *
     * The caller is the row's own composition, so when the row scrolls off the screen Compose
     * cancels this and the requests underneath it go with it. That is the whole point: a user
     * flicking through the list stops paying for the rows they flew past.
     *
     * Already loaded rows return straight away; failures are not remembered, so a row that comes
     * back into view tries again.
     */
    suspend fun load(ref: PokemonRef) {
        val flow = rowFlow(ref.id)
        if (flow.value is PokemonRowState.Loaded) return

        flow.value = when (val result = getPokemon(ref)) {
            is AppResult.Success -> PokemonRowState.Loaded(result.value)
            is AppResult.Failure -> PokemonRowState.Failed
        }
    }

    private fun rowFlow(id: Int): MutableStateFlow<PokemonRowState> =
        rows.getOrPut(id) { MutableStateFlow(PokemonRowState.Loading) }
}
