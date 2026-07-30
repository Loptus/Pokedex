package it.kata.pokedex.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.kata.pokedex.presentation.theme.PokedexTheme

/**
 * The two tabs. Stateless: it is told which one is current and reports which one was tapped, so it
 * knows nothing about navigation and can be previewed and tested on its own.
 */
@Composable
fun PokedexBottomBar(
    current: PokedexDestination,
    onSelect: (PokedexDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        PokedexDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        // The label right below is the accessible name already: describing the icon
                        // too would have a screen reader say it twice.
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}

@Preview
@Composable
private fun PokedexBottomBarPreview() {
    PokedexTheme {
        PokedexBottomBar(current = PokedexDestination.LIST, onSelect = {})
    }
}
