package cn.mindsoul.pico.wishdragon.ui.game.components
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cn.mindsoul.pico.wishdragon.domain.model.GameState
@Composable fun GameAudio(s:GameState,active:Boolean){
 val context=LocalContext.current
 val path=if(s.kind=="dragon"){if(s.charging)"mindsoul/WishDragon/wish-charge.mp3"else if(s.summoned)"mindsoul/WishDragon/wish-summon.mp3"else null}else null
 DisposableEffect(path,active){
 var player:MediaPlayer?=null
 if(path!=null&&active)try{val p=MediaPlayer();player=p;context.assets.openFd(path).use{p.setDataSource(it.fileDescriptor,it.startOffset,it.length)};p.isLooping=s.charging;p.prepare();p.start()}catch(e:Exception){android.util.Log.w("MindSoulAudio","Audio unavailable",e);player?.release();player=null}
 onDispose{player?.release()}
 }
}
