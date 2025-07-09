// sharedMain/commonMain
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

expect fun getPdfPickerHandler(): PdfPickerHandler


