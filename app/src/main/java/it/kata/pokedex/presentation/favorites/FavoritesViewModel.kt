package it.kata.pokedex.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.usecase.ObserveFavoritesUseCase
import it.kata.pokedex.domain.usecase.RemoveFavoriteUseCase
import it.kata.pokedex.presentation.common.PokemonRowLoader
import it.kata.pokedex.presentation.common.PokemonRowState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Long enough to survive a rotation, short enough not to sit on the database for no reason. */
private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Owns the state of the favorites screen.
 *
 * There is no `uiState` here, and that is not an oversight: this screen has no state of its own to
 * keep, no query and no filters, so what it has is the data and nothing else. The list screen keeps
 * the two apart for the same reason, and its `uiState` holds what the user typed and picked rather
 * than what came back.
 *
 * The saved entries arrive as pointers, so this screen loads its rows the way the list does, through
 * the same [PokemonRowLoader]. Its own instance, though: the two screens do not share the fetched
 * contents, which is why opening this page fetches again, from the HTTP cache rather than from the
 * network.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val rowLoader: PokemonRowLoader,
    private val removeFavorite: RemoveFavoriteUseCase,
) : ViewModel() {

    /**
     * Null until the database has answered, which is the difference between "nothing saved" and
     * "not asked yet": starting from an empty list would tell someone with twenty favorites that
     * they have none, for the frame before the first emission.
     */
    val favorites: StateFlow<List<PokemonRef>?> = observeFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = null,
        )

    /**
     * Runs in [viewModelScope] and not in the row's: the row is about to be removed from the list,
     * so its composition is exactly what cannot be relied on to finish the work.
     */
    fun onRemove(id: Int) {
        viewModelScope.launch { removeFavorite(id) }
    }

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowLoader.rowState(id)

    /** Suspends in the caller's scope, which is the row's composition: see [PokemonRowLoader]. */
    suspend fun loadRow(ref: PokemonRef) = rowLoader.load(ref)
}
