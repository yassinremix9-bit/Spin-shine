package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.MachineStatus
import com.example.ui.theme.LocalStatusColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WashingDrumGraphic(
    status: MachineStatus,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val statusColors = LocalStatusColors.current
    val isSpinning = status == MachineStatus.IN_USE || status == MachineStatus.ENDING_SOON

    val accentColor = when (status) {
        MachineStatus.AVAILABLE -> statusColors.available
        MachineStatus.IN_USE -> statusColors.inUse
        MachineStatus.ENDING_SOON -> statusColors.endingSoon
        MachineStatus.PAUSED -> statusColors.paused
        MachineStatus.MAINTENANCE -> statusColors.maintenance
    }
    val containerColor = when (status) {
        MachineStatus.AVAILABLE -> statusColors.availableContainer
        MachineStatus.IN_USE -> statusColors.inUseContainer
        MachineStatus.ENDING_SOON -> statusColors.endingSoonContainer
        MachineStatus.PAUSED -> statusColors.pausedContainer
        MachineStatus.MAINTENANCE -> statusColors.maintenanceContainer
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    val infiniteTransition = rememberInfiniteTransition(label = "drum_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drum_rotation"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_wave"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 4.5.dp.toPx()
            val outerRadius = this.size.minDimension / 2f - strokePx / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer background circle
            drawCircle(
                color = containerColor,
                radius = outerRadius,
                center = center
            )

            // Progress ring track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2f, outerRadius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress arc
            val sweep = when {
                isSpinning || status == MachineStatus.PAUSED -> (progressFraction * 360f).coerceAtLeast(18f)
                status == MachineStatus.AVAILABLE -> 360f
                else -> 0f
            }
            if (sweep > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (isSpinning) {
                val innerRadius = outerRadius * 0.72f
                val clipCircle = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center.x - innerRadius,
                            center.y - innerRadius,
                            center.x + innerRadius,
                            center.y + innerRadius
                        )
                    )
                }

                clipPath(clipCircle) {
                    // Animated water wave inside drum
                    val waterPath = Path()
                    val waterLevelY = center.y + innerRadius * 0.12f
                    val startX = center.x - innerRadius
                    val endX = center.x + innerRadius
                    waterPath.moveTo(startX, center.y + innerRadius)
                    waterPath.lineTo(startX, waterLevelY)

                    val steps = 16
                    val dx = (endX - startX) / steps
                    for (i in 0..steps) {
                        val x = startX + i * dx
                        val y = waterLevelY + sin(wavePhase + i * 0.6f) * 3.dp.toPx()
                        waterPath.lineTo(x, y)
                    }
                    waterPath.lineTo(endX, center.y + innerRadius)
                    waterPath.close()

                    drawPath(
                        path = waterPath,
                        color = accentColor.copy(alpha = 0.28f)
                    )

                    // Rotating drum paddles
                    rotate(degrees = rotationAngle, pivot = center) {
                        for (i in 0 until 3) {
                            rotate(degrees = i * 120f, pivot = center) {
                                drawLine(
                                    color = accentColor.copy(alpha = 0.85f),
                                    start = Offset(center.x, center.y - innerRadius * 0.35f),
                                    end = Offset(center.x, center.y - innerRadius * 0.88f),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        // Bubbles
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = 2.5.dp.toPx(),
                            center = Offset(center.x + innerRadius * 0.35f, center.y - innerRadius * 0.2f)
                        )
                        drawCircle(
                            color = accentColor.copy(alpha = 0.65f),
                            radius = 2.dp.toPx(),
                            center = Offset(center.x - innerRadius * 0.3f, center.y + innerRadius * 0.25f)
                        )
                    }
                }

                // Glass drum inner rim
                drawCircle(
                    color = accentColor.copy(alpha = 0.45f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        if (!isSpinning) {
            val icon = when (status) {
                MachineStatus.AVAILABLE -> Icons.Rounded.CheckCircle
                MachineStatus.PAUSED -> Icons.Rounded.Pause
                MachineStatus.MAINTENANCE -> Icons.Rounded.Build
                else -> Icons.Rounded.CheckCircle
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}
