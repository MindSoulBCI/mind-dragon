package com.pmtalk.mindsoul.xr.ui.screen.bcidata

import android.annotation.SuppressLint
import android.webkit.*
import androidx.xr.compose.platform.LocalSession
import androidx.xr.scenecore.*
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import kotlin.io.path.Path
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.*
import coil.compose.AsyncImage
import com.pmtalk.mindsoul.xr.core.di.LocalBrainlinkViewModel
import com.pmtalk.mindsoul.xr.core.brainlink.BrainDeviceState
import org.json.JSONArray

private data class Game(val id: String, val title: String, val cover: String, val assets: Set<String>)

@Composable
internal fun BrainGames(onSpatialScene: (Boolean) -> Unit = {}, onGameChanged: (String?) -> Unit = {}) {
    val context = LocalContext.current
    val games = remember {
        val list = context.assets.open("mindsoul/catalog.json").bufferedReader().use { JSONArray(it.readText()) }
        List(list.length()) { i -> list.getJSONObject(i).let { val assets = it.getJSONArray("assets"); Game(it.getString("id"),it.getString("title"),it.getString("cover"), (0 until assets.length()).map(assets::getString).toSet()) } }.filterNot { it.id == "popcorn" }
    }
    var selected by rememberSaveable { mutableStateOf<String?>(if(com.pmtalk.mindsoul.xr.BuildConfig.DEBUG) (context as? android.app.Activity)?.intent?.getStringExtra("preview_game") else null) }
    val game = games.find { it.id == selected }
    val updateTitle by rememberUpdatedState(onGameChanged)
    LaunchedEffect(game?.id) { updateTitle(game?.title) }
    DisposableEffect(Unit) { onDispose { updateTitle(null) } }
    if (game == null) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns=(maxWidth.value/225f).toInt().coerceIn(1,4)
            val rows=(games.size+columns-1)/columns
            val cardHeight=((maxHeight-32.dp-12.dp*(rows-1))/rows).coerceIn(180.dp,280.dp)
            LazyVerticalGrid(GridCells.Fixed(columns), modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
                horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(games,key={it.id}) {item ->
                    val interaction=remember {MutableInteractionSource()}
                    val hovered by interaction.collectIsHoveredAsState()
                    Card(onClick={selected=item.id}, modifier=Modifier.height(cardHeight).hoverable(interaction),
                        interactionSource=interaction,
                        colors=CardDefaults.cardColors(containerColor=if(hovered)Color(0xFF464A50) else Color(0xFF343139)),
                        border=BorderStroke(if(hovered)2.dp else 1.dp,if(hovered)Color(0xFFA6D6C7) else Color.White.copy(alpha=.18f))) {
                        AsyncImage("file:///android_asset/mindsoul/"+item.cover,null,Modifier.fillMaxWidth().weight(1f),contentScale=ContentScale.Crop)
                        Text(item.title,Modifier.padding(horizontal=14.dp,vertical=16.dp),style=MaterialTheme.typography.titleMedium,color=Color.White,maxLines=1)
                    }
                }
            }
        }
        return
    }
    val startedAt = remember(game.id) { System.currentTimeMillis() }
    if(game.id in listOf("aquarium","blocks","rocket","cat","weather") && LocalSpatialCapabilities.current.isSpatialUiEnabled){
        DisposableEffect(Unit){onSpatialScene(true);onDispose{onSpatialScene(false)}}
        BackHandler { selected=null }
        when(game.id) { "aquarium" -> SpatialAquarium {selected=null}; "rocket" -> SpatialRocket {selected=null}; "cat" -> SpatialCatCafe {selected=null}; "weather" -> SpatialWeather {selected=null}; else -> SpatialBlocks {selected=null} }
        TaskCompanionPanels(onReturnToGames={selected=null},immersive=true,taskStartedAt=startedAt,recordingName=game.title)
        return
    }
    var placement by remember(game.id) { mutableStateOf<Pair<String,Float>?>(null) }
    var modelError by remember(game.id) { mutableStateOf<String?>(null) }
    val spatial = LocalSpatialCapabilities.current.isSpatialUiEnabled
    val session = LocalSession.current
    LaunchedEffect(placement, session, spatial) {
        val (file, scale) = placement ?: return@LaunchedEffect
        if (!spatial || session == null) return@LaunchedEffect
        var entity: GltfModelEntity? = null
        var resource: GltfModel? = null
        try {
            val model = GltfModel.create(session, Path("mindsoul/$file"))
            resource = model
            entity = GltfModelEntity.create(session, model, Pose(translation=Vector3(0f, .1f, -1.2f)))
            entity.parent = session.scene.activitySpace
            entity.setScale(scale)
            entity.addComponent(MovableComponent.createSystemMovable(session, scaleInZ=true))
            awaitCancellation()
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { modelError = "空间模型加载失败" }
        finally { entity?.dispose(); resource?.close() }
    }
    BackHandler { selected = null }
    val brain = LocalBrainlinkViewModel.current
    val connection by brain.deviceState.collectAsState()
    val wave by brain.latestBrainWave.collectAsState()
    val live = connection is BrainDeviceState.Connected && wave != null
    var demoFocus by rememberSaveable { mutableFloatStateOf(50f) }
    val focus = if(live) wave!!.attention else demoFocus.toInt()
    var layoutVersion by remember(game.id) { mutableIntStateOf(0) }
    val returnToList = { placement = null; selected = null }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            com.pmtalk.mindsoul.xr.ui.component.MindSoulActionButton("‹ 返回脑机游戏列表",onClick=returnToList)
            if(spatial) TextButton(onClick={layoutVersion++}) { Text("重置左右看板", color=Color.White) }
            else DeviceBindingButton()
        }
        key(game.id) { GameSessionSummary(game.id) }
        if(!live && game.id!="creation") Row(Modifier.padding(horizontal=16.dp), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("模拟专注力 $focus · 非实测", color=Color.LightGray)
            Slider(demoFocus,{demoFocus=it},Modifier.weight(1f),valueRange=0f..100f)
        }
        if (placement != null) TextButton(onClick={placement=null; modelError=null}) { Text(modelError ?: "收回空间模型") }
        if(game.id=="creation") MindCreationContent {file,scale->placement=file to scale}
        else GameView(game.id,focus,!live,Modifier.fillMaxSize(), spatial) { file, scale ->
            if(file in game.assets && file.endsWith(".glb") && scale.isFinite() && scale > 0f) {
                modelError = null
                placement = file to scale
            }
        }
    }
    if(spatial) key(game.id, layoutVersion) { TaskCompanionPanels(onReturnToGames=returnToList,taskStartedAt=startedAt,recordingName=game.title) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GameView(id:String,focus:Int,demo:Boolean,modifier:Modifier,spatial:Boolean=false,onBrainSpace:()->Unit={},previewModel:String="creation-garden-miniature",onBrainMontage:(Int)->Unit={},onPlace:(String,Float)->Unit={_,_->}) {
    var pendingFile by remember { mutableStateOf<android.webkit.ValueCallback<Array<android.net.Uri>>?>(null) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> pendingFile?.onReceiveValue(uri?.let { arrayOf(it) }); pendingFile=null }
    val place by rememberUpdatedState(onPlace)
    val openBrain by rememberUpdatedState(onBrainSpace)
    val changeMontage by rememberUpdatedState(onBrainMontage)
    key(id,spatial,previewModel) {
        AndroidView(modifier=modifier, factory={ ctx ->
            WebView(ctx).apply {
                settings.cacheMode=WebSettings.LOAD_NO_CACHE;clearCache(true);settings.javaScriptEnabled=true
                settings.textZoom=100;settings.useWideViewPort=true;settings.loadWithOverviewMode=true
                settings.allowFileAccess=false
                settings.allowContentAccess=id=="fusion"
                if(id=="fusion") webChromeClient=object:WebChromeClient(){
                    override fun onShowFileChooser(view:WebView, callback:android.webkit.ValueCallback<Array<android.net.Uri>>, params:FileChooserParams):Boolean {
                        pendingFile?.onReceiveValue(null)
                        pendingFile=callback
                        picker.launch(arrayOf("*/*"))
                        return true
                    }
                }
                webViewClient=object:WebViewClient(){
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        if(id=="fusion" && uri.scheme=="mindsoul-game" && uri.host=="montage"){uri.getQueryParameter("n")?.toIntOrNull()?.takeIf{it in listOf(3,32,64)}?.let{changeMontage(it)};return true}
                        if(id=="fusion" && request.isForMainFrame && uri.scheme=="mindsoul-game" && uri.host=="brain") { openBrain(); return true }
                        if(spatial && request.isForMainFrame && uri.scheme=="mindsoul-game" && uri.host=="place"){
                            val file=uri.getQueryParameter("file")
                            val scale=uri.getQueryParameter("scale")?.toFloatOrNull()
                            if(file!=null && scale!=null) place(file,scale)
                        }
                        return true
                    }
                    override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                        val path=request.url.path.orEmpty().removePrefix("/")
                        if(request.url.host=="assets.mindsoul.invalid" && (path.startsWith("games/") || path.startsWith("mindsoul/") || path.startsWith("fusion/")) && ".." !in path){
                            val mime=when(path.substringAfterLast('.')){
                                "js"->"application/javascript";"html"->"text/html";"json"->"application/json";"mp4"->"video/mp4";"mp3"->"audio/mpeg";"wav"->"audio/wav";"png"->"image/png";else->"application/octet-stream"
                            }
                            try { return WebResourceResponse(mime,null,ctx.assets.open(path)) } catch(_:java.io.IOException){}
                        }
                        return WebResourceResponse("text/plain","UTF-8",404,"Not Found", emptyMap(),null)
                    }
                    override fun onPageFinished(view:WebView,url:String){
                        view.evaluateJavascript(view.tag as? String ?: "",null)
                    }
                }
                val query = if(spatial) "?spatial" else ""
                loadUrl(if(id=="creation-preview") "https://assets.mindsoul.invalid/games/creation-preview.html?model=${android.net.Uri.encode(previewModel)}" else if(id=="fusion") "https://assets.mindsoul.invalid/fusion/viewer.html?demo=$demo" else "https://assets.mindsoul.invalid/games/index.html$query#$id")
            }
        },onRelease={ pendingFile?.onReceiveValue(null); pendingFile=null; it.stopLoading(); it.destroy() },update={
            val js=if(id=="fusion") "window.setBrainDemo?.($demo)" else "window.setGameFocus?.($focus,$demo)"
            if(it.tag!=js){it.tag=js;it.evaluateJavascript(js,null)}
        })
    }
}
