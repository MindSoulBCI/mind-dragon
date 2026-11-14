package cn.mindsoul.pico.wishdragon
import org.junit.Test
import org.junit.Assert.*
import cn.mindsoul.pico.wishdragon.domain.model.*
import cn.mindsoul.pico.wishdragon.domain.usecase.GameUseCase
import cn.mindsoul.pico.wishdragon.ui.game.*
class GameViewModelTest {
 @Test fun initialStateLoads(){val vm=GameViewModelFactory().create();assertTrue(vm.state.value.creatures.isNotEmpty())}
 @Test fun petUpdatesCount(){val vm=GameViewModel(GameState(kind="cat"),GameUseCase());vm.onEvent(GameEvent.Pet);assertEquals(1,vm.state.value.pets)}
 @Test fun focusClamps(){val vm=GameViewModelFactory().create();vm.onEvent(GameEvent.Focus(150));assertEquals(100,vm.state.value.focus);vm.onEvent(GameEvent.Focus(-20));assertEquals(0,vm.state.value.focus)}
 @Test fun emptyRemoveIsSafe(){val vm=GameViewModel(GameState(kind="aquarium"),GameUseCase());vm.onEvent(GameEvent.Remove);assertTrue(vm.state.value.creatures.isEmpty())}
 @Test fun auxiliaryStatePreservesGame(){val vm=GameViewModel(GameState(kind="cat",pets=7),GameUseCase());vm.onEvent(GameEvent.Device("MUSE S"));assertEquals(7,vm.state.value.pets)}
}
