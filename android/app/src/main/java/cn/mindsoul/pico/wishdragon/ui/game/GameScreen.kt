package cn.mindsoul.pico.wishdragon.ui.game
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mindsoul.pico.wishdragon.ui.account.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.mindsoul.pico.wishdragon.domain.model.*
import cn.mindsoul.pico.wishdragon.ui.game.components.*
import com.pico.spatial.ui.platform.containers.*
import kotlinx.coroutines.*

@Composable fun GameScreen(vm:GameViewModel,surface:String){
 val context=LocalContext.current
 val repository=remember{vm.wishes?:cn.mindsoul.pico.wishdragon.data.wishes.WishRepository(context).also{vm.wishes=it}}
 val state by vm.state.collectAsStateWithLifecycle()
 val nav=LocalSpatialNavigator.current
 val scope=rememberCoroutineScope()
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 var active by remember{mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))}
 DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,_->active=lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
 val ticker=(surface=="home"&&!state.immersive)||(surface=="stage"&&state.immersive)
 LaunchedEffect(active,ticker){if(active&&ticker)while(isActive){delay(100);vm.onEvent(GameEvent.Tick(.1f))}}
 LaunchedEffect(state.charging){if(ticker&&state.charging)nav.openWindowContainer("WishProgress","progress")}
 val enter:()->Unit={scope.launch{when(val result=nav.openStage("GameStage",StageStyle.Mixed)){
  OpenStageResult.Allowed->{nav.closeWindowContainer("DragonWishes","wishes");nav.closeWindowContainer("Devices","devices");nav.closeWindowContainer("EEG","eeg");vm.onEvent(GameEvent.Immersive(true));nav.openWindowContainer("Controls","controls")}
  OpenStageResult.NotAllowed->vm.onEvent(GameEvent.Message("系统暂不允许进入空间体验"))
  is OpenStageResult.Error->vm.onEvent(GameEvent.Message("打开失败：${result.reason}"))
 }}}
 val exit:()->Unit={scope.launch{nav.restoreWindowContainer("GameHome");nav.closeStage();nav.closeWindowContainer("Controls","controls");vm.onEvent(GameEvent.Immersive(false))}}
 val brain:()->Unit={
 val enabled=!state.bciMode
 vm.onEvent(GameEvent.Bci(enabled))
 if(enabled){nav.openWindowContainer("Devices","devices");nav.openWindowContainer("EEG","eeg")}
 else{nav.closeWindowContainer("Devices","devices");nav.closeWindowContainer("EEG","eeg")}
 }
 when(surface){
 "home"->{
 val account=rememberAccount();val user by account.user.collectAsStateWithLifecycle()
 LaunchedEffect(user){nav.closeWindowContainer("Devices","devices");nav.closeWindowContainer("EEG","eeg");if(user!=null){vm.wishUser=cn.mindsoul.pico.wishdragon.data.wishes.WishRepository.identity(user!!);nav.openWindowContainer("DragonWishes","wishes")}else {nav.closeWindowContainer("DragonWishes","wishes");nav.closeWindowContainer("WishProgress","progress")}}
 if(user==null)AccountGate(account)else GameContent(state,vm::onEvent,enter,brain,vm.sceneSession){nav.openWindowContainer("DragonWishes","wishes")}
 }
 "stage"->{DisposableEffect(Unit){onDispose{vm.onEvent(GameEvent.Immersive(false))}};GameScene(state,vm::onEvent,true,session=vm.sceneSession,onReady={nav.minimizeWindowContainer("GameHome")})}
 "controls"->CompactControls(state,vm::onEvent,exit,brain)
 "wishes"->WishPanel(repository,vm.wishUser,{text->vm.onEvent(GameEvent.Wish(text));vm.onEvent(GameEvent.Start)}){nav.closeWindowContainer("DragonWishes","wishes")}
 "progress"->WishProgressPanel(state,vm::onEvent){nav.closeWindowContainer("WishProgress","progress")}
 "devices"->DevicePanel(state,vm::onEvent)
 "eeg"->EegPanel(state,vm::onEvent)
 }
 if(ticker)GameAudio(state,active)
}
