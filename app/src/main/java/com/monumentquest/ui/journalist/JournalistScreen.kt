package com.monumentquest.ui.journalist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalistScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReflectionViewModel = hiltViewModel()
) {
    var selectedMonument by remember { mutableStateOf("") }
    var reflectionText   by remember { mutableStateOf("") }
    var expanded         by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val profilePrefs = remember { context.getSharedPreferences("journalist_profile", 0) }
    var profileEditing by rememberSaveable { mutableStateOf(false) }
    var byline by remember { mutableStateOf(profilePrefs.getString("byline", "Heritage Correspondent") ?: "Heritage Correspondent") }
    var hometown by remember { mutableStateOf(profilePrefs.getString("hometown", "Bhubaneswar, Odisha") ?: "Bhubaneswar, Odisha") }
    var specialty by remember { mutableStateOf(profilePrefs.getString("specialty", "Living heritage") ?: "Living heritage") }
    var bio by remember { mutableStateOf(profilePrefs.getString("bio", "Documenting the stories behind India's living heritage.") ?: "Documenting the stories behind India's living heritage.") }

    val monuments = listOf("Big Ben", "Eiffel Tower", "Colosseum", "Taj Mahal", "Statue of Liberty")
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ForestDeep.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CreamWhite)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GoldBright.copy(alpha = 0.15f))
                        .border(1.dp, GoldBright.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HistoryEdu, null, tint = GoldBright, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Journalist Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = CreamWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reflect · Submit · Earn XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray
                    )
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        JournalistProfileCard(
            byline = byline,
            hometown = hometown,
            specialty = specialty,
            bio = bio,
            isEditing = profileEditing,
            onEditToggle = { profileEditing = !profileEditing },
            onSave = {
                profilePrefs.edit()
                    .putString("byline", byline.trim().ifEmpty { "Heritage Correspondent" })
                    .putString("hometown", hometown.trim().ifEmpty { "Bhubaneswar, Odisha" })
                    .putString("specialty", specialty.trim().ifEmpty { "Living heritage" })
                    .putString("bio", bio.trim().ifEmpty { "Documenting the stories behind India's living heritage." })
                    .apply()
                profileEditing = false
            },
            onBylineChange = { byline = it },
            onHometownChange = { hometown = it },
            onSpecialtyChange = { specialty = it },
            onBioChange = { bio = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "journalist_state"
        ) { currentState ->
            when (currentState) {
                is ReflectionState.Writing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Subtitle card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ForestDeep.copy(alpha = 0.4f))
                                .border(1.dp, GoldBright.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = "Reflect on your journey. Our AI historians will verify your deep dive and award XP based on accuracy and depth.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ParchmentLight,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Monument selector
                        Text(
                            text = "SELECT MONUMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedMonument.ifEmpty { "Choose a monument…" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = GoldBright,
                                    unfocusedBorderColor = SubtleGray,
                                    focusedTextColor     = CreamWhite,
                                    unfocusedTextColor   = if (selectedMonument.isEmpty()) MutedGray else CreamWhite,
                                    focusedContainerColor   = ElevatedSurface,
                                    unfocusedContainerColor = CardSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(ElevatedSurface)
                            ) {
                                monuments.forEach { monument ->
                                    DropdownMenuItem(
                                        text = { Text(monument, color = CreamWhite) },
                                        onClick = { selectedMonument = monument; expanded = false }
                                    )
                                }
                            }
                        }

                        // Reflection field
                        Text(
                            text = "YOUR REFLECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { reflectionText = it },
                            placeholder = {
                                Text(
                                    "Share your historical insights, personal connection, or fascinating facts about this monument…",
                                    color = MutedGray,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GoldBright,
                                unfocusedBorderColor    = SubtleGray,
                                focusedTextColor        = CreamWhite,
                                unfocusedTextColor      = CreamWhite,
                                cursorColor             = GoldBright,
                                focusedContainerColor   = ElevatedSurface,
                                unfocusedContainerColor = CardSurface
                            )
                        )

                        // Character count
                        Text(
                            text = "${reflectionText.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reflectionText.length > 50) SuccessGreen else MutedGray,
                            modifier = Modifier.align(Alignment.End)
                        )

                        // Submit button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedMonument.isNotEmpty() && reflectionText.isNotEmpty())
                                        Brush.horizontalGradient(listOf(ForestMid, GoldDark))
                                    else
                                        Brush.horizontalGradient(listOf(SubtleGray, SubtleGray))
                                )
                        ) {
                            Button(
                                onClick = { viewModel.verifyReflection(selectedMonument, reflectionText) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = selectedMonument.isNotEmpty() && reflectionText.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor   = CreamWhite,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = MutedGray
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(Icons.Default.EditNote, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit for Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                is ReflectionState.Verifying -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(ForestDeep.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = GoldBright,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Text(
                                text = "AI Historians at Work",
                                style = MaterialTheme.typography.titleMedium,
                                color = CreamWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verifying your reflection…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray
                            )
                        }
                    }
                }

                is ReflectionState.Success -> {
                    val score = (currentState as ReflectionState.Success).score
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = 0.15f))
                                    .border(2.dp, SuccessGreen.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(52.dp))
                            }
                            Text("Reflection Verified!", style = MaterialTheme.typography.headlineSmall, color = CreamWhite, fontWeight = FontWeight.Black)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldBright.copy(alpha = 0.15f))
                                    .border(1.dp, GoldBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text("Score: $score / 100", color = GoldBright, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                            Text("Points have been added to your profile.", style = MaterialTheme.typography.bodyMedium, color = MutedGray, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.resetToWriting(); onNavigateBack() },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestMid),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text("Back to Journey", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is ReflectionState.Error -> {
                    val message = (currentState as ReflectionState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(ErrorRed.copy(alpha = 0.15f))
                                    .border(2.dp, ErrorRed.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(52.dp))
                            }
                            Text("Verification Failed", style = MaterialTheme.typography.headlineSmall, color = CreamWhite, fontWeight = FontWeight.Black)
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedGray, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.resetToWriting() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmberMid),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun JournalistProfileCard(
    byline: String,
    hometown: String,
    specialty: String,
    bio: String,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onBylineChange: (String) -> Unit,
    onHometownChange: (String) -> Unit,
    onSpecialtyChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBright.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Journalist profile", color = CreamWhite, fontWeight = FontWeight.Bold)
                    Text("Your public byline for heritage stories", color = MutedGray, fontSize = 12.sp)
                }
                TextButton(onClick = if (isEditing) onSave else onEditToggle) {
                    Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditing) "Save" else "Edit")
                }
            }
            if (isEditing) {
                OutlinedTextField(value = byline, onValueChange = onBylineChange, label = { Text("Byline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hometown, onValueChange = onHometownChange, label = { Text("Based in") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specialty, onValueChange = onSpecialtyChange, label = { Text("Specialty") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = onBioChange, label = { Text("Short bio") }, minLines = 3, maxLines = 4, modifier = Modifier.fillMaxWidth())
            } else {
                Text(byline, color = GoldBright, fontWeight = FontWeight.SemiBold)
                Text("$hometown  ·  $specialty", color = CreamWhite, fontSize = 13.sp)
                Text(bio, color = MutedGray, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
