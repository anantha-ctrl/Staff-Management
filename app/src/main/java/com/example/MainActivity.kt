package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
  private val viewModel: StaffViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      // Local dark theme override toggle state, defaulting to system theme
      var darkThemeState by remember { mutableStateOf(false) }
      val systemInDark = isSystemInDarkTheme()
      
      // Sync local state on first load
      LaunchedEffect(systemInDark) {
        darkThemeState = systemInDark
      }

      MyApplicationTheme(darkTheme = darkThemeState, dynamicColor = false) {
        val employees by viewModel.employees.collectAsStateWithLifecycle()
        val timeRecords by viewModel.timeRecords.collectAsStateWithLifecycle()
        val payrollRecords by viewModel.payrollRecords.collectAsStateWithLifecycle()
        val performanceReviews by viewModel.performanceReviews.collectAsStateWithLifecycle()
        val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
              CenterAlignedTopAppBar(
                title = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Business,
                      contentDescription = "App logo",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Staff Manager",
                      fontWeight = FontWeight.Black,
                      fontSize = 20.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                },
                actions = {
                  // Connectivity Status Toggle (Online/Offline Offline capability trigger)
                  IconButton(
                    onClick = { viewModel.toggleOnlineStatus() },
                    modifier = Modifier.testTag("connectivity_toggle")
                  ) {
                    Icon(
                      imageVector = if (viewModel.isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                      contentDescription = if (viewModel.isOnline) "Online Mode" else "Offline Mode",
                      tint = if (viewModel.isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                  }

                  // Dark Mode Toggle Trigger
                  IconButton(
                    onClick = { darkThemeState = !darkThemeState },
                    modifier = Modifier.testTag("dark_mode_toggle")
                  ) {
                    Icon(
                      imageVector = if (darkThemeState) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                      contentDescription = "Toggle Dark Mode",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface
                )
              )

              // Role Switcher Panel for testing Role-Based Access Control (RBAC)
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Security,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Active Session Role: ${viewModel.currentRole}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }

                  TextButton(
                    onClick = {
                      val nextRole = if (viewModel.currentRole == "HR Admin") "Department Head" else "HR Admin"
                      viewModel.switchRole(nextRole)
                    },
                    modifier = Modifier
                      .height(28.dp)
                      .testTag("role_switcher_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                  ) {
                    Text(
                      text = "Switch Session to ${if (viewModel.currentRole == "HR Admin") "Dept Head" else "HR Admin"}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                  }
                }
              }
            }
          },
          bottomBar = {
            NavigationBar(
              modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              NavigationBarItem(
                selected = viewModel.currentScreen == "dashboard",
                onClick = { viewModel.currentScreen = "dashboard" },
                icon = { Icon(imageVector = Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard", fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_dashboard")
              )
              NavigationBarItem(
                selected = viewModel.currentScreen == "employees",
                onClick = { viewModel.currentScreen = "employees" },
                icon = { Icon(imageVector = Icons.Filled.People, contentDescription = "Staff Directory") },
                label = { Text("Directory", fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_employees")
              )
              
              // Only display Payroll Console to HR Admin role
              if (viewModel.currentRole == "HR Admin") {
                NavigationBarItem(
                  selected = viewModel.currentScreen == "payroll",
                  onClick = { viewModel.currentScreen = "payroll" },
                  icon = { Icon(imageVector = Icons.Filled.ReceiptLong, contentDescription = "Payroll Panel") },
                  label = { Text("Payroll", fontSize = 11.sp) },
                  modifier = Modifier.testTag("nav_payroll")
                )
                NavigationBarItem(
                  selected = viewModel.currentScreen == "logs",
                  onClick = { viewModel.currentScreen = "logs" },
                  icon = { Icon(imageVector = Icons.Filled.Security, contentDescription = "Audit System Logs") },
                  label = { Text("Audit Logs", fontSize = 11.sp) },
                  modifier = Modifier.testTag("nav_logs")
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            // Animated transitions between screens
            AnimatedContent(
              targetState = viewModel.currentScreen,
              transitionSpec = {
                fadeIn() togetherWith fadeOut()
              },
              label = "screen_transitions"
            ) { targetScreen ->
              when (targetScreen) {
                "dashboard" -> DashboardScreen(
                  viewModel = viewModel,
                  employees = employees,
                  timeRecords = timeRecords,
                  payrollRecords = payrollRecords,
                  performanceReviews = performanceReviews
                )
                "employees" -> EmployeesScreen(
                  viewModel = viewModel,
                  employees = employees,
                  timeRecords = timeRecords,
                  payrollRecords = payrollRecords,
                  performanceReviews = performanceReviews
                )
                "payroll" -> {
                  if (viewModel.currentRole == "HR Admin") {
                    PayrollConsoleScreen(
                      viewModel = viewModel,
                      employees = employees,
                      payrollRecords = payrollRecords,
                      timeRecords = timeRecords
                    )
                  } else {
                    // Fallback in case screen state stayed on payroll but role switched
                    LaunchedEffect(Unit) { viewModel.currentScreen = "dashboard" }
                  }
                }
                "logs" -> {
                  if (viewModel.currentRole == "HR Admin") {
                    AuditLogsScreen(auditLogs = auditLogs)
                  } else {
                    LaunchedEffect(Unit) { viewModel.currentScreen = "dashboard" }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
