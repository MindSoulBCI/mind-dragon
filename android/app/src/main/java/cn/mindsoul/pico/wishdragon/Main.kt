package cn.mindsoul.pico.wishdragon
import cn.mindsoul.pico.wishdragon.ui.navigation.gameWindows
import cn.mindsoul.pico.wishdragon.ui.game.GameViewModelFactory
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
fun mainApp(scope:SpatialAppScope){gameWindows(scope,GameViewModelFactory().create())}
