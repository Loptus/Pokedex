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
 * No `uiState` here, and not by oversight: this screen has no query and no filters, so it has the
 * data and nothing else.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val rowLoader: PokemonRowLoader,
    private val removeFavorite: RemoveFavoriteUseCase,
) : ViewModel() {

    /**
     * Null until the database has answered: with an empty list as the initial value, someone with
     * twenty favorites would be told they have none for the frame before the first emission.
     */
    val favorites: StateFlow<List<PokemonRef>?> = observeFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = null,
        )

    /** In [viewModelScope], not the row's: that row is about to leave the list. */
    fun onRemove(id: Int) {
        viewModelScope.launch { removeFavorite(id) }
    }

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowLoader.rowState(id)

    suspend fun loadRow(ref: PokemonRef) = rowLoader.load(ref)
}
