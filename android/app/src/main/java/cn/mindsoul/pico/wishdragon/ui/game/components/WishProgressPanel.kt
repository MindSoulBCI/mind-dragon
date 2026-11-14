package cn.mindsoul.pico.wishdragon.ui.game.components
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.*
import cn.mindsoul.pico.wishdragon.domain.model.*
import kotlin.math.ceil
@Composable fun WishProgressPanel(s:GameState,event:(GameEvent)->Unit,close:()->Unit){
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(verticalAlignment=Alignment.CenterVertically){Text("许愿过程",style=PicoTheme.typography.headlineMedium,modifier=Modifier.weight(1f));Button(onClick=close){Text("收起")}}
 Text(when{ s.summoned->"召唤成功 · 神龙已降临";s.charging&&s.bciMode&&s.focus<70->"等待模拟专注力达到70%";s.charging&&s.charge>=9.1f->"龙珠汇聚 · 光芒绽放";s.charging->"龙珠正在汇聚 · 剩余${ceil((10-s.charge).toDouble()).toInt()}秒";else->"许愿已暂停，可重新开始"},style=PicoTheme.typography.titleMedium)
 LinearProgressIndicator(progress={(s.charge/10f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
 Text("${(s.charge*10).toInt()}% · "+if(s.summoned)"心愿已寄托给神龙"else "许愿 → 龙珠汇聚 → 光芒绽放 → 神龙降临",style=PicoTheme.typography.bodyMedium)
 Text("心愿：${s.wish.take(48)}",style=PicoTheme.typography.bodySmall)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 if(s.charging)Button(onClick={event(GameEvent.CancelCharge)}){Text("取消本次许愿")}
 else Button(onClick={event(GameEvent.Start)},enabled=s.wish.isNotBlank()){Text("再次许愿")}
 }
 }
}
