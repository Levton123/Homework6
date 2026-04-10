package com.example.pokedex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pokedex.data.model.PokemonDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    uiState: PokemonDetailUiState,
    onEvent: (PokemonDetailEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val successState = uiState as? PokemonDetailUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pokemon Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (successState != null) {
                        IconButton(onClick = {
                            onEvent(PokemonDetailEvent.ToggleFavourite(successState.pokemon.name))
                        }) {
                            Icon(
                                imageVector = if (successState.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (successState.isFavourite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is PokemonDetailUiState.Loading -> LoadingDetailContent(modifier = Modifier.padding(padding))
            is PokemonDetailUiState.Success -> PokemonDetailContent(pokemon = uiState.pokemon, modifier = Modifier.padding(padding))
            is PokemonDetailUiState.Error -> ErrorDetailContent(
                message = uiState.message,
                onRetry = { onEvent(PokemonDetailEvent.Retry) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun LoadingDetailContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading Pokemon details...")
        }
    }
}

@Composable
private fun ErrorDetailContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun PokemonDetailContent(pokemon: PokemonDetail, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = pokemon.sprites.other?.officialArtwork?.frontDefault ?: pokemon.sprites.frontDefault,
            contentDescription = pokemon.name,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = pokemon.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "#${pokemon.id.toString().padStart(3, '0')}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCard(label = "Height", value = "${pokemon.height / 10.0} m")
            InfoCard(label = "Weight", value = "${pokemon.weight / 10.0} kg")
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Types")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            pokemon.types.sortedBy { it.slot }.forEach { typeSlot ->
                TypeBadge(type = typeSlot.type.name)
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Abilities")
        pokemon.abilities.forEach { abilitySlot ->
            AbilityItem(name = abilitySlot.ability.name, isHidden = abilitySlot.isHidden)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Base Stats")
        pokemon.stats.forEach { stat ->
            StatBar(name = stat.stat.name, value = stat.baseStat)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TypeBadge(type: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(
            text = type.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AbilityItem(name: String, isHidden: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name.replace("-", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (isHidden) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(text = "Hidden", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun StatBar(name: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = name.replace("-", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
            Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = (value / 255f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
    }
}