package com.dailydevchallenge.devstreaks.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.*
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.PDFKit.*
import kotlinx.cinterop.*
import platform.Foundation.dataWithBytes


@Composable
actual fun SafeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS
}

actual fun getPdfPickerHandler(): PdfPickerHandler = CalfPdfPickerHandler

actual fun getPdfTextExtractor(): PdfTextExtractor = IosPdfTextExtractor()

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

class IosPdfTextExtractor : PdfTextExtractor {
    override suspend fun extractText(pdfBytes: ByteArray): String {
        val nsData = pdfBytes.toNSData1()
        val doc = PDFDocument(nsData)
        val pageCount = doc.pageCount.toInt()
        val result = StringBuilder()
        for (i in 0 until pageCount) {
            val page = doc.pageAtIndex(i.toULong())
            val text = page?.string()
            if (text != null) result.append(text).append("\n")
        }
        return result.toString()
    }
}
@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData1(): NSData = this.usePinned {
    NSData.dataWithBytes(it.addressOf(0), size.toULong())
}