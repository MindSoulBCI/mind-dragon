package cn.mindsoul.pico.wishdragon.ui.game
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cn.mindsoul.pico.wishdragon.domain.model.*
import cn.mindsoul.pico.wishdragon.domain.usecase.GameUseCase
import kotlinx.coroutines.flow.*
class GameViewModel(initial:GameState,private val rules:GameUseCase):ViewModel(){
 var wishUser by androidx.compose.runtime.mutableStateOf("")
 var wishes:cn.mindsoul.pico.wishdragon.data.wishes.WishRepository?=null
 val sceneSession=cn.mindsoul.pico.wishdragon.ui.game.components.DragonSceneSession()
 private val mutable=MutableStateFlow(initial)
 val state:StateFlow<GameState> = mutable.asStateFlow()
 fun onEvent(e:GameEvent){mutable.value=rules.reduce(mutable.value,e)}
}
