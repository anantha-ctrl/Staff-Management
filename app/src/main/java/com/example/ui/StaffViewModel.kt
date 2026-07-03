package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StaffViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StaffDatabase.getDatabase(application)
    private val repository = StaffRepository(database.staffDao())

    // Current logged-in role: "HR Admin" or "Department Head"
    var currentRole by mutableStateOf("HR Admin")
        private set

    // Is the system online or offline
    var isOnline by mutableStateOf(true)
        private set

    // Selected department filter for directory
    var selectedDepartmentFilter by mutableStateOf("All")
        private set

    // Search query for employee directory
    var searchQuery by mutableStateOf("")
        private set

    // Selected employee for the detail view
    var selectedEmployeeId by mutableStateOf<Int?>(null)
        private set

    // Active screen navigation
    var currentScreen by mutableStateOf("dashboard") // dashboard, employees, payroll, logs

    // Backup and Restore simulation state
    var backupStatus by mutableStateOf("Idle") // Idle, BackingUp, Success, Error
    var backupProgress by mutableStateOf(0f)
    var syncStatus by mutableStateOf("In Sync") // In Sync, Offline Cache, Syncing...

    // UI state flows from Repository
    val employees: StateFlow<List<Employee>> = repository.employees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeRecords: StateFlow<List<TimeRecord>> = repository.timeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payrollRecords: StateFlow<List<PayrollRecord>> = repository.payrollRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val performanceReviews: StateFlow<List<PerformanceReview>> = repository.performanceReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate mock database if it's completely empty
        viewModelScope.launch {
            employees.take(1).collect { list ->
                if (list.isEmpty()) {
                    seedMockData()
                }
            }
        }
    }

    fun switchRole(role: String) {
        currentRole = role
        viewModelScope.launch {
            repository.logAction(
                userRole = role,
                action = "SWITCH_ROLE",
                details = "Switched active user session role to $role"
            )
        }
    }

    fun toggleOnlineStatus() {
        isOnline = !isOnline
        syncStatus = if (isOnline) "Syncing..." else "Offline Mode"
        viewModelScope.launch {
            repository.logAction(
                userRole = currentRole,
                action = "TOGGLE_CONNECTIVITY",
                details = "System toggled to ${if (isOnline) "ONLINE" else "OFFLINE"}"
            )
            if (isOnline) {
                // simulate sync
                kotlinx.coroutines.delay(1000)
                syncStatus = "In Sync"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateDepartmentFilter(dept: String) {
        selectedDepartmentFilter = dept
    }

    fun selectEmployee(id: Int?) {
        selectedEmployeeId = id
    }

    // ==========================================
    // EMPLOYEE OPERATIONS
    // ==========================================
    fun addEmployee(name: String, email: String, role: String, department: String, hourlyRate: Double, taxStatus: String) {
        viewModelScope.launch {
            val employee = Employee(
                name = name,
                email = email,
                role = role,
                department = department,
                hourlyRate = hourlyRate,
                taxStatus = taxStatus
            )
            repository.addEmployee(employee, currentRole)
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee, currentRole)
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            if (selectedEmployeeId == employee.id) {
                selectedEmployeeId = null
            }
            repository.deleteEmployee(employee, currentRole)
        }
    }

    // ==========================================
    // HOURS / TIME TRACKING OPERATIONS
    // ==========================================
    fun logHours(employeeId: Int, date: String, regularHours: Double, overtimeHours: Double, notes: String) {
        viewModelScope.launch {
            val employee = repository.getEmployeeById(employeeId)
            val empName = employee?.name ?: "Unknown Employee"
            val record = TimeRecord(
                employeeId = employeeId,
                date = date,
                regularHours = regularHours,
                overtimeHours = overtimeHours,
                status = "Approved", // Approved immediately by supervisor
                notes = notes
            )
            repository.addTimeRecord(record, empName, currentRole)
        }
    }

    fun addPendingTimeRecord(employeeId: Int, date: String, regularHours: Double, overtimeHours: Double, notes: String) {
        viewModelScope.launch {
            val employee = repository.getEmployeeById(employeeId)
            val empName = employee?.name ?: "Unknown Employee"
            val record = TimeRecord(
                employeeId = employeeId,
                date = date,
                regularHours = regularHours,
                overtimeHours = overtimeHours,
                status = "Pending", // Needs manager approval
                notes = notes
            )
            repository.addTimeRecord(record, empName, currentRole)
        }
    }

    fun approveTimeRecord(record: TimeRecord, employeeName: String) {
        viewModelScope.launch {
            val approved = record.copy(status = "Approved")
            repository.updateTimeRecord(approved, employeeName, currentRole)
        }
    }

    fun deleteTimeRecord(recordId: Int, employeeName: String, date: String) {
        viewModelScope.launch {
            repository.deleteTimeRecord(recordId, employeeName, date, currentRole)
        }
    }

    // ==========================================
    // PAYROLL CALCULATIONS & OPERATIONS
    // ==========================================
    fun calculatePayrollBreakdown(hourlyRate: Double, regularHours: Double, overtimeHours: Double, taxStatus: String): PayrollCalculations {
        val regPay = regularHours * hourlyRate
        val otPay = overtimeHours * hourlyRate * 1.5
        val grossPay = regPay + otPay

        // Federal withholding rates based on tax status
        val federalRate = when (taxStatus) {
            "Single" -> 0.15
            "Married" -> 0.12
            "Head of Household" -> 0.13
            else -> 0.14
        }
        val taxFederal = grossPay * federalRate
        val taxState = grossPay * 0.04 // 4% fixed State tax
        val taxFica = grossPay * 0.062 // 6.2% Social Security
        val taxMedicare = grossPay * 0.0145 // 1.45% Medicare

        val totalDeductions = taxFederal + taxState + taxFica + taxMedicare
        val netPay = maxOf(0.0, grossPay - totalDeductions)

        // Employer Side Contributions
        val employerFica = taxFica // Matching 6.2%
        val employerMedicare = taxMedicare // Matching 1.45%
        val employerFuta = grossPay * 0.006 // 0.6% Federal Unemployment
        val employerSuta = grossPay * 0.027 // 2.7% State Unemployment
        val totalEmployerTaxes = employerFica + employerMedicare + employerFuta + employerSuta
        val totalEmploymentCost = grossPay + totalEmployerTaxes

        return PayrollCalculations(
            grossPay = grossPay,
            taxFederal = taxFederal,
            taxState = taxState,
            taxFica = taxFica,
            taxMedicare = taxMedicare,
            netPay = netPay,
            employerTaxes = totalEmployerTaxes,
            totalCost = totalEmploymentCost
        )
    }

    fun processPayroll(employeeId: Int, payPeriodStart: String, payPeriodEnd: String, calculations: PayrollCalculations) {
        viewModelScope.launch {
            val employee = repository.getEmployeeById(employeeId)
            val empName = employee?.name ?: "Unknown Employee"
            val record = PayrollRecord(
                employeeId = employeeId,
                payPeriodStart = payPeriodStart,
                payPeriodEnd = payPeriodEnd,
                grossPay = calculations.grossPay,
                taxFederal = calculations.taxFederal,
                taxState = calculations.taxState,
                taxFica = calculations.taxFica,
                taxMedicare = calculations.taxMedicare,
                netPay = calculations.netPay,
                isReconciled = false
            )
            repository.addPayrollRecord(record, empName, currentRole)
        }
    }

    fun reconcilePayrollRecord(record: PayrollRecord, employeeName: String) {
        viewModelScope.launch {
            repository.reconcilePayroll(record, employeeName, currentRole)
        }
    }

    // ==========================================
    // PERFORMANCE REVIEWS OPERATIONS
    // ==========================================
    fun addPerformanceReview(employeeId: Int, rating: Int, feedback: String, goalsMet: Boolean) {
        viewModelScope.launch {
            val employee = repository.getEmployeeById(employeeId)
            val empName = employee?.name ?: "Unknown Employee"
            val reviewer = if (currentRole == "HR Admin") "HR Director" else "Department Head"
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val review = PerformanceReview(
                employeeId = employeeId,
                reviewerName = reviewer,
                date = today,
                rating = rating,
                feedback = feedback,
                goalsMet = goalsMet
            )
            repository.addPerformanceReview(review, empName, currentRole)
        }
    }

    // ==========================================
    // BACKUP & RESTORE SIMULATIONS
    // ==========================================
    fun triggerCloudBackup() {
        viewModelScope.launch {
            backupStatus = "BackingUp"
            repository.logAction(currentRole, "BACKUP_START", "Initiated automated encrypted cloud backup sync.")
            
            // simulate progress increments
            for (i in 1..10) {
                kotlinx.coroutines.delay(150)
                backupProgress = i / 10f
            }
            
            backupStatus = "Success"
            repository.logAction(currentRole, "BACKUP_COMPLETE", "Successfully backed up 100% of data with AES-256 secure end-to-end encryption.")
            kotlinx.coroutines.delay(2000)
            backupStatus = "Idle"
            backupProgress = 0f
        }
    }

    fun triggerDisasterRecovery() {
        viewModelScope.launch {
            backupStatus = "Restoring"
            repository.logAction(currentRole, "RECOVERY_START", "Initiating secure disaster recovery & data integrity handshake.")
            
            // simulate restore progress
            for (i in 1..10) {
                kotlinx.coroutines.delay(200)
                backupProgress = i / 10f
            }
            
            backupStatus = "Success"
            repository.logAction(currentRole, "RECOVERY_COMPLETE", "Disaster recovery complete. Verified database schema, transaction logs and synchronized cache.")
            kotlinx.coroutines.delay(2000)
            backupStatus = "Idle"
            backupProgress = 0f
        }
    }

    // ==========================================
    // DATABASE SEEDING
    // ==========================================
    private suspend fun seedMockData() {
        // 1. Log System Bootstrap
        repository.logAction("SYSTEM", "BOOTSTRAP", "Database seed initialized for cold start setup.")

        // 2. Add Mock Employees
        val e1 = Employee(name = "Sarah Jenkins", email = "s.jenkins@enterprise.com", role = "Lead Engineer", department = "Engineering", hourlyRate = 65.0, taxStatus = "Married")
        val e2 = Employee(name = "Marcus Chen", email = "m.chen@enterprise.com", role = "UI/UX Specialist", department = "Design", hourlyRate = 48.0, taxStatus = "Single")
        val e3 = Employee(name = "David Vance", email = "d.vance@enterprise.com", role = "Sales Lead", department = "Sales", hourlyRate = 42.0, taxStatus = "Head of Household")
        val e4 = Employee(name = "Elena Rostova", email = "e.rostova@enterprise.com", role = "HR Specialist", department = "HR", hourlyRate = 35.0, taxStatus = "Single")

        val id1 = repository.addEmployee(e1, "SYSTEM").toInt()
        val id2 = repository.addEmployee(e2, "SYSTEM").toInt()
        val id3 = repository.addEmployee(e3, "SYSTEM").toInt()
        val id4 = repository.addEmployee(e4, "SYSTEM").toInt()

        // 3. Add Mock Hours / Time Records (Some Approved, Some Pending to showcase approval flows)
        // Sarah Jenkins: 40 hours regular, 5 hours overtime (Approved)
        repository.addTimeRecord(TimeRecord(employeeId = id1, date = "2026-06-28", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Core feature architecture"), "Sarah Jenkins", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id1, date = "2026-06-29", regularHours = 8.0, overtimeHours = 2.0, status = "Approved", notes = "Database schema review"), "Sarah Jenkins", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id1, date = "2026-06-30", regularHours = 8.0, overtimeHours = 1.0, status = "Approved", notes = "Refactoring repositories"), "Sarah Jenkins", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id1, date = "2026-07-01", regularHours = 8.0, overtimeHours = 2.0, status = "Approved", notes = "Bugfixing CI/CD pipelines"), "Sarah Jenkins", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id1, date = "2026-07-02", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Team coordination & mentoring"), "Sarah Jenkins", "SYSTEM")

        // Marcus Chen: 38 hours regular, 12 hours overtime (FLAGGED/PENDING for overtime compliance inspection)
        repository.addTimeRecord(TimeRecord(employeeId = id2, date = "2026-06-28", regularHours = 8.0, overtimeHours = 4.0, status = "Pending", notes = "Crucial client dashboard design crunch"), "Marcus Chen", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id2, date = "2026-06-29", regularHours = 8.0, overtimeHours = 3.0, status = "Pending", notes = "Refined typography guidelines"), "Marcus Chen", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id2, date = "2026-06-30", regularHours = 8.0, overtimeHours = 5.0, status = "Pending", notes = "Late-night canvas charts prototyping"), "Marcus Chen", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id2, date = "2026-07-01", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Styleguide documentation"), "Marcus Chen", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id2, date = "2026-07-02", regularHours = 6.0, overtimeHours = 0.0, status = "Approved", notes = "Sprint planning"), "Marcus Chen", "SYSTEM")

        // David Vance: standard 40 hours (Approved)
        for (i in 28..30) {
            repository.addTimeRecord(TimeRecord(employeeId = id3, date = "2026-06-$i", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Client sales calls"), "David Vance", "SYSTEM")
        }
        repository.addTimeRecord(TimeRecord(employeeId = id3, date = "2026-07-01", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Account review meetings"), "David Vance", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id3, date = "2026-07-02", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Sales pipeline planning"), "David Vance", "SYSTEM")

        // Elena Rostova: standard 40 hours (Approved)
        for (i in 28..30) {
            repository.addTimeRecord(TimeRecord(employeeId = id4, date = "2026-06-$i", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Employee onboarding setups"), "Elena Rostova", "SYSTEM")
        }
        repository.addTimeRecord(TimeRecord(employeeId = id4, date = "2026-07-01", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Benefits configuration audits"), "Elena Rostova", "SYSTEM")
        repository.addTimeRecord(TimeRecord(employeeId = id4, date = "2026-07-02", regularHours = 8.0, overtimeHours = 0.0, status = "Approved", notes = "Payroll validation review"), "Elena Rostova", "SYSTEM")

        // 4. Add Mock Payroll Record (Sarah Jenkins - last month payroll)
        val sarahCalculations = calculatePayrollBreakdown(65.0, 160.0, 10.0, "Married")
        val payrollSarah = PayrollRecord(
            employeeId = id1,
            payPeriodStart = "2026-06-01",
            payPeriodEnd = "2026-06-30",
            grossPay = sarahCalculations.grossPay,
            taxFederal = sarahCalculations.taxFederal,
            taxState = sarahCalculations.taxState,
            taxFica = sarahCalculations.taxFica,
            taxMedicare = sarahCalculations.taxMedicare,
            netPay = sarahCalculations.netPay,
            isReconciled = true // Already reconciled in accounting software ledger
        )
        repository.addPayrollRecord(payrollSarah, "Sarah Jenkins", "SYSTEM")

        // David Vance - last month payroll
        val davidCalculations = calculatePayrollBreakdown(42.0, 160.0, 0.0, "Head of Household")
        val payrollDavid = PayrollRecord(
            employeeId = id3,
            payPeriodStart = "2026-06-01",
            payPeriodEnd = "2026-06-30",
            grossPay = davidCalculations.grossPay,
            taxFederal = davidCalculations.taxFederal,
            taxState = davidCalculations.taxState,
            taxFica = davidCalculations.taxFica,
            taxMedicare = davidCalculations.taxMedicare,
            netPay = davidCalculations.netPay,
            isReconciled = false // Pending accounting software ledger reconciliation
        )
        repository.addPayrollRecord(payrollDavid, "David Vance", "SYSTEM")

        // 5. Add Mock Performance Reviews
        repository.addPerformanceReview(
            PerformanceReview(employeeId = id1, reviewerName = "HR Director", date = "2026-06-15", rating = 5, feedback = "Sarah exhibits outstanding leadership. She spearheaded our repository migration and successfully finished ahead of deadlines.", goalsMet = true),
            "Sarah Jenkins", "SYSTEM"
        )
        repository.addPerformanceReview(
            PerformanceReview(employeeId = id2, reviewerName = "HR Director", date = "2026-05-20", rating = 4, feedback = "Marcus has great visual intuition. UI consistency has increased by 40% under his styles.", goalsMet = true),
            "Marcus Chen", "SYSTEM"
        )
        repository.addPerformanceReview(
            PerformanceReview(employeeId = id3, reviewerName = "Sales Manager", date = "2026-04-10", rating = 3, feedback = "David meets his goals, but needs to focus more on team collaboration and reporting schedules.", goalsMet = false),
            "David Vance", "SYSTEM"
        )

        // Log seeding completion
        repository.logAction("SYSTEM", "BOOTSTRAP_COMPLETE", "Successfully populated 4 staff files, 16 timesheets, 2 processed payrolls, and 3 appraisals.")
    }
}

// Data class to return calculations safely
data class PayrollCalculations(
    val grossPay: Double,
    val taxFederal: Double,
    val taxState: Double,
    val taxFica: Double,
    val taxMedicare: Double,
    val netPay: Double,
    val employerTaxes: Double,
    val totalCost: Double
)
