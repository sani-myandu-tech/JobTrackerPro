package com.jobtracker.presentation.addeditjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jobtracker.domain.model.ApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditJobScreen(
    jobId: String?,
    onNavigateBack: () -> Unit,
    onJobSaved: () -> Unit,
    viewModel: AddEditJobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) { viewModel.loadJob(jobId) }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onJobSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Application" else "New Application") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = viewModel::saveJob, enabled = !uiState.isLoading) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null) }
                    }
                }
            }

            SectionLabel("Job Details")

            OutlinedTextField(
                value = uiState.companyName, onValueChange = viewModel::onCompanyNameChange,
                label = { Text("Company Name *") }, leadingIcon = { Icon(Icons.Default.Business, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = uiState.jobTitle, onValueChange = viewModel::onJobTitleChange,
                label = { Text("Job Title *") }, leadingIcon = { Icon(Icons.Default.Work, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = uiState.location, onValueChange = viewModel::onLocationChange,
                label = { Text("Location") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = uiState.salary, onValueChange = viewModel::onSalaryChange,
                label = { Text("Salary / Range") }, leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = uiState.jobUrl, onValueChange = viewModel::onJobUrlChange,
                label = { Text("Job URL") }, leadingIcon = { Icon(Icons.Default.Link, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // Status dropdown
            ExposedDropdownMenuBox(
                expanded = statusDropdownExpanded,
                onExpandedChange = { statusDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.status.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    leadingIcon = { Icon(Icons.Default.Flag, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = statusDropdownExpanded, onDismissRequest = { statusDropdownExpanded = false }) {
                    ApplicationStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayName) },
                            onClick = { viewModel.onStatusChange(status); statusDropdownExpanded = false }
                        )
                    }
                }
            }

            SectionLabel("Job Description")
            OutlinedTextField(
                value = uiState.jobDescription, onValueChange = viewModel::onJobDescriptionChange,
                label = { Text("Paste job description here") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines = 4
            )

            SectionLabel("Contact Details")
            OutlinedTextField(
                value = uiState.contactName, onValueChange = viewModel::onContactNameChange,
                label = { Text("Recruiter / Contact Name") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = uiState.contactEmail, onValueChange = viewModel::onContactEmailChange,
                label = { Text("Contact Email") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            SectionLabel("Notes")
            OutlinedTextField(
                value = uiState.notes, onValueChange = viewModel::onNotesChange,
                label = { Text("Additional notes, interview prep, etc.") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = viewModel::saveJob,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(if (uiState.isEditing) "Update Application" else "Save Application")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}
