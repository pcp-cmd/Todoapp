package com.todoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.todoapp.data.entity.Category
import com.todoapp.data.entity.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: Task,
    category: Category?,
    subTaskProgress: Pair<Int, Int>?,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isSubTask: Boolean = false,
    depth: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 24).dp) // 每层缩进 24dp
            .background(
                color = if (isSubTask) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 子任务显示 ↳ 图标
        if (isSubTask) {
            Text(
                text = "↳",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
            )
        }
        TaskCheckbox(isChecked = task.isCompleted, onCheckedChange = { onToggleComplete() }, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = if (isSubTask) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSubTask) FontWeight.Normal else FontWeight.Medium,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            // 子任务简化显示：只显示截止日期
            if (!isSubTask) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    category?.let { CategoryTag(name = it.name, color = Color(android.graphics.Color.parseColor(it.color))) }
                    task.dueDate?.let {
                        Text(text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    task.repeatRule?.let { Text(text = "🔁", style = MaterialTheme.typography.labelSmall) }
                }
                subTaskProgress?.let { (completed, total) ->
                    if (total > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$completed / $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // 子任务只显示截止日期
                task.dueDate?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
