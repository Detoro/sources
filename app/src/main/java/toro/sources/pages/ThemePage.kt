package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.models.AppTheme
import toro.sources.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePage(
    sessionViewModel: SessionViewModel
) {
    val currentTheme by sessionViewModel.themeSelection.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Theme") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption(
                title = "System Default",
                selected = currentTheme == AppTheme.SYSTEM,
                onClick = { sessionViewModel.editTheme(AppTheme.SYSTEM) }
            )
            ThemeOption(
                title = "Light",
                selected = currentTheme == AppTheme.LIGHT,
                onClick = { sessionViewModel.editTheme(AppTheme.LIGHT) }
            )
            ThemeOption(
                title = "Dark",
                selected = currentTheme == AppTheme.DARK,
                onClick = { sessionViewModel.editTheme(AppTheme.DARK) }
            )
            ThemeOption(
                title = "Pink",
                selected = currentTheme == AppTheme.PINK,
                onClick = { sessionViewModel.editTheme(AppTheme.PINK) }
            )
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            RadioButton(selected = selected, onClick = null)
        }
    }
}