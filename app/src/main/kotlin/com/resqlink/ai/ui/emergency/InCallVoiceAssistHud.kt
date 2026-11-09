package com.resqlink.ai.ui.emergency

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.UserProfile

@Composable
fun InCallVoiceAssistHud(
    targetName: String,
    targetNumber: String,
    liveTranscript: String,
    cycleCount: Int,
    isHandoverComplete: Boolean,
    detectedUserWords: String?,
    lastDispatcherQuestion: String? = null,
    lastAiAnswer: String? = null,
    userProfile: UserProfile,
    location: LocationSnapshot?,
    onAskDispatcherQuestion: ((String) -> Unit)? = null,
    onManualTakeOver: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF060D1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top Status Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Status Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isHandoverComplete) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isHandoverComplete) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isHandoverComplete) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHandoverComplete) {
                                "LIVE LINE ACTIVE • PATIENT CONSCIOUS"
                            } else {
                                "AI VOICE ASSIST • SPEAKERPHONE MAX (CYCLE $cycleCount/3)"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Connected
                Text(
                    text = targetName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Connected Line: $targetNumber • Speakerphone Engaged",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Audio Waveform Visualization
                AudioWaveformDisplay(
                    isSpeaking = !isHandoverComplete,
                    barColor = if (isHandoverComplete) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            // Middle Content: Live Transcript & Dispatcher Q&A Box
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF101C36),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263964)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isHandoverComplete) Icons.Default.RecordVoiceOver else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isHandoverComplete) Color(0xFF10B981) else Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHandoverComplete) "DISPATCHER HANDOVER PROTOCOL" else "AI SPOKEN TRANSCRIPT (TO DISPATCHER)",
                                    color = if (isHandoverComplete) Color(0xFF10B981) else Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Bilingual / Taglish",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = liveTranscript.ifBlank { "Initializing synthesized emergency briefing..." },
                            color = Color(0xFFF8FAFC),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // If user spoke and woke up
                        if (!detectedUserWords.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Patient Heard: \"$detectedUserWords\"",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Dispatcher Q&A Dialogue Stream
                        if (!lastDispatcherQuestion.isNullOrBlank() && !lastAiAnswer.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Dispatcher Asked:",
                                            color = Color(0xFF93C5FD),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "\"$lastDispatcherQuestion\"",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ResQLink Voice AI Answer:",
                                            color = Color(0xFF6EE7B7),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "\"$lastAiAnswer\"",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Dispatcher Test Question Simulation Bar
                        if (!isHandoverComplete && onAskDispatcherQuestion != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "SIMULATE DISPATCHER INQUIRY (TEST SLM):",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuestionChip("Is patient conscious?", onAskDispatcherQuestion)
                                QuestionChip("Crash severity?", onAskDispatcherQuestion)
                                QuestionChip("Exact landmark?", onAskDispatcherQuestion)
                                QuestionChip("Are they bleeding or trapped?", onAskDispatcherQuestion, isGuardrail = true)
                                QuestionChip("Medical allergies?", onAskDispatcherQuestion)
                                QuestionChip("Emergency contact?", onAskDispatcherQuestion)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Patient Information Pill Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF162544),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263964)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PATIENT IDENTITY",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val cleanDisplayName = userProfile.displayName
                                .replace("(?i)google\\s*(user|account)?".toRegex(), "")
                                .trim()
                                .ifBlank { "Patient" }
                            Text(
                                text = cleanDisplayName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BLOOD TYPE",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile.bloodType.ifBlank { "O+" },
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ALLERGIES",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile.allergies.ifBlank { "None documented" },
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom Actions: Take Over Button & End Call Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isHandoverComplete) {
                    Button(
                        onClick = onManualTakeOver,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I AM CONSCIOUS (TAKE OVER CALL)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "END EMERGENCY CALL",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionChip(
    text: String,
    onSelect: (String) -> Unit,
    isGuardrail: Boolean = false
) {
    Surface(
        onClick = { onSelect(text) },
        shape = RoundedCornerShape(8.dp),
        color = if (isGuardrail) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGuardrail) Color(0xFFEF4444) else Color(0xFF3B82F6)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGuardrail) "⚠️ $text" else "❓ $text",
                color = if (isGuardrail) Color(0xFFFCA5A5) else Color(0xFF93C5FD),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AudioWaveformDisplay(
    isSpeaking: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")
    val heights = List(12) { index ->
        val duration = 400 + (index * 80)
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = if (isSpeaking) 28f + (index % 4 * 6f) else 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barHeight$index"
        )
    }

    Row(
        modifier = modifier
            .height(34.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightState ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightState.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
