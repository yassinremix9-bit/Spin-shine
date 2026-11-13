package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class WashingMachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val model: String,
    val location: String,
    val capacityKg: Int,
    val status: String = MachineStatus.AVAILABLE.storageKey,
    val programName: String = "",
    val totalDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val temperatureCelsius: Int = 40,
    val spinRpm: Int = 1200,
    val reminderEnabled: Boolean = false,
    val endingSoonNotified: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val machineStatus: MachineStatus
        get() = MachineStatus.fromKey(status)

    val progressFraction: Float
        get() = if (totalDurationSeconds > 0 && remainingSeconds >= 0) {
            ((totalDurationSeconds - remainingSeconds).toFloat() / totalDurationSeconds.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }
}
