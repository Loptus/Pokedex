package it.kata.pokedex.data.remote.mapper

import it.kata.pokedex.data.remote.dto.ArtworkDto
import it.kata.pokedex.data.remote.dto.FlavorTextEntryDto
import it.kata.pokedex.data.remote.dto.NamedResourceDto
import it.kata.pokedex.data.remote.dto.OtherSpritesDto
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import it.kata.pokedex.data.remote.dto.SpritesDto
import it.kata.pokedex.data.remote.dto.TypeSlotDto
import it.kata.pokedex.domain.model.PokemonType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PokemonMappersTest {

    @Test
    fun `prefers the official artwork over the small sprite`() {
        val detail = detailDto(
            sprites = SpritesDto(
                frontDefault = "small.png",
                other = OtherSpritesDto(ArtworkDto("artwork.png")),
            ),
        )

        assertEquals("artwork.png", detail.toDomain("")?.imageUrl)
    }

    @Test
    fun `falls back to the small sprite when there is no artwork`() {
        val detail = detailDto(sprites = SpritesDto(frontDefault = "small.png", other = null))

        assertEquals("small.png", detail.toDomain("")?.imageUrl)
    }

    @Test
    fun `falls back again when the artwork entry exists but is empty`() {
        val detail = detailDto(
            sprites = SpritesDto(
                frontDefault = "small.png",
                other = OtherSpritesDto(ArtworkDto(frontDefault = "")),
            ),
        )

        assertEquals("small.png", detail.toDomain("")?.imageUrl)
    }

    @Test
    fun `accepts a pokemon with no image at all`() {
        assertNull(detailDto(sprites = null).toDomain("")?.imageUrl)
    }

    @Test
    fun `reads the types in the order the api gives them`() {
        val detail = detailDto(
            types = listOf(TypeSlotDto(NamedResourceDto(name = "grass")), TypeSlotDto(NamedResourceDto(name = "poison"))),
        )

        assertEquals(listOf(PokemonType.GRASS, PokemonType.POISON), detail.toDomain("")?.types)
    }

    /** A type added to the API later must not take the whole entry down with it. */
    @Test
    fun `drops a type it does not know and keeps the rest`() {
        val detail = detailDto(
            types = listOf(TypeSlotDto(NamedResourceDto(name = "stellar")), TypeSlotDto(NamedResourceDto(name = "fire"))),
        )

        assertEquals(listOf(PokemonType.FIRE), detail.toDomain("")?.types)
    }

    @Test
    fun `gives up on an entry with no id or no name`() {
        assertNull(detailDto(id = null).toDomain(""))
        assertNull(detailDto(name = null).toDomain(""))
        assertNull(detailDto(name = "  ").toDomain(""))
    }

    /**
     * The API stores the text as it was laid out in the games, so it arrives with literal newlines
     * and form feeds inside. Rendering it untouched breaks a line mid word.
     */
    @Test
    fun `cleans the control characters out of the description`() {
        val species = PokemonSpeciesDto(
            flavorTextEntries = listOf(
                FlavorTextEntryDto(
                    flavorText = "It uses the nutrients\nthat are packedon its back  in order to grow.",
                    language = NamedResourceDto(name = "en"),
                ),
            ),
        )

        assertEquals(
            "It uses the nutrients that are packed on its back in order to grow.",
            species.toDescription(),
        )
    }

    @Test
    fun `takes the first english entry and ignores the other languages`() {
        val species = PokemonSpeciesDto(
            flavorTextEntries = listOf(
                FlavorTextEntryDto("Testo italiano", NamedResourceDto(name = "it")),
                FlavorTextEntryDto("First english", NamedResourceDto(name = "en")),
                FlavorTextEntryDto("Second english", NamedResourceDto(name = "en")),
            ),
        )

        assertEquals("First english", species.toDescription())
    }

    @Test
    fun `returns an empty description when there is nothing usable`() {
        assertEquals("", PokemonSpeciesDto(flavorTextEntries = null).toDescription())
        assertEquals(
            "",
            PokemonSpeciesDto(
                flavorTextEntries = listOf(FlavorTextEntryDto("Solo italiano", NamedResourceDto(name = "it"))),
            ).toDescription(),
        )
    }

    private fun detailDto(
        id: Int? = 1,
        name: String? = "bulbasaur",
        types: List<TypeSlotDto>? = emptyList(),
        sprites: SpritesDto? = null,
    ) = PokemonDetailDto(id = id, name = name, types = types, sprites = sprites)
}
