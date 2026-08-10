package com.jobtracker.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.presentation.components.StatusChip
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToJobList: () -> Unit,
    onNavigateToAddJob: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut(); onSignOut() }) { Text("Sign Out") }
            },
            dismissButton = { TextButton({ showSignOutDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToChatbot) {
                        Icon(Icons.Default.SmartToy, "Help assistant")
                    }
                    IconButton({ showSignOutDialog = true }) { Icon(Icons.Default.Logout, "Sign out") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddJob, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add job", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Hello, ${uiState.userName.split(" ").firstOrNull() ?: "there"} 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Here's your job search overview", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Stats row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total", uiState.analytics.totalApplications.toString(), Icons.Default.Work, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatCard("Interviews", uiState.analytics.interviews.toString(), Icons.Default.People, Color(0xFF9C27B0), Modifier.weight(1f))
                    StatCard("Offers", uiState.analytics.offers.toString(), Icons.Default.CheckCircle, Color(0xFF4CAF50), Modifier.weight(1f))
                }
            }

            // Quick actions
            item {
                Text("Quick Actions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("All Jobs", Icons.Default.List, onNavigateToJobList, Modifier.weight(1f))
                    QuickActionCard("Analytics", Icons.Default.BarChart, onNavigateToAnalytics, Modifier.weight(1f))
                    QuickActionCard("Add Job", Icons.Default.AddCircle, onNavigateToAddJob, Modifier.weight(1f))
                    QuickActionCard("Help", Icons.Default.SmartToy, onNavigateToChatbot, Modifier.weight(1f))
                }
            }

            // Success rate
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Success Rate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${uiState.analytics.successRate.toInt()}%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.Default.TrendingUp, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Recent applications
            item { Text("Recent Applications", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }

            if (uiState.recentJobs.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.WorkOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No applications yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onNavigateToAddJob) { Text("Add your first job") }
                            TextButton(onClick = onNavigateToChatbot) { Text("Need help? Ask the assistant →") }
                        }
                    }
                }
            } else {
                items(uiState.recentJobs) { job ->
                    RecentJobCard(job = job, onClick = onNavigateToJobList)
                }
                item {
                    TextButton(onClick = onNavigateToJobList, modifier = Modifier.fillMaxWidth()) {
                        Text("View all applications →")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RecentJobCard(job: JobApplication, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(job.jobTitle, fontWeight = FontWeight.SemiBold)
                Text(job.companyName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(dateFormat.format(job.appliedDate), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            StatusChip(job.status)
        }
    }
}
