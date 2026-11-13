package com.example.data.model

enum class MachineStatus(val storageKey: String) {
    AVAILABLE("AVAILABLE"),
    IN_USE("IN_USE"),
    ENDING_SOON("ENDING_SOON"),
    PAUSED("PAUSED"),
    MAINTENANCE("MAINTENANCE");

    companion object {
        fun fromKey(key: String): MachineStatus =
            entries.firstOrNull { it.storageKey == key } ?: AVAILABLE
    }
}

enum class MachineFilter {
    ALL,
    AVAILABLE,
    IN_USE,
    ENDING_SOON,
    MAINTENANCE
}

enum class NotificationType(val storageKey: String) {
    CYCLE_COMPLETE("CYCLE_COMPLETE"),
    ENDING_SOON("ENDING_SOON"),
    MAINTENANCE("MAINTENANCE"),
    INFO("INFO");

    companion object {
        fun fromKey(key: String): NotificationType =
            entries.firstOrNull { it.storageKey == key } ?: INFO
    }
}

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromKey(key: String): AppThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

data class WashProgram(
    val id: String,
    val titleAr: String,
    val durationMinutes: Int,
    val temperatureCelsius: Int,
    val spinRpm: Int
)

object WashPrograms {
    val presets = listOf(
        WashProgram("quick", "غسيل سريع", 15, 30, 800),
        WashProgram("delicates", "عناية فائقة", 30, 30, 600),
        WashProgram("sports", "ملابس رياضية", 35, 40, 1000),
        WashProgram("cotton", "قطن عميق", 45, 60, 1200),
        WashProgram("eco", "توفير اقتصادي", 50, 40, 1000),
        WashProgram("drum_clean", "تنظيف الحوض", 60, 70, 800)
    )
}
