package com.example.runway.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.runway.R
import com.example.runway.domain.model.Item
import com.example.runway.ui.theme.RunwayTheme

/**
 * Stateful entry point. This is what navigation points at: it owns the
 * ViewModel and nothing else.
 */
@Composable
fun ItemsRoute(
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ItemsScreen(
        uiState = uiState,
        onAddItem = viewModel::onAddItem,
        onDeleteItem = viewModel::onDeleteItem,
        onItemClick = onItemClick,
        onErrorShown = viewModel::onErrorShown,
        modifier = modifier
    )
}

/**
 * Stateless. Takes state in, sends events out - so it can be previewed and
 * tested with no ViewModel, no database and no device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    uiState: ItemsUiState,
    onAddItem: (String, String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.items_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }) {
                Text(stringResource(R.string.items_add))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.items_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, note ->
                onAddItem(title, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

/**
 * Dialog input is transient UI state, so it lives here rather than in the
 * ViewModel. Not everything belongs in the ViewModel.
 */
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.items_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.item_title_hint)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.item_note_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, note) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ItemsScreenPreview() {
    RunwayTheme {
        ItemsScreen(
            uiState = ItemsUiState(
                items = listOf(
                    Item(1, "Wire up Room", "Entity, DAO, database"),
                    Item(2, "Prove the ViewModel", "StateFlow + unit test")
                ),
                isLoading = false
            ),
            onAddItem = { _, _ -> },
            onDeleteItem = {},
            onItemClick = {},
            onErrorShown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemsScreenEmptyPreview() {
    RunwayTheme {
        ItemsScreen(
            uiState = ItemsUiState(isLoading = false),
            onAddItem = { _, _ -> },
            onDeleteItem = {},
            onItemClick = {},
            onErrorShown = {}
        )
    }
}
