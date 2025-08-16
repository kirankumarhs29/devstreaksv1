// sharedMain/commonMain
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.dailydevchallenge.devstreaks.utils

import androidx.compose.runtime.Composable

expect object PlatformUtils {
    fun isAndroid(): Boolean
    fun pickPdfAndExtract(onExtracted: (String) -> Unit)
}
interface PdfPickerHandler {
    @Composable
    fun PickPdf(onTextExtracted: (String) -> Unit)
}
interface PdfTextExtractor {
    suspend fun extractText(pdfBytes: ByteArray): String
}

expect fun getPdfTextExtractor(): PdfTextExtractor

expect fun getPdfPickerHandler(): PdfPickerHandler

@Composable
expect fun SafeBackHandler(enabled: Boolean = true, onBack: () -> Unit)


