package it.kata.pokedex.presentation.common

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.usecase.GetPokemonUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * One instance per ViewModel, deliberately not a singleton: a shared cache would save the second
 * screen a round trip, but would grow for as long as the app is alive with nothing deciding what to
 * drop.
 */
class PokemonRowLoader @Inject constructor(
    private val getPokemon: GetPokemonUseCase,
) {

    /**
     * One flow per Pokemon rather than one map for all of them, so a row arriving recomposes itself
     * and leaves the others alone. No lock: every access happens on the main thread.
     */
    private val rows = mutableMapOf<Int, MutableStateFlow<PokemonRowState>>()

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowFlow(id)

    /**
     * Suspends in the caller's scope on purpose. The caller is the row's composition, so Compose
     * cancels this when the row leaves the screen and the requests underneath go with it: scrolling
     * fast stops paying for the rows flown past.
     *
     * Failures are not remembered, so a row that comes back into view tries again.
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
