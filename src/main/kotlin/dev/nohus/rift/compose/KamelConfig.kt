package dev.nohus.rift.compose

import io.kamel.core.config.Core
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.animatedImageDecoder
import io.kamel.image.config.imageBitmapDecoder
import io.kamel.image.config.resourcesFetcher

val kamelConfig = KamelConfig {
    takeFrom(KamelConfig.Core)
    resourcesFetcher()
    imageBitmapDecoder()
    animatedImageDecoder()
    imageBitmapCacheSize = 1000
}
