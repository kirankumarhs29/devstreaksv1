package com.dailydevchallenge.devstreaks.utils

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import kotlinx.coroutines.launch
//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

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

actual fun getPdfTextExtractor(): PdfTextExtractor = AndroidPdfTextExtractor()

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
                        val text = getPdfTextExtractor().extractText(fileBytes)
                        val summary = "📄 $fileName ($fileSizeKb KB)"
                        Log.d("PDF", "Extracted text: $text")

                        onTextExtracted(text)
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
class AndroidPdfTextExtractor : PdfTextExtractor {
    override suspend fun extractText(pdfBytes: ByteArray): String {
        return try {
            PDDocument.load(ByteArrayInputStream(pdfBytes)).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } catch (e: Exception) {
            "❌ Failed to extract text from PDF: ${e.message}"
        }
    }
}
