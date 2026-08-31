package com.lorbit.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorbit.app.ui.components.*
import com.lorbit.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lorbitViewModel: LorbitViewModel = viewModel()
            LorbitTheme {
                LiquidBackground {
                    MainAppShell(lorbitViewModel)
                }
            }
        }
    }
}

enum class LorbitTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    ATTENDANCE("Attendance", Icons.Default.CheckCircle),
    TASKS("Tasks", Icons.AutoMirrored.Filled.Assignment),
    NOTES("Notes & PDFs", Icons.AutoMirrored.Filled.Notes),
    EXPENSES("Expenses", Icons.Default.AccountBalanceWallet)
}

@Composable
fun MainAppShell(viewModel: LorbitViewModel) {
    var currentTab by remember { mutableStateOf(LorbitTab.DASHBOARD) }
    var isAppReady by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                LorbitMorphingHeader(
                    onSplashComplete = { isAppReady = true }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isAppReady,
                enter = slideInVertically(spring(0.75f, Spring.StiffnessMediumLow)) { it } + fadeIn(tween(400))
            ) {
                TrueLiquidLensBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
        ) {
            if (isAppReady) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) },
                    label = "ScreenTransition"
                ) { tab ->
                    when (tab) {
                        LorbitTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAttendance = { currentTab = LorbitTab.ATTENDANCE },
                            onOpenProfile = { showAccountDialog = true }
                        )
                        LorbitTab.ATTENDANCE -> AttendanceScreen(viewModel)
                        LorbitTab.TASKS -> TasksScreen(viewModel)
                        LorbitTab.NOTES -> NotesScreen(viewModel)
                        LorbitTab.EXPENSES -> ExpensesScreen(viewModel)
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        MultiAccountDialog(
            viewModel = viewModel,
            onDismiss = { showAccountDialog = false }
        )
    }
}

// ---------------- MULTI-ACCOUNT PROFILE SWITCHER ----------------
@Composable
fun MultiAccountDialog(viewModel: LorbitViewModel, onDismiss: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    var showNewAccountInput by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newCollege by remember { mutableStateOf("") }
    var newSem by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Student Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                accounts.forEach { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.switchAccount(account.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (account.isActive) Color(0x35FFFFFF) else Color(0x15FFFFFF),
                        border = BorderStroke(1.dp, if (account.isActive) Color.White else Color(0x25FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(account.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("${account.college} • ${account.semester}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            if (account.isActive) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (showNewAccountInput) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LiquidGlassTextField(value = newName, onValueChange = { newName = it }, placeholder = "Your Name")
                    Spacer(modifier = Modifier.height(4.dp))
                    LiquidGlassTextField(value = newCollege, onValueChange = { newCollege = it }, placeholder = "College Name")
                    Spacer(modifier = Modifier.height(4.dp))
                    LiquidGlassTextField(value = newSem, onValueChange = { newSem = it }, placeholder = "Semester (e.g. Sem 3)")
                }
            }
        },
        confirmButton = {
            if (showNewAccountInput) {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.addAccount(newName, newCollege.ifBlank { "My College" }, newSem.ifBlank { "Semester 1" })
                        showNewAccountInput = false
                    }
                }) { Text("Save Profile") }
            } else {
                TextButton(onClick = { showNewAccountInput = true }) { Text("+ Add Profile") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// ---------------- TRUE LIQUID LENS DOCK ----------------
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TrueLiquidLensBottomBar(
    currentTab: LorbitTab,
    onTabSelected: (LorbitTab) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tabCount = LorbitTab.entries.size

    val pillIndex = remember { Animatable(currentTab.ordinal.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentTab) {
        if (!isDragging) {
            pillIndex.animateTo(
                targetValue = currentTab.ordinal.toFloat(),
                animationSpec = spring(0.72f, Spring.StiffnessMediumLow)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color(0x18FFFFFF),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        val tabWidthPx = size.width.toFloat() / tabCount

                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                val target = pillIndex.value.roundToInt().coerceIn(0, tabCount - 1)
                                coroutineScope.launch {
                                    pillIndex.animateTo(
                                        targetValue = target.toFloat(),
                                        animationSpec = spring(0.72f, Spring.StiffnessMediumLow)
                                    )
                                    onTabSelected(LorbitTab.entries[target])
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    pillIndex.animateTo(currentTab.ordinal.toFloat(), spring(0.72f, Spring.StiffnessMediumLow))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.x / tabWidthPx
                                val newPos = (pillIndex.value + delta).coerceIn(0f, (tabCount - 1).toFloat())
                                coroutineScope.launch {
                                    pillIndex.snapTo(newPos)
                                }
                            }
                        )
                    }
            ) {
                val tabWidth = maxWidth / tabCount
                val indicatorOffset = tabWidth * pillIndex.value

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDragging) 0.25f else 0.16f),
                                    Color.White.copy(alpha = if (isDragging) 0.12f else 0.06f)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDragging) 0.45f else 0.28f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    LorbitTab.entries.forEachIndexed { index, tab ->
                        val distanceToLens = abs(pillIndex.value - index)
                        val proximityFactor = (1f - distanceToLens.coerceIn(0f, 1f))
                        val localLensScale = 1.0f + (0.30f * proximityFactor)
                        val isHighlighted = pillIndex.value.roundToInt().coerceIn(0, tabCount - 1) == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .pointerInput(tab) {
                                    detectTapGestures {
                                        coroutineScope.launch {
                                            pillIndex.animateTo(
                                                targetValue = index.toFloat(),
                                                animationSpec = spring(0.72f, Spring.StiffnessMediumLow)
                                            )
                                            onTabSelected(tab)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(localLensScale)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.38f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DASHBOARD TAB ----------------
@Composable
fun DashboardScreen(
    viewModel: LorbitViewModel,
    onNavigateToAttendance: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsState()
    val schedule by viewModel.todaySchedule.collectAsState()
    val assignments by viewModel.assignments.collectAsState()

    val pendingCount = assignments.count { !it.isCompleted }
    val overallAttendance = if (subjects.isNotEmpty()) {
        val totalAttended = subjects.sumOf { it.attendedClasses }
        val totalHeld = subjects.sumOf { it.totalClasses }
        if (totalHeld > 0) (totalAttended.toFloat() / totalHeld.toFloat()) * 100f else 100f
    } else 100f

    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var userApiKey by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    val timetableImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    if (userApiKey.isBlank()) {
                        showApiKeyDialog = true
                    } else {
                        isScanning = true
                        viewModel.scanAndImportTimetable(bitmap, userApiKey) { count ->
                            isScanning = false
                            Toast.makeText(context, "AI imported $count classes & synced attendance!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showHeader by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        showHeader = true
        delay(100)
        showStats = true
        delay(120)
        showSchedule = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = showHeader,
                enter = slideInVertically(spring(0.72f, Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(350))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome Back", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Here's your college overview", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1EFFFFFF),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onOpenProfile)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showStats,
                enter = slideInVertically(spring(0.72f, Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(350))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAttendance
                    ) {
                        Column {
                            Text("Overall Attendance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", overallAttendance),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (overallAttendance >= 75f) "Safe (≥ 75%)" else "Critical (< 75%)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Assignments", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$pendingCount Pending",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text("Upcoming deadlines", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showSchedule,
                enter = slideInVertically(spring(0.72f, Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(350))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's Timetable", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { timetableImagePicker.launch("image/*") },
                            modifier = Modifier
                                .background(Color(0x22FFFFFF), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "AI Scan Screenshot", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { showAddSlotDialog = true },
                            modifier = Modifier
                                .background(Color(0x22FFFFFF), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Class", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (isScanning) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Vision is analyzing your timetable screenshot...", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (schedule.isEmpty() && !isScanning) {
            item {
                AnimatedVisibility(
                    visible = showSchedule,
                    enter = slideInVertically(spring(0.72f, Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(350))
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("No classes scheduled for today.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("💡 Tap scanner to auto-import screenshot, or + to add class manually!", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            items(schedule) { slot ->
                AnimatedVisibility(
                    visible = showSchedule,
                    enter = slideInVertically(spring(0.72f, Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(350))
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(slot.subjectName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("${slot.startTime} - ${slot.endTime}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                                ) {
                                    Text(slot.room, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteTimetableSlot(slot) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (showAddSlotDialog) {
        var subName by remember { mutableStateOf("") }
        var day by remember { mutableStateOf(1) }
        var start by remember { mutableStateOf("09:00 AM") }
        var end by remember { mutableStateOf("10:30 AM") }
        var room by remember { mutableStateOf("Room 101") }

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text("Add Timetable Class", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassTextField(value = subName, onValueChange = { subName = it }, placeholder = "Subject Name")
                    LiquidGlassTextField(value = room, onValueChange = { room = it }, placeholder = "Room / Hall")
                    LiquidGlassTextField(value = start, onValueChange = { start = it }, placeholder = "Start Time (e.g. 09:00 AM)")
                    LiquidGlassTextField(value = end, onValueChange = { end = it }, placeholder = "End Time (e.g. 10:30 AM)")
                    Text("💡 Auto-Sync: Adding this class automatically tracks attendance for this subject!", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (subName.isNotBlank()) {
                        viewModel.addTimetableSlot(subName, day, start, end, room)
                        showAddSlotDialog = false
                    }
                }) { Text("Save & Sync") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini AI Key (Free)", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your free Google Gemini API key from AI Studio to enable AI screenshot parsing with zero server cost:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    LiquidGlassTextField(value = userApiKey, onValueChange = { userApiKey = it }, placeholder = "Paste AI Studio API Key")
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Done") }
            }
        )
    }
}

// ---------------- ATTENDANCE TAB WITH OVERALL STATS & AI SCREENSHOT SCANNER ----------------
@Composable
fun AttendanceScreen(viewModel: LorbitViewModel) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    val totalAttended = subjects.sumOf { it.attendedClasses }
    val totalHeld = subjects.sumOf { it.totalClasses }
    val overallPercentage = if (totalHeld > 0) (totalAttended.toFloat() / totalHeld.toFloat()) * 100f else 100f

    val attendanceImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    isScanning = true
                    viewModel.scanAndImportAttendance(bitmap, "DEFAULT_KEY") { count ->
                        isScanning = false
                        Toast.makeText(context, "AI updated attendance for $count subjects!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Attendance Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Tap Present / Absent or scan screenshot", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { attendanceImagePicker.launch("image/*") },
                    modifier = Modifier
                        .background(Color(0x22FFFFFF), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Screenshot", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .background(Color(0x22FFFFFF), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total College Attendance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", overallPercentage),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$totalAttended attended / $totalHeld total classes",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Text(
                        text = if (overallPercentage >= 75f) "SAFE" else "CRITICAL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isScanning) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI is extracting attendance percentages from your portal screenshot...", color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (subjects.isEmpty() && !isScanning) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No subjects yet. Add classes to Timetable or tap + above to track attendance!", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects) { subject ->
                    val (bunkMsg, isSafe) = viewModel.calculateBunkStatus(subject.attendedClasses, subject.totalClasses, subject.targetAttendance)
                    val currentPct = if (subject.totalClasses > 0) (subject.attendedClasses.toFloat() / subject.totalClasses.toFloat()) * 100f else 0f

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subject.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Text("${subject.code} • ${subject.professor}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSubject(subject) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f%%", currentPct),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x18FFFFFF),
                                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 $bunkMsg",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${subject.attendedClasses} / ${subject.totalClasses} classes",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.markPresent(subject.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x28FFFFFF)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Present", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.markAbsent(subject.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x18FFFFFF)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Absent", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var prof by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subject", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassTextField(value = name, onValueChange = { name = it }, placeholder = "Subject Name")
                    LiquidGlassTextField(value = code, onValueChange = { code = it }, placeholder = "Code (e.g. CS201)")
                    LiquidGlassTextField(value = prof, onValueChange = { prof = it }, placeholder = "Professor")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addSubject(name, code, prof, "Room 101")
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- TASKS & ASSIGNMENTS TAB ----------------
@Composable
fun TasksScreen(viewModel: LorbitViewModel) {
    val assignments by viewModel.assignments.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val customHistory by viewModel.customCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Assignments & Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Track deadlines and submissions", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .background(Color(0x22FFFFFF), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (assignments.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No pending tasks. Tap + above to add an assignment!", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(assignments) { task ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.toggleAssignment(task) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleAssignment(task) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (task.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White,
                                        fontSize = 14.sp,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "${task.subjectName} • Due: ${task.dueDate}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteAssignment(task) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Assignment", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassTextField(value = title, onValueChange = { title = it }, placeholder = "Task Title")
                    LiquidSubjectDropdownSelector(
                        selectedSubject = subject,
                        onSubjectSelected = { subject = it },
                        collegeSubjects = subjects.map { it.name },
                        customHistorySubjects = customHistory,
                        onAddCustomSubject = { viewModel.addCustomCategory(it) }
                    )
                    LiquidGlassTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = "Due Date (e.g. Friday)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addAssignment(title, subject.ifBlank { "General" }, dueDate.ifBlank { "Upcoming" }, "Medium")
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- NOTES, PDF LOCKER & 1-TAP SHARING SHEET ----------------
@Composable
fun NotesScreen(viewModel: LorbitViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val customHistory by viewModel.customCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    fun shareNoteToClassmates(note: NoteEntity) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (note.pdfUri != null) "application/pdf" else "text/plain"
            if (note.pdfUri != null) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(note.pdfUri))
                putExtra(Intent.EXTRA_SUBJECT, "Lorbit Notes: ${note.title}")
                putExtra(Intent.EXTRA_TEXT, "Notes from Lorbit: ${note.title} (${note.subjectName})\n\n${note.content}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            } else {
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, "📚 ${note.title} (${note.subjectName})\n\n${note.content}\n\n— Shared via Lorbit")
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share note with classmates via"))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Notes & PDF Sharing", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Upload PDFs or share notes with friends", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .background(Color(0x22FFFFFF), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note/PDF", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notes.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No notes or PDFs yet. Tap + above to write a note or upload a lecture PDF!", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes) { note ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (!note.pdfUri.isNullOrBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(note.pdfUri), "application/pdf")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            }
                        }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Row {
                                    IconButton(
                                        onClick = { shareNoteToClassmates(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share with classmates", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Text(note.subjectName, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(note.content, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            }

                            if (!note.pdfFileName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x28FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x38FFFFFF))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${note.pdfFileName} (Tap to view)",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
        var selectedPdfName by remember { mutableStateOf<String?>(null) }

        val pdfPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedPdfUri = uri
            uri?.let {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            selectedPdfName = c.getString(nameIndex)
                        }
                    }
                }
                if (selectedPdfName == null) selectedPdfName = "Attached_Document.pdf"
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Note / Upload PDF", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
                    LiquidSubjectDropdownSelector(
                        selectedSubject = subject,
                        onSubjectSelected = { subject = it },
                        collegeSubjects = subjects.map { it.name },
                        customHistorySubjects = customHistory,
                        onAddCustomSubject = { viewModel.addCustomCategory(it) }
                    )
                    LiquidGlassTextField(value = content, onValueChange = { content = it }, placeholder = "Note content (Optional)", minLines = 2)

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { pdfPicker.launch("application/pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x28FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedPdfName ?: "Attach PDF of Notes", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() || selectedPdfName != null) {
                        viewModel.addNote(
                            title = title.ifBlank { selectedPdfName ?: "Untitled Note" },
                            content = content,
                            subjectName = subject.ifBlank { "General" },
                            pdfUri = selectedPdfUri?.toString(),
                            pdfFileName = selectedPdfName
                        )
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- EXPENSES TAB ----------------
@Composable
fun ExpensesScreen(viewModel: LorbitViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Expense Tracker", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("College spending & allowance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .background(Color(0x22FFFFFF), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Spent", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Text(String.format(Locale.getDefault(), "₹%.2f", totalExpense), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (expenses.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No expenses logged yet. Tap + above to track your spending!", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { expense ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                Text("${expense.category} • ${expense.date}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { viewModel.deleteExpense(expense) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                            Text(String.format(Locale.getDefault(), "₹%.2f", expense.amount), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Food") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Expense", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassTextField(value = title, onValueChange = { title = it }, placeholder = "Expense Title (e.g. Canteen)")
                    LiquidGlassTextField(value = amount, onValueChange = { amount = it }, placeholder = "Amount (₹)")
                    LiquidGlassTextField(value = category, onValueChange = { category = it }, placeholder = "Category (Food, Books, Travel)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        viewModel.addExpense(title, amt, category.ifBlank { "Food" })
                        showAddDialog = false
                    }
                }) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}