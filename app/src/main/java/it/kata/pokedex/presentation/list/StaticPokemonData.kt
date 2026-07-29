package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.domain.model.PokemonType.BUG
import it.kata.pokedex.domain.model.PokemonType.ELECTRIC
import it.kata.pokedex.domain.model.PokemonType.FAIRY
import it.kata.pokedex.domain.model.PokemonType.FIRE
import it.kata.pokedex.domain.model.PokemonType.FLYING
import it.kata.pokedex.domain.model.PokemonType.GRASS
import it.kata.pokedex.domain.model.PokemonType.GROUND
import it.kata.pokedex.domain.model.PokemonType.NORMAL
import it.kata.pokedex.domain.model.PokemonType.POISON
import it.kata.pokedex.domain.model.PokemonType.WATER

/**
 * Temporary stand in for the API, and the fixture the previews render.
 *
 * One copy on purpose: if the previews kept a list of their own it would drift from what the app
 * actually shows. The whole file goes away once the data layer is in.
 *
 * Long enough to span three pages of twenty, otherwise paging cannot be exercised at all.
 *
 * The descriptions are written here rather than copied from the real Pokedex entries: the app will
 * pull the genuine text from the API, so there is no reason to bake game text into the repo.
 * [Pokemon.imageUrl] stays null for the same reason: the artwork URL is something the API gives us,
 * not something to guess from an id.
 */
private class Entry(val pokemon: Pokemon, val description: String)

private fun entry(
    id: Int,
    name: String,
    description: String,
    vararg types: PokemonType,
) = Entry(
    pokemon = Pokemon(id = id, name = name, imageUrl = null, types = types.toList()),
    description = description,
)

private val entries = listOf(
    entry(1, "bulbasaur", "A seed on its back grows larger as it soaks up the sun.", GRASS, POISON),
    entry(2, "ivysaur", "The bud on its back has grown heavy enough to slow it down.", GRASS, POISON),
    entry(3, "venusaur", "Its flower drinks in sunlight and gives off a faint sweet scent.", GRASS, POISON),
    entry(4, "charmander", "The flame on its tail burns lower when it is tired or unwell.", FIRE),
    entry(5, "charmeleon", "It grows restless before a fight and lashes its burning tail.", FIRE),
    entry(6, "charizard", "It flies high enough to look for opponents worth its time.", FIRE, FLYING),
    entry(7, "squirtle", "Its shell is smooth enough to cut cleanly through the water.", WATER),
    entry(8, "wartortle", "It hides in ponds and watches the shore before surfacing.", WATER),
    entry(9, "blastoise", "The cannons on its shell fire jets strong enough to dent steel.", WATER),
    entry(10, "caterpie", "Its feet grip almost any surface, even wet stone.", BUG),
    entry(11, "metapod", "It stays still for days while everything inside it changes.", BUG),
    entry(12, "butterfree", "It finds pollen from far away and carries it between fields.", BUG, FLYING),
    entry(13, "weedle", "The spike on its head keeps most predators at a distance.", BUG, POISON),
    entry(14, "kakuna", "It barely moves, but grows warm when something comes too close.", BUG, POISON),
    entry(15, "beedrill", "It defends its nest in groups and never chases alone.", BUG, POISON),
    entry(16, "pidgey", "It kicks up sand to cover its escape rather than fight.", NORMAL, FLYING),
    entry(17, "pidgeotto", "It circles a wide territory and rarely leaves it.", NORMAL, FLYING),
    entry(18, "pidgeot", "Its wingbeats are strong enough to flatten tall grass.", NORMAL, FLYING),
    entry(19, "rattata", "It settles anywhere there is food, and there usually is.", NORMAL),
    entry(20, "raticate", "Its front teeth never stop growing, so it gnaws constantly.", NORMAL),
    entry(21, "spearow", "It flaps hard and low, and its cry carries a long way.", NORMAL, FLYING),
    entry(22, "fearow", "Its long neck lets it snatch prey without landing.", NORMAL, FLYING),
    entry(23, "ekans", "It sleeps coiled so it can strike from any direction.", POISON),
    entry(24, "arbok", "The pattern on its hood changes from one population to the next.", POISON),
    entry(25, "pikachu", "It stores energy in its cheeks and lets it go when startled.", ELECTRIC),
    entry(26, "raichu", "Its tail drains excess charge into the ground as it walks.", ELECTRIC),
    entry(27, "sandshrew", "It rolls into a ball and lets the slope do the running.", GROUND),
    entry(28, "sandslash", "Its claws work as well for digging as for defending.", GROUND),
    entry(29, "nidoran-f", "Small and cautious, it prefers to warn rather than fight.", POISON),
    entry(30, "nidorina", "It keeps its horn lowered around its own young.", POISON),
    entry(31, "nidoqueen", "Its scales stand on end when it braces for an attack.", POISON, GROUND),
    entry(32, "nidoran-m", "It listens with its large ears long before it moves.", POISON),
    entry(33, "nidorino", "It is quick to anger and quicker to charge.", POISON),
    entry(34, "nidoking", "One swing of its tail can snap a young tree.", POISON, GROUND),
    entry(35, "clefairy", "It is rarely seen, and mostly on clear nights.", FAIRY),
    entry(36, "clefable", "Its hearing catches a pin dropped a long way off.", FAIRY),
    entry(37, "vulpix", "It splits its single tail as it grows older.", FIRE),
    entry(38, "ninetales", "It is said to live a very long time, and to hold grudges.", FIRE),
    entry(39, "jigglypuff", "It sings until whoever is listening drifts off.", NORMAL, FAIRY),
    entry(40, "wigglytuff", "It takes in air until it is far larger than it started.", NORMAL, FAIRY),
    entry(41, "zubat", "It has no need for eyes in the caves where it lives.", POISON, FLYING),
    entry(42, "golbat", "It hunts in the dark and rests through the whole day.", POISON, FLYING),
    entry(43, "oddish", "It plants itself in the soil at night to feed.", GRASS, POISON),
    entry(44, "gloom", "The scent it gives off can be noticed from far away.", GRASS, POISON),
    entry(45, "vileplume", "Its petals are the largest of any flower Pokemon.", GRASS, POISON),
)

val staticPokemon: List<Pokemon> = entries.map(Entry::pokemon)

val staticDescriptions: Map<Int, String> = entries.associate { it.pokemon.id to it.description }
