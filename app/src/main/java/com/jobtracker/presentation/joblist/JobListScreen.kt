package com.jobtracker.presentation.joblist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JobListScreen(
    onNavigateToAddJob: () -> Unit,
    onNavigateToEditJob: (String) -> Unit,
    onNavigateToAiAnalysis: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: JobListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var jobToDelete by remember { mutableStateOf<JobApplication?>(null) }
    var selectedJob by remember { mutableStateOf<JobApplication?>(null) }

    // Delete confirmation dialog
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title = { Text("Delete Application") },
            text = { Text("Delete your application to ${job.companyName} for ${job.jobTitle}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteJob(job.id); jobToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton({ jobToDelete = null }) { Text("Cancel") } }
        )
    }

    // Action bottom sheet
    selectedJob?.let { job ->
        ModalBottomSheet(onDismissRequest = { selectedJob = null }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "${job.jobTitle} at ${job.companyName}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Edit Application") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.combinedClickable(onClick = { onNavigateToEditJob(job.id); selectedJob = null })
                )
                ListItem(
                    headlineContent = { Text("AI CV Analysis") },
                    leadingContent = { Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.combinedClickable(onClick = { onNavigateToAiAnalysis(job.id); selectedJob = null })
                )
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.combinedClickable(onClick = { jobToDelete = job; selectedJob = null })
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Applications") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = onNavigateToAddJob) { Icon(Icons.Default.Add, "Add job") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddJob, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search jobs, companies...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton({ viewModel.onSearchQueryChange("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Status filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == null,
                        onClick = { viewModel.onFilterChange(null) },
                        label = { Text("All (${uiState.jobs.size})") }
                    )
                }
                items(ApplicationStatus.entries) { status ->
                    val count = uiState.jobs.count { it.status == status }
                    FilterChip(
                        selected = uiState.selectedFilter == status,
                        onClick = { viewModel.onFilterChange(if (uiState.selectedFilter == status) null else status) },
                        label = { Text("${status.displayName} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(status.color).copy(alpha = 0.15f),
                            selectedLabelColor = Color(status.color)
                        )
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredJobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(if (uiState.jobs.isEmpty()) "No applications yet" else "No results found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.jobs.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onNavigateToAddJob) { Text("Add First Application") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredJobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            onClick = { selectedJob = job },
                            onLongClick = { jobToDelete = job }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JobCard(job: JobApplication, onClick: () -> Unit, onLongClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(job.jobTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(job.companyName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                StatusChip(job.status)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (job.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(job.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (job.salary.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AttachMoney, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(job.salary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Applied: ${dateFormat.format(job.appliedDate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (job.cvAnalysisResult != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Psychology, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("${job.cvAnalysisResult.matchScore}% match", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
