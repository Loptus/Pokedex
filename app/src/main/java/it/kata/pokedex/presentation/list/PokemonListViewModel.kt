package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.domain.model.Pokemon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Twenty per page, as required. */
private const val PAGE_SIZE = 20

/**
 * Owns the state of the list screen.
 *
 * It still pages over the static list: the point of this step is paging itself, and the data layer
 * will replace the source without changing the shape of what the screen consumes.
 */
@HiltViewModel
class PokemonListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        PokemonListUiState(descriptions = staticDescriptions),
    )
    val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

    /**
     * `initialLoadSize` has to be set explicitly. Paging defaults it to three times the page size,
     * which here would pull sixty items in one go and never append anything: the requirement is a
     * page of twenty, first load included.
     *
     * `cachedIn` keeps the loaded pages across configuration changes, so rotating the device does
     * not start the list over.
     */
    val pokemon: Flow<PagingData<Pokemon>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { StaticPokemonPagingSource(staticPokemon) },
    ).flow.cachedIn(viewModelScope)
}
