package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Long enough to skip the letters typed on the way to a word, short enough not to feel laggy. */
private const val SEARCH_DEBOUNCE_MILLIS = 300L

/**
 * Owns the state of the list screen.
 *
 * `cachedIn` keeps the loaded pages across configuration changes, so rotating the device does not
 * start the list, and its forty odd requests, all over again.
 */
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    getPokemonPaging: GetPokemonPagingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonListUiState())
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

    /**
     * The text field reads [uiState], which updates on every keystroke, while the search itself
     * waits for the typing to settle. Debouncing the state instead would make the field feel stuck.
     *
     * An empty query skips the wait: clearing the field, and the very first load, should be
     * immediate rather than sitting on a timer for no reason.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pokemon: Flow<PagingData<Pokemon>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .flatMapLatest { query -> getPokemonPaging(query) }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
