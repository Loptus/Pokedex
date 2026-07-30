package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import it.kata.pokedex.domain.usecase.GetPokemonUseCase
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
 * `cachedIn` keeps the paged pointers across configuration changes, and [rows] keeps the contents
 * that were already fetched, so rotating the device does not send the whole screen back to the API.
 */
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    getPokemonPaging: GetPokemonPagingUseCase,
    private val getPokemon: GetPokemonUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonListUiState())
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

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

    /**
     * The text field reads [uiState], which updates on every keystroke, while the search itself
     * waits for the typing to settle. Debouncing the state instead would make the field feel stuck.
     *
     * An empty query skips the wait: clearing the field, and the very first load, should be
     * immediate rather than sitting on a timer for no reason.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pokemon: Flow<PagingData<PokemonRef>> = _uiState
        .map { it.query }
        .distinctUntilChanged()
        .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .flatMapLatest { query -> getPokemonPaging(query) }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowFlow(id)

    /**
     * Fetches one row, and is deliberately a suspend function rather than a `viewModelScope.launch`.
     *
     * The caller is the row's own composition, so when the row scrolls off the screen Compose
     * cancels this and the two requests underneath it go with it. That is the whole point: a user
     * flicking through the list stops paying for the rows they flew past.
     *
     * Already loaded rows return straight away; failures are not remembered, so a row that comes
     * back into view tries again.
     */
    suspend fun loadRow(ref: PokemonRef) {
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
