package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import it.kata.pokedex.domain.usecase.ObserveFavoriteIdsUseCase
import it.kata.pokedex.domain.usecase.ToggleFavoriteUseCase
import it.kata.pokedex.presentation.common.PokemonRowLoader
import it.kata.pokedex.presentation.common.PokemonRowState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Long enough to skip the letters typed on the way to a word, short enough not to feel laggy. */
private const val SEARCH_DEBOUNCE_MILLIS = 300L

/**
 * Owns the state of the list screen.
 *
 * `cachedIn` keeps the paged pointers across configuration changes, and [rowLoader] keeps the
 * contents that were already fetched, so rotating the device does not send the whole screen back to
 * the API.
 */
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    getPokemonPaging: GetPokemonPagingUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val rowLoader: PokemonRowLoader,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonListUiState())
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

    /**
     * Only the typing is debounced, and the two halves of the query are debounced separately for a
     * reason: tapping a chip is one deliberate action and should take effect at once, while typing
     * arrives one letter at a time and would otherwise fire a search for every prefix.
     *
     * An empty name skips the wait too: clearing the field, and the very first load, should not sit
     * on a timer for no reason.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val pagedRefs: Flow<PagingData<PokemonRef>> = combine(
        _uiState
            .map { it.query }
            .distinctUntilChanged()
            .debounce { name -> if (name.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS },
        _uiState
            .map { it.selectedTypes }
            .distinctUntilChanged(),
    ) { name, types -> PokemonQuery(name = name, types = types) }
        .distinctUntilChanged()
        .flatMapLatest { query -> getPokemonPaging(query) }
        .cachedIn(viewModelScope)

    /**
     * The paged pointers with the saved ones already marked, so the screen renders a flag instead of
     * working one out.
     *
     * The combine sits after `cachedIn` on purpose: what is worth caching across a rotation is the
     * pages themselves, and marking them is cheap enough to redo whenever the favorites change.
     * Putting it the other way round would cache the flags too and leave a heart stale.
     */
    val pokemon: Flow<PagingData<PokemonListItem>> = combine(
        pagedRefs,
        observeFavoriteIds(),
    ) { refs, favoriteIds ->
        refs.map { ref -> PokemonListItem(ref = ref, isFavorite = ref.id in favoriteIds) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onTypeToggle(type: PokemonType) {
        _uiState.update { state ->
            val selected = state.selectedTypes
            state.copy(selectedTypes = if (type in selected) selected - type else selected + type)
        }
    }

    /**
     * Runs in [viewModelScope] and not in the row's own scope on purpose: the row that started the
     * write can scroll away half a second later, and a saved favorite should not depend on the user
     * keeping it on screen.
     */
    fun onFavoriteToggle(ref: PokemonRef) {
        viewModelScope.launch { toggleFavorite(ref) }
    }

    fun rowState(id: Int): StateFlow<PokemonRowState> = rowLoader.rowState(id)

    /** Suspends in the caller's scope, which is the row's composition: see [PokemonRowLoader]. */
    suspend fun loadRow(ref: PokemonRef) = rowLoader.load(ref)
}
