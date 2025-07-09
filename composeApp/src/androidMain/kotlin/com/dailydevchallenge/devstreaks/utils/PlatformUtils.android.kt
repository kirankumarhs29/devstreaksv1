package com.dailydevchallenge.devstreaks.utils

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.InputStream
//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.text.PDFTextStripper

actual fun getPdfPickerHandler(): PdfPickerHandler = AndroidPdfPickerHandler


actual object PlatformUtils {
    actual fun isAndroid(): Boolean = true

    // No-op function for commonMain compatibility. Actual logic is in the composable below.
    actual fun pickPdfAndExtract(onExtracted: (String) -> Unit) {
        // This should be triggered from the Composable below
    }
}

/**
 * This composable is used in your UI layer to trigger the Android PDF picker and extract text.
 */


private fun extractTextFromPdf(inputStream: InputStream?): String {
//    var document: PDDocument? = null
//    return try {
//        document = PDDocument.load(inputStream)
//        val text = PDFTextStripper().getText(document)
//        text
//    } catch (e: Exception) {
//        "❌ Failed to read PDF: ${e.message}"
//    } finally {
//        try { document?.close() } catch (_: Exception) {}
//    }
    return "❌ Failed to read PDF: "
}


object AndroidPdfPickerHandler : PdfPickerHandler {
    @Composable
    override fun PickPdf(onTextExtracted: (String) -> Unit) {
        LaunchPdfPicker(onTextExtracted)
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun LaunchPdfPicker(onExtracted: (String) -> Unit) {
    val context = LocalContext.current as ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val contentResolver = context.contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    onExtracted("❌ Could not open PDF")
                    return@launch
                }
                val text = extractTextFromPdf(inputStream)
                onExtracted(text)
            }
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/pdf"))
    }
}
