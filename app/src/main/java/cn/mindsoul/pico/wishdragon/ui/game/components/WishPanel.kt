package cn.mindsoul.pico.wishdragon.ui.game.components
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.pico.spatial.ui.design.*
import cn.mindsoul.pico.wishdragon.data.wishes.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun WishPanel(repository:WishRepository,user:String,onWish:(String)->Unit,close:()->Unit){
 val rows by repository.rows.collectAsStateWithLifecycle()
 var draft by rememberSaveable{mutableStateOf("")}
 var publish by rememberSaveable{mutableStateOf(false)}
 var tab by rememberSaveable{mutableStateOf(0)}
 var selectedWish by rememberSaveable(user){mutableStateOf<String?>(null)}
 var notice by rememberSaveable{mutableStateOf("")}
 val visible=when(tab){1->rows.filter{it.shared}.sortedWith(compareByDescending<DragonWish>{it.likes.size}.thenByDescending{it.date});2->rows.filter{it.author==user};else->rows.filter{it.shared}}
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 Row(verticalAlignment=Alignment.CenterVertically){Text(if(tab==1)"心愿点赞榜"else"向神龙许愿",style=PicoTheme.typography.headlineMedium,modifier=Modifier.weight(1f));Button(onClick=close){Text("收起")}}
 if(tab==0){
 TextField(value=draft,onValueChange={draft=it.take(100);notice=""},placeholder={Text("输入你的心愿…")},modifier=Modifier.fillMaxWidth().height(120.dp))
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
 Switch(checked=publish,onCheckedChange={publish=it})
 Text("允许上心愿墙",modifier=Modifier.weight(1f))
 Text("${draft.length}/100",style=PicoTheme.typography.bodySmall)
 Button(enabled=draft.isNotBlank(),onClick={val error=repository.add(user,draft,publish);if(error.isEmpty()){onWish(draft.trim());draft="";notice=if(publish)"已保存并加入本机心愿墙"else"私人心愿已保存，仅自己可见"}else notice=error}){Text("许愿并开始倒计时")}
 }
 }
 Text("本机账号心愿墙 · 未联网，暂不能查看其他设备用户",style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
 if(tab!=1&&notice.isNotEmpty())Text(notice,style=PicoTheme.typography.bodySmall)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("心愿看板","点赞排行榜","我的心愿").forEachIndexed{i,title->Button(onClick={tab=i},modifier=Modifier.weight(1f)){Text((if(tab==i)"✓ "else"")+title)}}}
 if(tab==1){
 Row(verticalAlignment=Alignment.CenterVertically){
 Text("按点赞人数排序",style=PicoTheme.typography.bodySmall,modifier=Modifier.weight(1f))
 Text("榜单人数：${visible.map{it.author}.distinct().size}人",style=PicoTheme.typography.bodyMedium)
 }
 WishTableHeader()
 }
 Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
 if(tab==2){
 Row(verticalAlignment=Alignment.CenterVertically){
 Text("我的心愿列表",style=PicoTheme.typography.titleMedium,modifier=Modifier.weight(1f))
 Text("共${visible.size}条心愿",style=PicoTheme.typography.bodyMedium)
 }
 }
 if(visible.isEmpty())Text(if(tab==1)"当前还没有榜单"else if(tab==2)"你还没有心愿，请到心愿看板输入并提交。"else"暂无公开心愿。开启上墙后，这台设备上的其他账号可以看到并点赞。")
 if(tab==2){
 FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 visible.forEach{wish->ToggleableChip(isToggleOn=selectedWish==wish.id,onClick={selectedWish=if(selectedWish==wish.id)null else wish.id},modifier=Modifier.widthIn(max=320.dp),label={Text(wish.text,maxLines=1,overflow=TextOverflow.Ellipsis)})}
 }
 visible.firstOrNull{it.id==selectedWish}?.let{wish->
 Text(wish.text,style=PicoTheme.typography.titleMedium)
 Text(wish.date+if(wish.shared)" · 已上墙"else" · 仅自己可见",style=PicoTheme.typography.bodySmall)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 Button(onClick={onWish(wish.text);notice="已使用所选心愿重新开始召唤"}){Text("重新许愿")}
 Button(onClick={repository.visibility(wish.id,user,!wish.shared)}){Text(if(wish.shared)"从墙上撤回"else"加入心愿墙")}
 Button(onClick={repository.remove(wish.id,user);selectedWish=null}){Text("删除")}
 }
 }
 }else visible.forEachIndexed{index,wish->
 if(tab==1){WishTableRow(index+1,wish,user){repository.like(wish.id,user)}}
 else Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
 Text((if(tab==1)"第${index+1}名 · "else"")+wish.text,style=PicoTheme.typography.titleMedium)
 Text((if(wish.author==user)"我的心愿"else"本机用户 ${wish.author.takeLast(4)}")+" · ${wish.date}"+(if(!wish.shared)" · 仅自己可见"else" · 已上墙"),style=PicoTheme.typography.bodySmall,color=PicoTheme.colorScheme.labelSecondary)
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 if(wish.shared)Button(onClick={repository.like(wish.id,user)}){Text((if(user in wish.likes)"已点赞 "else"点赞 ")+wish.likes.size)}
 if(wish.author==user){Button(onClick={repository.visibility(wish.id,user,!wish.shared)}){Text(if(wish.shared)"从墙上撤回"else"加入心愿墙")};Button(onClick={repository.remove(wish.id,user)}){Text("删除")}}
 }
 }}
 }
 }
}

@Composable private fun WishTableHeader(){
 Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
 Text("序号",Modifier.weight(.5f),style=PicoTheme.typography.bodySmall)
 Text("心愿名称",Modifier.weight(2.4f),style=PicoTheme.typography.bodySmall)
 Text("心愿人",Modifier.weight(1.1f),style=PicoTheme.typography.bodySmall)
 Text("点赞心愿人数",Modifier.weight(1.4f),style=PicoTheme.typography.bodySmall)
 Text("心愿时间",Modifier.weight(1.2f),style=PicoTheme.typography.bodySmall)
 }
}
@Composable private fun WishTableRow(rank:Int,wish:DragonWish,user:String,like:()->Unit){
 Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
 Text(rank.toString(),Modifier.weight(.5f),style=PicoTheme.typography.bodyMedium)
 Text(wish.text,Modifier.weight(2.4f),style=PicoTheme.typography.bodyMedium)
 Text(if(wish.author==user)"我"else"本机用户 ${wish.author.takeLast(4)}",Modifier.weight(1.1f),style=PicoTheme.typography.bodySmall)
 Button(onClick=like,modifier=Modifier.weight(1.4f)){Text("${wish.likes.size}人 · "+if(user in wish.likes)"已赞"else"点赞")}
 Text(wish.date,Modifier.weight(1.2f),style=PicoTheme.typography.bodySmall)
 }
}
