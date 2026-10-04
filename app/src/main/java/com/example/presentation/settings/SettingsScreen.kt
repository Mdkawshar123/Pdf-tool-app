package com.example.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PdfMasterApplication
import com.example.R
import com.example.core.storage.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: String,
    onThemeModeChange: (String) -> Unit,
    onShowOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PdfMasterApplication
    val coroutineScope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.BrightnessMedium,
                            title = stringResource(R.string.settings_theme),
                            subtitle = when (currentThemeMode) {
                                "light" -> stringResource(R.string.settings_theme_light)
                                "dark" -> stringResource(R.string.settings_theme_dark)
                                else -> stringResource(R.string.settings_theme_system)
                            },
                            onClick = { showThemeDialog = true },
                            tag = "settings_theme_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.HelpOutline,
                            title = "Tutorial & Guide",
                            subtitle = "View intro and feature tour",
                            onClick = onShowOnboarding,
                            tag = "settings_tutorial_row"
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Storage & Cache",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        val tempSize = remember { FileUtils.getTempFilesSize(context) }
                        SettingsRow(
                            icon = Icons.Default.CleaningServices,
                            title = stringResource(R.string.settings_clear_temp),
                            subtitle = "Freed temp files: ${FileUtils.formatFileSize(tempSize)}",
                            onClick = {
                                val freed = FileUtils.clearTempFiles(context)
                                Toast.makeText(context, "${context.getString(R.string.settings_temp_cleared)}: ${FileUtils.formatFileSize(freed)}", Toast.LENGTH_SHORT).show()
                            },
                            tag = "settings_clear_temp_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = stringResource(R.string.settings_clear_history),
                            subtitle = "Clear all processing history database records",
                            onClick = {
                                coroutineScope.launch {
                                    app.historyRepository.clearAll()
                                    Toast.makeText(context, context.getString(R.string.settings_history_cleared), Toast.LENGTH_SHORT).show()
                                }
                            },
                            tag = "settings_clear_history_row"
                        )
                    }
                }
            }

            item {
                Text(
                    text = "About & Legal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.PrivacyTip,
                            title = stringResource(R.string.settings_privacy),
                            subtitle = "100% offline-first & privacy protected",
                            onClick = { showPrivacyDialog = true },
                            tag = "settings_privacy_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.Gavel,
                            title = stringResource(R.string.settings_terms),
                            subtitle = "Terms of service and usage license",
                            onClick = { showTermsDialog = true },
                            tag = "settings_terms_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.Quiz,
                            title = stringResource(R.string.settings_faq),
                            subtitle = "Frequently asked questions & solutions",
                            onClick = { showFaqDialog = true },
                            tag = "settings_faq_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_about),
                            subtitle = "Version 1.0 • Master PDF Tool",
                            onClick = { showAboutDialog = true },
                            tag = "settings_about_row"
                        )
                    }
                }
            }
        }
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    listOf(
                        "system" to stringResource(R.string.settings_theme_system),
                        "light" to stringResource(R.string.settings_theme_light),
                        "dark" to stringResource(R.string.settings_theme_dark)
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentThemeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.settings_privacy)) },
            text = {
                Text(
                    "Master PDF Tool processes all PDF documents locally on your device storage.\n\n" +
                    "• Your files are never uploaded to any remote server or third-party service without your explicit action.\n" +
                    "• All temporary files created during merging, splitting, or converting are securely saved to the app's cache directory and can be purged at any time.\n" +
                    "• Advertising is delivered through Google AdMob following Google's Privacy and EU User Consent standards."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }

    // Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(stringResource(R.string.settings_terms)) },
            text = {
                Text(
                    "By using Master PDF Tool, you agree to:\n\n" +
                    "1. Use the app for legal document creation, viewing, and conversion.\n" +
                    "2. Protect your own intellectual property and only process files you have permission to modify.\n" +
                    "3. Standard open software terms apply without liability for document corruption caused by faulty source files."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }

    // FAQ Dialog
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text(stringResource(R.string.settings_faq)) },
            text = {
                Text(
                    "Q: Does merging PDFs degrade quality?\n" +
                    "A: No, pages are drawn into the new PDF preserving crisp vector clarity and original resolution.\n\n" +
                    "Q: Where are converted files stored?\n" +
                    "A: All processed files are stored safely in the app's MasterPdfOutputs folder and accessible in the Files tab.\n\n" +
                    "Q: Does the app work offline?\n" +
                    "A: Yes! All core PDF features operate completely offline without internet."
                )
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Master PDF Tool") },
            text = {
                Column {
                    Text(
                        text = "Professional All-in-One PDF Utility & Document Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Engineered with modern Kotlin, Jetpack Compose, Material Design 3, Room local database, and Google Mobile Ads SDK.\n\nVersion 1.0 (Build 2026)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
