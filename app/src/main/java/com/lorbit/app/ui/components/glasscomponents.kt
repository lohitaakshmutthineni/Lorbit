package com.lorbit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pure OLED Black
val OledTrueBlack = Color(0xFF000000)
val FrostedGlassSurface = Color(0x18FFFFFF)
val FrostedGlassBorder = Color(0x24FFFFFF)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = FrostedGlassSurface,
    borderColor: Color = FrostedGlassBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "glass_press"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun LiquidBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledTrueBlack)
    ) {
        content()
    }
}

/**
 * Unique Liquid Glass Text Input Field
 */
@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1AFFFFFF),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.08f))
            )
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = minLines,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Smart Liquid Glass Subject Dropdown with Persistent "Others" History
 */
@Composable
fun LiquidSubjectDropdownSelector(
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
    collegeSubjects: List<String>,
    customHistorySubjects: List<String>,
    onAddCustomSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isAddingNewOther by remember { mutableStateOf(false) }
    var newOtherText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = Color(0x1EFFFFFF),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedSubject.isBlank()) "Select Subject..." else selectedSubject,
                    color = if (selectedSubject.isBlank()) Color.White.copy(alpha = 0.4f) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; isAddingNewOther = false },
            modifier = Modifier
                .background(Color(0xEE121826))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            if (collegeSubjects.isNotEmpty()) {
                Text("COLLEGE SUBJECTS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                collegeSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject, color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            onSubjectSelected(subject)
                            expanded = false
                        }
                    )
                }
            }

            if (customHistorySubjects.isNotEmpty()) {
                Text("PREVIOUS OTHERS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                customHistorySubjects.forEach { otherSubject ->
                    DropdownMenuItem(
                        text = { Text(otherSubject, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp) },
                        onClick = {
                            onSubjectSelected(otherSubject)
                            expanded = false
                        }
                    )
                }
            }

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add New Other Subject", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = { isAddingNewOther = true }
            )

            if (isAddingNewOther) {
                Column(modifier = Modifier.padding(12.dp)) {
                    LiquidGlassTextField(
                        value = newOtherText,
                        onValueChange = { newOtherText = it },
                        placeholder = "e.g. Biology, Aptitude"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (newOtherText.isNotBlank()) {
                                    onAddCustomSubject(newOtherText.trim())
                                    onSubjectSelected(newOtherText.trim())
                                    newOtherText = ""
                                    expanded = false
                                    isAddingNewOther = false
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text("Save & Select", modifier = Modifier.padding(8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}