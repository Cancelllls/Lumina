package com.cancellls.lumina.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cancellls.lumina.domain.ParsedReminderInput
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.SmartListType
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.glass.GlassChromeBar
import com.cancellls.lumina.ui.glass.GlassOrbButton
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.hazeGlassContainer

private data class DockTabItem(
    val label: String,
    val icon: ImageVector,
    val screen: LuminaScreen
)

private val dockTabs = listOf(
    DockTabItem("Home", Icons.Rounded.Home, LuminaScreen.Home),
    DockTabItem("Today", Icons.Rounded.Today, LuminaScreen.SmartList(SmartListType.TODAY)),
    DockTabItem("Scheduled", Icons.Rounded.CalendarMonth, LuminaScreen.SmartList(SmartListType.SCHEDULED)),
    DockTabItem("All", Icons.Rounded.Inbox, LuminaScreen.SmartList(SmartListType.ALL))
)

/**
 * BitChord-inspired iOS 26 Dual-Island Liquid Glass Dock:
 * - Collapsed: a floating tab pill with a spring-animated travelling glass selection lens
 *   paired with a detached circular Quick-Add orb inside a [hazeGlassContainer].
 * - Expanded: morphs smoothly into the natural-language Quick-Add capsule with live NLP chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddDock(
    currentScreen: LuminaScreen,
    isExpanded: Boolean,
    text: String,
    parsed: ParsedReminderInput,
    chromeHazeState: HazeState,
    onSelectScreen: (LuminaScreen) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    val hasDetectedMeta = isExpanded && text.isNotBlank() && (
        parsed.dueAtMillis != null ||
            parsed.recurrenceRule.isNotEmpty() ||
            parsed.priority != Priority.NONE ||
            parsed.isFlagged
        )

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(visible = hasDetectedMeta) {
            GlassChromeBar(
                hazeState = chromeHazeState,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (parsed.dueAtMillis != null) {
                        GlassChip(
                            label = UiFormatters.formatDueChip(parsed.dueAtMillis, parsed.hasTime),
                            icon = Icons.Rounded.Schedule,
                            tint = colors.accent,
                            selected = true
                        )
                    }
                    if (parsed.recurrenceRule.isNotEmpty()) {
                        GlassChip(
                            label = RecurrenceEngine.formatLabel(parsed.recurrenceRule),
                            icon = Icons.Rounded.Repeat,
                            tint = colors.accentSecondary,
                            selected = true
                        )
                    }
                    if (parsed.priority != Priority.NONE) {
                        val pColor = when (parsed.priority) {
                            Priority.HIGH -> colors.danger
                            Priority.MEDIUM -> colors.warning
                            Priority.LOW -> colors.accent
                            Priority.NONE -> colors.textSecondary
                        }
                        GlassChip(
                            label = parsed.priority.label,
                            icon = Icons.Rounded.PriorityHigh,
                            tint = pColor,
                            selected = true
                        )
                    }
                    if (parsed.isFlagged) {
                        GlassChip(
                            label = "Flagged",
                            icon = Icons.Rounded.Flag,
                            tint = colors.warning,
                            selected = true
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "liquid_glass_dock_mode"
        ) { expanded ->
            if (!expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hazeGlassContainer(mergeDistance = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main Floating Tab Pill with travelling liquid glass selection pill
                    GlassChromeBar(
                        hazeState = chromeHazeState,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                    ) {
                        val selectedIndex = dockTabs.indexOfFirst { it.screen == currentScreen }
                            .coerceAtLeast(0)

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                        ) {
                            val tabWidth = maxWidth / dockTabs.size
                            val indicatorOffset by animateDpAsState(
                                targetValue = tabWidth * selectedIndex,
                                animationSpec = spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "glass_pill_offset"
                            )
                            val travelDelta = kotlin.math.abs(
                                (indicatorOffset - tabWidth * selectedIndex).value
                            )
                            val stretchX by animateFloatAsState(
                                targetValue = if (travelDelta > 4f) 1.09f else 1f,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                                label = "glass_pill_stretch"
                            )

                            // Travelling Selection Lens Pill
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = stretchX
                                        scaleY = 1f / (1f + (stretchX - 1f) * 0.35f)
                                    }
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (colors.isDark) Color(0x2EFFFFFF) else Color(0xCCFFFFFF)
                                    )
                                    .border(
                                        0.8.dp,
                                        colors.rimTopLeft.copy(alpha = 0.75f),
                                        RoundedCornerShape(50)
                                    )
                            )

                            Row(modifier = Modifier.fillMaxSize()) {
                                dockTabs.forEachIndexed { idx, tab ->
                                    val isSelected = idx == selectedIndex &&
                                        (currentScreen == tab.screen || (idx == 0 && currentScreen !in dockTabs.map { it.screen }))
                                    val itemColor = if (isSelected) colors.textPrimary else colors.textSecondary
                                    val interactionSource = remember { MutableInteractionSource() }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelectScreen(tab.screen)
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (isSelected) colors.accent else itemColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) colors.textPrimary else itemColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Detached Circular Liquid Glass Quick-Add Orb (BitChord style)
                    GlassOrbButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Quick add reminder",
                        hazeState = chromeHazeState,
                        size = 60.dp,
                        tint = Color.White,
                        accentTint = colors.accent,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onExpandedChange(true)
                        }
                    )
                }
            } else {
                // Expanded Floating Quick-Add Glass Capsule
                GlassChromeBar(
                    hazeState = chromeHazeState,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassOrbButton(
                            icon = Icons.Rounded.Tune,
                            contentDescription = "Detailed reminder editor",
                            hazeState = chromeHazeState,
                            size = 40.dp,
                            onClick = onOpenFullEditor
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Remind me... tomorrow 9am !!!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textTertiary,
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = onTextChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = colors.textPrimary
                                ),
                                cursorBrush = SolidColor(colors.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        GlassOrbButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Collapse quick add",
                            hazeState = chromeHazeState,
                            size = 36.dp,
                            tint = colors.textSecondary,
                            onClick = { onExpandedChange(false) }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        val canSend = text.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) {
                                        Brush.linearGradient(
                                            colors = listOf(colors.accent, colors.accentSecondary)
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                colors.accent.copy(alpha = 0.28f),
                                                colors.accentSecondary.copy(alpha = 0.24f)
                                            )
                                        )
                                    }
                                )
                                .border(0.8.dp, Color.White.copy(alpha = 0.48f), CircleShape)
                                .clickable {
                                    if (canSend) onSubmit() else onOpenFullEditor()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (canSend) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Tune,
                                contentDescription = if (canSend) "Add reminder" else "Full editor",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
