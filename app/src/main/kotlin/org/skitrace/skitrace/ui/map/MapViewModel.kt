package org.skitrace.skitrace.ui.map

import androidx.lifecycle.ViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

class MapViewModel : ViewModel() {
    val cameraState = CameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 58.86441106582072, latitude = 57.58123541662825),
            zoom = 12.0
        )
    )
}