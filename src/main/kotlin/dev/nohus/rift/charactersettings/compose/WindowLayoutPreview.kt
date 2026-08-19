package dev.nohus.rift.charactersettings.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.nohus.rift.charactersettings.CategorizeWindowUseCase
import dev.nohus.rift.charactersettings.getEveReopenedWindowLayerOrder
import dev.nohus.rift.charactersettings.io.ReadAccountSettingsUseCase
import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase.CharacterSettings
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.arrow_down_16px
import dev.nohus.rift.generated.resources.arrow_up_16px
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.layout_preview_background_space
import dev.nohus.rift.generated.resources.layout_preview_background_station
import dev.nohus.rift.generated.resources.layout_preview_background_structure
import dev.nohus.rift.generated.resources.layout_preview_ship_ui
import dev.nohus.rift.generated.resources.layout_preview_target_hp
import dev.nohus.rift.generated.resources.layout_preview_target_outercircle
import dev.nohus.rift.generated.resources.layout_preview_target_spincorners
import dev.nohus.rift.generated.resources.layout_preview_target_targetbackground
import dev.nohus.rift.generated.resources.layout_preview_top_left
import dev.nohus.rift.utils.HsbColor
import dev.nohus.rift.utils.toColor
import org.jetbrains.compose.resources.painterResource
import java.awt.Cursor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun WindowLayoutPreview(
    selectedProfile: String?,
    settings: CharacterSettings?,
    screenResolution: Pair<Int, Int>?,
    shipState: ShipState,
    isShowingFleet: Boolean,
    isShowingDrones: Boolean,
    isShowingMinimized: Boolean,
    accountSettings: ReadAccountSettingsUseCase.AccountSettings?,
    onWindowBoundsChanged: (String, Int, Int, Int, Int, Pair<Int, Int>) -> Unit,
    onWindowMinimizedChanged: (String) -> Unit,
    onWindowClosed: (String) -> Unit,
    onShipUiOffsetChanged: (Float) -> Unit,
    onShipUiVerticalPositionChanged: () -> Unit,
    onTargetOriginChanged: (Pair<Float, Float>) -> Unit,
    onTargetsAlignmentChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (selectedProfile == null) {
            Text(
                text = "No settings profiles found for this character.\nMake sure to log in to the game at least once.",
                style = RiftTheme.typography.bodySecondary
            )
        } else {
            if (settings != null) {
                val previewResolution = screenResolution ?: settings.screenResolution
                val displayedWindowBounds = remember(settings.windowSizesAndPositions, previewResolution) {
                    settings.windowSizesAndPositions.mapValues { (_, bounds) ->
                        adaptWindowBoundsToResolution(bounds, previewResolution)
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    val widthRatio = previewResolution.first / this@BoxWithConstraints.maxWidth.value // How many width layout px fit into 1.dp
                    val heightRatio = previewResolution.second / this@BoxWithConstraints.maxHeight.value // How many height layout px fit into 1.dp
                    val ratio = maxOf(widthRatio, heightRatio) // How many layout px fit into 1.dp
                    val screenWidth = (previewResolution.first / ratio).dp
                    val screenHeight = (previewResolution.second / ratio).dp
                    val neocomWidth = (settings.neocomWidthPx / ratio).dp
                    val hazeState = rememberHazeState()
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .size(screenWidth, screenHeight)
                            .clipToBounds()
                    ) {
                        // Screen background
                        ScreenBackground(shipState, hazeState, screenWidth, screenHeight)

                    // Neocom
                    Column(
                        modifier = Modifier
                            .hazeEffect(hazeState) {
                                blurEffect {
                                    blurRadius = 2.dp
                                }
                            }
                            .background(Color.Black.copy(alpha = 0.4f))
                            .size(neocomWidth, screenHeight)
                    ) {
                        settings.neocomButtons.forEach { button ->
                            val colorFilter = button.colorId?.let { ColorFilter.tint(getNeocomIconColor(it)) }
                            EveResourceIcon(
                                resourcePath = button.iconPath,
                                colorFilter = colorFilter,
                                modifier = Modifier.size(neocomWidth)
                            )
                        }
                    }

                    // Top-left info
                    Image(
                        painter = painterResource(Res.drawable.layout_preview_top_left),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = neocomWidth)
                            .width((274 / ratio).dp)
                    )

                    // Ship UI
                    if (shipState == ShipState.InSpace) {
                        val width = (650 / ratio).dp
                        val leftOffsetToCenterWidth = (142 / ratio).dp
                        val height = (188 / ratio).dp
                        val centerSnapDistance = 8f
                        val verticalOffset = if (accountSettings?.isShipUiOnTop == true) {
                            0.dp
                        } else {
                            (screenHeight - height)
                        }
                        val shipUiPointerInteraction = rememberPointerInteractionStateHolder()
                        val currentShipUiOffset by rememberUpdatedState(settings.shipUiLeftOffsetPx)
                        var shipUiDragStartOffset by remember { mutableStateOf<Float?>(null) }
                        var shipUiDragAmount by remember { mutableStateOf(0f) }
                        var shipUiDragOffset by remember { mutableStateOf<Float?>(null) }
                        val displayedShipUiOffset = shipUiDragOffset ?: settings.shipUiLeftOffsetPx
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (screenWidth / 2) - leftOffsetToCenterWidth + (displayedShipUiOffset / ratio).dp,
                                    y = verticalOffset,
                                )
                                .width(width)
                                .pointerInteraction(shipUiPointerInteraction)
                                .pointerHoverIcon(PointerIcon(Cursors.dragHorizontal))
                                .pointerInput(ratio) {
                                    detectDragGestures(
                                        onDragStart = {
                                            shipUiDragStartOffset = currentShipUiOffset
                                            shipUiDragAmount = 0f
                                            shipUiDragOffset = currentShipUiOffset
                                        },
                                        onDragEnd = {
                                            shipUiDragStartOffset = null
                                            shipUiDragOffset = null
                                        },
                                        onDragCancel = {
                                            shipUiDragStartOffset = null
                                            shipUiDragOffset = null
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            shipUiDragAmount += amount.x
                                            shipUiDragStartOffset?.let { startOffset ->
                                                val offset = startOffset + shipUiDragAmount / density.density * ratio
                                                val snappedOffset = if (offset in -centerSnapDistance..centerSnapDistance) 0f else offset
                                                shipUiDragOffset = snappedOffset
                                                onShipUiOffsetChanged(snappedOffset)
                                            }
                                        },
                                    )
                                },
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.layout_preview_ship_ui),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AnimatedVisibility(
                                visible = shipUiPointerInteraction.isHovered,
                                modifier = Modifier.align(Alignment.TopCenter),
                            ) {
                                RiftImageButton(
                                    resource = if (accountSettings?.isShipUiOnTop == true) {
                                        Res.drawable.arrow_down_16px
                                    } else {
                                        Res.drawable.arrow_up_16px
                                    },
                                    size = 20.dp,
                                    onClick = onShipUiVerticalPositionChanged,
                                )
                            }
                        }
                    }

                    // Targets
                    if (shipState == ShipState.InSpace) {
                        accountSettings?.targetOrigin?.let { (x, y) ->
                            val targetVisualSize = 120f / ratio
                            val targetLayoutWidth = 110f / ratio
                            val targetLayoutHeight = 180f / ratio
                            val targetFitHeight = 200f / ratio
                            val targetVisualHorizontalOverflow = (targetVisualSize - targetLayoutWidth) / 2
                            val targetVisualTopOverflow = (targetVisualSize - 94f / ratio) / 2
                            val targetAnchorRadius = 8f / ratio
                            val minTargetOriginX = targetAnchorRadius / screenWidth.value
                            val maxTargetOriginX = 1f - minTargetOriginX
                            val minTargetOriginY = targetAnchorRadius / screenHeight.value
                            val maxTargetOriginY = 1f - minTargetOriginY
                            val isHorizontal = accountSettings.isTargetsAlignHorizontal
                            val count = if (isHorizontal) 8 else 8
                            var targetDragStartOrigin by remember { mutableStateOf<Pair<Float, Float>?>(null) }
                            var targetDragAmount by remember { mutableStateOf(Offset.Zero) }
                            var targetDragPointerStart by remember { mutableStateOf<Offset?>(null) }
                            val currentTargetOrigin by rememberUpdatedState(x to y)
                            val targetPointerInteraction = rememberPointerInteractionStateHolder()
                            val anchorX = screenWidth.value * x
                            val anchorY = screenHeight.value * y
                            val extendsLeft = x > 0.5f
                            val extendsUp = y > 0.5f
                            val availablePrimarySpace = if (isHorizontal) {
                                if (extendsLeft) anchorX else screenWidth.value - anchorX
                            } else {
                                if (extendsUp) anchorY else screenHeight.value - anchorY
                            }
                            val primaryTargetSize = if (isHorizontal) targetLayoutWidth else targetFitHeight
                            val targetsPerRowOrColumn = (ceil(availablePrimarySpace / primaryTargetSize).toInt() - 1)
                                .coerceAtLeast(1)
                            repeat(count) { index ->
                                val column = if (isHorizontal) {
                                    index % targetsPerRowOrColumn
                                } else {
                                    index / targetsPerRowOrColumn
                                }
                                val row = if (isHorizontal) {
                                    index / targetsPerRowOrColumn
                                } else {
                                    index % targetsPerRowOrColumn
                                }
                                val targetX = if (extendsLeft) {
                                    anchorX - targetLayoutWidth * (column + 1)
                                } else {
                                    anchorX + targetLayoutWidth * column
                                } - targetVisualHorizontalOverflow
                                val targetY = if (extendsUp) {
                                    anchorY - targetLayoutHeight * (row + 1)
                                } else {
                                    anchorY + targetLayoutHeight * row
                                } - targetVisualTopOverflow
                                val currentTargetPosition by rememberUpdatedState(Offset(targetX, targetY))
                                Box(
                                    modifier = Modifier
                                        .offset(x = targetX.dp, y = targetY.dp)
                                        .size(targetVisualSize.dp)
                                        .zIndex(1f)
                                        .pointerInteraction(targetPointerInteraction)
                                        .pointerHoverIcon(PointerIcon(Cursors.move))
                                        .pointerInput(ratio) {
                                            detectDragGestures(
                                                onDragStart = { position ->
                                                    targetDragStartOrigin = currentTargetOrigin.let { (originX, originY) ->
                                                        originX.coerceIn(minTargetOriginX, maxTargetOriginX) to
                                                            originY.coerceIn(minTargetOriginY, maxTargetOriginY)
                                                    }
                                                    targetDragAmount = Offset.Zero
                                                    targetDragPointerStart = Offset(
                                                        x = currentTargetPosition.x + position.x / density.density,
                                                        y = currentTargetPosition.y + position.y / density.density,
                                                    )
                                                },
                                                onDragEnd = {
                                                    targetDragStartOrigin = null
                                                    targetDragPointerStart = null
                                                },
                                                onDragCancel = {
                                                    targetDragStartOrigin = null
                                                    targetDragPointerStart = null
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    targetDragAmount += amount
                                                    val startOrigin = targetDragStartOrigin
                                                    val pointerStart = targetDragPointerStart
                                                    if (startOrigin != null && pointerStart != null) {
                                                        onTargetOriginChanged(
                                                            getDraggedTargetOrigin(
                                                                startOrigin = startOrigin.first,
                                                                dragAmount = targetDragAmount.x / density.density,
                                                                pointerStart = pointerStart.x,
                                                                screenSize = screenWidth.value,
                                                                minOrigin = minTargetOriginX,
                                                                maxOrigin = maxTargetOriginX,
                                                            ) to getDraggedTargetOrigin(
                                                                startOrigin = startOrigin.second,
                                                                dragAmount = targetDragAmount.y / density.density,
                                                                pointerStart = pointerStart.y,
                                                                screenSize = screenHeight.value,
                                                                minOrigin = minTargetOriginY,
                                                                maxOrigin = maxTargetOriginY,
                                                            )
                                                        )
                                                    }
                                                },
                                            )
                                        },
                                ) {
                                    LockedTarget(
                                        index = index,
                                        isSelected = index == 0,
                                        ratio = ratio,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (index == 0) {
                                        AnimatedVisibility(
                                            visible = targetPointerInteraction.isHovered,
                                            modifier = Modifier.align(Alignment.TopEnd),
                                        ) {
                                            RiftImageButton(
                                                resource = if (isHorizontal) Res.drawable.arrow_down_16px else Res.drawable.arrow_up_16px,
                                                size = 20.dp,
                                                onClick = onTargetsAlignmentChanged,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Windows
                    val categorizeWindowUseCase: CategorizeWindowUseCase = remember { koin.get() }
                    val visibleWindows = settings.windowSizesAndPositions.keys.filterTo(mutableSetOf()) { window ->
                        val isMinimized = window in settings.minimizedWindows
                        val parentStack = settings.windowStacks.firstOrNull { it.first == window }?.second
                        val childrenWindows = settings.windowStacks.filter { it.second == window }.map { it.first }
                        val isOpen = isWindowOrStackOpen(window, childrenWindows, settings.openWindows)
                        val eveWindow = categorizeWindowUseCase(window, childrenWindows, settings.joinedChatChannels)
                        parentStack == null && eveWindow.isVisible(
                            isOpen = isOpen,
                            isMinimized = isMinimized,
                            shipState = shipState,
                            isShowingMinimized = isShowingMinimized,
                            isShowingFleet = isShowingFleet,
                            isShowingDrones = isShowingDrones,
                        )
                    }
                    val windowZIndices = getEveReopenedWindowLayerOrder(settings)
                        .withIndex()
                        .associate { (index, window) -> window to index.toFloat() }
                    val currentWindowBounds by rememberUpdatedState(
                        displayedWindowBounds
                            .filterKeys { it in visibleWindows }
                    )
                    displayedWindowBounds.forEach { (window, bounds) ->
                        key(window) {
                            val isMinimized = window in settings.minimizedWindows
                            val parentStack = settings.windowStacks.firstOrNull { it.first == window }?.second
                            if (parentStack == null) {
                                val childrenWindows = settings.windowStacks.filter { it.second == window }.map { it.first }
                                val isOpen = isWindowOrStackOpen(window, childrenWindows, settings.openWindows)
                                val (x, y, width, height) = bounds
                                val xDp = (x / ratio).dp
                                val yDp = (y / ratio).dp
                                val widthDp = (width / ratio).dp
                                val heightDp = (height / ratio).dp

                            val eveWindow = categorizeWindowUseCase(window, childrenWindows, settings.joinedChatChannels)
                            val isVisible = eveWindow.isVisible(
                                isOpen = isOpen,
                                isMinimized = isMinimized,
                                shipState = shipState,
                                isShowingMinimized = isShowingMinimized,
                                isShowingFleet = isShowingFleet,
                                isShowingDrones = isShowingDrones,
                            )

                            if (isVisible) {
                                val neocomOffset = if (x in 0..settings.neocomWidthPx) neocomWidth else 0.dp
                                val currentBounds by rememberUpdatedState(WindowBounds(x, y, width, height))
                                var dragStartBounds by remember(window) { mutableStateOf<WindowBounds?>(null) }
                                var dragAmount by remember(window) { mutableStateOf(Offset.Zero) }
                                var isEditMenuOpen by remember(window) { mutableStateOf(false) }
                                val windowPointerInteraction = rememberPointerInteractionStateHolder()
                                val borderColor = RiftTheme.colors.borderPrimaryDark
                                val resizeWindow = { bounds: WindowBounds, deltaX: Int, deltaY: Int, edges: ResizeEdges ->
                                    val resized = snapResizedWindowBounds(
                                        bounds = bounds,
                                        deltaX = deltaX,
                                        deltaY = deltaY,
                                        edges = edges,
                                        otherWindows = currentWindowBounds.filterKeys { it != window }.values,
                                        screenWidth = previewResolution.first,
                                        screenHeight = previewResolution.second,
                                        leftInset = settings.neocomWidthPx + SCREEN_EDGE_INSET_PX,
                                    )
                                    onWindowBoundsChanged(
                                        window,
                                        resized.x,
                                        resized.y,
                                        resized.width,
                                        resized.height,
                                        previewResolution,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .offset(xDp + neocomOffset, yDp)
                                        .zIndex(windowZIndices.getValue(window))
                                        .pointerInteraction(windowPointerInteraction)
                                        .pointerHoverIcon(PointerIcon(Cursors.move))
                                        .pointerInput(window, ratio, previewResolution) {
                                            detectDragGestures(
                                                onDragStart = { pointerPosition ->
                                                    val handleSize = with(density) { 8.dp.toPx() }
                                                    val windowWidth = currentBounds.width / ratio * density.density
                                                    val windowHeight = currentBounds.height / ratio * density.density
                                                    val isOnResizeHandle = pointerPosition.x <= handleSize ||
                                                        pointerPosition.x >= windowWidth - handleSize ||
                                                        pointerPosition.y <= handleSize ||
                                                        pointerPosition.y >= windowHeight - handleSize
                                                    dragStartBounds = currentBounds.takeUnless { isOnResizeHandle }
                                                    dragAmount = Offset.Zero
                                                },
                                                onDragEnd = { dragStartBounds = null },
                                                onDragCancel = { dragStartBounds = null },
                                                onDrag = { change, amount ->
                                                    val startBounds = dragStartBounds
                                                    if (startBounds != null) {
                                                        change.consume()
                                                        dragAmount += amount
                                                        val deltaX = (dragAmount.x / density.density * ratio).roundToInt()
                                                        val deltaY = (dragAmount.y / density.density * ratio).roundToInt()
                                                        val rawX = (startBounds.x + deltaX).coerceIn(
                                                            -startBounds.width / 2,
                                                            previewResolution.first - startBounds.width / 2,
                                                        )
                                                        val rawY = (startBounds.y + deltaY).coerceIn(
                                                            -startBounds.height / 2,
                                                            previewResolution.second - startBounds.height / 2,
                                                        )
                                                        val snappedBounds = snapWindowBounds(
                                                            bounds = startBounds.copy(x = rawX, y = rawY),
                                                            otherWindows = currentWindowBounds.filterKeys { it != window }.values,
                                                            screenWidth = previewResolution.first,
                                                            screenHeight = previewResolution.second,
                                                            leftInset = settings.neocomWidthPx + SCREEN_EDGE_INSET_PX,
                                                        )
                                                        onWindowBoundsChanged(
                                                            window,
                                                            snappedBounds.x,
                                                            snappedBounds.y,
                                                            startBounds.width,
                                                            startBounds.height,
                                                            previewResolution,
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                        .hazeEffect(hazeState) {
                                            blurEffect {
                                                blurRadius = 4.dp
                                            }
                                        }
                                        .background(RiftTheme.colors.windowBackground.copy(alpha = 0.2f))
                                        .modifyIf(!isMinimized) {
                                            border(1.dp, borderColor)
                                        }
                                        .size(widthDp, heightDp)
                                ) {
                                    if (isMinimized) {
                                        Canvas(Modifier.fillMaxSize()) {
                                            drawRoundRect(
                                                color = borderColor,
                                                style = Stroke(
                                                    width = with(density) { 2.dp.toPx() },
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                                                ),
                                            )
                                        }
                                    }
                                    if (eveWindow.icon != null) {
                                        Image(
                                            painter = painterResource(eveWindow.icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                    }
                                    Text(
                                        text = eveWindow.name,
                                        style = RiftTheme.typography.detailPrimary,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    AnimatedVisibility(
                                        visible = windowPointerInteraction.isHovered || isEditMenuOpen,
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        RiftContextMenuArea(
                                            items = listOf(
                                                ContextMenuItem.TextItem(
                                                    text = if (isMinimized) "Unminimize" else "Minimize",
                                                    onClick = { onWindowMinimizedChanged(window) },
                                                ),
                                            ) + if (eveWindow.isCloseable) {
                                                listOf(
                                                    ContextMenuItem.TextItem(
                                                        text = "Close",
                                                        onClick = { onWindowClosed(window) },
                                                    )
                                                )
                                            } else {
                                                emptyList()
                                            },
                                            acceptsLeftClick = true,
                                            acceptsRightClick = false,
                                            onMenuShownChange = { isEditMenuOpen = it },
                                        ) {
                                            RiftImageButton(
                                                resource = Res.drawable.editplanicon,
                                                size = 20.dp,
                                                onClick = { isEditMenuOpen = true },
                                                modifier = Modifier.padding(Spacing.small),
                                            )
                                        }
                                    }
                                    WindowResizeHandle(
                                        alignment = Alignment.CenterStart,
                                        cursor = Cursors.resizeHorizontal,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, _ ->
                                            resizeWindow(bounds, deltaX, 0, ResizeEdges(left = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.CenterEnd,
                                        cursor = Cursors.resizeHorizontal,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, _ ->
                                            resizeWindow(bounds, deltaX, 0, ResizeEdges(right = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.TopCenter,
                                        cursor = Cursors.resizeVertical,
                                        horizontal = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, _, deltaY ->
                                            resizeWindow(bounds, 0, deltaY, ResizeEdges(top = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.BottomCenter,
                                        cursor = Cursors.resizeVertical,
                                        horizontal = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, _, deltaY ->
                                            resizeWindow(bounds, 0, deltaY, ResizeEdges(bottom = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.TopStart,
                                        cursor = Cursors.resizeBackslash,
                                        corner = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, deltaY ->
                                            resizeWindow(bounds, deltaX, deltaY, ResizeEdges(left = true, top = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.TopEnd,
                                        cursor = Cursors.resizeSlash,
                                        corner = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, deltaY ->
                                            resizeWindow(bounds, deltaX, deltaY, ResizeEdges(right = true, top = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.BottomStart,
                                        cursor = Cursors.resizeSlash,
                                        corner = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, deltaY ->
                                            resizeWindow(bounds, deltaX, deltaY, ResizeEdges(left = true, bottom = true))
                                        },
                                    )
                                    WindowResizeHandle(
                                        alignment = Alignment.BottomEnd,
                                        cursor = Cursors.resizeBackslash,
                                        corner = true,
                                        ratio = ratio,
                                        bounds = currentBounds,
                                        onResize = { bounds, deltaX, deltaY ->
                                            resizeWindow(bounds, deltaX, deltaY, ResizeEdges(right = true, bottom = true))
                                        },
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
    }
}

private const val MIN_WINDOW_SIZE_PX = 50
private const val WINDOW_SNAP_DISTANCE_PX = 10
private const val SCREEN_EDGE_INSET_PX = 16

private data class WindowBounds(val x: Int, val y: Int, val width: Int, val height: Int)

private fun adaptWindowBoundsToResolution(
    savedBounds: List<Int>,
    targetResolution: Pair<Int, Int>,
): WindowBounds {
    val savedX = savedBounds[0]
    val savedY = savedBounds[1]
    val savedWidth = savedBounds[2]
    val savedHeight = savedBounds[3]
    val sourceWidth = savedBounds[4]
    val sourceHeight = savedBounds[5]
    val targetWidth = targetResolution.first
    val targetHeight = targetResolution.second

    var x = savedX
    if (sourceWidth > 0 && sourceWidth != targetWidth) {
        x = when {
            savedX + savedWidth == sourceWidth || savedX + savedWidth == sourceWidth - SCREEN_EDGE_INSET_PX -> {
                savedX + targetWidth - sourceWidth
            }
            savedX !in listOf(0, SCREEN_EDGE_INSET_PX) -> {
                val oldCenterX = Math.floorDiv(sourceWidth - savedWidth, 2)
                val xPortion = oldCenterX / sourceWidth.toDouble()
                val newCenterX = (xPortion * targetWidth).toInt()
                savedX + newCenterX - oldCenterX
            }
            else -> savedX
        }
    }

    var y = savedY
    if (sourceHeight > 0 && sourceHeight != targetHeight) {
        y = when {
            savedY in listOf(0, SCREEN_EDGE_INSET_PX) -> savedY
            savedY + savedHeight == sourceHeight || savedY + savedHeight == sourceHeight - SCREEN_EDGE_INSET_PX -> {
                savedY + targetHeight - sourceHeight
            }
            else -> {
                val oldCenterY = Math.floorDiv(sourceHeight - savedHeight, 2)
                val yPortion = oldCenterY / sourceHeight.toDouble()
                val newCenterY = (yPortion * targetHeight).toInt()
                savedY + newCenterY - oldCenterY
            }
        }
    }

    val width = savedWidth.coerceIn(0, targetWidth)
    val height = savedHeight.coerceIn(0, targetHeight)
    return WindowBounds(
        x = x.coerceIn(0, targetWidth - width),
        y = y.coerceIn(0, targetHeight - height),
        width = width,
        height = height,
    )
}

private data class ResizeEdges(
    val left: Boolean = false,
    val right: Boolean = false,
    val top: Boolean = false,
    val bottom: Boolean = false,
)

private fun snapResizedWindowBounds(
    bounds: WindowBounds,
    deltaX: Int,
    deltaY: Int,
    edges: ResizeEdges,
    otherWindows: Collection<WindowBounds>,
    screenWidth: Int,
    screenHeight: Int,
    leftInset: Int,
): WindowBounds {
    val xGuides = buildList {
        add(0)
        add(leftInset)
        add(screenWidth - SCREEN_EDGE_INSET_PX)
        add(screenWidth)
        otherWindows.forEach { other ->
            add(other.x)
            add(other.x + other.width)
        }
    }
    val yGuides = buildList {
        add(0)
        add(SCREEN_EDGE_INSET_PX)
        add(screenHeight - SCREEN_EDGE_INSET_PX)
        add(screenHeight)
        otherWindows.forEach { other ->
            add(other.y)
            add(other.y + other.height)
        }
    }

    val fixedRight = bounds.x + bounds.width
    val fixedBottom = bounds.y + bounds.height
    val left = if (edges.left) {
        xGuides.snapClosest((bounds.x + deltaX).coerceAtMost(fixedRight - MIN_WINDOW_SIZE_PX))
            .coerceAtMost(fixedRight - MIN_WINDOW_SIZE_PX)
    } else {
        bounds.x
    }
    val right = if (edges.right) {
        xGuides.snapClosest((fixedRight + deltaX).coerceAtLeast(bounds.x + MIN_WINDOW_SIZE_PX))
            .coerceAtLeast(bounds.x + MIN_WINDOW_SIZE_PX)
    } else {
        fixedRight
    }
    val top = if (edges.top) {
        yGuides.snapClosest((bounds.y + deltaY).coerceAtMost(fixedBottom - MIN_WINDOW_SIZE_PX))
            .coerceAtMost(fixedBottom - MIN_WINDOW_SIZE_PX)
    } else {
        bounds.y
    }
    val bottom = if (edges.bottom) {
        yGuides.snapClosest((fixedBottom + deltaY).coerceAtLeast(bounds.y + MIN_WINDOW_SIZE_PX))
            .coerceAtLeast(bounds.y + MIN_WINDOW_SIZE_PX)
    } else {
        fixedBottom
    }

    return WindowBounds(left, top, right - left, bottom - top)
}

private fun snapWindowBounds(
    bounds: WindowBounds,
    otherWindows: Collection<WindowBounds>,
    screenWidth: Int,
    screenHeight: Int,
    leftInset: Int,
): WindowBounds {
    val xCandidates = mutableListOf(
        0,
        leftInset,
        screenWidth - bounds.width,
        screenWidth - SCREEN_EDGE_INSET_PX - bounds.width,
    )
    val yCandidates = mutableListOf(
        0,
        SCREEN_EDGE_INSET_PX,
        screenHeight - bounds.height,
        screenHeight - SCREEN_EDGE_INSET_PX - bounds.height,
    )

    otherWindows.forEach { other ->
        val overlapsVertically = bounds.y < other.y + other.height && bounds.y + bounds.height > other.y
        if (overlapsVertically) {
            xCandidates += other.x
            xCandidates += other.x + other.width
            xCandidates += other.x - bounds.width
            xCandidates += other.x + other.width - bounds.width
        }

        val overlapsHorizontally = bounds.x < other.x + other.width && bounds.x + bounds.width > other.x
        if (overlapsHorizontally) {
            yCandidates += other.y
            yCandidates += other.y + other.height
            yCandidates += other.y - bounds.height
            yCandidates += other.y + other.height - bounds.height
        }
    }

    return bounds.copy(
        x = xCandidates.snapClosest(bounds.x),
        y = yCandidates.snapClosest(bounds.y),
    )
}

private fun Collection<Int>.snapClosest(value: Int): Int {
    val closest = minByOrNull { abs(it - value) } ?: return value
    return closest.takeIf { abs(it - value) <= WINDOW_SNAP_DISTANCE_PX } ?: value
}

@Composable
private fun BoxScope.WindowResizeHandle(
    alignment: Alignment,
    cursor: Cursor,
    horizontal: Boolean = false,
    corner: Boolean = false,
    ratio: Float,
    bounds: WindowBounds,
    onResize: (WindowBounds, Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    var dragAmount by remember { mutableStateOf(Offset.Zero) }
    val currentBounds by rememberUpdatedState(bounds)
    var dragStartBounds by remember { mutableStateOf<WindowBounds?>(null) }
    Box(
        modifier = Modifier
            .align(alignment)
            .then(
                when {
                    corner -> Modifier.size(8.dp)
                    horizontal -> Modifier.fillMaxWidth().height(6.dp)
                    else -> Modifier.fillMaxHeight().width(6.dp)
                }
            )
            .pointerHoverIcon(PointerIcon(cursor))
            .pointerInput(cursor) {
                detectDragGestures(
                    onDragStart = {
                        dragAmount = Offset.Zero
                        dragStartBounds = currentBounds
                    },
                    onDragEnd = { dragStartBounds = null },
                    onDragCancel = { dragStartBounds = null },
                    onDrag = { change, amount ->
                        dragStartBounds?.let { startBounds ->
                            change.consume()
                            dragAmount += amount
                            onResize(
                                startBounds,
                            (dragAmount.x / density.density * ratio).roundToInt(),
                            (dragAmount.y / density.density * ratio).roundToInt(),
                            )
                        }
                    },
                )
            },
    )
}

private fun getDraggedTargetOrigin(
    startOrigin: Float,
    dragAmount: Float,
    pointerStart: Float,
    screenSize: Float,
    minOrigin: Float,
    maxOrigin: Float,
): Float {
    val constrainedPointerStart = pointerStart.coerceIn(0f, screenSize)
    val origin = if (dragAmount >= 0f) {
        val availablePointerDistance = screenSize - constrainedPointerStart
        if (availablePointerDistance > 0f) {
            startOrigin + (maxOrigin - startOrigin) * dragAmount / availablePointerDistance
        } else {
            maxOrigin
        }
    } else {
        val availablePointerDistance = constrainedPointerStart
        if (availablePointerDistance > 0f) {
            startOrigin + (startOrigin - minOrigin) * dragAmount / availablePointerDistance
        } else {
            minOrigin
        }
    }
    return origin.coerceIn(minOrigin, maxOrigin)
}

@Composable
private fun LockedTarget(
    index: Int,
    isSelected: Boolean = false,
    ratio: Float,
    modifier: Modifier
) {
    val size = (120 / ratio).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
    ) {
        Image(
            painter = painterResource(Res.drawable.layout_preview_target_targetbackground),
            contentDescription = null,
            modifier = Modifier
                .size((94 / ratio).dp)
        )
        Image(
            painter = painterResource(Res.drawable.layout_preview_target_hp),
            contentDescription = null,
            modifier = Modifier
                .size((94 / ratio).dp)
        )
        Image(
            painter = painterResource(Res.drawable.layout_preview_target_outercircle),
            contentDescription = null,
            modifier = Modifier
                .alpha(0.5f)
                .size((96 / ratio).dp)
        )
        if (isSelected) {
            val transition = rememberInfiniteTransition()
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4_000, easing = LinearEasing),
                )
            )
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Image(
                painter = painterResource(Res.drawable.layout_preview_target_spincorners),
                contentDescription = null,
                modifier = Modifier
                    .rotate(rotation)
                    .alpha(alpha)
                    .size(size)
            )
        }
        Text(
            text = "${index + 1}",
            style = RiftTheme.typography.detailPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = 1.dp, y = 1.dp)
        )
    }
}

@Composable
private fun ScreenBackground(
    shipState: ShipState,
    hazeState: HazeState,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val image = when (shipState) {
        ShipState.InSpace -> Res.drawable.layout_preview_background_space
        ShipState.DockedStation -> Res.drawable.layout_preview_background_station
        ShipState.DockedStructure -> Res.drawable.layout_preview_background_structure
    }
    val cameraBobTransition = rememberInfiniteTransition()
    val cameraBobX by cameraBobTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val cameraBobY by cameraBobTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Box(
        modifier = Modifier
            .hazeSource(hazeState)
            .size(screenWidth, screenHeight)
            .clipToBounds()
    ) {
        val overscan = 0.01f
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset(
                    x = screenWidth * (overscan * cameraBobX / 2),
                    y = screenHeight * (overscan * cameraBobY / 2),
                )
                .requiredSize(screenWidth * (1 + overscan), screenHeight * (1 + overscan))
        )
    }
}

private fun CategorizeWindowUseCase.EveWindow.isAvailable(state: ShipState): Boolean = when (state) {
    ShipState.InSpace -> inSpace
    ShipState.DockedStation -> inStation
    ShipState.DockedStructure -> inStructure
}

private fun isWindowOrStackOpen(
    window: String,
    childrenWindows: List<String>,
    openWindows: List<String>,
): Boolean {
    return if (childrenWindows.isEmpty()) {
        window in openWindows
    } else {
        childrenWindows.any { it in openWindows }
    }
}

private fun CategorizeWindowUseCase.EveWindow.isVisible(
    isOpen: Boolean,
    isMinimized: Boolean,
    shipState: ShipState,
    isShowingMinimized: Boolean,
    isShowingFleet: Boolean,
    isShowingDrones: Boolean,
): Boolean {
    return isPersistent &&
        (!isOpenStateControlled || isOpen) &&
        isAvailable(shipState) &&
        (!isMinimized || isShowingMinimized) &&
        ("Fleet" !in name || isShowingFleet) &&
        ("Drones" !in name || isShowingDrones)
}

private fun getNeocomIconColor(colorId: Int) = HsbColor(
    hue = (colorId - 1).toFloat() / 12,
    saturation = 0.75f,
    brightness = 1f,
).toColor()
