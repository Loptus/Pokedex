package it.kata.pokedex.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Twenty per page, as required. */
private const val PAGE_SIZE = 20

/**
 * The only way the list gets its data. Keeping the [Pager] here, rather than in the ViewModel,
 * is what lets the ViewModel stay clear of the repository.
 */
class GetPokemonPagingUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {

    /**
     * `initialLoadSize` has to be set explicitly. Paging defaults it to three times the page size,
     * which would hand the screen sixty rows to fill on the first load instead of twenty.
     */
    operator fun invoke(query: String): Flow<PagingData<PokemonRef>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { repository.pokemonPagingSource(query) },
    ).flow
}
