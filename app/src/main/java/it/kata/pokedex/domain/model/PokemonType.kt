package it.kata.pokedex.domain.model

/**
 * The eighteen official Pokemon types.
 *
 * [apiName] is the exact value used by the PokeAPI, so this enum is the single source of truth
 * both for parsing responses and for building the type filter in the UI.
 *
 * Colors and labels are deliberately absent: they belong to the presentation layer.
 */
enum class PokemonType(val apiName: String) {
    NORMAL("normal"),
    FIRE("fire"),
    WATER("water"),
    GRASS("grass"),
    ELECTRIC("electric"),
    ICE("ice"),
    FIGHTING("fighting"),
    POISON("poison"),
    GROUND("ground"),
    FLYING("flying"),
    PSYCHIC("psychic"),
    BUG("bug"),
    ROCK("rock"),
    GHOST("ghost"),
    DRAGON("dragon"),
    DARK("dark"),
    STEEL("steel"),
    FAIRY("fairy");

    companion object {
        private val byApiName = entries.associateBy(PokemonType::apiName)

        /**
         * Returns null when the value is not one of the known types, so a type added to the API
         * later is skipped instead of breaking the whole response.
         */
        fun fromApiName(apiName: String): PokemonType? = byApiName[apiName.trim().lowercase()]
    }
}
