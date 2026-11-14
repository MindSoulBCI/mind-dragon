package cn.mindsoul.pico.wishdragon.ui.game
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.mindsoul.pico.wishdragon.data.repository.LocalGameRepository
import cn.mindsoul.pico.wishdragon.domain.usecase.GameUseCase
class GameViewModelFactory(private val shared:GameViewModel?=null):ViewModelProvider.Factory {
 fun create()=shared?:GameViewModel(LocalGameRepository().initial(),GameUseCase())
 @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=create() as T
}
