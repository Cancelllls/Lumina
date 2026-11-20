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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.cancellls.lumina.ui.haptics.LuminaHaptic
import com.cancellls.lumina.ui.haptics.rememberLuminaHaptics
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.hazeGlassContainer
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Scroll connection mirroring BitChord's FloatingTabBarScrollConnection:
 * scrolling down smoothly collapses the dock inline; scrolling back up restores it.
 */
@Stable
class DockScrollState : NestedScrollConnection {
    var isInline by mutableStateOf(false)
        private set

    private var accumulatedDelta = 0f

    fun expand() {
        accumulatedDelta = 0f
        isInline = false
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy < -4f) {
            accumulatedDelta += dy
            if (accumulatedDelta < -48f) {
                isInline = true
            }
        } else if (dy > 4f) {
            accumulatedDelta = 0f
            isInline = false
        }
        return Offset.Zero
    }
}

@Composable
fun rememberDockScrollState(): DockScrollState = remember { DockScrollState() }

/**
 * BitChord-inspired iOS 26 Dual-Island Liquid Glass Dock:
 * - Collapsed (scroll inline): compact 50dp glass pill + 50dp orb.
 * - Expanded (default): 62dp tab pill with horizontal drag + lifted lens selection physics
 *   paired with detached circular Quick-Add orb inside [hazeGlassContainer].
 * - Quick-Add mode: morphs smoothly into the NLP capture capsule with live chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddDock(
    currentScreen: LuminaScreen,
    isInline: Boolean,
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
    val haptics = rememberLuminaHaptics()
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

    val dockHeight by animateDpAsState(
        targetValue = if (isInline) 50.dp else 62.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_height"
    )

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
                    // Main Floating Tab Pill with horizontal drag & lift selection lens
                    GlassChromeBar(
                        hazeState = chromeHazeState,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(dockHeight)
                    ) {
                        val selectedIndex = dockTabs.indexOfFirst { it.screen == currentScreen }
                            .coerceAtLeast(0)
                        val density = LocalDensity.current

                        var isDragging by remember { mutableStateOf(false) }
                        var dragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
                        var lastTickIndex by remember { mutableIntStateOf(selectedIndex) }

                        LaunchedEffect(selectedIndex) {
                            if (!isDragging) {
                                dragPosition = selectedIndex.toFloat()
                                lastTickIndex = selectedIndex
                            }
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .pointerInput(dockTabs.size) {
                                    val tabWidthPx = with(density) { (maxWidth / dockTabs.size).toPx() }
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            isDragging = true
                                            val pos = (offset.x / tabWidthPx - 0.5f)
                                                .coerceIn(0f, (dockTabs.size - 1).toFloat())
                                            dragPosition = pos
                                            lastTickIndex = pos.roundToInt()
                                            haptics.play(LuminaHaptic.Select)
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragPosition = (dragPosition + dragAmount / tabWidthPx)
                                                .coerceIn(-0.12f, (dockTabs.size - 1) + 0.12f)
                                            val hovered = dragPosition.roundToInt()
                                                .coerceIn(0, dockTabs.lastIndex)
                                            if (hovered != lastTickIndex) {
                                                lastTickIndex = hovered
                                                haptics.play(LuminaHaptic.Tick)
                                            }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val target = dragPosition.roundToInt()
                                                .coerceIn(0, dockTabs.lastIndex)
                                            dragPosition = target.toFloat()
                                            haptics.play(LuminaHaptic.Select)
                                            onSelectScreen(dockTabs[target].screen)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            dragPosition = selectedIndex.toFloat()
                                        }
                                    )
                                }
                        ) {
                            val tabWidth = maxWidth / dockTabs.size
                            val targetX = if (isDragging) {
                                tabWidth * dragPosition.coerceIn(0f, (dockTabs.size - 1).toFloat())
                            } else {
                                tabWidth * selectedIndex
                            }

                            val indicatorOffset by animateDpAsState(
                                targetValue = targetX,
                                animationSpec = if (isDragging) {
                                    spring(stiffness = Spring.StiffnessHigh)
                                } else {
                                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                },
                                label = "glass_pill_offset"
                            )

                            val travelDelta = abs((indicatorOffset - targetX).value)
                            val isLifted = isDragging || travelDelta > 4f

                            val stretchX by animateFloatAsState(
                                targetValue = if (isLifted) 1.08f else 1f,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                                label = "glass_pill_stretch_x"
                            )
                            val liftScaleY by animateFloatAsState(
                                targetValue = if (isDragging) 1.08f else if (isLifted) 0.96f else 1f,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                                label = "glass_pill_lift_y"
                            )

                            // Travelling Selection Lens Pill
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = stretchX
                                        scaleY = liftScaleY
                                    }
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (colors.isDark) Color(0x30FFFFFF) else Color(0xD9FFFFFF)
                                    )
                                    .border(
                                        0.8.dp,
                                        colors.rimTopLeft.copy(alpha = if (isLifted) 0.95f else 0.72f),
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
                                                haptics.play(LuminaHaptic.Select)
                                                onSelectScreen(tab.screen)
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (isSelected) colors.accent else itemColor,
                                            modifier = Modifier.size(if (isInline) 22.dp else 19.dp)
                                        )
                                        AnimatedVisibility(visible = !isInline) {
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
                    }

                    // Detached Circular Liquid Glass Quick-Add Orb
                    GlassOrbButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Quick add reminder",
                        hazeState = chromeHazeState,
                        size = dockHeight,
                        tint = Color.White,
                        accentTint = colors.accent,
                        onClick = {
                            haptics.play(LuminaHaptic.Select)
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
                                keyboardActions = KeyboardActions(onSend = {
                                    haptics.play(LuminaHaptic.Confirm)
                                    onSubmit()
                                }),
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
                                    if (canSend) {
                                        haptics.play(LuminaHaptic.Confirm)
                                        onSubmit()
                                    } else {
                                        onOpenFullEditor()
                                    }
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
