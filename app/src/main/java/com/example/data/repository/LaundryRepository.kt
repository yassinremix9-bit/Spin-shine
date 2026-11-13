package com.example.data.repository

import com.example.data.local.AppSettings
import com.example.data.local.MachineDao
import com.example.data.local.NotificationDao
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.MachineStatus
import com.example.data.model.NotificationEntity
import com.example.data.model.NotificationType
import com.example.data.model.WashProgram
import com.example.data.model.WashingMachineEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow

class LaundryRepository(
    private val machineDao: MachineDao,
    private val notificationDao: NotificationDao,
    private val settingsDataStore: SettingsDataStore,
    private val notificationHelper: NotificationHelper
) {
    val machinesFlow: Flow<List<WashingMachineEntity>> = machineDao.observeAllMachines()
    val notificationsFlow: Flow<List<NotificationEntity>> = notificationDao.observeAllNotifications()
    val unreadCountFlow: Flow<Int> = notificationDao.observeUnreadCount()
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun ensureInitialData() {
        if (machineDao.getMachineCount() == 0) {
            seedDefaultData()
        }
    }

    suspend fun resetDefaultData() {
        machineDao.deleteAllMachines()
        notificationDao.clearAll()
        seedDefaultData()
    }

    private suspend fun seedDefaultData() {
        val now = System.currentTimeMillis()
        val initialMachines = listOf(
            WashingMachineEntity(
                name = "غسالة #01 - TurboWash",
                model = "LG ThinQ Pro",
                location = "الطابق الأول - جناح أ",
                capacityKg = 10,
                status = MachineStatus.IN_USE.storageKey,
                programName = "قطن عميق",
                totalDurationSeconds = 45 * 60,
                remainingSeconds = 26 * 60 + 40,
                temperatureCelsius = 60,
                spinRpm = 1200,
                reminderEnabled = true,
                endingSoonNotified = false,
                updatedAtMillis = now
            ),
            WashingMachineEntity(
                name = "غسالة #02 - EcoBubble",
                model = "Samsung Bespoke",
                location = "الطابق الأول - جناح أ",
                capacityKg = 12,
                status = MachineStatus.ENDING_SOON.storageKey,
                programName = "غسيل سريع",
                totalDurationSeconds = 15 * 60,
                remainingSeconds = 3 * 60 + 18,
                temperatureCelsius = 30,
                spinRpm = 800,
                reminderEnabled = true,
                endingSoonNotified = true,
                updatedAtMillis = now
            ),
            WashingMachineEntity(
                name = "غسالة #03 - i-DOS Smart",
                model = "Bosch Serie 8",
                location = "الطابق الأول - جناح ب",
                capacityKg = 9,
                status = MachineStatus.AVAILABLE.storageKey,
                updatedAtMillis = now
            )
        )
        machineDao.insertAll(initialMachines)

        val initialNotifications = listOf(
            NotificationEntity(
                machineName = "غسالة #02 - EcoBubble",
                title = "توشك الدورة على الانتهاء",
                message = "متبقي أقل من 5 دقائق لانتهاء برنامج (غسيل سريع).",
                type = NotificationType.ENDING_SOON.storageKey,
                isRead = false,
                timestampMillis = now - 2 * 60_000L
            ),
            NotificationEntity(
                machineName = "غسالة #03 - i-DOS Smart",
                title = "اكتملت دورة الغسيل",
                message = "أصبحت الغسالة متاحة الآن وجاهزة للاستخدام التالي.",
                type = NotificationType.CYCLE_COMPLETE.storageKey,
                isRead = true,
                timestampMillis = now - 25 * 60_000L
            )
        )
        notificationDao.insertAll(initialNotifications)
    }

    suspend fun tickActiveMachines(settings: AppSettings) {
        val machines = machineDao.getAllMachinesOnce()
        val updated = mutableListOf<WashingMachineEntity>()
        val now = System.currentTimeMillis()

        for (machine in machines) {
            val status = machine.machineStatus
            if (status == MachineStatus.IN_USE || status == MachineStatus.ENDING_SOON) {
                val nextRemaining = (machine.remainingSeconds - 1).coerceAtLeast(0)
                if (nextRemaining == 0) {
                    val completedMachine = machine.copy(
                        status = MachineStatus.AVAILABLE.storageKey,
                        programName = "",
                        totalDurationSeconds = 0,
                        remainingSeconds = 0,
                        reminderEnabled = false,
                        endingSoonNotified = false,
                        updatedAtMillis = now
                    )
                    updated.add(completedMachine)

                    val title = "اكتملت دورة الغسيل"
                    val msg = "انتهت دورة (${machine.programName.ifBlank { "الغسيل" }}) في ${machine.name} وأصبحت متاحة."
                    recordNotification(
                        machineId = machine.id,
                        machineName = machine.name,
                        title = title,
                        message = msg,
                        type = NotificationType.CYCLE_COMPLETE,
                        shouldPush = settings.pushNotificationsEnabled && (settings.cycleCompleteAlert || machine.reminderEnabled),
                        soundAndVibration = settings.soundAndVibration
                    )
                } else {
                    val nextStatus = if (nextRemaining <= 600) {
                        MachineStatus.ENDING_SOON
                    } else {
                        MachineStatus.IN_USE
                    }
                    var notifiedEndingSoon = machine.endingSoonNotified
                    if (nextRemaining <= 300 && !notifiedEndingSoon) {
                        notifiedEndingSoon = true
                        val title = "تنتهي قريباً"
                        val msg = "متبقي 5 دقائق على انتهاء الدورة في ${machine.name}."
                        recordNotification(
                            machineId = machine.id,
                            machineName = machine.name,
                            title = title,
                            message = msg,
                            type = NotificationType.ENDING_SOON,
                            shouldPush = settings.pushNotificationsEnabled && (settings.endingSoonAlert || machine.reminderEnabled),
                            soundAndVibration = settings.soundAndVibration
                        )
                    }
                    updated.add(
                        machine.copy(
                            status = nextStatus.storageKey,
                            remainingSeconds = nextRemaining,
                            endingSoonNotified = notifiedEndingSoon,
                            updatedAtMillis = now
                        )
                    )
                }
            }
        }

        if (updated.isNotEmpty()) {
            machineDao.updateMachines(updated)
        }
    }

    suspend fun addMachine(
        name: String,
        model: String,
        location: String,
        capacityKg: Int
    ) {
        val now = System.currentTimeMillis()
        val entity = WashingMachineEntity(
            name = name.trim(),
            model = model.trim().ifBlank { "Smart Washer" },
            location = location.trim().ifBlank { "الطابق الأول" },
            capacityKg = capacityKg,
            status = MachineStatus.AVAILABLE.storageKey,
            updatedAtMillis = now
        )
        val id = machineDao.insertMachine(entity).toInt()
        notificationDao.insertNotification(
            NotificationEntity(
                machineId = id,
                machineName = entity.name,
                title = "تمت إضافة آلة جديدة",
                message = "تم تسجيل ${entity.name} (${entity.capacityKg} كجم) في ${entity.location}.",
                type = NotificationType.INFO.storageKey,
                isRead = false,
                timestampMillis = now
            )
        )
    }

    suspend fun startCycle(
        machine: WashingMachineEntity,
        program: WashProgram,
        enableReminder: Boolean
    ) {
        val totalSeconds = program.durationMinutes * 60
        val initialStatus = if (totalSeconds <= 600) {
            MachineStatus.ENDING_SOON
        } else {
            MachineStatus.IN_USE
        }
        machineDao.updateMachine(
            machine.copy(
                status = initialStatus.storageKey,
                programName = program.titleAr,
                totalDurationSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                temperatureCelsius = program.temperatureCelsius,
                spinRpm = program.spinRpm,
                reminderEnabled = enableReminder,
                endingSoonNotified = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun togglePauseResume(machine: WashingMachineEntity) {
        val nextStatus = when (machine.machineStatus) {
            MachineStatus.PAUSED -> if (machine.remainingSeconds <= 600) MachineStatus.ENDING_SOON else MachineStatus.IN_USE
            MachineStatus.IN_USE, MachineStatus.ENDING_SOON -> MachineStatus.PAUSED
            else -> machine.machineStatus
        }
        machineDao.updateMachine(
            machine.copy(
                status = nextStatus.storageKey,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun extendCycleFiveMinutes(machine: WashingMachineEntity) {
        val newRemaining = machine.remainingSeconds + 300
        val newTotal = maxOf(machine.totalDurationSeconds, newRemaining)
        val nextStatus = if (machine.machineStatus == MachineStatus.PAUSED) {
            MachineStatus.PAUSED
        } else if (newRemaining <= 600) {
            MachineStatus.ENDING_SOON
        } else {
            MachineStatus.IN_USE
        }
        machineDao.updateMachine(
            machine.copy(
                status = nextStatus.storageKey,
                totalDurationSeconds = newTotal,
                remainingSeconds = newRemaining,
                endingSoonNotified = newRemaining <= 300,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeCycleNow(
        machine: WashingMachineEntity,
        settings: AppSettings
    ) {
        val programTitle = machine.programName.ifBlank { "الغسيل" }
        machineDao.updateMachine(
            machine.copy(
                status = MachineStatus.AVAILABLE.storageKey,
                programName = "",
                totalDurationSeconds = 0,
                remainingSeconds = 0,
                reminderEnabled = false,
                endingSoonNotified = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        recordNotification(
            machineId = machine.id,
            machineName = machine.name,
            title = "تم إنهاء دورة الغسيل",
            message = "انتهت دورة ($programTitle) في ${machine.name} وأصبحت متاحة الآن.",
            type = NotificationType.CYCLE_COMPLETE,
            shouldPush = settings.pushNotificationsEnabled && settings.cycleCompleteAlert,
            soundAndVibration = settings.soundAndVibration
        )
    }

    suspend fun toggleMaintenance(
        machine: WashingMachineEntity,
        settings: AppSettings
    ) {
        val enteringMaintenance = machine.machineStatus != MachineStatus.MAINTENANCE
        machineDao.updateMachine(
            machine.copy(
                status = if (enteringMaintenance) MachineStatus.MAINTENANCE.storageKey else MachineStatus.AVAILABLE.storageKey,
                programName = "",
                totalDurationSeconds = 0,
                remainingSeconds = 0,
                reminderEnabled = false,
                endingSoonNotified = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        val title = if (enteringMaintenance) "تم تفعيل وضع الصيانة" else "اكتملت الصيانة"
        val message = if (enteringMaintenance) {
            "تم تحويل ${machine.name} إلى وضع الصيانة وإيقاف التشغيل."
        } else {
            "عادت ${machine.name} للعمل وأصبحت متاحة للغسيل."
        }
        recordNotification(
            machineId = machine.id,
            machineName = machine.name,
            title = title,
            message = message,
            type = if (enteringMaintenance) NotificationType.MAINTENANCE else NotificationType.INFO,
            shouldPush = settings.pushNotificationsEnabled,
            soundAndVibration = settings.soundAndVibration
        )
    }

    suspend fun toggleReminder(machine: WashingMachineEntity) {
        machineDao.updateMachine(
            machine.copy(
                reminderEnabled = !machine.reminderEnabled,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMachine(machine: WashingMachineEntity) {
        machineDao.deleteMachineById(machine.id)
    }

    suspend fun sendTestNotification(settings: AppSettings) {
        recordNotification(
            machineId = 0,
            machineName = "Spin & Shine",
            title = "تنبيه تجريبي ذكي",
            message = "نظام الإشعارات يعمل بنجاح! ستصلك تنبيهات فورية عند انتهاء دورات الغسيل.",
            type = NotificationType.INFO,
            shouldPush = settings.pushNotificationsEnabled,
            soundAndVibration = settings.soundAndVibration
        )
    }

    private suspend fun recordNotification(
        machineId: Int,
        machineName: String,
        title: String,
        message: String,
        type: NotificationType,
        shouldPush: Boolean,
        soundAndVibration: Boolean
    ) {
        val now = System.currentTimeMillis()
        notificationDao.insertNotification(
            NotificationEntity(
                machineId = machineId,
                machineName = machineName,
                title = title,
                message = message,
                type = type.storageKey,
                isRead = false,
                timestampMillis = now
            )
        )
        if (shouldPush) {
            notificationHelper.postAlert(
                notificationId = (now % Int.MAX_VALUE).toInt(),
                title = "$title - $machineName",
                message = message,
                soundAndVibration = soundAndVibration
            )
        }
    }

    suspend fun markNotificationRead(id: Int) = notificationDao.markAsRead(id)
    suspend fun markAllNotificationsRead() = notificationDao.markAllAsRead()
    suspend fun clearNotifications() = notificationDao.clearAll()

    suspend fun setOnlineMode(enabled: Boolean) = settingsDataStore.setOnlineMode(enabled)
    suspend fun setPushNotificationsEnabled(enabled: Boolean) = settingsDataStore.setPushNotificationsEnabled(enabled)
    suspend fun setCycleCompleteAlert(enabled: Boolean) = settingsDataStore.setCycleCompleteAlert(enabled)
    suspend fun setEndingSoonAlert(enabled: Boolean) = settingsDataStore.setEndingSoonAlert(enabled)
    suspend fun setSoundAndVibration(enabled: Boolean) = settingsDataStore.setSoundAndVibration(enabled)
    suspend fun setThemeMode(mode: AppThemeMode) = settingsDataStore.setThemeMode(mode)
}
