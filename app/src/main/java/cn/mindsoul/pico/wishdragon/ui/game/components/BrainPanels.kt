package cn.mindsoul.pico.wishdragon.ui.game.components
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.*
import com.pico.spatial.ui.design.windows.AlertDialog
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import cn.mindsoul.pico.wishdragon.domain.model.*
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable fun DevicePanel(s:GameState,event:(GameEvent)->Unit){
 val nav=LocalSpatialNavigator.current;val context=LocalContext.current
 var contact by rememberSaveable{mutableStateOf(false)}
 val website:()->Unit={try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://mindsoul.cn")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:android.content.ActivityNotFoundException){Toast.makeText(context,"未找到浏览器，请访问 mindsoul.cn",Toast.LENGTH_LONG).show()}}
 if(contact)AlertDialog(onDismissRequest={contact=false},title={Text("脑机接口体验与商务合作")},content={Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
 Text("添加 PMTalk123，购买脑机接口体验，或咨询商务合作。")
 Text("当前为模拟接入，不代表真实设备已连接。")
 Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
 Button(onClick={contact=false}){Text("我知道了")}
 Button(onClick=website,modifier=Modifier.weight(1f)){Text("去查看 MindSoul\n脑机接口基座大模型")}
 }
 }})
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 Text("脑机接口设备",style=PicoTheme.typography.headlineSmall)
 Text("当前：${s.device} · 模拟接入")
 Text("模拟测试数据 · 非真实采集",style=PicoTheme.typography.bodySmall)
 listOf("BrainLink Pro","博瑞恩","博睿康","MUSE S").forEach{name->Button(onClick={event(GameEvent.Device(name));contact=true},modifier=Modifier.fillMaxWidth()){Text(name)}}
 Text("设备展示名单，不构成硬件兼容认证。",style=PicoTheme.typography.bodySmall)
 Spacer(Modifier.weight(1f))
 Button(onClick=website,modifier=Modifier.fillMaxWidth()){Text("MindSoul 支持 · 官网")}
 Button(onClick={nav.closeWindowContainer()},modifier=Modifier.fillMaxWidth()){Text("关闭设备窗口")}
 }
}
@Composable fun EegPanel(s:GameState,event:(GameEvent)->Unit){
 val nav=LocalSpatialNavigator.current
 var t by remember{mutableFloatStateOf(0f)}
 LaunchedEffect(s.waves){while(s.waves){delay(50);t+=.05f}}
 val count=if(s.channels==3)3 else 8;val start=if(s.channels==3)0 else s.page*8
 val ink=PicoTheme.colorScheme.labelPrimary;val grid=PicoTheme.colorScheme.labelSecondary.copy(alpha=.2f)
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("EEG 脑电看板",style=PicoTheme.typography.headlineSmall)
 Text("模拟测试数据 · 非真实采集",style=PicoTheme.typography.titleMedium)
 Text("${s.device} · CH${start+1}–CH${start+count}")
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 Button(onClick={event(GameEvent.Channels)}){Text("${s.channels}通道 · 切换")}
 Button(onClick={event(GameEvent.Page)},enabled=s.channels==32){Text("下一页 ${s.page+1}/4")}
 }
 Row(Modifier.fillMaxWidth().weight(1f)){
 Column(Modifier.width(46.dp).fillMaxHeight(),verticalArrangement=Arrangement.SpaceAround){repeat(count){Text("CH${start+it+1}",style=PicoTheme.typography.bodySmall)}}
 Canvas(Modifier.weight(1f).fillMaxHeight()){
 val row=size.height/count
 repeat(count){channel->val center=row*(channel+.5f);drawLine(grid,Offset(0f,center),Offset(size.width,center));val path=Path()
 for(i in 0..200){val x=size.width*i/200f;val sample=t+i/50f;val y=center+(sin(sample*(7+channel+start))*.6f+sin(sample*19f)*.2f)*row*.35f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
 drawPath(path,ink,style=Stroke(1.3.dp.toPx()))}
 }
 }
 Button(onClick={event(GameEvent.Waves)},modifier=Modifier.fillMaxWidth()){Text(if(s.waves)"暂停波形"else"继续波形")}
 Button(onClick={nav.closeWindowContainer("Devices","devices");nav.closeWindowContainer("EEG","eeg")},modifier=Modifier.fillMaxWidth()){Text("返回主看板")}
 Button(onClick={nav.closeWindowContainer()},modifier=Modifier.fillMaxWidth()){Text("仅关闭 EEG")}
 }
}
