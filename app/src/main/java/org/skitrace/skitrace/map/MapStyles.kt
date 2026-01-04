package org.skitrace.skitrace.map

object MapStyles {

    private const val SOURCE_BASE = "base_source"
    private const val SOURCE_PISTES = "pistes_source"

    private const val TILE_LIGHT_URL = "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
    private const val TILE_DARK_URL = "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"

    private const val TILE_PISTES_URL = "https://tiles.opensnowmap.org/pistes/{z}/{x}/{y}.png"

    fun getDynamicStyle(isDarkTheme: Boolean): String {

        val tileUrl: String
        val backgroundColor: String
        val pistesOpacity: Double

        if (isDarkTheme) {
            tileUrl = TILE_DARK_URL
            backgroundColor = "#262626"
            pistesOpacity = 0.85
        } else {
            tileUrl = TILE_LIGHT_URL
            backgroundColor = "#F0F0F0"
            pistesOpacity = 1.0
        }

        return """
        {
          "version": 8,
          "name": "SkiTrace Dynamic",
          "sources": {
            "$SOURCE_BASE": {
              "type": "raster",
              "tiles": ["$tileUrl"],
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