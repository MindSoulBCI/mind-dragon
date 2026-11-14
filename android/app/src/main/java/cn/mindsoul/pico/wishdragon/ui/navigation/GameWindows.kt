package cn.mindsoul.pico.wishdragon.ui.navigation
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.foundation.dsl.*
import com.pico.spatial.ui.design.PicoTheme
import cn.mindsoul.pico.wishdragon.ui.game.*
fun gameWindows(scope:SpatialAppScope,vm:GameViewModel)=with(scope){
 DefaultWindowContainer { PicoTheme { GameScreen(vm,"home") } }
 Stage("GameStage") { PicoTheme { GameScreen(vm,"stage") } }
 WindowContainer("DragonWishes",form=Form.Planar,defaultSize=WindowContainerSize(760.dp,480.dp),
 placement={containers.firstOrNull{it.id=="GameHome"}?.let{Placement.top(it,64.dp)}?:Placement.none()}){PicoTheme{GameScreen(vm,"wishes")}}
 WindowContainer("WishProgress",form=Form.Planar,defaultSize=WindowContainerSize(620.dp,280.dp),
 placement={containers.firstOrNull{it.id=="GameHome"}?.let{Placement.bottom(it,64.dp)}?:Placement.none()}){PicoTheme{GameScreen(vm,"progress")}}
 WindowContainer("Devices",form=Form.Planar,defaultSize=WindowContainerSize(480.dp,720.dp),
 placement={containers.firstOrNull{it.id=="GameHome"}?.let{Placement.left(it,120.dp)}?:Placement.none()}){PicoTheme{GameScreen(vm,"devices")}}
 WindowContainer("EEG",form=Form.Planar,defaultSize=WindowContainerSize(480.dp,680.dp),
 placement={containers.firstOrNull{it.id=="GameHome"}?.let{Placement.right(it,120.dp)}?:Placement.none()}){PicoTheme{GameScreen(vm,"eeg")}}
 WindowContainer("Controls",form=Form.Planar,defaultSize=WindowContainerSize(360.dp,340.dp),
 placement={containers.firstOrNull{it.id=="GameHome"}?.let{Placement.right(it,120.dp)}?:Placement.none()}){PicoTheme{GameScreen(vm,"controls")}}
}
