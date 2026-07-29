package it.kata.pokedex.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import it.kata.pokedex.presentation.list.PokemonListScreen
import it.kata.pokedex.presentation.list.sampleDescriptions
import it.kata.pokedex.presentation.list.samplePokemon
import it.kata.pokedex.presentation.theme.PokedexTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokedexTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Static data for now: the ViewModel takes over in the next step.
                    PokemonListScreen(
                        pokemon = samplePokemon,
                        descriptionFor = { sampleDescriptions.getValue(it) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
