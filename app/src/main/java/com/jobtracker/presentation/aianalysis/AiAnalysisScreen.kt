package com.jobtracker.presentation.aianalysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jobtracker.domain.model.CvAnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisScreen(
    jobId: String,
    onNavigateBack: () -> Unit,
    viewModel: AiAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(jobId) { viewModel.loadJob(jobId) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "cv.pdf"
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) viewModel.onFileSelected(fileName, bytes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI CV Analysis") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job info header
            uiState.job?.let { job ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Work, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(job.jobTitle, fontWeight = FontWeight.SemiBold)
                            Text(job.companyName, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Error card
            if (uiState.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null) }
                    }
                }
            }

            // Input mode toggle
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your CV", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !uiState.useManualText,
                            onClick = { viewModel.onToggleInputMode(false) },
                            label = { Text("Upload PDF") },
                            leadingIcon = { Icon(Icons.Default.Upload, null, Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = uiState.useManualText,
                            onClick = { viewModel.onToggleInputMode(true) },
                            label = { Text("Paste Text") },
                            leadingIcon = { Icon(Icons.Default.TextFields, null, Modifier.size(16.dp)) }
                        )
                    }

                    if (!uiState.useManualText) {
                        // File upload
                        OutlinedCard(
                            onClick = { fileLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (uiState.selectedFileName.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                    null, Modifier.size(40.dp),
                                    tint = if (uiState.selectedFileName.isNotBlank()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (uiState.selectedFileName.isNotBlank()) uiState.selectedFileName else "Tap to select PDF",
                                    textAlign = TextAlign.Center,
                                    color = if (uiState.selectedFileName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.selectedFileName.isBlank()) {
                                    Text("PDF files only", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        // Manual text input
                        OutlinedTextField(
                            value = uiState.manualCvText,
                            onValueChange = viewModel::onManualCvTextChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                            placeholder = { Text("Paste your CV / resume text here...") },
                            minLines = 6
                        )
                        if (uiState.hasCachedCv) {
                            TextButton(onClick = viewModel::useCachedCv) {
                                Icon(Icons.Default.History, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Use last CV (saved securely on this device)", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Analyse button
            Button(
                onClick = viewModel::analyse,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isAnalysing,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isAnalysing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Analysing with AI...")
                } else {
                    Icon(Icons.Default.Psychology, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyse My CV", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Results
            AnimatedVisibility(visible = uiState.result != null, enter = fadeIn() + expandVertically()) {
                uiState.result?.let { result ->
                    AnalysisResultCard(result)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AnalysisResultCard(result: CvAnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Analysis Results", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Match score gauge
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Match Score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                val scoreColor = when {
                    result.matchScore >= 75 -> Color(0xFF4CAF50)
                    result.matchScore >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    CircularProgressIndicator(
                        progress = { result.matchScore / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = scoreColor.copy(alpha = 0.1f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${result.matchScore}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                        Text("Match", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))
                val label = when {
                    result.matchScore >= 75 -> "Strong Match ✅"
                    result.matchScore >= 50 -> "Moderate Match ⚡"
                    else -> "Needs Work 🔧"
                }
                Text(label, fontWeight = FontWeight.Medium, color = scoreColor)
            }
        }

        // Summary
        if (result.summary.isNotBlank()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Summarize, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Summary", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(result.summary, lineHeight = 22.sp)
                }
            }
        }

        // Present skills
        if (result.presentSkills.isNotEmpty()) {
            SkillsCard(title = "Skills You Have", skills = result.presentSkills, color = Color(0xFF4CAF50), icon = Icons.Default.CheckCircle)
        }

        // Missing skills
        if (result.missingSkills.isNotEmpty()) {
            SkillsCard(title = "Missing Skills", skills = result.missingSkills, color = Color(0xFFF44336), icon = Icons.Default.Cancel)
        }

        // Suggestions
        if (result.suggestions.isNotEmpty()) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFF9800))
                        Text("Suggestions", fontWeight = FontWeight.SemiBold)
                    }
                    result.suggestions.forEachIndexed { i, suggestion ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) { Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            Text(suggestion, modifier = Modifier.weight(1f), lineHeight = 20.sp, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillsCard(title: String, skills: List<String>, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = color)
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                skills.forEach { skill ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text(skill, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}
