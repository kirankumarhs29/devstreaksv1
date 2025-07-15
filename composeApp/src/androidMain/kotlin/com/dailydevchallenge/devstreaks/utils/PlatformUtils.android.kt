package com.dailydevchallenge.devstreaks.utils

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import kotlinx.coroutines.launch

actual fun getPdfPickerHandler(): PdfPickerHandler = AndroidPdfPickerHandler


actual object PlatformUtils {
    actual fun isAndroid(): Boolean = true

    // No-op function for commonMain compatibility. Actual logic is in the composable below.
    actual fun pickPdfAndExtract(onExtracted: (String) -> Unit) {
        // This should be triggered from the Composable below
    }
}
@Composable
actual fun SafeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/**
 * This composable is used in your UI layer to trigger the Android PDF picker and extract text.
 */



object AndroidPdfPickerHandler : PdfPickerHandler {
    @Composable
    override fun PickPdf(onTextExtracted: (String) -> Unit) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val filePicker = rememberFilePickerLauncher(
            type = FilePickerFileType.Pdf,
            selectionMode = FilePickerSelectionMode.Single,
            onResult = { files ->
                val file = files.firstOrNull()
                if (file != null) {
                    coroutineScope.launch {
                        val fileName = file.getName(context) ?: "Unknown"
                        val fileBytes = file.readByteArray(context)
                        val fileSizeKb = fileBytes.size / 1024
                        val summary = "📄 $fileName ($fileSizeKb KB)"
                        onTextExtracted(summary)
                    }
                } else {
                    onTextExtracted("❌ No file selected")
                }
            }
        )

        LaunchedEffect(Unit) {
            filePicker.launch()
        }
    }
}

