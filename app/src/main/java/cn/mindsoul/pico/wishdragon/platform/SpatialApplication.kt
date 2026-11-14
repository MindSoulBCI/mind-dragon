package cn.mindsoul.pico.wishdragon.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import cn.mindsoul.pico.wishdragon.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
