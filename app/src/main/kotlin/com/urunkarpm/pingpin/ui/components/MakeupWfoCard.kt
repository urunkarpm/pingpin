package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import com.urunkarpm.pingpin.service.MakeupWfoManager
import com.urunkarpm.pingpin.ui.theme.AmberOrange
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen

@Composable
fun MakeupWfoCard(
    suggestion: MakeupWfoSuggestionEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancelAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val suggestedDateReadable = MakeupWfoManager.formatReadableDate(suggestion.suggestedDateYyyyMmDd)
    val missedDateReadable = MakeupWfoManager.formatReadableDate(suggestion.missedDateYyyyMmDd)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (suggestion.status == "ACCEPTED") EmeraldGreen.copy(alpha = 0.5f)
            else AmberOrange.copy(alpha = 0.5f)
        ),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (suggestion.status == "ACCEPTED") EmeraldGreen.copy(alpha = 0.15f)
                                else AmberOrange.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (suggestion.status == "ACCEPTED") Icons.Default.CheckCircle else Icons.Default.Alarm,
                            contentDescription = "Makeup WFO",
                            tint = if (suggestion.status == "ACCEPTED") EmeraldGreen else AmberOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (suggestion.status == "ACCEPTED") "Makeup WFO Scheduled" else "Missed WFO Suggestion",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (suggestion.status == "ACCEPTED") "7:00 AM Alarm Active" else "Post 2:00 PM Recommendation",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (suggestion.status == "ACCEPTED") EmeraldGreen.copy(alpha = 0.15f) else AmberOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (suggestion.status == "ACCEPTED") "SCHEDULED" else "ACTION SUGGESTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (suggestion.status == "ACCEPTED") EmeraldGreen else AmberOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Body Content Text
            if (suggestion.status == "PENDING") {
                Text(
                    text = buildAnnotatedString {
                        append("Attendance was not recorded by 2:00 PM on ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(missedDateReadable)
                        }
                        append(". Would you like to compensate by going to office on ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                            append(suggestedDateReadable)
                        }
                        append(" (normally a WFH day)?")
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDecline) {
                        Text("Not Now", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yes, Set 7 AM Alarm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else if (suggestion.status == "ACCEPTED") {
                Text(
                    text = buildAnnotatedString {
                        append("Great! An exact 7:00 AM alarm is set for ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EmeraldGreen)) {
                            append(suggestedDateReadable)
                        }
                        append(" to remind you of your planned office day.")
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelAlarm) {
                        Text("Cancel Alarm", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
