package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppSettings
import com.example.data.local.SettingsDataStore
import com.example.data.local.SpinShineDatabase
import com.example.data.model.AppThemeMode
import com.example.data.model.MachineFilter
import com.example.data.model.MachineStatus
import com.example.data.model.NotificationEntity
import com.example.data.model.WashProgram
import com.example.data.model.WashingMachineEntity
import com.example.data.repository.LaundryRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MachineStats(
    val total: Int = 0,
    val available: Int = 0,
    val inUse: Int = 0,
    val endingSoon: Int = 0,
    val maintenance: Int = 0
)

data class DashboardUiState(
    val allMachines: List<WashingMachineEntity> = emptyList(),
    val filteredMachines: List<WashingMachineEntity> = emptyList(),
    val selectedFilter: MachineFilter = MachineFilter.ALL,
    val stats: MachineStats = MachineStats(),
    val isRefreshing: Boolean = false
)

class SpinShineViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpinShineDatabase.getInstance(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val notificationHelper = NotificationHelper(application)
    private val repository = LaundryRepository(
        machineDao = database.machineDao(),
        notificationDao = database.notificationDao(),
        settingsDataStore = settingsDataStore,
        notificationHelper = notificationHelper
    )

    private val _selectedFilter = MutableStateFlow(MachineFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)
    private val _selectedMachineId = MutableStateFlow<Int?>(null)
    val selectedMachineId: StateFlow<Int?> = _selectedMachineId.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val notifications: StateFlow<List<NotificationEntity>> = repository.notificationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadCount: StateFlow<Int> = repository.unreadCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.machinesFlow,
        _selectedFilter,
        _isRefreshing
    ) { machines, filter, refreshing ->
        val stats = MachineStats(
            total = machines.size,
            available = machines.count { it.machineStatus == MachineStatus.AVAILABLE },
            inUse = machines.count {
                it.machineStatus == MachineStatus.IN_USE ||
                    it.machineStatus == MachineStatus.ENDING_SOON ||
                    it.machineStatus == MachineStatus.PAUSED
            },
            endingSoon = machines.count { it.machineStatus == MachineStatus.ENDING_SOON },
            maintenance = machines.count { it.machineStatus == MachineStatus.MAINTENANCE }
        )

        val filtered = when (filter) {
            MachineFilter.ALL -> machines
            MachineFilter.AVAILABLE -> machines.filter { it.machineStatus == MachineStatus.AVAILABLE }
            MachineFilter.IN_USE -> machines.filter {
                it.machineStatus == MachineStatus.IN_USE ||
                    it.machineStatus == MachineStatus.ENDING_SOON ||
                    it.machineStatus == MachineStatus.PAUSED
            }
            MachineFilter.ENDING_SOON -> machines.filter { it.machineStatus == MachineStatus.ENDING_SOON }
            MachineFilter.MAINTENANCE -> machines.filter { it.machineStatus == MachineStatus.MAINTENANCE }
        }

        DashboardUiState(
            allMachines = machines,
            filteredMachines = filtered,
            selectedFilter = filter,
            stats = stats,
            isRefreshing = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val currentSettings = settings.value
                if (currentSettings.isOnlineMode) {
                    repository.tickActiveMachines(currentSettings)
                }
            }
        }
    }

    fun selectFilter(filter: MachineFilter) {
        _selectedFilter.value = filter
    }

    fun openMachineSheet(machineId: Int) {
        _selectedMachineId.value = machineId
    }

    fun closeMachineSheet() {
        _selectedMachineId.value = null
    }

    fun toggleNetworkState() {
        viewModelScope.launch {
            val next = !settings.value.isOnlineMode
            repository.setOnlineMode(next)
            _snackbarMessages.tryEmit(
                if (next) "تمت استعادة الاتصال المباشر بالآلات" else "تم التحويل إلى وضع الذاكرة المؤقتة"
            )
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(650L)
            _isRefreshing.value = false
            val msg = if (settings.value.isOnlineMode) {
                "تم تحديث مؤشرات جميع الآلات بنجاح"
            } else {
                "غير متصل بالشبكة — يتم عرض آخر بيانات محفوظة"
            }
            _snackbarMessages.tryEmit(msg)
        }
    }

    fun addMachine(name: String, model: String, location: String, capacityKg: Int) {
        if (name.isBlank()) {
            _snackbarMessages.tryEmit("يرجى إدخال اسم الآلة")
            return
        }
        viewModelScope.launch {
            repository.addMachine(name, model, location, capacityKg)
            _snackbarMessages.tryEmit("تمت إضافة الآلة بنجاح")
        }
    }

    fun startCycle(machine: WashingMachineEntity, program: WashProgram, enableReminder: Boolean) {
        viewModelScope.launch {
            repository.startCycle(machine, program, enableReminder)
            _snackbarMessages.tryEmit("بدأت دورة (${program.titleAr}) في ${machine.name}")
        }
    }

    fun togglePauseResume(machine: WashingMachineEntity) {
        viewModelScope.launch {
            repository.togglePauseResume(machine)
            val paused = machine.machineStatus != MachineStatus.PAUSED
            _snackbarMessages.tryEmit(
                if (paused) "تم إيقاف ${machine.name} مؤقتاً" else "تم استئناف التشغيل في ${machine.name}"
            )
        }
    }

    fun extendCycleFiveMinutes(machine: WashingMachineEntity) {
        viewModelScope.launch {
            repository.extendCycleFiveMinutes(machine)
            _snackbarMessages.tryEmit("تمت إضافة 5 دقائق إلى ${machine.name}")
        }
    }

    fun completeCycleNow(machine: WashingMachineEntity) {
        viewModelScope.launch {
            repository.completeCycleNow(machine, settings.value)
            _snackbarMessages.tryEmit("تم إنهاء الدورة وأصبحت ${machine.name} متاحة")
        }
    }

    fun toggleMaintenance(machine: WashingMachineEntity) {
        viewModelScope.launch {
            repository.toggleMaintenance(machine, settings.value)
            val entering = machine.machineStatus != MachineStatus.MAINTENANCE
            _snackbarMessages.tryEmit(
                if (entering) "تم وضع ${machine.name} تحت الصيانة" else "عادت ${machine.name} للخدمة"
            )
        }
    }

    fun toggleReminder(machine: WashingMachineEntity) {
        viewModelScope.launch {
            repository.toggleReminder(machine)
            val enabled = !machine.reminderEnabled
            _snackbarMessages.tryEmit(
                if (enabled) "سيتم تنبيهك فور انتهاء ${machine.name}" else "تم إلغاء التنبيه لـ ${machine.name}"
            )
        }
    }

    fun deleteMachine(machine: WashingMachineEntity) {
        viewModelScope.launch {
            _selectedMachineId.value = null
            repository.deleteMachine(machine)
            _snackbarMessages.tryEmit("تم حذف ${machine.name}")
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            _snackbarMessages.tryEmit("تم تحديد جميع الإشعارات كمقروءة")
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
            _snackbarMessages.tryEmit("تم مسح سجل الإشعارات")
        }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPushNotificationsEnabled(enabled) }
    }

    fun setCycleCompleteAlert(enabled: Boolean) {
        viewModelScope.launch { repository.setCycleCompleteAlert(enabled) }
    }

    fun setEndingSoonAlert(enabled: Boolean) {
        viewModelScope.launch { repository.setEndingSoonAlert(enabled) }
    }

    fun setSoundAndVibration(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundAndVibration(enabled) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            repository.sendTestNotification(settings.value)
            _snackbarMessages.tryEmit("تم إرسال التنبيه التجريبي إلى مركز الإشعارات")
        }
    }

    fun resetDefaultMachines() {
        viewModelScope.launch {
            repository.resetDefaultData()
            _snackbarMessages.tryEmit("تمت استعادة الآلات الافتراضية بنجاح")
        }
    }

    fun buildStatusReportText(): String {
        val machines = dashboardState.value.allMachines
        val stats = dashboardState.value.stats
        val sb = StringBuilder()
        sb.appendLine("تقرير حالة آلات الغسيل - Spin & Shine")
        sb.appendLine("الإجمالي: ${stats.total} | متاحة: ${stats.available} | مشغولة: ${stats.inUse} | صيانة: ${stats.maintenance}")
        sb.appendLine("---")
        machines.forEach { m ->
            val statusLabel = when (m.machineStatus) {
                MachineStatus.AVAILABLE -> "متاحة"
                MachineStatus.IN_USE -> "مشغولة (${m.programName})"
                MachineStatus.ENDING_SOON -> "تنتهي قريباً (${m.programName})"
                MachineStatus.PAUSED -> "متوقفة مؤقتاً"
                MachineStatus.MAINTENANCE -> "صيانة"
            }
            sb.appendLine("• ${m.name} (${m.location}): $statusLabel")
        }
        return sb.toString().trim()
    }
}
