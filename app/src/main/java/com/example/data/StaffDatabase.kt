package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val role: String,
    val department: String,
    val hourlyRate: Double,
    val taxStatus: String // "Single", "Married", "Head of Household"
)

@Entity(tableName = "time_records")
data class TimeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val date: String, // "YYYY-MM-DD"
    val regularHours: Double,
    val overtimeHours: Double,
    val status: String, // "Pending", "Approved"
    val notes: String
)

@Entity(tableName = "payroll_records")
data class PayrollRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val payPeriodStart: String,
    val payPeriodEnd: String,
    val grossPay: Double,
    val taxFederal: Double,
    val taxState: Double,
    val taxFica: Double,
    val taxMedicare: Double,
    val netPay: Double,
    val processedDate: Long = System.currentTimeMillis(),
    val isReconciled: Boolean = false
)

@Entity(tableName = "performance_reviews")
data class PerformanceReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val reviewerName: String,
    val date: String, // "YYYY-MM-DD"
    val rating: Int, // 1 to 5
    val feedback: String,
    val goalsMet: Boolean
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userRole: String, // "HR Admin", "Department Head"
    val action: String, // "ADD_EMPLOYEE", "PROCESS_PAYROLL", etc.
    val details: String
)

// ==========================================
// DAOs
// ==========================================

@Dao
interface StaffDao {

    // Employees
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Int): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    // Time Records
    @Query("SELECT * FROM time_records ORDER BY date DESC")
    fun getAllTimeRecords(): Flow<List<TimeRecord>>

    @Query("SELECT * FROM time_records WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getTimeRecordsForEmployee(employeeId: Int): Flow<List<TimeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeRecord(record: TimeRecord): Long

    @Update
    suspend fun updateTimeRecord(record: TimeRecord)

    @Query("DELETE FROM time_records WHERE id = :id")
    suspend fun deleteTimeRecordById(id: Int)

    // Payroll Records
    @Query("SELECT * FROM payroll_records ORDER BY processedDate DESC")
    fun getAllPayrollRecords(): Flow<List<PayrollRecord>>

    @Query("SELECT * FROM payroll_records WHERE employeeId = :employeeId ORDER BY processedDate DESC")
    fun getPayrollRecordsForEmployee(employeeId: Int): Flow<List<PayrollRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollRecord(record: PayrollRecord): Long

    @Update
    suspend fun updatePayrollRecord(record: PayrollRecord)

    // Performance Reviews
    @Query("SELECT * FROM performance_reviews ORDER BY date DESC")
    fun getAllPerformanceReviews(): Flow<List<PerformanceReview>>

    @Query("SELECT * FROM performance_reviews WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getPerformanceReviewsForEmployee(employeeId: Int): Flow<List<PerformanceReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceReview(review: PerformanceReview): Long

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}

// ==========================================
// DATABASE HOLDER
// ==========================================

@Database(
    entities = [
        Employee::class,
        TimeRecord::class,
        PayrollRecord::class,
        PerformanceReview::class,
        AuditLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StaffDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao

    companion object {
        @Volatile
        private var INSTANCE: StaffDatabase? = null

        fun getDatabase(context: Context): StaffDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StaffDatabase::class.java,
                    "staff_management_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// REPOSITORY
// ==========================================

class StaffRepository(private val staffDao: StaffDao) {

    val employees: Flow<List<Employee>> = staffDao.getAllEmployees()
    val timeRecords: Flow<List<TimeRecord>> = staffDao.getAllTimeRecords()
    val payrollRecords: Flow<List<PayrollRecord>> = staffDao.getAllPayrollRecords()
    val performanceReviews: Flow<List<PerformanceReview>> = staffDao.getAllPerformanceReviews()
    val auditLogs: Flow<List<AuditLog>> = staffDao.getAllAuditLogs()

    suspend fun getEmployeeById(id: Int): Employee? = staffDao.getEmployeeById(id)

    suspend fun addEmployee(employee: Employee, actorRole: String): Long {
        val id = staffDao.insertEmployee(employee)
        val details = "Added employee ${employee.name} (Role: ${employee.role}, Dept: ${employee.department}, Hourly Rate: $${employee.hourlyRate})"
        logAction(actorRole, "ADD_EMPLOYEE", details)
        return id
    }

    suspend fun updateEmployee(employee: Employee, actorRole: String) {
        staffDao.updateEmployee(employee)
        val details = "Updated employee ${employee.name} (Role: ${employee.role}, Dept: ${employee.department}, Hourly Rate: $${employee.hourlyRate})"
        logAction(actorRole, "UPDATE_EMPLOYEE", details)
    }

    suspend fun deleteEmployee(employee: Employee, actorRole: String) {
        staffDao.deleteEmployee(employee)
        val details = "Deleted employee ${employee.name}"
        logAction(actorRole, "DELETE_EMPLOYEE", details)
    }

    fun getTimeRecordsForEmployee(employeeId: Int): Flow<List<TimeRecord>> =
        staffDao.getTimeRecordsForEmployee(employeeId)

    suspend fun addTimeRecord(record: TimeRecord, employeeName: String, actorRole: String): Long {
        val id = staffDao.insertTimeRecord(record)
        val details = "Logged ${record.regularHours} reg hours and ${record.overtimeHours} ot hours for $employeeName on ${record.date}"
        logAction(actorRole, "LOG_HOURS", details)
        return id
    }

    suspend fun updateTimeRecord(record: TimeRecord, employeeName: String, actorRole: String) {
        staffDao.updateTimeRecord(record)
        val details = "Updated hours log for $employeeName on ${record.date}: Regular=${record.regularHours}, Overtime=${record.overtimeHours}, Status=${record.status}"
        logAction(actorRole, "UPDATE_HOURS", details)
    }

    suspend fun deleteTimeRecord(recordId: Int, employeeName: String, date: String, actorRole: String) {
        staffDao.deleteTimeRecordById(recordId)
        val details = "Deleted hours log for $employeeName on $date"
        logAction(actorRole, "DELETE_HOURS", details)
    }

    fun getPayrollRecordsForEmployee(employeeId: Int): Flow<List<PayrollRecord>> =
        staffDao.getPayrollRecordsForEmployee(employeeId)

    suspend fun addPayrollRecord(record: PayrollRecord, employeeName: String, actorRole: String): Long {
        val id = staffDao.insertPayrollRecord(record)
        val details = "Processed payroll for $employeeName (Gross: $${"%.2f".format(record.grossPay)}, Net: $${"%.2f".format(record.netPay)})"
        logAction(actorRole, "PROCESS_PAYROLL", details)
        return id
    }

    suspend fun reconcilePayroll(record: PayrollRecord, employeeName: String, actorRole: String) {
        val updated = record.copy(isReconciled = true)
        staffDao.updatePayrollRecord(updated)
        val details = "Reconciled payroll ID #${record.id} for $employeeName in accounting integration ledger"
        logAction(actorRole, "RECONCILE_PAYROLL", details)
    }

    fun getPerformanceReviewsForEmployee(employeeId: Int): Flow<List<PerformanceReview>> =
        staffDao.getPerformanceReviewsForEmployee(employeeId)

    suspend fun addPerformanceReview(review: PerformanceReview, employeeName: String, actorRole: String): Long {
        val id = staffDao.insertPerformanceReview(review)
        val details = "Added performance review for $employeeName. Rating: ${review.rating}/5. Goals Met: ${review.goalsMet}"
        logAction(actorRole, "ADD_PERFORMANCE_REVIEW", details)
        return id
    }

    suspend fun logAction(userRole: String, action: String, details: String) {
        staffDao.insertAuditLog(
            AuditLog(
                userRole = userRole,
                action = action,
                details = details
            )
        )
    }
}
