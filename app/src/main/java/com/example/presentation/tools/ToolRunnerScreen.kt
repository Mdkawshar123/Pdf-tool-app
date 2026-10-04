package com.example.presentation.tools

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.PdfMasterApplication
import com.example.R
import com.example.ads.AdMobBanner
import com.example.core.storage.FileUtils
import com.example.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolRunnerScreen(
    toolId: String,
    onNavigateBack: () -> Unit,
    onOpenViewer: (String) -> Unit,
    viewModel: ToolRunnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as PdfMasterApplication
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(toolId) {
        viewModel.setToolId(toolId)
    }

    val tool = remember(toolId) { PdfToolsList.getToolById(toolId) }

    // Natural ad transition on success
    var adShownForCurrentResult by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.result) {
        if (uiState.result?.isSuccess == true && !adShownForCurrentResult && activity != null) {
            adShownForCurrentResult = true
            app.adManager.showInterstitial(activity) {}
        } else if (uiState.result == null) {
            adShownForCurrentResult = false
        }
    }

    // Pickers based on tool
    val isMultiSelect = toolId in listOf("merge", "image_to_pdf")
    val mimeTypes = when (toolId) {
        "image_to_pdf" -> arrayOf("image/*")
        "word_to_pdf" -> arrayOf("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*")
        "ppt_to_pdf" -> arrayOf("application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "*/*")
        "excel_to_pdf" -> arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv", "*/*")
        else -> arrayOf("application/pdf")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addFiles(listOf(it)) }
    }

    fun launchPicker() {
        if (isMultiSelect) {
            filePickerLauncher.launch(mimeTypes)
        } else {
            singlePickerLauncher.launch(mimeTypes)
        }
    }

    BackHandler {
        if (uiState.isProcessing) {
            viewModel.cancelOperation()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tool?.let { stringResource(it.titleRes) } ?: "Tool",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.selectedFiles.isNotEmpty() && uiState.result == null) {
                        IconButton(
                            onClick = { viewModel.clearFiles() },
                            modifier = Modifier.testTag("clear_all_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear selected files")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AdMobBanner()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content based on state
            when {
                uiState.isProcessing -> {
                    ProcessingOverlay(
                        statusText = uiState.progressStatusText,
                        percent = uiState.progressPercent,
                        onCancel = { viewModel.cancelOperation() }
                    )
                }
                uiState.result?.isSuccess == true -> {
                    SuccessResultView(
                        result = uiState.result!!,
                        toolId = toolId,
                        onOpen = {
                            uiState.result?.outputFile?.let { file ->
                                if (toolId == "pdf_to_jpg") {
                                    FileUtils.openFile(context, file, "image/jpeg")
                                } else {
                                    onOpenViewer(file.absolutePath)
                                }
                            }
                        },
                        onShare = {
                            uiState.result?.outputFile?.let { file ->
                                val mime = if (toolId == "pdf_to_jpg") "image/jpeg"
                                else if (toolId == "pdf_to_word") "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                else if (toolId == "pdf_to_excel") "text/csv"
                                else "application/pdf"
                                FileUtils.shareFile(context, file, mime)
                            }
                        },
                        onDone = onNavigateBack
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Tool description hero banner
                            tool?.let { t ->
                                ToolInfoBanner(tool = t)
                            }
                        }

                        // Error Banner if present
                        if (uiState.errorMessage != null) {
                            item {
                                ErrorCard(
                                    message = uiState.errorMessage!!,
                                    onRetry = { viewModel.executeOperation() },
                                    onPickAnother = { launchPicker() }
                                )
                            }
                        }

                        // Selected Files List
                        if (uiState.selectedFiles.isEmpty()) {
                            item {
                                EmptyFilePickerCard(
                                    toolId = toolId,
                                    onPick = { launchPicker() }
                                )
                            }
                        } else {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Selected Files (${uiState.selectedFiles.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isMultiSelect) {
                                        TextButton(
                                            onClick = { launchPicker() },
                                            modifier = Modifier.testTag("add_more_files_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.action_add_more))
                                        }
                                    }
                                }
                            }

                            itemsIndexed(uiState.selectedFiles) { index, fileItem ->
                                SelectedFileItemCard(
                                    item = fileItem,
                                    index = index,
                                    totalCount = uiState.selectedFiles.size,
                                    onRemove = { viewModel.removeFile(index) },
                                    onMoveUp = if (index > 0) { { viewModel.moveFile(index, index - 1) } } else null,
                                    onMoveDown = if (index < uiState.selectedFiles.size - 1) { { viewModel.moveFile(index, index + 1) } } else null
                                )
                            }

                            // Tool Specific Configuration Section
                            item {
                                ToolConfigurationSection(
                                    toolId = toolId,
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }

                            // Process Action Button
                            item {
                                Button(
                                    onClick = { viewModel.executeOperation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("execute_operation_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tool?.color ?: MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = getActionButtonText(toolId),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolInfoBanner(tool: PdfTool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = tool.color.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tool.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(tool.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(tool.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFilePickerCard(
    toolId: String,
    onPick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .testTag("select_files_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (toolId in listOf("merge", "image_to_pdf")) "Select Files to Process" else "Choose Document",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap here to browse from device storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("choose_file_button")
            ) {
                Text(stringResource(R.string.action_select_files))
            }
        }
    }
}

@Composable
private fun SelectedFileItemCard(
    item: PdfFileItem,
    index: Int,
    totalCount: Int,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_item_$index"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FileUtils.formatFileSize(item.sizeBytes)} • ${item.pageCount} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onMoveUp != null || onMoveDown != null) {
                Column {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("remove_file_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove file",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolConfigurationSection(
    toolId: String,
    uiState: ToolRunnerUiState,
    viewModel: ToolRunnerViewModel
) {
    when (toolId) {
        "compress" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Compression Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CompressionLevel.values().forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCompressionLevel(level) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.compressionLevel == level,
                                onClick = { viewModel.setCompressionLevel(level) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = level.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        "split" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Split Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SplitMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSplitMode(mode) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.splitMode == mode,
                                onClick = { viewModel.setSplitMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (uiState.splitMode == SplitMode.CUSTOM_RANGE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.splitCustomRange,
                            onValueChange = { viewModel.setSplitRange(it) },
                            label = { Text("Page Range (e.g. 1-3, 5)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        "watermark" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Watermark Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.watermarkText,
                        onValueChange = { viewModel.setWatermarkText(it) },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Opacity: ${(uiState.watermarkOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.watermarkOpacity,
                        onValueChange = { viewModel.setWatermarkOpacity(it) },
                        valueRange = 0.1f..0.9f
                    )
                }
            }
        }

        "page_numbers" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Page Number Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PageNumberFormat.values().forEach { fmt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPageNumberFormat(fmt) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.pageNumberFormat == fmt,
                                onClick = { viewModel.setPageNumberFormat(fmt) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Format: ${fmt.label}")
                        }
                    }
                }
            }
        }

        "rotate" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rotation Angle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PageRotationAngle.values().forEach { angle ->
                            FilterChip(
                                selected = uiState.rotateAngle == angle,
                                onClick = { viewModel.setRotateAngle(angle) },
                                label = { Text("${angle.degrees}°") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        "image_to_pdf" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Page Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PdfPageSize.values().forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setImagePageSize(size) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.imagePageSize == size,
                                onClick = { viewModel.setImagePageSize(size) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = size.label)
                        }
                    }
                }
            }
        }

        "excel_to_pdf" -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Landscape Orientation", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = uiState.isExcelLandscape,
                        onCheckedChange = { viewModel.setExcelLandscape(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(
    statusText: String,
    percent: Float,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 5.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.status_processing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(percent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun SuccessResultView(
    result: PdfOperationResult,
    toolId: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = getSuccessMessage(toolId),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = result.outputFileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${FileUtils.formatFileSize(result.fileSizeBytes)} • ${result.pageCount} pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("open_result_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_open))
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("share_result_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_done), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Processing Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_retry))
                }
                OutlinedButton(
                    onClick = onPickAnother,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.action_choose_another))
                }
            }
        }
    }
}

private fun getActionButtonText(toolId: String): String = when (toolId) {
    "merge" -> "Merge PDFs"
    "split" -> "Split PDF"
    "compress" -> "Compress PDF"
    "watermark" -> "Apply Watermark"
    "page_numbers" -> "Add Page Numbers"
    "rotate" -> "Rotate PDF"
    "image_to_pdf" -> "Generate PDF"
    "pdf_to_jpg" -> "Export Images"
    else -> "Convert Document"
}

@Composable
private fun getSuccessMessage(toolId: String): String = when (toolId) {
    "merge" -> stringResource(R.string.msg_success_merge)
    "split" -> stringResource(R.string.msg_success_split)
    "compress" -> stringResource(R.string.msg_success_compress)
    "watermark" -> stringResource(R.string.msg_success_watermark)
    "page_numbers" -> stringResource(R.string.msg_success_page_numbers)
    "rotate" -> stringResource(R.string.msg_success_rotate)
    "image_to_pdf" -> stringResource(R.string.msg_success_image_to_pdf)
    "pdf_to_jpg" -> stringResource(R.string.msg_success_pdf_to_jpg)
    else -> stringResource(R.string.msg_success_convert)
}
