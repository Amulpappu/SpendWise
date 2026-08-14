package com.example.smartexpensetracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.ui.theme.*

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector? = null,
    subtitle: String? = null,
    gradientBrush: Brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
    ),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryPieChart(
    categoryData: Map<String, Double>,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val total = categoryData.values.sum()
    val colors = listOf(
        PrimaryEmerald, SecondaryTeal, AccentIndigo, AccentPurple,
        AccentPink, AccentOrange, WarningYellow, DangerRed,
        Color(0xFF38BDF8), Color(0xFFA855F7)
    )

    if (total == 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No expense recorded for this month", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                var startAngle = -90f
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                categoryData.entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / total) * 360f).toFloat()
                    val color = colors[index % colors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("$currencySymbol${total.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categoryData.entries.take(5).forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                val pct = ((entry.value / total) * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = entry.key, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(
                        text = "$currencySymbol${entry.value.toInt()} ($pct%)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(
    dailyAmounts: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (dailyAmounts.isEmpty()) return
    val maxVal = (dailyAmounts.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text("Daily Spending Trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (dailyAmounts.size - 1).coerceAtLeast(1)

            val points = dailyAmounts.mapIndexed { index, pair ->
                val x = index * stepX
                val y = height - ((pair.second / maxVal) * (height - 24.dp.toPx())).toFloat()
                Offset(x, y)
            }

            // Fill gradient under curve
            val fillPath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            // Line
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Points
            points.forEach { pt ->
                drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = pt)
            }
        }
    }
}
