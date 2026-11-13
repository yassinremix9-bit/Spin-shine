package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val machineId: Int = 0,
    val machineName: String,
    val title: String,
    val message: String,
    val type: String = NotificationType.INFO.storageKey,
    val isRead: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val notificationType: NotificationType
        get() = NotificationType.fromKey(type)
}
