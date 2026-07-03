package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)


// ==========================================
// CUSTOM CHARTS & GRAPHICS USING CANVAS
// ==========================================

@Composable
fun PerformanceBarChart(
    reviews: List<PerformanceReview>,
    employees: List<Employee>,
    modifier: Modifier = Modifier
) {
    val departments = listOf("Engineering", "HR", "Sales", "Design")
    val avgRatings = departments.associateWith { dept ->
        val deptEmpIds = employees.filter { it.department == dept }.map { it.id }
        val deptReviews = reviews.filter { it.employeeId in deptEmpIds }
        if (deptReviews.isEmpty()) 0.0 else deptReviews.map { it.rating.toDouble() }.average()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val chartHeight = height - 40.dp.toPx()
            val barWidth = 40.dp.toPx()
            val spacing = (width - (barWidth * departments.size)) / (departments.size + 1)

            // Draw Y-axis guideline for 5 stars
            drawLine(
                color = outlineVariantColor.copy(alpha = 0.5f),
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            // Midpoint guideline (2.5 stars)
            drawLine(
                color = outlineVariantColor.copy(alpha = 0.3f),
                start = Offset(0f, chartHeight / 2f),
                end = Offset(width, chartHeight / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            departments.forEachIndexed { index, dept ->
                val rating = avgRatings[dept] ?: 0.0
                // Max rating is 5.0
                val ratio = rating / 5.0
                val barHeight = (chartHeight * ratio).toFloat()
                val x = spacing + index * (barWidth + spacing)
                val y = chartHeight - barHeight

                // Select individual bar colors for aesthetic depth
                val brush = Brush.linearGradient(
                    colors = when (dept) {
                        "Engineering" -> listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                        "Design" -> listOf(tertiaryColor, tertiaryColor.copy(alpha = 0.7f))
                        "Sales" -> listOf(secondaryColor, secondaryColor.copy(alpha = 0.7f))
                        else -> listOf(primaryColor.copy(alpha = 0.5f), secondaryColor.copy(alpha = 0.5f))
                    }
                )

                // Draw rounded bar
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                // Draw rating text on top of bar if rating > 0
                if (rating > 0) {
                    // Compose doesn't support easy drawText in generic DrawScope without native Canvas,
                    // so we use standard layouts, or draw simple indicators.
                }
            }
        }

        // Overlay Labels for Department & Ratings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            departments.forEach { dept ->
                val rating = avgRatings[dept] ?: 0.0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (rating > 0) "%.1f ★".format(rating) else "N/A",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dept,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HoursTrendLineChart(
    records: List<TimeRecord>,
    modifier: Modifier = Modifier
) {
    // Generate hours aggregated by day for the last 5 days
    // Let's mock a simple sequence if records are empty, or aggregate records.
    val last5Days = listOf("06-28", "06-29", "06-30", "07-01", "07-02")
    val regularHoursTrend = FloatArray(5) { 0f }
    val overtimeHoursTrend = FloatArray(5) { 0f }

    last5Days.forEachIndexed { idx, day ->
        val dayRecords = records.filter { it.date.endsWith(day) }
        regularHoursTrend[idx] = dayRecords.sumOf { it.regularHours }.toFloat()
        overtimeHoursTrend[idx] = dayRecords.sumOf { it.overtimeHours }.toFloat()
    }

    val regColor = Color(0xFF4CAF50) // Green
    val otColor = Color(0xFFFF9800) // Orange/Overtime
    val neutralColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(regColor, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regular Hrs", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(otColor, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Overtime Hrs", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(modifier = Modifier.fillMaxSize().weight(1f)) {
                val width = size.width
                val height = size.height
                val pointsCount = last5Days.size
                val xSpacing = width / (pointsCount - 1)
                
                // Max hours limit for scaling (e.g., max 40 hours logged across company in a day)
                val maxHoursVal = maxOf(32f, maxOf(regularHoursTrend.maxOrNull() ?: 10f, overtimeHoursTrend.maxOrNull() ?: 5f))

                // Grid lines
                for (i in 0..2) {
                    val y = height * (i / 2f)
                    drawLine(
                        color = neutralColor.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw paths
                val regPath = Path()
                val otPath = Path()

                regularHoursTrend.forEachIndexed { index, value ->
                    val x = index * xSpacing
                    val ratio = value / maxHoursVal
                    val y = height - (height * ratio)

                    if (index == 0) {
                        regPath.moveTo(x, y)
                    } else {
                        regPath.lineTo(x, y)
                    }
                }

                overtimeHoursTrend.forEachIndexed { index, value ->
                    val x = index * xSpacing
                    val ratio = value / maxHoursVal
                    val y = height - (height * ratio)

                    if (index == 0) {
                        otPath.moveTo(x, y)
                    } else {
                        otPath.lineTo(x, y)
                    }
                }

                // Draw Regular line
                drawPath(
                    path = regPath,
                    color = regColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Overtime line
                drawPath(
                    path = otPath,
                    color = otColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots at points
                regularHoursTrend.forEachIndexed { index, value ->
                    val x = index * xSpacing
                    val ratio = value / maxHoursVal
                    val y = height - (height * ratio)
                    drawCircle(color = regColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                overtimeHoursTrend.forEachIndexed { index, value ->
                    val x = index * xSpacing
                    val ratio = value / maxHoursVal
                    val y = height - (height * ratio)
                    drawCircle(color = otColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last5Days.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayrollBudgetAllocationChart(
    payrollRecords: List<PayrollRecord>,
    employees: List<Employee>
) {
    val departments = listOf("Engineering", "HR", "Sales", "Design")
    val deptCost = departments.associateWith { dept ->
        val deptEmpIds = employees.filter { it.department == dept }.map { it.id }
        payrollRecords.filter { it.employeeId in deptEmpIds }.sumOf { it.grossPay }
    }
    val totalCost = deptCost.values.sum()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Payroll Allocation by Department",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (totalCost == 0.0) {
            Text(
                "No processed payroll data available for chart.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Horizontal stacked segmented bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                departments.forEach { dept ->
                    val cost = deptCost[dept] ?: 0.0
                    if (cost > 0) {
                        val weight = (cost / totalCost).toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weight)
                                .background(
                                    color = when (dept) {
                                        "Engineering" -> MaterialTheme.colorScheme.primary
                                        "Design" -> MaterialTheme.colorScheme.tertiary
                                        "Sales" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                departments.forEach { dept ->
                    val cost = deptCost[dept] ?: 0.0
                    val pct = if (totalCost > 0) (cost / totalCost) * 100 else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when (dept) {
                                        "Engineering" -> MaterialTheme.colorScheme.primary
                                        "Design" -> MaterialTheme.colorScheme.tertiary
                                        "Sales" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$dept: ${"%.1f".format(pct)}% ($${"%,.0f".format(cost)})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// UTILITY CHIP / METRIC VIEWS
// ==========================================

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color)
                }
            }
        }
    }
}

@Composable
fun ComplianceWarningAlert(
    records: List<TimeRecord>,
    employees: List<Employee>
) {
    // Audit weekly hours for labor regulation infractions
    // (e.g., if total weekly hours of an employee exceeds 48 hours or single day overtime exceeds 4 hours)
    val groupedRecords = records.groupBy { it.employeeId }
    val infractionStaff = mutableListOf<String>()

    groupedRecords.forEach { (empId, empRecs) ->
        val totalHours = empRecs.sumOf { it.regularHours + it.overtimeHours }
        val emp = employees.find { it.id == empId }
        if (totalHours > 48.0 && emp != null) {
            infractionStaff.add("${emp.name} (Logged ${totalHours.toInt()}h this week - exceeds 48h limit)")
        }
        val highOtDay = empRecs.any { it.overtimeHours > 4.0 }
        if (highOtDay && emp != null && !infractionStaff.any { it.startsWith(emp.name) }) {
            infractionStaff.add("${emp.name} (Single-day overtime exceeds 4 hours)")
        }
    }

    if (infractionStaff.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Compliance warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Labor Regulation Alerts (FCLS-Compliance)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    infractionStaff.forEach { alertText ->
                        Text(
                            text = "• $alertText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD
// ==========================================

@Composable
fun DashboardScreen(
    viewModel: StaffViewModel,
    employees: List<Employee>,
    timeRecords: List<TimeRecord>,
    payrollRecords: List<PayrollRecord>,
    performanceReviews: List<PerformanceReview>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val totalSalary = payrollRecords.sumOf { it.grossPay }
    val avgPerformance = if (performanceReviews.isEmpty()) 0.0 else performanceReviews.map { it.rating }.average()
    val pendingApprovals = timeRecords.filter { it.status == "Pending" }.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Block
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome Back, HR Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Visual dashboard as ${viewModel.currentRole}. Secure database: ${viewModel.syncStatus}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (viewModel.isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800), CircleShape)
                )
            }
        }

        // Labor compliance warning
        ComplianceWarningAlert(records = timeRecords, employees = employees)

        // KPI metrics grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                title = "Total Staff",
                value = employees.size.toString(),
                icon = Icons.Filled.People,
                color = primaryColor,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Gross Payroll",
                value = "$${"%,.0f".format(totalSalary)}",
                icon = Icons.Filled.ReceiptLong,
                color = secondaryColor,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                title = "Avg Rating",
                value = if (avgPerformance > 0) "%.1f ★".format(avgPerformance) else "N/A",
                icon = Icons.Filled.Star,
                color = tertiaryColor,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Pending Timesheets",
                value = pendingApprovals.toString(),
                icon = Icons.Filled.History,
                color = if (pendingApprovals > 0) errorColor else Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        // Analytics Line Graph Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Hours Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aggregated company-wide hours trend over last 5 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                HoursTrendLineChart(records = timeRecords)
            }
        }

        // Departmental rating bar chart card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Departmental Performance Indexes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Average appraisal score by category (1 - 5 Scale)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                PerformanceBarChart(reviews = performanceReviews, employees = employees)
            }
        }

        // Department payroll share
        PayrollBudgetAllocationChart(payrollRecords = payrollRecords, employees = employees)

        // Administrative Backup & Disaster Recovery Block (offline-first security)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = primaryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted Cloud Backups & Continuity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data is automatically synchronized using AES-256 local database encryption. Initiate manual compliance sync or recover from local cache.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.backupStatus != "Idle") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Backup / Recovery Task: ${viewModel.backupStatus}...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { viewModel.backupProgress },
                            modifier = Modifier.fillMaxWidth().clip(CircleShape)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerCloudBackup() },
                            modifier = Modifier.weight(1f).testTag("backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cloud Backup", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.triggerDisasterRecovery() },
                            modifier = Modifier.weight(1f).testTag("recovery_button")
                        ) {
                            Icon(imageVector = Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disaster Recovery", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: EMPLOYEES DIRECTORY
// ==========================================

@Composable
fun EmployeesScreen(
    viewModel: StaffViewModel,
    employees: List<Employee>,
    timeRecords: List<TimeRecord>,
    payrollRecords: List<PayrollRecord>,
    performanceReviews: List<PerformanceReview>
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Screen splits: If an employee is selected, we show detail panel. Otherwise directory list.
    val selectedId = viewModel.selectedEmployeeId

    if (selectedId != null) {
        val employee = employees.find { it.id == selectedId }
        if (employee != null) {
            EmployeeDetailView(
                viewModel = viewModel,
                employee = employee,
                timeRecords = timeRecords.filter { it.employeeId == employee.id },
                payrollRecords = payrollRecords.filter { it.employeeId == employee.id },
                performanceReviews = performanceReviews.filter { it.employeeId == employee.id },
                onBack = { viewModel.selectEmployee(null) }
            )
        } else {
            viewModel.selectEmployee(null)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Employee Directory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Manage accounts, log timesheets & appraise reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Only HR Admin can add employees
                if (viewModel.currentRole == "HR Admin") {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_employee_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Staff")
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().testTag("search_input"),
                placeholder = { Text("Search by name, role or email...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Department Filters Row
            val departments = listOf("All", "Engineering", "Design", "Sales", "HR")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                departments.forEach { dept ->
                    FilterChip(
                        selected = viewModel.selectedDepartmentFilter == dept,
                        onClick = { viewModel.updateDepartmentFilter(dept) },
                        label = { Text(dept) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Filter lists based on search & filter selection
            val filteredEmployees = employees.filter { emp ->
                val matchesSearch = emp.name.contains(viewModel.searchQuery, ignoreCase = true) ||
                        emp.role.contains(viewModel.searchQuery, ignoreCase = true) ||
                        emp.email.contains(viewModel.searchQuery, ignoreCase = true)
                val matchesDept = viewModel.selectedDepartmentFilter == "All" || emp.department == viewModel.selectedDepartmentFilter
                matchesSearch && matchesDept
            }

            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No employee records matched",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEmployees, key = { it.id }) { emp ->
                        EmployeeItemCard(
                            employee = emp,
                            onClick = { viewModel.selectEmployee(emp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, email, role, dept, rate, status ->
                viewModel.addEmployee(name, email, role, dept, rate, status)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EmployeeItemCard(
    employee: Employee,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("employee_card_${employee.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar with initials
            val initials = employee.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            val bgColors = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800))
            val avatarColor = bgColors[employee.name.hashCode().coerceAtLeast(0) % bgColors.size]

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${employee.role} • ${employee.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "$${employee.hourlyRate}/h",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

// ==========================================
// EMPLOYEE DETAILS VIEW & ACTION DIALOGS
// ==========================================

@Composable
fun EmployeeDetailView(
    viewModel: StaffViewModel,
    employee: Employee,
    timeRecords: List<TimeRecord>,
    payrollRecords: List<PayrollRecord>,
    performanceReviews: List<PerformanceReview>,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf("Hours") } // Hours, Payroll, Reviews
    var showLogHoursDialog by remember { mutableStateOf(false) }
    var showAppraisalDialog by remember { mutableStateOf(false) }
    var showProcessPayrollDialog by remember { mutableStateOf(false) }
    var showEditEmployeeDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = employee.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }

        // Employee Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "OFFICIAL PROFILE FILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Email: ${employee.email}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Department: ${employee.department}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Designation: ${employee.role}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Wage: $${employee.hourlyRate}/hr", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Tax Status: ${employee.taxStatus}", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Options for HR Admin
                    if (viewModel.currentRole == "HR Admin") {
                        Row {
                            IconButton(onClick = { showEditEmployeeDialog = true }, modifier = Modifier.testTag("edit_employee")) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Profile", tint = primaryColor)
                            }
                            IconButton(onClick = { viewModel.deleteEmployee(employee) }, modifier = Modifier.testTag("delete_employee")) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove Staff", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Quick Context Actions for adding logs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showLogHoursDialog = true },
                modifier = Modifier.weight(1f).testTag("log_hours_action"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Icon(imageVector = Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Hours", fontSize = 12.sp)
            }

            Button(
                onClick = { showAppraisalDialog = true },
                modifier = Modifier.weight(1f).testTag("appraise_action"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Appraisal", fontSize = 12.sp)
            }

            if (viewModel.currentRole == "HR Admin") {
                Button(
                    onClick = { showProcessPayrollDialog = true },
                    modifier = Modifier.weight(1f).testTag("process_payroll_action"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(imageVector = Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Employee", fontSize = 12.sp)
                }
            }
        }

        // Section Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Hours", "Payroll", "Reviews")
            tabs.forEach { tab ->
                val selected = activeTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = tab },
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab Screen Body
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "Hours" -> TimeRecordsList(viewModel, timeRecords, employee.name)
                "Payroll" -> PayrollHistoryList(viewModel, payrollRecords, employee.name)
                "Reviews" -> PerformanceReviewsList(performanceReviews)
            }
        }
    }

    // Modal dialog overlays
    if (showLogHoursDialog) {
        LogHoursDialog(
            onDismiss = { showLogHoursDialog = false },
            onLog = { date, reg, ot, notes ->
                if (viewModel.currentRole == "HR Admin") {
                    viewModel.logHours(employee.id, date, reg, ot, notes)
                } else {
                    viewModel.addPendingTimeRecord(employee.id, date, reg, ot, notes)
                }
                showLogHoursDialog = false
            }
        )
    }

    if (showAppraisalDialog) {
        AddPerformanceReviewDialog(
            onDismiss = { showAppraisalDialog = false },
            onAdd = { rating, comments, goals ->
                viewModel.addPerformanceReview(employee.id, rating, comments, goals)
                showAppraisalDialog = false
            }
        )
    }

    if (showProcessPayrollDialog) {
        // Calculate pendingapproved hours
        val approvedRecords = timeRecords.filter { it.status == "Approved" }
        val processedPeriodIds = payrollRecords.map { it.id } // simple mockup
        val approvedReg = approvedRecords.sumOf { it.regularHours }
        val approvedOt = approvedRecords.sumOf { it.overtimeHours }

        ProcessPayrollDialog(
            employee = employee,
            regularHours = approvedReg,
            overtimeHours = approvedOt,
            onDismiss = { showProcessPayrollDialog = false },
            onCalculate = { rate, rHrs, oHrs -> viewModel.calculatePayrollBreakdown(rate, rHrs, oHrs, employee.taxStatus) },
            onProcess = { calculations, start, end ->
                viewModel.processPayroll(employee.id, start, end, calculations)
                showProcessPayrollDialog = false
            }
        )
    }

    if (showEditEmployeeDialog) {
        EditEmployeeDialog(
            employee = employee,
            onDismiss = { showEditEmployeeDialog = false },
            onSave = { updated ->
                viewModel.updateEmployee(updated)
                showEditEmployeeDialog = false
            }
        )
    }
}

@Composable
fun TimeRecordsList(
    viewModel: StaffViewModel,
    records: List<TimeRecord>,
    employeeName: String
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No timesheet hours logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = record.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            
                            // Approval Status Tag
                            val isApproved = record.status == "Approved"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isApproved) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isApproved) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isApproved) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = record.status,
                                        fontSize = 11.sp,
                                        color = if (isApproved) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = "Regular: ${record.regularHours}h", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Overtime: ${record.overtimeHours}h", style = MaterialTheme.typography.bodySmall)
                        }

                        if (record.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${record.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // supervisor quick approve actions
                        if (record.status == "Pending" && viewModel.currentRole == "HR Admin") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.deleteTimeRecord(record.id, employeeName, record.date) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(32.dp).padding(end = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Reject", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.approveTimeRecord(record, employeeName) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Approve Hours", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PayrollHistoryList(
    viewModel: StaffViewModel,
    records: List<PayrollRecord>,
    employeeName: String
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No paychecks processed yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Period: ${record.payPeriodStart} to ${record.payPeriodEnd}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Accounting Software Reconciliation Sync Indicator
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (record.isReconciled) Color(0xFFE0F7FA) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = if (record.isReconciled) "Reconciled ✓" else "Not Reconciled ✖",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.isReconciled) Color(0xFF006064) else Color(0xFFB71C1C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tax & Payment Tables
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gross Payment:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${"%.2f".format(record.grossPay)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Tax Withheld:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val totTax = record.taxFederal + record.taxState + record.taxFica + record.taxMedicare
                                Text("$${"%.2f".format(totTax)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Net Handout Pay:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${"%.2f".format(record.netPay)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Withholding: Federal=$${"%.2f".format(record.taxFederal)} | State=$${"%.2f".format(record.taxState)} | FICA=$${"%.2f".format(record.taxFica)} | Med=$${"%.2f".format(record.taxMedicare)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // If not reconciled, offer quick accounting software reconciliation simulation
                        if (!record.isReconciled && viewModel.currentRole == "HR Admin") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { viewModel.reconcilePayrollRecord(record, employeeName) },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reconcile with Accounting", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceReviewsList(
    reviews: List<PerformanceReview>
) {
    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No appraisal reviews file for this staff member.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reviews) { review ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Appraiser: ${review.reviewerName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Reviewed Date: ${review.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            // Rating Stars
                            Row {
                                repeat(5) { starIndex ->
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (starIndex < review.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Feedback: ${review.feedback}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (review.goalsMet) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (review.goalsMet) "✔ Met all set OKRs" else "✖ Some goals pending review",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (review.goalsMet) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: PAYROLL CONSOLE & TAX COMPLIANCE
// ==========================================

@Composable
fun PayrollConsoleScreen(
    viewModel: StaffViewModel,
    employees: List<Employee>,
    payrollRecords: List<PayrollRecord>,
    timeRecords: List<TimeRecord>
) {
    var selectedEmpForPay by remember { mutableStateOf<Employee?>(null) }
    var payPeriodStart by remember { mutableStateOf("2026-07-01") }
    var payPeriodEnd by remember { mutableStateOf("2026-07-31") }

    val unProcessedStaff = employees.filter { emp ->
        // Employees with approved records that are not processed in payroll
        timeRecords.any { it.employeeId == emp.id && it.status == "Approved" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Payroll Console",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Automated withholding tax, FICA, Medicare filing compliance engine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Staff Pending Payroll Processing
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Awaiting Processing List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (unProcessedStaff.isEmpty()) {
                    Text(
                        "All timesheets have been fully processed into cash flow ledgers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    unProcessedStaff.forEach { emp ->
                        val empApprovedRecs = timeRecords.filter { it.employeeId == emp.id && it.status == "Approved" }
                        val regHrs = empApprovedRecs.sumOf { it.regularHours }
                        val otHrs = empApprovedRecs.sumOf { it.overtimeHours }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(emp.name, fontWeight = FontWeight.Bold)
                                Text("Approved: ${regHrs}h Reg + ${otHrs}h OT", style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    selectedEmpForPay = emp
                                },
                                modifier = Modifier.testTag("pay_calc_button_${emp.id}")
                            ) {
                                Text("Process Pay", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Automated tax compliance & filing statistics (Quarterly/Form 941 Estimates)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Tax Filing & W-2 Estimator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val totalGross = payrollRecords.sumOf { it.grossPay }
                val totalFed = payrollRecords.sumOf { it.taxFederal }
                val totalState = payrollRecords.sumOf { it.taxState }
                val totalFica = payrollRecords.sumOf { it.taxFica }
                val totalMedicare = payrollRecords.sumOf { it.taxMedicare }

                // Employer matches
                val employerFica = totalFica
                val employerMedicare = totalMedicare
                val totalTaxLiability = totalFed + totalState + (totalFica * 2) + (totalMedicare * 2)

                Text(
                    text = "Tax compliance aggregates processed through automated systems. Handshakes are formatted according to standard IRS schedules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Detailed burden sheet
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaxBurdenRow("Cumulative Gross Wages", totalGross)
                    TaxBurdenRow("Federal Inc Tax Withheld", totalFed)
                    TaxBurdenRow("State Withholding Tax", totalState)
                    TaxBurdenRow("Social Security (FICA) Combined", totalFica * 2) // Employee + Employer
                    TaxBurdenRow("Medicare (Employee + Match)", totalMedicare * 2)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    TaxBurdenRow("Total IRS Liability Projection", totalTaxLiability, highlight = true)
                }
            }
        }

        // Section 3: Third Party Accounting Integration API & Export Control
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Accounting Reconciliation Sync API",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Synchronize ledgers with external cloud suites (Xero, QuickBooks online). Generates end-to-end encrypted transaction vouchers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val unreconciledCount = payrollRecords.filter { !it.isReconciled }.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ledger Status", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (unreconciledCount > 0) "$unreconciledCount Transactions Unsynced" else "Ledger Fully Reconciled",
                            fontWeight = FontWeight.Bold,
                            color = if (unreconciledCount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }

                    Button(
                        onClick = {
                            // Reconcile all outstanding records
                            payrollRecords.forEach { record ->
                                if (!record.isReconciled) {
                                    val emp = employees.find { it.id == record.employeeId }
                                    viewModel.reconcilePayrollRecord(record, emp?.name ?: "Unknown")
                                }
                            }
                        },
                        enabled = unreconciledCount > 0,
                        modifier = Modifier.testTag("reconcile_all_button")
                    ) {
                        Icon(imageVector = Icons.Filled.CloudDone, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync All Ledger Vouchers")
                    }
                }
            }
        }
    }

    if (selectedEmpForPay != null) {
        val emp = selectedEmpForPay!!
        val empApprovedRecs = timeRecords.filter { it.employeeId == emp.id && it.status == "Approved" }
        val regHrs = empApprovedRecs.sumOf { it.regularHours }
        val otHrs = empApprovedRecs.sumOf { it.overtimeHours }

        ProcessPayrollDialog(
            employee = emp,
            regularHours = regHrs,
            overtimeHours = otHrs,
            onDismiss = { selectedEmpForPay = null },
            onCalculate = { rate, r, o -> viewModel.calculatePayrollBreakdown(rate, r, o, emp.taxStatus) },
            onProcess = { calculations, s, e ->
                viewModel.processPayroll(emp.id, s, e, calculations)
                selectedEmpForPay = null
            }
        )
    }
}

@Composable
fun TaxBurdenRow(label: String, valDouble: Double, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (highlight) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$${"%,.2f".format(valDouble)}",
            fontWeight = FontWeight.Bold,
            style = if (highlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// SCREEN 4: SYSTEM AUDIT LOGS
// ==========================================

@Composable
fun AuditLogsScreen(
    auditLogs: List<AuditLog>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "System Audit Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Immutable compliance and access logs tracking operations securely",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FCLS Standard Audit Vault",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (auditLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No system activity recorded in vault.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (log.userRole) {
                                        "HR Admin" -> MaterialTheme.colorScheme.primaryContainer
                                        "Department Head" -> MaterialTheme.colorScheme.secondaryContainer
                                        "SYSTEM" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = log.userRole,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val logDate = SimpleDateFormat("HH:mm:ss • yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp))
                                Text(
                                    text = logDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Action: ${log.action}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL FORMS & INPUT DIALOGS
// ==========================================

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Engineering") }
    var hourlyRate by remember { mutableStateOf("") }
    var taxStatus by remember { mutableStateOf("Single") }

    val departments = listOf("Engineering", "Design", "Sales", "HR")
    val taxStatuses = listOf("Single", "Married", "Head of Household")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add New Staff Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().testTag("add_name_input"))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().testTag("add_email_input"))
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Designation Role") }, modifier = Modifier.fillMaxWidth().testTag("add_role_input"))

                Text("Department", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    departments.forEach { dept ->
                        FilterChip(
                            selected = department == dept,
                            onClick = { department = dept },
                            label = { Text(dept) }
                        )
                    }
                }

                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = { hourlyRate = it },
                    label = { Text("Hourly Wage Rate ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_rate_input")
                )

                Text("Tax Filing Status", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    taxStatuses.forEach { status ->
                        FilterChip(
                            selected = taxStatus == status,
                            onClick = { taxStatus = status },
                            label = { Text(status) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rate = hourlyRate.toDoubleOrNull() ?: 15.0
                            if (name.isNotEmpty() && email.isNotEmpty()) {
                                onAdd(name, email, role, department, rate, taxStatus)
                            }
                        },
                        modifier = Modifier.testTag("submit_employee_button")
                    ) {
                        Text("Add Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun EditEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var email by remember { mutableStateOf(employee.email) }
    var role by remember { mutableStateOf(employee.role) }
    var department by remember { mutableStateOf(employee.department) }
    var hourlyRate by remember { mutableStateOf(employee.hourlyRate.toString()) }
    var taxStatus by remember { mutableStateOf(employee.taxStatus) }

    val departments = listOf("Engineering", "Design", "Sales", "HR")
    val taxStatuses = listOf("Single", "Married", "Head of Household")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Staff Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())

                Text("Department", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    departments.forEach { dept ->
                        FilterChip(selected = department == dept, onClick = { department = dept }, label = { Text(dept) })
                    }
                }

                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = { hourlyRate = it },
                    label = { Text("Hourly Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tax Filing Status", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    taxStatuses.forEach { status ->
                        FilterChip(selected = taxStatus == status, onClick = { taxStatus = status }, label = { Text(status) })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rate = hourlyRate.toDoubleOrNull() ?: employee.hourlyRate
                            onSave(employee.copy(name = name, email = email, role = role, department = department, hourlyRate = rate, taxStatus = taxStatus))
                        }
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun LogHoursDialog(
    onDismiss: () -> Unit,
    onLog: (String, Double, Double, String) -> Unit
) {
    var date by remember { mutableStateOf("") }
    var regHours by remember { mutableStateOf("8") }
    var otHours by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    // set today as default formatted date
    LaunchedEffect(Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        date = today
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Log Work Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = regHours,
                    onValueChange = { regHours = it },
                    label = { Text("Regular Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("reg_hours_input")
                )
                OutlinedTextField(
                    value = otHours,
                    onValueChange = { otHours = it },
                    label = { Text("Overtime Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("ot_hours_input")
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Tasks Description Notes") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val reg = regHours.toDoubleOrNull() ?: 0.0
                            val ot = otHours.toDoubleOrNull() ?: 0.0
                            onLog(date, reg, ot, notes)
                        },
                        modifier = Modifier.testTag("submit_hours_button")
                    ) {
                        Text("Save Logs")
                    }
                }
            }
        }
    }
}

@Composable
fun AddPerformanceReviewDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, String, Boolean) -> Unit
) {
    var rating by remember { mutableStateOf(4) }
    var comments by remember { mutableStateOf("") }
    var goalsMet by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Write Performance Appraisal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Text("Appraisal Star Rating: $rating / 5", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = rating.toFloat(),
                    onValueChange = { rating = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth().testTag("rating_slider")
                )

                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Feedback & Goals Performance Appraisal Notes") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("feedback_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = goalsMet, onCheckedChange = { goalsMet = it }, modifier = Modifier.testTag("goals_checkbox"))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Employee completed all target goals & OKRs")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (comments.isNotEmpty()) {
                                onAdd(rating, comments, goalsMet)
                            }
                        },
                        modifier = Modifier.testTag("submit_appraisal_button")
                    ) {
                        Text("Add Appraisal")
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessPayrollDialog(
    employee: Employee,
    regularHours: Double,
    overtimeHours: Double,
    onDismiss: () -> Unit,
    onCalculate: (Double, Double, Double) -> PayrollCalculations,
    onProcess: (PayrollCalculations, String, String) -> Unit
) {
    var startPeriod by remember { mutableStateOf("2026-07-01") }
    var endPeriod by remember { mutableStateOf("2026-07-31") }
    var customRegHrs by remember { mutableStateOf(regularHours.toString()) }
    var customOtHrs by remember { mutableStateOf(overtimeHours.toString()) }

    val regVal = customRegHrs.toDoubleOrNull() ?: 0.0
    val otVal = customOtHrs.toDoubleOrNull() ?: 0.0
    val calcs = onCalculate(employee.hourlyRate, regVal, otVal)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Process Payroll Check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Text("Employee: ${employee.name}", fontWeight = FontWeight.Bold)
                Text("Filing Rate: $${employee.hourlyRate}/h (${employee.taxStatus})", style = MaterialTheme.typography.bodySmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startPeriod, onValueChange = { startPeriod = it }, label = { Text("Period Start") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endPeriod, onValueChange = { endPeriod = it }, label = { Text("Period End") }, modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customRegHrs,
                        onValueChange = { customRegHrs = it },
                        label = { Text("Reg Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customOtHrs,
                        onValueChange = { customOtHrs = it },
                        label = { Text("OT Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Automated withholding breakdowns
                Text("Tax Filing Breakdown (Automated)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                TaxBurdenRow("Regular Wages Pay", regVal * employee.hourlyRate)
                TaxBurdenRow("Overtime Wages Pay (1.5x)", otVal * employee.hourlyRate * 1.5)
                TaxBurdenRow("Gross Salary Pay", calcs.grossPay)
                Spacer(modifier = Modifier.height(4.dp))
                TaxBurdenRow("Federal Inc Tax Withheld", calcs.taxFederal)
                TaxBurdenRow("State Withholding Tax", calcs.taxState)
                TaxBurdenRow("FICA Social Security (6.2%)", calcs.taxFica)
                TaxBurdenRow("Medicare Tax (1.45%)", calcs.taxMedicare)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                TaxBurdenRow("Net Employee Take-home", calcs.netPay, highlight = true)

                Spacer(modifier = Modifier.height(4.dp))
                Text("Employer Cost Analysis", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                TaxBurdenRow("Matching FICA & Medicare", calcs.taxFica + calcs.taxMedicare)
                TaxBurdenRow("FUTA (0.6%) & SUTA (2.7%) Burden", calcs.grossPay * 0.033)
                TaxBurdenRow("Total Company Payroll Outflow", calcs.totalCost)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onProcess(calcs, startPeriod, endPeriod)
                        },
                        modifier = Modifier.testTag("submit_payroll_button")
                    ) {
                        Icon(imageVector = Icons.Filled.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Issue Paycheck")
                    }
                }
            }
        }
    }
}
