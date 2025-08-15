package com.dailydevchallenge.devstreaks.config

import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.imageBitmapDecoder

object ImageCacheConfig {
    val enhancedKamelConfig = KamelConfig {
        takeFrom(KamelConfig.Default)

        // Add bitmap decoder for better image processing
        imageBitmapDecoder()
    }
}
