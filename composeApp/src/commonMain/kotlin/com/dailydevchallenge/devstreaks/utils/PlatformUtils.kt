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

expect fun getPdfPickerHandler(): PdfPickerHandler

@Composable
expect fun SafeBackHandler(enabled: Boolean = true, onBack: () -> Unit)


