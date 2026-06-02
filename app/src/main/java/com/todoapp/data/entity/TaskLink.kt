package com.todoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_links",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["sourceTaskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["targetTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceTaskId"), Index("targetTaskId")]
)
data class TaskLink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceTaskId: Long,
    val targetTaskId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
