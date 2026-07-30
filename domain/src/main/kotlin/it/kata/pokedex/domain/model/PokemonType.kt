package it.kata.pokedex.domain.model

/**
 * The API also serves `stellar`, `unknown` and `shadow`. They are left out on purpose, and the
 * mapper drops them.
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

        /** Null for anything unknown, so a type added to the API later does not break a response. */
        fun fromApiName(apiName: String): PokemonType? = byApiName[apiName.trim().lowercase()]
    }
}
