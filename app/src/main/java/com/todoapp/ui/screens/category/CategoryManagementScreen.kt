package com.todoapp.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.data.entity.Category
import com.todoapp.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(viewModel: CategoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var createParentId by remember { mutableStateOf<Long?>(null) }
    var deleteConfirmCategory by remember { mutableStateOf<Category?>(null) }

    // ── Create / Edit dialog ──
    if (showCreateDialog || editingCategory != null) {
        CategoryEditDialog(
            existing = editingCategory,
            onDismiss = { showCreateDialog = false; editingCategory = null; createParentId = null },
            onSave = { name, color ->
                if (editingCategory != null) {
                    viewModel.updateCategory(editingCategory!!.copy(name = name, color = color))
                } else {
                    viewModel.addCategory(name, color, parentId = createParentId)
                }
                showCreateDialog = false
                editingCategory = null
                createParentId = null
            }
        )
    }

    // ── Delete confirmation ──
    deleteConfirmCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteConfirmCategory = null },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${cat.name}」吗？该分类下的任务将变为无分类。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(cat)
                    deleteConfirmCategory = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmCategory = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "分类管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createParentId = null; showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加分类")
            }
        }
    ) { padding ->
        if (uiState.topLevelCategories.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有分类",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角 + 创建第一个分类",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.topLevelCategories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        subCategories = uiState.subCategories[category.id] ?: emptyList(),
                        onEdit = { editingCategory = category },
                        onDelete = { deleteConfirmCategory = category },
                        onAddSub = { createParentId = category.id; showCreateDialog = true },
                        onEditSub = { sub -> editingCategory = sub },
                        onDeleteSub = { sub -> deleteConfirmCategory = sub }
                    )
                }
            }
        }
    }
}

// ── Category card ─────────────────────────────────────────────────────
@Composable
private fun CategoryCard(
    category: Category,
    subCategories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSub: () -> Unit,
    onEditSub: (Category) -> Unit,
    onDeleteSub: (Category) -> Unit
) {
    val catColor = safeColor(category.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color = catColor, shape = CircleShape)
                    )
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (subCategories.isNotEmpty()) {
                            Text(
                                text = "${subCategories.size} 个子分类",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sub-categories
            if (subCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                @Suppress("DEPRECATION")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(subCategories) { sub ->
                    val subColor = safeColor(sub.color)
                    Surface(
                        color = subColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onEditSub(sub) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(subColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = subColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除子分类",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeleteSub(sub) },
                                tint = subColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Add sub-category button
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onAddSub,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加子分类", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Simple flow row for chips ─────────────────────────────────────────
@Composable
private fun FlowRow(items: List<Category>, content: @Composable (Category) -> Unit) {
    // Use a simple wrapping approach with Column of Rows
    val chunks = items.chunked(3) // ~3 per row on phone
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunks.forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEach { item ->
                    content(item)
                }
            }
        }
    }
}

// ── Create / Edit dialog ──────────────────────────────────────────────
@Composable
private fun CategoryEditDialog(
    existing: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(
            existing?.let { safeColor(it.color) } ?: CategoryColors.first()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing != null) "编辑分类" else "新建分类",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "选择颜色",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CategoryColors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (isSelected)
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), colorToHex(selectedColor))
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────
private fun safeColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF8A8478)
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
