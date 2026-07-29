package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Owns the state of the list screen.
 *
 * It still serves the static data: the point of this step is only that the screen stops reaching
 * for it directly. The data layer replaces the source, not the shape.
 */
@HiltViewModel
class PokemonListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        PokemonListUiState(
            pokemon = staticPokemon,
            descriptions = staticDescriptions,
        ),
    )
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()
}
