package dev.nohus.rift.charactersettings.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.charactersettings.io.ReadAccountSettingsUseCase.Probe
import dev.nohus.rift.charactersettings.io.ReadAccountSettingsUseCase.ProbeFormation
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.KeyName
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftIconButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.buttoniconminus
import dev.nohus.rift.generated.resources.buttoniconplus
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.mass_32px
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.generated.resources.window_info
import dev.nohus.rift.windowing.LocalRiftWindowState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

@Composable
fun WindowScope.ProbeFormations(
    formations: List<ProbeFormation>,
    onFormationsChanged: (List<ProbeFormation>) -> Unit,
) {
    var workingFormations by remember(formations) { mutableStateOf(formations) }
    var formationToDelete by remember { mutableStateOf<ProbeFormation?>(null) }
    var formationToRename by remember { mutableStateOf<ProbeFormation?>(null) }
    var selectedFormationName by remember { mutableStateOf(formations.firstOrNull()?.name) }
    var isCreateDialogShown by remember { mutableStateOf(false) }
    var newFormationName by remember { mutableStateOf("") }
    var renamedFormationName by remember { mutableStateOf("") }

    LaunchedEffect(workingFormations) {
        if (workingFormations != formations) onFormationsChanged(workingFormations)
    }

    val onCreateClick = {
        newFormationName = ""
        isCreateDialogShown = true
    }
    val createFormation = { name: String ->
        val formation = createDefaultFormation(name)
        workingFormations = workingFormations + formation
        selectedFormationName = formation.name
        isCreateDialogShown = false
    }

    if (isCreateDialogShown) {
        val parentState = LocalRiftWindowState.current
        if (parentState != null) {
            val trimmedName = newFormationName.trim()
            val isNameAvailable = workingFormations.none { it.name.equals(trimmedName, ignoreCase = true) }
            RiftDialog(
                title = "New formation",
                icon = Res.drawable.window_info,
                parentState = parentState,
                state = rememberWindowState(width = 360.dp, height = Dp.Unspecified),
                onCloseClick = { isCreateDialogShown = false },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RiftTextField(
                        text = newFormationName,
                        placeholder = "Formation name (1–16 characters)",
                        onTextChanged = { newFormationName = it.take(16) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                        RiftButton(
                            text = "Cancel",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { isCreateDialogShown = false },
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Create",
                            isEnabled = trimmedName.length in 1..16 && isNameAvailable,
                            onClick = { createFormation(trimmedName) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    formationToRename?.let { formation ->
        val parentState = LocalRiftWindowState.current
        if (parentState != null) {
            val trimmedName = renamedFormationName.trim()
            val isNameAvailable = workingFormations.none {
                it.name != formation.name && it.name.equals(trimmedName, ignoreCase = true)
            }
            RiftDialog(
                title = "Rename formation",
                icon = Res.drawable.window_info,
                parentState = parentState,
                state = rememberWindowState(width = 360.dp, height = Dp.Unspecified),
                onCloseClick = { formationToRename = null },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RiftTextField(
                        text = renamedFormationName,
                        placeholder = "Formation name (1–$MAX_FORMATION_NAME_LENGTH characters)",
                        onTextChanged = { renamedFormationName = it.take(MAX_FORMATION_NAME_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                        RiftButton(
                            text = "Cancel",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { formationToRename = null },
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Rename",
                            isEnabled = trimmedName.isNotEmpty() && isNameAvailable,
                            onClick = {
                                workingFormations = workingFormations.map {
                                    if (it.name == formation.name) it.copy(name = trimmedName) else it
                                }
                                if (selectedFormationName == formation.name) selectedFormationName = trimmedName
                                formationToRename = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (workingFormations.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text(
                text = "You have no custom probe formations.",
                style = RiftTheme.typography.bodySecondary,
            )
            RiftButton(text = "Create formation", onClick = onCreateClick)
        }
        return
    }

    val selectedFormation = workingFormations.firstOrNull { it.name == selectedFormationName } ?: workingFormations.first()
    var editableProbes by remember(selectedFormation) { mutableStateOf(selectedFormation.probes) }
    var selectedProbeIndex by remember(selectedFormation.name) { mutableStateOf(0) }
    val editableFormation = selectedFormation.copy(probes = editableProbes)
    var scaleFormation by remember(selectedFormation.name) { mutableStateOf<ProbeFormation?>(null) }
    val updateProbes: (List<Probe>) -> Unit = { probes ->
        editableProbes = probes
        workingFormations = workingFormations.map {
            if (it.name == selectedFormation.name) it.copy(probes = probes) else it
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.fillMaxSize(),
    ) {
        FormationList(
            formations = workingFormations,
            selectedFormation = selectedFormation,
            onCreateClick = onCreateClick,
            onSelect = { selectedFormationName = it.name },
            onRename = {
                renamedFormationName = it.name
                formationToRename = it
            },
            onDuplicate = { formation ->
                val duplicate = formation.copy(name = getDuplicateFormationName(formation.name, workingFormations))
                workingFormations = workingFormations + duplicate
                selectedFormationName = duplicate.name
            },
            onDelete = { formationToDelete = it },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.weight(0.72f).fillMaxHeight(),
        ) {
            ProbeValuesEditor(
                probes = editableProbes,
                selectedProbeIndex = selectedProbeIndex,
                onProbeSelected = { selectedProbeIndex = it },
                onProbeAdded = {
                    if (editableProbes.size < MAX_PROBES) {
                        updateProbes(editableProbes + createProbeAtFormationCenter(editableProbes, selectedProbeIndex))
                        selectedProbeIndex = editableProbes.size
                    }
                },
                onProbeRemoved = {
                    if (editableProbes.size > 1) {
                        val removedIndex = selectedProbeIndex.coerceIn(editableProbes.indices)
                        updateProbes(editableProbes.filterIndexed { index, _ -> index != removedIndex })
                        selectedProbeIndex = removedIndex.coerceAtMost(editableProbes.lastIndex - 1)
                    }
                },
                onFormationBalanced = {
                    if (editableProbes.size < MAX_PROBES) {
                        updateProbes(editableProbes + createBalancingProbe(editableProbes, selectedProbeIndex))
                        selectedProbeIndex = editableProbes.size
                    }
                },
                onProbeChanged = { index, probe ->
                    updateProbes(editableProbes.toMutableList().also { it[index] = probe })
                },
                modifier = Modifier.fillMaxWidth(),
            )
            FormationViewGrid(
                formation = editableFormation,
                scaleFormation = scaleFormation,
                selectedProbeIndex = selectedProbeIndex,
                onProbeSelected = { selectedProbeIndex = it },
                onProbesChange = updateProbes,
                onDragStarted = { scaleFormation = editableFormation },
                onDragStopped = { scaleFormation = null },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }

    formationToDelete?.let { formation ->
        val parentState = LocalRiftWindowState.current
        if (parentState != null) {
            RiftDialog(
                title = "Delete formation?",
                icon = Res.drawable.window_warning,
                parentState = parentState,
                state = rememberWindowState(width = 360.dp, height = Dp.Unspecified),
                onCloseClick = { formationToDelete = null },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Are you sure you want to delete “${formation.name}”?", style = RiftTheme.typography.bodyPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                        RiftButton(
                            text = "Cancel",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { formationToDelete = null },
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Delete",
                            type = ButtonType.Negative,
                            onClick = {
                                workingFormations = workingFormations - formation
                                if (selectedFormationName == formation.name) {
                                    selectedFormationName = workingFormations.firstOrNull()?.name
                                }
                                formationToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormationViewGrid(
    formation: ProbeFormation,
    scaleFormation: ProbeFormation?,
    selectedProbeIndex: Int,
    onProbeSelected: (Int) -> Unit,
    onProbesChange: (List<Probe>) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier,
) {
    val onHoveredProbeChange: (Int?) -> Unit = { it?.let(onProbeSelected) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.weight(1f).fillMaxWidth()) {
            FixedFormationView(
                title = "Top",
                formation = formation,
                scaleFormation = scaleFormation,
                projection = Projection.Top,
                selectedProbeIndex = selectedProbeIndex,
                onHoveredProbeChange = onHoveredProbeChange,
                onProbesChange = onProbesChange,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            FixedFormationView(
                title = "Front",
                formation = formation,
                scaleFormation = scaleFormation,
                projection = Projection.Front,
                selectedProbeIndex = selectedProbeIndex,
                onHoveredProbeChange = onHoveredProbeChange,
                onProbesChange = onProbesChange,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.weight(1f).fillMaxWidth()) {
            FixedFormationView(
                title = "Side",
                formation = formation,
                scaleFormation = scaleFormation,
                projection = Projection.Left,
                selectedProbeIndex = selectedProbeIndex,
                onHoveredProbeChange = onHoveredProbeChange,
                onProbesChange = onProbesChange,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            InteractiveFormationView(
                formation = formation,
                scaleFormation = scaleFormation,
                selectedProbeIndex = selectedProbeIndex,
                onHoveredProbeChange = onHoveredProbeChange,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun FormationList(
    formations: List<ProbeFormation>,
    selectedFormation: ProbeFormation,
    onCreateClick: () -> Unit,
    onSelect: (ProbeFormation) -> Unit,
    onRename: (ProbeFormation) -> Unit,
    onDuplicate: (ProbeFormation) -> Unit,
    onDelete: (ProbeFormation) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.fillMaxHeight().width(200.dp),
    ) {
        RiftButton(text = "Create formation", onClick = onCreateClick, modifier = Modifier.fillMaxWidth())
        ScrollbarLazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(formations.size) { index ->
                val formation = formations[index]
                FormationListItem(
                    formation = formation,
                    isSelected = formation == selectedFormation,
                    onSelect = { onSelect(formation) },
                    onRename = { onRename(formation) },
                    onDuplicate = { onDuplicate(formation) },
                    onDelete = { onDelete(formation) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormationListItem(
    formation: ProbeFormation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .hoverBackground(isSelected = isSelected)
            .onClick { onSelect() }
            .padding(Spacing.medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(formation.name, style = RiftTheme.typography.bodyPrimary)
            Text("${formation.probes.size} probes", style = RiftTheme.typography.detailSecondary)
        }
        RiftTooltipArea("Rename formation") {
            RiftImageButton(resource = Res.drawable.editplanicon, size = 20.dp, onClick = onRename)
        }
        RiftTooltipArea("Duplicate formation") {
            RiftImageButton(resource = Res.drawable.copy_16px, size = 16.dp, onClick = onDuplicate)
        }
        RiftTooltipArea("Delete formation") {
            RiftImageButton(resource = Res.drawable.deleteicon, size = 20.dp, onClick = onDelete)
        }
    }
}

private fun createProbeAtFormationCenter(probes: List<Probe>, selectedProbeIndex: Int): Probe {
    val selectedProbe = probes[selectedProbeIndex.coerceIn(probes.indices)]
    return Probe(
        x = probes.map { it.x }.average(),
        y = probes.map { it.y }.average(),
        z = probes.map { it.z }.average(),
        scanRadius = selectedProbe.scanRadius,
    )
}

private fun createBalancingProbe(probes: List<Probe>, selectedProbeIndex: Int): Probe {
    val selectedProbe = probes[selectedProbeIndex.coerceIn(probes.indices)]
    return Probe(
        x = -probes.sumOf { it.x },
        y = -probes.sumOf { it.y },
        z = -probes.sumOf { it.z },
        scanRadius = selectedProbe.scanRadius,
    )
}

@Composable
private fun ProbeValuesEditor(
    probes: List<Probe>,
    selectedProbeIndex: Int,
    onProbeSelected: (Int) -> Unit,
    onProbeAdded: () -> Unit,
    onProbeRemoved: () -> Unit,
    onFormationBalanced: () -> Unit,
    onProbeChanged: (Int, Probe) -> Unit,
    modifier: Modifier,
) {
    val index = selectedProbeIndex.coerceIn(probes.indices)
    val probe = probes[index]
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LabeledDropdown(
                label = "Probe",
                items = probes.indices.toList(),
                selectedItem = index,
                onItemSelected = onProbeSelected,
                getItemName = { "Probe ${it + 1}" },
                modifier = Modifier.weight(1f),
            )
            RiftTooltipArea(if (probes.size > 1) "Remove probe" else "A formation must have at least one probe") {
                RiftIconButton(
                    icon = Res.drawable.buttoniconminus,
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.BottomLeft,
                    isEnabled = probes.size > 1,
                    onClick = onProbeRemoved,
                )
            }
            RiftTooltipArea(if (probes.size < MAX_PROBES) "Add probe" else "A formation can have at most $MAX_PROBES probes") {
                RiftIconButton(
                    icon = Res.drawable.buttoniconplus,
                    isEnabled = probes.size < MAX_PROBES,
                    onClick = onProbeAdded,
                )
            }
            CoordinateField("X", Color.Red, probe.x, { probe.copy(x = it) }, onChanged = { onProbeChanged(index, it) }, Modifier.weight(1f))
            CoordinateField("Y", Color.Green, probe.y, { probe.copy(y = it) }, onChanged = { onProbeChanged(index, it) }, Modifier.weight(1f))
            CoordinateField("Z", Color.Blue, probe.z, { probe.copy(z = it) }, onChanged = { onProbeChanged(index, it) }, Modifier.weight(1f))
            val selectedRadius = ProbeRadius.entries.minBy { abs(it.au * ASTRONOMICAL_UNIT - probe.scanRadius) }
            LabeledDropdown(
                label = "Radius",
                items = ProbeRadius.entries,
                selectedItem = selectedRadius,
                onItemSelected = { onProbeChanged(index, probe.copy(scanRadius = it.au * ASTRONOMICAL_UNIT)) },
                getItemName = { "${it.label} AU" },
                modifier = Modifier.weight(1f),
            )
        }
        if (!isFormationBalanced(probes)) {
            val averageX = probes.map { it.x }.average()
            val averageY = probes.map { it.y }.average()
            val averageZ = probes.map { it.z }.average()
            val averageOffset = sqrt(averageX * averageX + averageY * averageY + averageZ * averageZ)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val canAddBalancingProbe = probes.size < MAX_PROBES
                RiftTooltipArea(
                    if (canAddBalancingProbe) {
                        "Add a counterbalance probe to correct the center of mass of the formation"
                    } else {
                        "Cannot balance the formation because it already has $MAX_PROBES probes.\nRemove one probe to add a counterbalance probe."
                    },
                ) {
                    RiftIconButton(
                        icon = Res.drawable.mass_32px,
                        isEnabled = canAddBalancingProbe,
                        cornerCut = ButtonCornerCut.BottomLeft,
                        onClick = onFormationBalanced,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "The center of mass ",
                        style = RiftTheme.typography.detailSecondary,
                    )
                    Canvas(Modifier.size(8.dp)) {
                        drawCircle(
                            color = averageProbePositionColor,
                            radius = 4.dp.toPx(),
                            center = center,
                        )
                    }
                    Text(
                        text = " is offset from the formation center by ${formatAu(averageOffset)} AU. This is the position of your ship when you launch this formation.",
                        style = RiftTheme.typography.detailSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> LabeledDropdown(
    label: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    getItemName: (T) -> String,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, style = RiftTheme.typography.detailSecondary)
        RiftDropdown(items, selectedItem, onItemSelected, getItemName, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CoordinateField(
    label: String,
    labelColor: Color,
    value: Double,
    update: (Double) -> Probe,
    onChanged: (Probe) -> Unit,
    modifier: Modifier,
) {
    var text by remember { mutableStateOf(formatAu(value)) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = formatAu(value)
    }
    Column(modifier) {
        Text("$label (AU)", color = labelColor, style = RiftTheme.typography.detailSecondary.copy(fontWeight = FontWeight.Bold))
        RiftTextField(
            text = text,
            onTextChanged = { newText ->
                text = newText
                newText.toDoubleOrNull()?.let { onChanged(update(it * ASTRONOMICAL_UNIT)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
        )
    }
}

private enum class ProbeRadius(val au: Double, val label: String) {
    Quarter(0.25, "0.25"),
    Half(0.5, "0.5"),
    One(1.0, "1"),
    Two(2.0, "2"),
    Four(4.0, "4"),
    Eight(8.0, "8"),
    Sixteen(16.0, "16"),
    ThirtyTwo(32.0, "32"),
}

private const val ASTRONOMICAL_UNIT = 149_597_870_700.0
private const val MAX_PROBES = 8
private const val MAX_FORMATION_NAME_LENGTH = 16
private const val FORMATION_IMBALANCE_THRESHOLD_METERS = 1_000_000.0
private const val DISTANCE_CIRCLE_SEGMENTS = 96
private val averageProbePositionColor = Color(0xFFFFB74D)
private val distanceCircleRadii = listOf(2.0, 8.0, 16.0, 32.0).map { it * ASTRONOMICAL_UNIT }

private fun formatAu(value: Double): String = String.format(Locale.US, "%.4f", value / ASTRONOMICAL_UNIT)

private fun getVisibleDistanceCircleRadii(requiredDistance: Double): List<Double> {
    val coveringCircleIndex = distanceCircleRadii.indexOfFirst { it >= requiredDistance }
    return if (coveringCircleIndex >= 0) {
        distanceCircleRadii.take(coveringCircleIndex + 1)
    } else {
        distanceCircleRadii
    }
}

private fun isFormationBalanced(probes: List<Probe>): Boolean {
    val averageX = probes.map { it.x }.average()
    val averageY = probes.map { it.y }.average()
    val averageZ = probes.map { it.z }.average()
    val averageOffset = sqrt(averageX * averageX + averageY * averageY + averageZ * averageZ)
    return averageOffset < FORMATION_IMBALANCE_THRESHOLD_METERS
}

private fun getDuplicateFormationName(name: String, formations: List<ProbeFormation>): String {
    val existingNames = formations.map { it.name.lowercase() }.toSet()
    var copyNumber = 1
    while (true) {
        val suffix = if (copyNumber == 1) " copy" else " copy $copyNumber"
        val candidate = name.take(MAX_FORMATION_NAME_LENGTH - suffix.length) + suffix
        if (candidate.lowercase() !in existingNames) return candidate
        copyNumber++
    }
}

private fun createDefaultFormation(name: String): ProbeFormation {
    val offset = 2.0 * ASTRONOMICAL_UNIT
    val radius = 4.0 * ASTRONOMICAL_UNIT
    val probes = listOf(
        Probe(-offset, -offset, -offset, radius),
        Probe(-offset, -offset, offset, radius),
        Probe(-offset, offset, -offset, radius),
        Probe(-offset, offset, offset, radius),
        Probe(offset, -offset, -offset, radius),
        Probe(offset, -offset, offset, radius),
        Probe(offset, offset, -offset, radius),
        Probe(offset, offset, offset, radius),
    )
    return ProbeFormation(name, probes)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InteractiveFormationView(
    formation: ProbeFormation,
    scaleFormation: ProbeFormation?,
    selectedProbeIndex: Int,
    onHoveredProbeChange: (Int?) -> Unit,
    modifier: Modifier,
) {
    var yaw by remember(formation.name) { mutableStateOf(-0.75f) }
    var pitch by remember(formation.name) { mutableStateOf(0.55f) }
    val extentFormation = scaleFormation ?: formation
    val visibleDistanceCircleRadii = getVisibleDistanceCircleRadii(
        extentFormation.probes.maxOf { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) },
    )
    val formationExtent = extentFormation.probes.maxOf { probe ->
        sqrt(probe.x * probe.x + probe.y * probe.y + probe.z * probe.z) + probe.scanRadius
    }
    val fixedExtent = formationExtent
    val hitRadius = with(LocalDensity.current) { 10.dp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val projection = { probe: Probe -> projectRotated(probe, yaw, pitch) }
    val canvasModifier = Modifier
        .onSizeChanged { canvasSize = it }
        .onPointerEvent(PointerEventType.Move) { event ->
            val pointer = event.changes.first().position
            val index = findProbeAt(pointer, formation, projection, canvasSize, fixedExtent, hitRadius)
            onHoveredProbeChange(index)
        }
        .onPointerEvent(PointerEventType.Exit) { onHoveredProbeChange(null) }
    FormationView(
        title = "3D · drag view to rotate",
        formation = formation,
        projection = projection,
        axes = listOf(
            AxisIndicator(Color.Red, projectRotated(Probe(1.0, 0.0, 0.0, 0.0), yaw, pitch)),
            AxisIndicator(Color.Green, projectRotated(Probe(0.0, 1.0, 0.0, 0.0), yaw, pitch)),
            AxisIndicator(Color.Blue, projectRotated(Probe(0.0, 0.0, 1.0, 0.0), yaw, pitch)),
        ),
        fixedExtent = fixedExtent,
        distanceCircleRadii = visibleDistanceCircleRadii,
        distanceCirclePlane = DistanceCirclePlane.XZ,
        selectedProbeIndex = selectedProbeIndex,
        canvasModifier = canvasModifier,
        modifier = modifier
            .pointerHoverIcon(PointerIcon(Cursors.move))
            .pointerInput(formation) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yaw += dragAmount.x * 0.01f
                    pitch = (pitch - dragAmount.y * 0.01f).coerceIn(-1.5f, 1.5f)
                }
            },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FixedFormationView(
    title: String,
    formation: ProbeFormation,
    scaleFormation: ProbeFormation?,
    projection: Projection,
    selectedProbeIndex: Int,
    onHoveredProbeChange: (Int?) -> Unit,
    onProbesChange: (List<Probe>) -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier,
) {
    val currentFormation by rememberUpdatedState(formation)
    val currentOnProbesChange by rememberUpdatedState(onProbesChange)
    val currentOnDragStarted by rememberUpdatedState(onDragStarted)
    val currentOnDragStopped by rememberUpdatedState(onDragStopped)
    val hitRadius = with(LocalDensity.current) { 10.dp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isHoveringProbe by remember { mutableStateOf(false) }
    val isCtrlPressed = remember { mutableStateOf(false) }
    val isAltPressed = remember { mutableStateOf(false) }
    val isShiftPressed = remember { mutableStateOf(false) }
    val snapRadiusPreview = remember { mutableStateOf<List<Double?>?>(null) }
    val radiusDragFormation = remember { mutableStateOf<ProbeFormation?>(null) }
    val radiusDragDelta = remember { mutableStateOf(0.0) }
    var draggedProbeIndex by remember { mutableStateOf<Int?>(null) }
    var dragScale by remember { mutableStateOf<Double?>(null) }
    val extentFormation = scaleFormation ?: formation
    val visibleDistanceCircleRadii = getVisibleDistanceCircleRadii(
        extentFormation.probes.maxOf {
            val point = projection.project(it)
            sqrt(point.first * point.first + point.second * point.second)
        },
    )
    val viewExtent = getProjectionExtent(extentFormation, projection::project)
    val currentViewExtent by rememberUpdatedState(viewExtent)
    val currentCanvasSize by rememberUpdatedState(canvasSize)
    val findProbe: (Offset) -> Int? = { pointer ->
        findProbeAt(pointer, formation, projection::project, canvasSize, viewExtent, hitRadius)
    }
    val interactionModifier = Modifier
        .onSizeChanged { canvasSize = it }
        .onPointerEvent(PointerEventType.Move) { event ->
            isCtrlPressed.value = event.keyboardModifiers.isCtrlPressed
            isAltPressed.value = event.keyboardModifiers.isAltPressed
            isShiftPressed.value = event.keyboardModifiers.isShiftPressed
            val index = findProbe(event.changes.first().position)
            isHoveringProbe = index != null
            onHoveredProbeChange(index)
        }
        .onPointerEvent(PointerEventType.Exit) {
            isHoveringProbe = false
            isCtrlPressed.value = false
            isAltPressed.value = false
            isShiftPressed.value = false
            onHoveredProbeChange(null)
        }
        .pointerHoverIcon(PointerIcon(if (isHoveringProbe) Cursors.move else Cursors.pointer))
        .pointerInput(projection) {
            detectDragGestures(
                onDragStart = { position ->
                    snapRadiusPreview.value = null
                    radiusDragFormation.value = null
                    radiusDragDelta.value = 0.0
                    draggedProbeIndex = findProbeAt(
                        pointer = position,
                        formation = currentFormation,
                        projection = projection::project,
                        size = currentCanvasSize,
                        extent = currentViewExtent,
                        hitRadius = hitRadius,
                    )
                    if (draggedProbeIndex != null) {
                        dragScale = getScale(currentCanvasSize, currentViewExtent)
                        currentOnDragStarted()
                    }
                },
                onDragEnd = {
                    snapRadiusPreview.value?.let { snappedRadii ->
                        currentOnProbesChange(
                            currentFormation.probes.mapIndexed { index, probe ->
                                snappedRadii[index]?.let { probe.copy(scanRadius = it) } ?: probe
                            },
                        )
                    }
                    snapRadiusPreview.value = null
                    radiusDragFormation.value = null
                    radiusDragDelta.value = 0.0
                    draggedProbeIndex = null
                    dragScale = null
                    currentOnDragStopped()
                },
                onDragCancel = {
                    snapRadiusPreview.value = null
                    radiusDragFormation.value = null
                    radiusDragDelta.value = 0.0
                    draggedProbeIndex = null
                    dragScale = null
                    currentOnDragStopped()
                },
            ) { change, dragAmount ->
                val index = draggedProbeIndex ?: return@detectDragGestures
                change.consume()
                val scale = dragScale ?: getScale(currentCanvasSize, currentViewExtent)
                if (scale > 0.0) {
                    val probes: List<Probe>? = when {
                        isShiftPressed.value || isAltPressed.value -> {
                            val dragFormation = radiusDragFormation.value ?: currentFormation.also {
                                radiusDragFormation.value = it
                            }
                            radiusDragDelta.value += getRadialDragDistance(
                                formation = dragFormation,
                                probeIndex = index,
                                projection = projection::project,
                                dragAmount = dragAmount,
                                projectionScale = scale,
                            )
                            val changedProbes = changeProbeRadii(
                                formation = dragFormation,
                                probeIndex = index,
                                radiusDelta = radiusDragDelta.value,
                                changeOnlySelectedProbe = isShiftPressed.value,
                            )
                            snapRadiusPreview.value = changedProbes.mapIndexed { probeIndex, probe ->
                                if (!isShiftPressed.value || probeIndex == index) {
                                    getClosestLegalProbeRadius(probe.scanRadius)
                                } else {
                                    null
                                }
                            }
                            null
                        }
                        isCtrlPressed.value -> {
                            scaleFormationFromProbe(
                                formation = currentFormation,
                                probeIndex = index,
                                projection = projection::project,
                                dragAmount = dragAmount,
                                projectionScale = scale,
                            )
                        }
                        else -> {
                            currentFormation.probes.toMutableList().also {
                                it[index] = projection.drag(it[index], dragAmount.x / scale, -dragAmount.y / scale)
                            }
                        }
                    }
                    probes?.let {
                        currentOnProbesChange(it)
                        onHoveredProbeChange(index)
                    }
                }
            }
        }
    FormationView(
        title = "$title · drag probe to move",
        formation = formation,
        projection = projection::project,
        axes = projection.axes,
        fixedExtent = viewExtent,
        selectedProbeIndex = selectedProbeIndex,
        showScaleHint = isHoveringProbe,
        helperRadii = snapRadiusPreview.value,
        distanceCircleRadii = visibleDistanceCircleRadii,
        distanceCirclePlane = projection.distanceCirclePlane,
        canvasModifier = interactionModifier,
        modifier = modifier,
    )
}

@Composable
private fun FormationView(
    title: String,
    formation: ProbeFormation,
    projection: (Probe) -> Pair<Double, Double>,
    axes: List<AxisIndicator>,
    fixedExtent: Double? = null,
    selectedProbeIndex: Int,
    showScaleHint: Boolean = false,
    helperRadii: List<Double?>? = null,
    distanceCircleRadii: List<Double> = emptyList(),
    distanceCirclePlane: DistanceCirclePlane? = null,
    canvasModifier: Modifier = Modifier,
    modifier: Modifier,
) {
    val borderColor = RiftTheme.colors.borderGrey
    val probeColor = RiftTheme.colors.primary
    val distanceCircleColor = RiftTheme.colors.divider
    Box(
        modifier = modifier
            .background(RiftTheme.colors.windowBackgroundSecondary)
            .border(1.dp, borderColor)
    ) {
        Canvas(Modifier.fillMaxSize().clipToBounds().padding(Spacing.medium).then(canvasModifier)) {
            val points = formation.probes.map { probe ->
                projection(probe) to probe.scanRadius
            }
            val extent = (fixedExtent ?: points.maxOf { (point, radius) ->
                max(abs(point.first), abs(point.second)) + radius
            }).coerceAtLeast(1.0)
            val scale = minOf(size.width, size.height).toDouble() * 0.42 / extent
            val origin = Offset(size.width / 2, size.height / 2)
            distanceCirclePlane?.let { plane ->
                distanceCircleRadii.forEach { radius ->
                    val path = Path()
                    repeat(DISTANCE_CIRCLE_SEGMENTS + 1) { index ->
                        val angle = index.toDouble() / DISTANCE_CIRCLE_SEGMENTS * 2.0 * Math.PI
                        val point = projection(plane.point(radius, angle))
                        val position = Offset(
                            x = origin.x + (point.first * scale).toFloat(),
                            y = origin.y - (point.second * scale).toFloat(),
                        )
                        if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
                    }
                    drawPath(
                        path = path,
                        color = distanceCircleColor.copy(alpha = 0.6f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            val centeredAxisLength = minOf(size.width, size.height) * 0.42f
            axes.forEach { axis ->
                val offset = Offset(
                    x = axis.direction.first.toFloat() * centeredAxisLength,
                    y = -axis.direction.second.toFloat() * centeredAxisLength,
                )
                drawLine(
                    color = axis.color.copy(alpha = 0.65f),
                    start = origin - offset,
                    end = origin + offset,
                    strokeWidth = 1.dp.toPx(),
                )
                val offsetLength = offset.getDistance()
                if (offsetLength > 0f) {
                    val direction = Offset(offset.x / offsetLength, offset.y / offsetLength)
                    val perpendicular = Offset(-direction.y, direction.x)
                    val arrowTip = origin + offset
                    val arrowBase = arrowTip - direction * 8.dp.toPx()
                    val arrowHalfWidth = 4.dp.toPx()
                    drawLine(
                        color = axis.color.copy(alpha = 0.65f),
                        start = arrowTip,
                        end = arrowBase + perpendicular * arrowHalfWidth,
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = axis.color.copy(alpha = 0.65f),
                        start = arrowTip,
                        end = arrowBase - perpendicular * arrowHalfWidth,
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            if (!isFormationBalanced(formation.probes)) {
                val averageProbe = Probe(
                    x = formation.probes.map { it.x }.average(),
                    y = formation.probes.map { it.y }.average(),
                    z = formation.probes.map { it.z }.average(),
                    scanRadius = 0.0,
                )
                val averagePoint = projection(averageProbe)
                val averagePosition = Offset(
                    x = origin.x + (averagePoint.first * scale).toFloat(),
                    y = origin.y - (averagePoint.second * scale).toFloat(),
                )
                drawCircle(
                    color = averageProbePositionColor,
                    radius = 4.dp.toPx(),
                    center = averagePosition,
                )
            }
            points.forEachIndexed { index, (point, radius) ->
                val position = Offset(
                    x = origin.x + (point.first * scale).toFloat(),
                    y = origin.y - (point.second * scale).toFloat(),
                )
                val radiusPx = (radius * scale).toFloat()
                if (radiusPx > 0f) {
                    val highlightOffset = Offset(radiusPx * 0.3f, radiusPx * 0.3f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to probeColor.copy(alpha = 0.12f),
                                0.45f to probeColor.copy(alpha = 0.07f),
                                1f to probeColor.copy(alpha = 0.012f),
                            ),
                            center = position - highlightOffset,
                            radius = radiusPx * 1.3f,
                        ),
                        radius = radiusPx,
                        center = position,
                    )
                    drawCircle(
                        color = probeColor.copy(alpha = 0.3f),
                        radius = radiusPx,
                        center = position,
                        style = Stroke(1.dp.toPx()),
                    )
                }
                drawCircle(probeColor, 4.dp.toPx(), position)
                if (index == selectedProbeIndex) {
                    drawCircle(Color.White, 9.dp.toPx(), position, style = Stroke(2.dp.toPx()))
                }
                helperRadii?.getOrNull(index)?.let { helperRadius ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = (helperRadius * scale).toFloat(),
                        center = position,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                        ),
                    )
                }
            }
        }
        Text(
            text = title,
            style = RiftTheme.typography.detailPrimary,
            modifier = Modifier.align(Alignment.TopStart).padding(Spacing.small),
        )
        if (showScaleHint) {
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.small)) {
                KeyName(
                    name = "Hold",
                    key = "CTRL",
                    suffix = "to scale",
                    style = RiftTheme.typography.detailPrimary,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
                KeyName(
                    name = "Hold",
                    key = "ALT",
                    suffix = "to change all radii",
                    style = RiftTheme.typography.detailPrimary,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
                KeyName(
                    name = "Hold",
                    key = "SHIFT",
                    suffix = "to change radius",
                    style = RiftTheme.typography.detailPrimary,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}

private fun scaleFormationFromProbe(
    formation: ProbeFormation,
    probeIndex: Int,
    projection: (Probe) -> Pair<Double, Double>,
    dragAmount: Offset,
    projectionScale: Double,
): List<Probe> {
    val probe = formation.probes[probeIndex]
    val projectedOffset = projection(probe)
    val screenOffset = Offset(
        x = (projectedOffset.first * projectionScale).toFloat(),
        y = (-projectedOffset.second * projectionScale).toFloat(),
    )
    val lengthSquared = screenOffset.x * screenOffset.x + screenOffset.y * screenOffset.y
    if (lengthSquared == 0f) return formation.probes
    val scaleFactor = (1f + (dragAmount.x * screenOffset.x + dragAmount.y * screenOffset.y) / lengthSquared)
        .coerceAtLeast(0.01f)
    return formation.probes.map {
        it.copy(
            x = it.x * scaleFactor,
            y = it.y * scaleFactor,
            z = it.z * scaleFactor,
        )
    }
}

private fun changeProbeRadii(
    formation: ProbeFormation,
    probeIndex: Int,
    radiusDelta: Double,
    changeOnlySelectedProbe: Boolean,
): List<Probe> {
    return formation.probes.mapIndexed { index, probe ->
        if (!changeOnlySelectedProbe || index == probeIndex) {
            probe.copy(scanRadius = (probe.scanRadius + radiusDelta).coerceAtLeast(0.0))
        } else {
            probe
        }
    }
}

private fun getClosestLegalProbeRadius(radius: Double): Double {
    return ProbeRadius.entries.minBy { abs(it.au * ASTRONOMICAL_UNIT - radius) }.au * ASTRONOMICAL_UNIT
}

private fun getRadialDragDistance(
    formation: ProbeFormation,
    probeIndex: Int,
    projection: (Probe) -> Pair<Double, Double>,
    dragAmount: Offset,
    projectionScale: Double,
): Double {
    val probe = formation.probes[probeIndex]
    val projectedOffset = projection(probe)
    val screenOffset = Offset(projectedOffset.first.toFloat(), -projectedOffset.second.toFloat())
    val screenDistance = screenOffset.getDistance()
    if (screenDistance == 0f || projectionScale == 0.0) return 0.0
    val direction = screenOffset / screenDistance
    return (dragAmount.x * direction.x + dragAmount.y * direction.y) / projectionScale
}

private data class AxisIndicator(
    val color: Color,
    val direction: Pair<Double, Double>,
)

private enum class DistanceCirclePlane {
    XY,
    XZ,
    ZY;

    fun point(radius: Double, angle: Double): Probe = when (this) {
        XY -> Probe(cos(angle) * radius, sin(angle) * radius, 0.0, 0.0)
        XZ -> Probe(cos(angle) * radius, 0.0, sin(angle) * radius, 0.0)
        ZY -> Probe(0.0, sin(angle) * radius, cos(angle) * radius, 0.0)
    }
}

private fun getProjectedProbes(
    formation: ProbeFormation,
    projection: (Probe) -> Pair<Double, Double>,
    size: IntSize,
    fixedExtent: Double? = null,
): List<Offset> {
    if (size == IntSize.Zero) return emptyList()
    val scale = fixedExtent
        ?.let { minOf(size.width, size.height).toDouble() * 0.42 / it.coerceAtLeast(1.0) }
        ?: getProjectionScale(formation, projection, size)
    return formation.probes.map { probe ->
        val point = projection(probe)
        Offset(
            x = size.width / 2f + (point.first * scale).toFloat(),
            y = size.height / 2f - (point.second * scale).toFloat(),
        )
    }
}

private fun findProbeAt(
    pointer: Offset,
    formation: ProbeFormation,
    projection: (Probe) -> Pair<Double, Double>,
    size: IntSize,
    extent: Double,
    hitRadius: Float,
): Int? {
    return getProjectedProbes(formation, projection, size, extent)
        .mapIndexed { index, position -> index to (position - pointer).getDistance() }
        .minByOrNull { it.second }
        ?.takeIf { it.second <= hitRadius }
        ?.first
}

private fun getProjectionScale(
    formation: ProbeFormation,
    projection: (Probe) -> Pair<Double, Double>,
    size: IntSize,
): Double {
    if (size == IntSize.Zero) return 0.0
    val extent = getProjectionExtent(formation, projection)
    return minOf(size.width, size.height).toDouble() * 0.42 / extent
}

private fun getScale(size: IntSize, extent: Double): Double {
    if (size == IntSize.Zero) return 0.0
    return minOf(size.width, size.height).toDouble() * 0.42 / extent.coerceAtLeast(1.0)
}

private fun getProjectionExtent(
    formation: ProbeFormation,
    projection: (Probe) -> Pair<Double, Double>,
): Double {
    return formation.probes.maxOf { probe ->
        val point = projection(probe)
        max(abs(point.first), abs(point.second)) + probe.scanRadius
    }.coerceAtLeast(1.0)
}

private fun projectRotated(probe: Probe, yaw: Float, pitch: Float): Pair<Double, Double> {
    val yawCos = cos(yaw.toDouble())
    val yawSin = sin(yaw.toDouble())
    val pitchCos = cos(pitch.toDouble())
    val pitchSin = sin(pitch.toDouble())
    val rotatedX = probe.x * yawCos - probe.z * yawSin
    val rotatedZ = probe.x * yawSin + probe.z * yawCos
    val rotatedY = probe.y * pitchCos - rotatedZ * pitchSin
    return rotatedX to rotatedY
}

private enum class Projection {
    Top,
    Front,
    Left;

    fun project(probe: Probe): Pair<Double, Double> = when (this) {
        Top -> probe.x to probe.z
        Front -> probe.x to probe.y
        Left -> probe.z to probe.y
    }

    fun drag(probe: Probe, horizontal: Double, vertical: Double): Probe = when (this) {
        Top -> probe.copy(x = probe.x + horizontal, z = probe.z + vertical)
        Front -> probe.copy(x = probe.x + horizontal, y = probe.y + vertical)
        Left -> probe.copy(z = probe.z + horizontal, y = probe.y + vertical)
    }

    val axes: List<AxisIndicator>
        get() = when (this) {
            Top -> listOf(
                AxisIndicator(Color.Red, 1.0 to 0.0),
                AxisIndicator(Color.Blue, 0.0 to 1.0),
            )
            Front -> listOf(
                AxisIndicator(Color.Red, 1.0 to 0.0),
                AxisIndicator(Color.Green, 0.0 to 1.0),
            )
            Left -> listOf(
                AxisIndicator(Color.Blue, 1.0 to 0.0),
                AxisIndicator(Color.Green, 0.0 to 1.0),
            )
        }

    val distanceCirclePlane: DistanceCirclePlane
        get() = when (this) {
            Top -> DistanceCirclePlane.XZ
            Front -> DistanceCirclePlane.XY
            Left -> DistanceCirclePlane.ZY
        }
}
