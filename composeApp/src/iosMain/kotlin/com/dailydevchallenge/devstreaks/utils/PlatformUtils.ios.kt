package com.dailydevchallenge.devstreaks.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.*
import kotlinx.coroutines.launch

@Composable
actual fun SafeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS
}

actual fun getPdfPickerHandler(): PdfPickerHandler = CalfPdfPickerHandler

object CalfPdfPickerHandler : PdfPickerHandler {
    @Composable
    override fun PickPdf(onTextExtracted: (String) -> Unit) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalPlatformContext.current
        val launcher = rememberFilePickerLauncher(
            type = FilePickerFileType.Pdf,
            selectionMode = FilePickerSelectionMode.Single,
            onResult = { files ->
                val file = files.firstOrNull()
                if (file != null) {
                    coroutineScope.launch {
                        // iOS and Android both support these suspend functions
                        val fileName = file.getName(context) ?: "Unknown"
                        val fileBytes = file.readByteArray(context)
                        val fileSizeKb = fileBytes.size / 1024
                        val summary = "📄 $fileName ($fileSizeKb KB)"
                        onTextExtracted(summary)

                        // You can now use fileBytes to upload/analyze
                    }
                } else {
                    onTextExtracted("❌ No file selected")
                }
            }
        )

        LaunchedEffect(Unit) {
            launcher.launch()
        }
    }
}