package com.liquidglass.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.player.data.DailyListeningPoint
import com.liquidglass.player.data.ListeningStatsSummary
import com.liquidglass.player.data.PlayerCustomizationConfig
import com.liquidglass.player.ui.components.LiquidGlass2Panel
import com.liquidglass.player.ui.theme.PlayerPalette
import kotlin.math.max

private fun formatMinutesReadable(totalSeconds: Long): String {
    val totalMins = totalSeconds / 60
    val hours = totalMins / 60
    val mins = totalMins % 60
    return when {
        hours > 0 -> "${hours}ч ${mins}м"
        totalMins > 0 -> "${totalMins} мин"
        else -> "${totalSeconds} сек"
    }
}

@Composable
fun StatsScreen(
    stats: ListeningStatsSummary,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    onClearStats: () -> Unit
) {
    var show30Days by remember { mutableStateOf(false) }
    var useAreaCurve by remember { mutableStateOf(true) }

    val points = if (show30Days) stats.last30Days else stats.last7Days

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Статистика",
                    color = palette.textPrimary,
                    fontSize = (24f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "График прослушиваний и любимые треки",
                    color = palette.textSecondary,
                    fontSize = (12f * config.fontScale).sp
                )
            }
            IconButton(onClick = onClearStats) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Сбросить статистику", tint = palette.textSecondary)
            }
        }

        // Top KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Всего прослушано",
                value = formatMinutesReadable(stats.totalSeconds),
                palette = palette,
                config = config,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Сегодня",
                value = formatMinutesReadable(stats.todaySeconds),
                palette = palette,
                config = config,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Запусков треков",
                value = "${stats.totalPlays}",
                palette = palette,
                config = config,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "Серия дней",
                value = "${stats.streakDays} дн.",
                palette = palette,
                config = config,
                modifier = Modifier.weight(1f)
            )
        }

        // Listening Chart Card
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (show30Days) "График за 30 дней (мин)" else "График за 7 дней (мин)",
                        color = palette.textPrimary,
                        fontSize = (15f * config.fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallTogglePill(
                            label = if (show30Days) "30д" else "7д",
                            selected = true,
                            palette = palette,
                            config = config,
                            onClick = { show30Days = !show30Days }
                        )
                        SmallTogglePill(
                            label = if (useAreaCurve) "Волна" else "Столбцы",
                            selected = false,
                            palette = palette,
                            config = config,
                            onClick = { useAreaCurve = !useAreaCurve }
                        )
                    }
                }

                ListeningGlassChart(
                    points = points,
                    useAreaCurve = useAreaCurve,
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )

                // Day labels row for 7-day mode
                if (!show30Days && points.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEach { p ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${p.minutes.toInt()}м",
                                    color = palette.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = p.dayLabel,
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Tracks
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Топ прослушиваемых треков",
                    color = palette.textPrimary,
                    fontSize = (15f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                if (stats.topTracks.isEmpty()) {
                    Text(
                        text = "Послушайте музыку пару минут — здесь появится ваш персональный рейтинг.",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    stats.topTracks.forEachIndexed { idx, t ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Column {
                                    Text(
                                        text = t.title,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = t.artist,
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Text(
                                text = formatMinutesReadable(t.secondsListened),
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Top Artists
        if (stats.topArtists.isNotEmpty()) {
            LiquidGlass2Panel(
                palette = palette,
                config = config,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Любимые исполнители",
                        color = palette.textPrimary,
                        fontSize = (15f * config.fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    stats.topArtists.forEach { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(a.artist, color = palette.textPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(formatMinutesReadable(a.secondsListened), color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    modifier: Modifier = Modifier
) {
    LiquidGlass2Panel(
        palette = palette,
        config = config,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = palette.textSecondary, fontSize = (11f * config.fontScale).sp)
            Text(value, color = palette.textPrimary, fontSize = (20f * config.fontScale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallTogglePill(
    label: String,
    selected: Boolean,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    onClick: () -> Unit
) {
    LiquidGlass2Panel(
        palette = palette,
        config = config,
        shape = RoundedCornerShape(50),
        tintOverride = if (selected) palette.accent else palette.glassTint,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ListeningGlassChart(
    points: List<DailyListeningPoint>,
    useAreaCurve: Boolean,
    palette: PlayerPalette,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 14f
        val padBottom = 10f
        val chartH = (h - padTop - padBottom).coerceAtLeast(10f)
        val maxVal = max(points.maxOfOrNull { it.minutes } ?: 1f, 5f)

        // Subtle horizontal grid lines
        for (i in 0..3) {
            val y = padTop + chartH * (i / 3f)
            drawLine(
                color = palette.textSecondary.copy(alpha = 0.14f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f
            )
        }

        if (useAreaCurve && points.size >= 2) {
            val stepX = w / (points.size - 1)
            val coords = points.mapIndexed { idx, p ->
                val x = idx * stepX
                val y = padTop + chartH * (1f - (p.minutes / maxVal).coerceIn(0f, 1f))
                Offset(x, y)
            }

            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, padTop + chartH)
                lineTo(coords.first().x, padTop + chartH)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.45f),
                        palette.secondaryAccent.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startY = padTop,
                    endY = padTop + chartH
                )
            )

            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(palette.accent, palette.secondaryAccent)),
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            coords.forEach { pt ->
                drawCircle(color = palette.accent, radius = 6f, center = pt)
                drawCircle(color = Color.White, radius = 3f, center = pt)
            }
        } else {
            val count = points.size
            val gap = (w / (count * 3.2f)).coerceAtLeast(3f)
            val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(4f)
            points.forEachIndexed { idx, p ->
                val ratio = (p.minutes / maxVal).coerceIn(0f, 1f)
                val barH = (chartH * ratio).coerceAtLeast(5f)
                val x = idx * (barW + gap)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(palette.accent, palette.secondaryAccent)),
                    topLeft = Offset(x, padTop + chartH - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }
    }
}
