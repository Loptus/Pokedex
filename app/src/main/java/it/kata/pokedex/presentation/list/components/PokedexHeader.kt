package it.kata.pokedex.presentation.list.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.kata.pokedex.R
import it.kata.pokedex.presentation.theme.PokedexTheme

@Composable
fun PokedexHeader(modifier: Modifier = Modifier) {
    val title = buildAnnotatedString {
        append(stringResource(R.string.header_title_regular))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.header_title_bold))
        }
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun PokedexHeaderPreview() {
    PokedexTheme { PokedexHeader() }
}
