package it.kata.pokedex.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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

    val pokemon: Flow<PagingData<Pokemon>> = getPokemonPaging().cachedIn(viewModelScope)
}
