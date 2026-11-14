package cn.mindsoul.pico.wishdragon.ui.game.components
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.*
import com.pico.spatial.ui.design.windows.AlertDialog
import cn.mindsoul.pico.wishdragon.R
import cn.mindsoul.pico.wishdragon.domain.model.*
import kotlin.math.ceil
private val DragonAction=Color(0xFF6750A4) // design-style: fixed-figma-color 统一规范沉浸和脑机入口
@Composable fun GameContent(s:GameState,event:(GameEvent)->Unit,enter:()->Unit,brain:()->Unit,session:DragonSceneSession,wishes:()->Unit){
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
 Image(painterResource(R.mipmap.ic_spatial_launcher),"意念神龙",Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
 Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
 Text("意念神龙",style=PicoTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
 Text("让专注点亮你的愿望",style=PicoTheme.typography.bodyMedium,color=PicoTheme.colorScheme.labelSecondary)
 }
 Button(onClick=enter,colors=ButtonDefaults.buttonColors(DragonAction,Color.White)){Text("沉浸召唤")}
 Button(onClick=brain,colors=ButtonDefaults.buttonColors(DragonAction,Color.White)){Text(if(s.bciMode)"恢复到空间计算许愿"else"脑机接口模拟体验")}
 }
 Text(if(s.summoned)"蓝色 MindSoul 神龙"else"七颗龙珠 · 等待你的心愿",fontSize=30.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.align(Alignment.CenterHorizontally))
 Column(Modifier.align(Alignment.CenterHorizontally).width(360.dp),verticalArrangement=Arrangement.spacedBy(6.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text(if(s.summoned)"召唤成功"else if(s.charging)"许愿倒计时 ${ceil((10f-s.charge).coerceAtLeast(0f).toDouble()).toInt()} 秒 · ${(s.charge*10).toInt()}%"else"许愿倒计时 · 10秒",style=PicoTheme.typography.titleMedium)
 LinearProgressIndicator(progress={(s.charge/10f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
 }
 GameScene(s,event,false,Modifier.fillMaxWidth().weight(1f),session=session)
 Text(status(s),style=PicoTheme.typography.titleMedium)

 if(s.message.isNotEmpty())Text(s.message,style=PicoTheme.typography.bodySmall)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
 Text("蓝色神龙",style=PicoTheme.typography.titleMedium)
 Button(onClick=wishes){Text("心愿看板 / 写下心愿")}
 Button(onClick={if(s.wish.isBlank())wishes()else event(GameEvent.Start)},enabled=!s.charging){Text(if(s.summoned)"再次许愿"else"开始许愿")}
 if(s.charging)Button(onClick={event(GameEvent.CancelCharge)}){Text("取消许愿")}
 }
 if(s.bciMode){FocusControl(s,event)}


 Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
 Button(onClick={event(GameEvent.Reset)}){Text("召回到面前")}
 if(s.hidden||s.hiddenParts.isNotEmpty())Button(onClick={event(GameEvent.Show)}){Text("恢复显示")}
 if(s.selected){Button(onClick={event(GameEvent.Hide)}){Text("隐藏选中")};Button(onClick={event(GameEvent.Delete)}){Text("移出场景")}}
 Text(if(s.wish.isBlank())"先写下心愿，开始倒计时召唤神龙"else"心愿：${s.wish.take(32)}"+(if(s.wish.length>32)"…"else""),modifier=Modifier.weight(1f),style=PicoTheme.typography.bodyMedium)
 }
 MindSoulSupport()
 }
}

fun status(s:GameState):String=when {
 s.creatures.isEmpty()->"场景已清空 · 重新许愿恢复龙珠"
 s.hidden->"已隐藏 · 点击恢复显示"
 s.summoned->"召唤成功 · 神龙已来到你的空间"
 s.charging&&s.bciMode&&s.focus<70->"等待专注 · 调整模拟专注力至70%以上"
 s.charging&&s.charge>=9.1f->"龙珠汇聚 · MindSoul光芒绽放"
 s.charging->"许愿 ${(s.charge*10).toInt()}% · 再保持${ceil((10f-s.charge).toDouble()).toInt()}秒"
 else->if(s.bciMode)"模拟专注力≥70%并持续10秒，召唤神龙"else"点击许愿开始10秒倒计时，召唤蓝色神龙"
}
@Composable private fun FocusControl(s:GameState,event:(GameEvent)->Unit){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
 Text("模拟专注力 ${s.focus}%",style=PicoTheme.typography.bodyMedium)
 Slider(value=s.focus.toFloat(),onValueChange={event(GameEvent.Focus(it.toInt()))},valueRange=0f..100f,modifier=Modifier.weight(1f))
 Text("非真实脑电采集",style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
 }
}
@Composable fun CompactControls(s:GameState,e:(GameEvent)->Unit,exit:()->Unit,brain:()->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("意念神龙 · 沉浸召唤",style=PicoTheme.typography.titleMedium)
 Text(status(s),style=PicoTheme.typography.bodySmall)
 Button(onClick=exit,modifier=Modifier.fillMaxWidth()){Text("退出沉浸 · 返回主看板")}
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 Button(onClick={e(if(s.hidden)GameEvent.Show else GameEvent.Reset)},modifier=Modifier.weight(1f)){Text(if(s.hidden)"恢复显示"else"召回到面前")}
 Button(onClick={e(if(s.charging)GameEvent.CancelCharge else GameEvent.Start)},modifier=Modifier.weight(1f)){Text(if(s.charging)"取消蓄能"else"再次许愿")}
 }
 Button(onClick=brain,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(DragonAction,Color.White)){Text(if(s.bciMode)"恢复到空间计算许愿"else"脑机接口模拟体验")}
 Text("逐颗拖动 · 双手旋转缩放 · 悬停圆点发光",style=PicoTheme.typography.bodySmall)
 }
}
