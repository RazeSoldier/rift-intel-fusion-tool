package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.file_chooser_button_select
import dev.nohus.rift.windowing.LocalRiftWindow
import org.jetbrains.compose.resources.stringResource
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun RiftFileChooserButton(
    text: String = stringResource(Res.string.file_chooser_button_select),
    fileSelectionMode: Int = JFileChooser.FILES_ONLY,
    typesDescription: String,
    extensions: List<String> = emptyList(),
    currentPath: String? = null,
    type: ButtonType = ButtonType.Primary,
    cornerCut: ButtonCornerCut = ButtonCornerCut.BottomRight,
    onFileChosen: (Path) -> Unit,
) {
    val frame = LocalRiftWindow.current ?: return
    RiftButton(
        text = text,
        type = type,
        cornerCut = cornerCut,
        onClick = {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val chooser = JFileChooser(currentPath)
            chooser.fileSelectionMode = fileSelectionMode
            if (extensions.isNotEmpty()) {
                chooser.fileFilter = FileNameExtensionFilter(typesDescription, *extensions.toTypedArray())
            }
            val returnValue = chooser.showOpenDialog(frame)
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    onFileChosen(chooser.selectedFile.toPath())
                } catch (_: InvalidPathException) {
                    // No valid path selected, ignore
                }
            }
        },
    )
}
