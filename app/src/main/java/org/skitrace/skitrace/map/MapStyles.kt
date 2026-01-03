package org.skitrace.skitrace.map

object MapStyles {

    private const val SOURCE_BASE = "base_source"
    private const val SOURCE_PISTES = "pistes_source"

    private const val TILE_BASE_URL = "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
    private const val TILE_PISTES_URL = "https://tiles.opensnowmap.org/pistes/{z}/{x}/{y}.png"

    fun getDynamicStyle(isDarkTheme: Boolean): String {


        val backgroundColor: String
        val baseBrightnessMax: Double
        val baseSaturation: Double
        val baseContrast: Double
        val pistesOpacity: Double

        if (isDarkTheme) {
            backgroundColor = "#121212"
            baseBrightnessMax = 0.6
            baseSaturation = -0.8
            baseContrast = 0.2
            pistesOpacity = 0.85
        } else {
            backgroundColor = "#F0F0F0"
            baseBrightnessMax = 1.0
            baseSaturation = 0.0
            baseContrast = 0.0
            pistesOpacity = 1.0
        }

        return """
        {
          "version": 8,
          "name": "SkiTrace Efficient",
          "sources": {
            "$SOURCE_BASE": {
              "type": "raster",
              "tiles": ["$TILE_BASE_URL"],
              "tileSize": 256,
              "attribution": "&copy; OpenStreetMap, &copy; CARTO"
            },
            "$SOURCE_PISTES": {
              "type": "raster",
              "tiles": ["$TILE_PISTES_URL"],
              "tileSize": 256,
              "attribution": "&copy; OpenSnowMap"
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "$backgroundColor"
              }
            },
            {
              "id": "base_layer",
              "type": "raster",
              "source": "$SOURCE_BASE",
              "paint": {
                "raster-opacity": 1.0,
                "raster-brightness-max": $baseBrightnessMax,
                "raster-saturation": $baseSaturation,
                "raster-contrast": $baseContrast,
                "raster-fade-duration": 300
              }
            },
            {
              "id": "pistes_layer",
              "type": "raster",
              "source": "$SOURCE_PISTES",
              "paint": {
                "raster-opacity": $pistesOpacity,
                "raster-fade-duration": 300
              }
            }
          ]
        }
        """.trimIndent()
    }
}