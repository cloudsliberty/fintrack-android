@file:OptIn(ExperimentalMaterial3Api::class)

package com.fintrack.android.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fintrack.android.data.model.Category
import com.fintrack.android.ui.common.*

@Composable
fun CategoriesScreen() {
    val viewModel: CategoriesViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddCategory by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf<Category?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<Category?>(null) }
    var showAddTag by remember { mutableStateOf(false) }
    var showRenameTag by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTag by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories & Tags") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategory = true }) { Icon(Icons.Filled.Add, contentDescription = "Add category") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = s.data
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Text("Categories", style = MaterialTheme.typography.titleMedium) }
                        if (data.categories.isEmpty()) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("No categories yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                    TextButton(onClick = viewModel::createDefaultCategories) { Text("Add defaults") }
                                }
                            }
                        }
                        items(data.categories, key = { it.id }) { category ->
                            CategoryRow(category, onEdit = { showEditCategory = category }, onDelete = { pendingDeleteCategory = category })
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { showAddTag = true }) { Text("Add tag") }
                            }
                        }
                        if (data.tags.isEmpty()) {
                            item { Text("No tags yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            item {
                                TagFlowGrid(
                                    tags = data.tags,
                                    onRename = { showRenameTag = it },
                                    onDelete = { pendingDeleteTag = it }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        CategoryEditorDialog(category = null, onDismiss = { showAddCategory = false }, onSave = { name, type, icon, color ->
            viewModel.saveCategory(null, name, type, icon, color); showAddCategory = false
        })
    }
    showEditCategory?.let { category ->
        CategoryEditorDialog(category = category, onDismiss = { showEditCategory = null }, onSave = { name, type, icon, color ->
            viewModel.saveCategory(category.id, name, type, icon, color); showEditCategory = null
        })
    }
    pendingDeleteCategory?.let { category ->
        ConfirmDialog(
            title = "Delete category?", message = "\"${category.name}\" will be removed. Existing transactions keep their category name as text.",
            onConfirm = { viewModel.deleteCategory(category.id) }, onDismiss = { pendingDeleteCategory = null }
        )
    }
    if (showAddTag) {
        TextPromptDialog(title = "Add tag", label = "Tag name", onConfirm = { viewModel.addTag(it); showAddTag = false }, onDismiss = { showAddTag = false })
    }
    showRenameTag?.let { tag ->
        TextPromptDialog(
            title = "Rename tag", label = "New name", initialValue = tag,
            onConfirm = { viewModel.renameTag(tag, it); showRenameTag = null },
            onDismiss = { showRenameTag = null }
        )
    }
    pendingDeleteTag?.let { tag ->
        ConfirmDialog(
            title = "Delete tag?", message = "\"$tag\" will be removed from the tag list. It stays on transactions that already used it.",
            onConfirm = { viewModel.deleteTag(tag) }, onDismiss = { pendingDeleteTag = null }
        )
    }
    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearActionError, title = { Text("Couldn't save") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearActionError) { Text("OK") } }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category.icon.ifBlank { "🏷️" }, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
                Text(category.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun TagFlowGrid(tags: List<String>, onRename: (String) -> Unit, onDelete: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.height((((tags.size + 1) / 2) * 56 + 8).dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tags) { tag ->
            ElevatedCard {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tag, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { onRename(tag) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDelete(tag) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(category: Category?, onDismiss: () -> Unit, onSave: (name: String, type: String, icon: String, color: String) -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: "expense") }
    var icon by remember { mutableStateOf(category?.icon ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = icon, onValueChange = { icon = it.take(2) }, label = { Text("Icon (emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "expense", onClick = { type = "expense" }, label = { Text("Expense") })
                    FilterChip(selected = type == "income", onClick = { type = "income" }, label = { Text("Income") })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), type, icon.trim(), category?.color ?: "#4f8ef7") }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TextPromptDialog(title: String, label: String, initialValue: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
