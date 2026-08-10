package com.jobtracker.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.jobtracker.domain.model.AnalyticsData
import com.jobtracker.domain.model.ApplicationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
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
            // Key metrics
            Text("Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricSmall("Total", analytics.totalApplications.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricSmall("Interviews", analytics.interviews.toString(), Color(0xFF9C27B0), Modifier.weight(1f))
                MetricSmall("Offers", analytics.offers.toString(), Color(0xFF4CAF50), Modifier.weight(1f))
                MetricSmall("Rejected", analytics.rejections.toString(), Color(0xFFF44336), Modifier.weight(1f))
            }

            // Success rate
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Success Rate", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${analytics.successRate.toInt()}%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("of applications", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("led to interviews or offers", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (analytics.successRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Weekly bar chart
            if (analytics.weeklyData.isNotEmpty()) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Applications per Week", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        WeeklyBarChart(analytics)
                    }
                }
            }

            // Status distribution pie chart
            if (analytics.totalApplications > 0) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Status Distribution", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        StatusPieChart(analytics)
                        Spacer(Modifier.height(12.dp))
                        // Legend
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            analytics.statusDistribution.entries
                                .filter { it.value > 0 }
                                .forEach { (status, count) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(status.color)))
                                        Text(status.displayName, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetricSmall(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun WeeklyBarChart(analytics: AnalyticsData) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawBorders(false)
                setTouchEnabled(true)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = labelColor
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    granularity = 1f
                    axisMinimum = 0f
                    textColor = labelColor
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val labels = analytics.weeklyData.map { it.weekLabel }
            val entries = analytics.weeklyData.mapIndexed { i, d -> BarEntry(i.toFloat(), d.count.toFloat()) }
            val dataSet = BarDataSet(entries, "Applications").apply {
                color = primaryColor
                valueTextColor = labelColor
                valueTextSize = 11f
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = BarData(dataSet).apply { barWidth = 0.5f }
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
fun StatusPieChart(analytics: AnalyticsData) {
    val filteredEntries = analytics.statusDistribution.entries.filter { it.value > 0 }
    if (filteredEntries.isEmpty()) return

    AndroidView(
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                isDrawHoleEnabled = true
                holeRadius = 55f
                setHoleColor(android.graphics.Color.TRANSPARENT)
                setEntryLabelTextSize(12f)
                setEntryLabelColor(android.graphics.Color.WHITE)
                isRotationEnabled = true
                setUsePercentValues(false)
            }
        },
        update = { chart ->
            val entries = filteredEntries.map { (status, count) ->
                PieEntry(count.toFloat(), "")
            }
            val colors = filteredEntries.map { (status, _) -> Color(status.color).toArgb() }
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                setDrawValues(false)
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}
