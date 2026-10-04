package com.example.presentation.editor

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.storage.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    initialFilePath: String = "",
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: PdfEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val file = FileUtils.copyUriToTempFile(context, it)
            viewModel.loadPdf(file.absolutePath)
        }
    }

    LaunchedEffect(initialFilePath) {
        if (initialFilePath.isNotBlank()) {
            viewModel.loadPdf(initialFilePath)
        }
    }

    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Annotate & Edit PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (uiState.fileName.isNotBlank()) uiState.fileName else "Select a PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { viewModel.clearAllAnnotations() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                    Button(
                        onClick = { viewModel.saveAnnotatedPdf { onSaved(it) } },
                        enabled = uiState.currentBitmap != null && !uiState.isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_annotation_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Color bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(
                            AndroidColor.RED,
                            AndroidColor.BLUE,
                            AndroidColor.BLACK,
                            AndroidColor.rgb(22, 163, 74), // Green
                            AndroidColor.rgb(217, 119, 6)   // Orange
                        )
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .clickable { viewModel.setColor(c) }
                            )
                        }
                    }

                    // Tools toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setTool(EditorToolType.PEN) },
                            colors = if (uiState.activeTool == EditorToolType.PEN) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = "Draw Pen")
                        }
                        IconButton(
                            onClick = { viewModel.setTool(EditorToolType.HIGHLIGHTER) },
                            colors = if (uiState.activeTool == EditorToolType.HIGHLIGHTER) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(Icons.Default.Highlight, contentDescription = "Highlighter")
                        }
                        IconButton(
                            onClick = {
                                viewModel.setTool(EditorToolType.TEXT)
                                showTextInputDialog = true
                            }
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = "Add Text")
                        }
                        IconButton(
                            onClick = {
                                viewModel.setTool(EditorToolType.DATE_STAMP)
                                viewModel.addDateStamp(Offset(100f, 100f))
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date Stamp")
                        }
                        IconButton(
                            onClick = {
                                viewModel.setTool(EditorToolType.SIGNATURE)
                            }
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = "Signature")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.currentBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select a PDF document to annotate", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Select PDF")
                    }
                }
            } else {
                val bmp = uiState.currentBitmap!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        viewModel.addPath(
                                            DrawPath(
                                                points = currentPoints,
                                                color = uiState.selectedColor,
                                                strokeWidth = uiState.strokeWidth,
                                                isHighlighter = uiState.activeTool == EditorToolType.HIGHLIGHTER
                                            )
                                        )
                                        currentPoints = emptyList()
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (uiState.activeTool == EditorToolType.TEXT) {
                                    tapPosition = offset
                                    showTextInputDialog = true
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page Canvas",
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw finalized paths
                        for (path in uiState.paths) {
                            if (path.points.size > 1) {
                                val strokeColor = Color(path.color)
                                for (i in 0 until path.points.size - 1) {
                                    drawLine(
                                        color = strokeColor,
                                        start = path.points[i],
                                        end = path.points[i + 1],
                                        strokeWidth = path.strokeWidth
                                    )
                                }
                            }
                        }

                        // Draw ongoing active stroke
                        if (currentPoints.size > 1) {
                            val activeColor = Color(uiState.selectedColor)
                            for (i in 0 until currentPoints.size - 1) {
                                drawLine(
                                    color = activeColor,
                                    start = currentPoints[i],
                                    end = currentPoints[i + 1],
                                    strokeWidth = uiState.strokeWidth
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Insert Text") },
            text = {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Enter annotation text") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customText.isNotBlank()) {
                            val pos = if (tapPosition != Offset.Zero) tapPosition else Offset(120f, 150f)
                            viewModel.addStamp(customText, pos)
                            customText = ""
                        }
                        showTextInputDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
