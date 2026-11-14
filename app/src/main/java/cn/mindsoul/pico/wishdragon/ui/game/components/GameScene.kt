package cn.mindsoul.pico.wishdragon.ui.game.components
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.*
import com.pico.spatial.ui.foundation.geometry.*
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.Button
import cn.mindsoul.pico.wishdragon.domain.model.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

internal data class DragonPose(val position:Vector3,val scale:Vector3,val rotation:Quat)
class DragonSceneSession {
 internal val poses=mutableMapOf<Int,DragonPose>()
 internal var revision=-1
}
private data class Actor(val id:Int,val anchor:Entity,val visual:Entity,val base:Vector3,var rest:Vector3,var origin:Vector3,val random:Vector3,val idle:Entity?,val glow:Entity?,val halo:Entity?,val joints:List<Pair<Entity,Quat>>)
private class DragonScene {
 var root:Entity?=null
 val actors=linkedMapOf<Int,Actor>()
 val panels=mutableMapOf<Int,Entity>()
 val hovered=mutableSetOf<Int>()
 var held:Int?=null
 var pointer:((List<SpatialPointerInfo>)->Boolean)?=null
 var reset:(()->Unit)?=null
 var disposed=false
 fun target(e:Entity?):Actor?{var item=e;while(item!=null){if(item in panels.values)return null;actors.values.firstOrNull{it.anchor==item}?.let{return it};item=item.getParent()};return null}
 fun dispose(){disposed=true;pointer=null;reset=null;root?.destroy();root=null;actors.clear();panels.clear();hovered.clear()}
}
@Composable fun GameScene(s:GameState,event:(GameEvent)->Unit,immersive:Boolean,modifier:Modifier=Modifier.fillMaxSize(),session:DragonSceneSession,onReady:()->Unit={}){
 val context=LocalContext.current
 val current by rememberUpdatedState(s);val send by rememberUpdatedState(event);val notifyReady by rememberUpdatedState(onReady)
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 key(s.revision,s.summoned,s.choice,s.removedParts,s.creatures.isEmpty(),immersive){
 val scene=remember{DragonScene()}
 var ready by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("正在准备独立龙珠…")}
 DisposableEffect(scene,lifecycle){val observer=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_PAUSE){scene.reset?.invoke();scene.hovered.clear()}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);scene.dispose()}}
 LaunchedEffect(ready){if(ready){var time=0f;var wasCharging=false
 while(isActive){delay(33);time+=.033f
 val state=current
 if(state.charging&&!wasCharging)scene.actors.values.forEach{it.origin=it.anchor.components[TransformComponent::class.java]!!.position}
 if(!state.charging&&wasCharging&&!state.summoned)scene.actors.values.forEach{it.rest=it.anchor.components[TransformComponent::class.java]!!.position}
 wasCharging=state.charging
 scene.actors.values.forEach{a->
 a.anchor.enabled=!state.hidden&&a.id !in state.hiddenParts
 scene.panels[a.id]?.enabled=state.selected&&state.selectedPart==a.id&&a.anchor.enabled&&!state.charging&&scene.held==null
 val glowing=a.id in scene.hovered||scene.held==a.id||state.charging&&state.charge>=9.1f
 a.idle?.enabled=!glowing;a.glow?.enabled=glowing;a.halo?.enabled=false // Avoid overlapping transparent shells occluding the ball on hover.
 val at=a.anchor.components[TransformComponent::class.java]!!
 if(scene.held!=a.id){
 if(a.id!=100&&state.charging){
 val p=(state.charge/9.4f).coerceIn(0f,1f);val ease=p*p*(3f-2f*p)
 val swirl=Vector3(sin(time*1.9f+a.random.x)*.22f,cos(time*1.5f+a.random.y)*.17f,sin(time*1.3f+a.random.z)*.12f)*(sin(PI*p).toFloat())
 val target=Vector3(cos(a.id*2.4f)*.055f,sin(a.id*2.4f)*.055f,(a.id-4)*.016f)
 at.setPosition(a.origin*(1f-ease)+target*ease+swirl)
 }else{
 val amp=if(state.summoned).045f else .035f
 at.setPosition(a.rest+Vector3(sin(time*.38f+a.random.x)*amp,cos(time*.51f+a.random.y)*amp,sin(time*.31f+a.random.z)*amp*.5f))
 }
 }
 // Whisker and jaw pivots were isolated and authored in Blender.
 a.joints.forEachIndexed{i,(joint,rest)->
 val angle=if(i==2)(.025f+.025f*sin(time*.8f)) else sin(time*.8f+i*.8f)*(if(i==0).10f else -.09f)
 val delta=if(i==2)Quat(sin(angle/2),0f,0f,cos(angle/2))else Quat(0f,0f,sin(angle/2),cos(angle/2))
 joint.components[TransformComponent::class.java]?.setQuaternion(rest*delta)
 }
 }
 }
 }}
 Box(modifier){
 SpatialView(modifier=Modifier.fillMaxSize().pointerInput(scene){detectSpatialPointerEvent(context=context){scene.pointer?.invoke(it)?:false}},
 attachments={ ((1..7).toList()+if(s.summoned)listOf(100)else emptyList()).forEach{id->AttachmentPanel("part-$id"){
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
 Button(onClick={send(GameEvent.Part(id));send(GameEvent.Hide)}){Text("隐藏")}
 Button(onClick={send(GameEvent.Part(id));send(GameEvent.Delete)}){Text("删除")}
 }
 }}},
 initial={content,attachments->
 try{
 if(session.revision!=s.revision){session.poses.clear();session.revision=s.revision}
 val owner=Entity();scene.root=owner;content.addEntity(owner);val fitted=Entity();owner.addChild(fitted)
 val ids=if(s.creatures.isEmpty())emptyList()else ((1..7).toList()+(if(s.summoned)listOf(100)else emptyList())).filter{it !in s.removedParts}
 for(id in ids){
 val visual=Entity.loadSuspend("asset://mindsoul/WishDragon/interactive/"+(if(id==100)"dragon-azure.glb"else"ball-$id.glb"))
 if(scene.disposed){visual.destroy();return@SpatialView}
 val anchor=Entity();fitted.addChild(anchor);anchor.addChild(visual)
 val vt=visual.components[TransformComponent::class.java]!!
 visual.findEntity("LogosGlow")?.enabled=false;visual.findEntity("BallGlow")?.enabled=false
 val bounds=visual.getVisualBounds(anchor);val extent=maxOf(bounds.size.x,bounds.size.y,bounds.size.z);check(extent>0&&extent.isFinite()){"模型几何无效"}
 val scale=(if(id==100)4.05f else .32f)/extent
 vt.setScaleVector(Vector3(scale,scale,scale));val base=visual.getVisualBounds(anchor).center*(-1f);vt.setPosition(base)
 val pos=if(id==100)Vector3(0f,2.25f,0f) else if(s.summoned)Vector3(cos(id*2.4f)*.055f,sin(id*2.4f)*.055f,(id-4)*.016f) else if(id<=3)Vector3((id-2)*.46f,.22f,0f)else Vector3((id-5.5f)*.46f,-.22f,0f)
 val saved=if(s.summoned&&id!=100)null else session.poses[id];val rest=saved?.position?:pos
 anchor.components[TransformComponent::class.java]!!.apply{setPosition(rest);if(saved!=null){setScaleVector(saved.scale);setQuaternion(saved.rotation)}}
 anchor.components.set(CollisionComponent(listOf(if(id==100)ShapeResource.createBox(visual.getVisualBounds(anchor).size)else ShapeResource.createSphere(.17f)),PhysicsMaterialResource()))
 anchor.components.set(InteractableComponent());anchor.components.set(HoverEffectComponent())
 val random=Random(s.revision*31+id*157)
 val joints=if(id==100)listOf("WhiskerLeft","WhiskerRight","LowerJaw").mapNotNull{name->visual.findEntity(name)?.let{it to it.components[TransformComponent::class.java]!!.quaternion}}else emptyList()
 scene.actors[id]=Actor(id,anchor,visual,base,rest,rest,Vector3(random.nextFloat()*6,random.nextFloat()*6,random.nextFloat()*6),visual.findEntity("LogosIdle"),visual.findEntity("LogosGlow"),visual.findEntity("BallGlow"),joints)
 }
 if(scene.actors.isNotEmpty()){
 val bounds=fitted.getVisualBounds(owner);val ft=fitted.components[TransformComponent::class.java]!!
 if(!immersive){val corner=content.convertPosition(Vector3.ZERO,ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace);val fit=min(abs(corner.x)*1.4f/bounds.size.x.coerceAtLeast(.01f),abs(corner.y)*1.4f/bounds.size.y.coerceAtLeast(.01f));check(fit.isFinite()&&fit>0);ft.setScaleVector(Vector3(fit,fit,fit));ft.setPosition(bounds.center*(-fit))}
 else{ft.setPosition(bounds.center*(-1f));owner.components[TransformComponent::class.java]!!.setPosition(Vector3(0f,1.25f,-2.2f))}
 }
 scene.actors.forEach{(id,a)->attachments.entity("part-$id")?.let{panel->a.anchor.addChild(panel);scene.panels[id]=panel;panel.enabled=false;panel.components[TransformComponent::class.java]!!.apply{setPosition(Vector3(0f,if(id==100)-2.15f else -.23f,.22f));setScaleVector(Vector3(1.5f,1.5f,1.5f))}}}
 val active=linkedMapOf<PointerId,SpatialPointerInfo>();val hoverByPointer=mutableMapOf<PointerId,Int>();var previous=emptyList<Vector3>();var pressOrigin:Vector3?=null;var dragging=false
 fun reset(){active.clear();hoverByPointer.clear();scene.held=null;scene.hovered.clear();previous=emptyList();pressOrigin=null;dragging=false}
 scene.reset={reset()}
 fun length(v:Vector3)=sqrt(v.x*v.x+v.y*v.y+v.z*v.z)
 fun rotation(from:Vector3,to:Vector3):Quat{
 val a=from*(1f/length(from).coerceAtLeast(.0001f));val b=to*(1f/length(to).coerceAtLeast(.0001f));val dot=(a.x*b.x+a.y*b.y+a.z*b.z).coerceIn(-1f,1f)
 if(dot<-.999f){val axis=if(abs(a.x)<.9f)Vector3(0f,a.z,-a.y)else Vector3(-a.z,0f,a.x);val n=length(axis).coerceAtLeast(.0001f);return Quat(axis.x/n,axis.y/n,axis.z/n,0f)}
 val x=a.y*b.z-a.z*b.y;val y=a.z*b.x-a.x*b.z;val z=a.x*b.y-a.y*b.x;val w=1f+dot;val n=sqrt(x*x+y*y+z*z+w*w).coerceAtLeast(.0001f);return Quat(x/n,y/n,z/n,w/n)
 }
 scene.pointer={events->
 for(input in events){
 val target=scene.target(input.targetedEntity)?.takeIf{it.anchor.enabled}
 if(target!=null)hoverByPointer[input.pointerId]=target.id else hoverByPointer.remove(input.pointerId)
 if(input.pressed&&!current.charging){
 if(scene.held==null&&target!=null){scene.held=target.id;pressOrigin=Vector3(input.x,input.y,input.z);dragging=false;send(GameEvent.Part(target.id));previous=emptyList();android.util.Log.i("DragonGesture","select part=${target.id}")}
 if(scene.held!=null&&(input.pointerId in active||target?.id==scene.held))active[input.pointerId]=input
 }else active.remove(input.pointerId)
 }
 val hovered=hoverByPointer.values.toSet();if(hovered!=scene.hovered){scene.hovered.clear();scene.hovered.addAll(hovered);android.util.Log.i("DragonGesture","hover=$hovered")}
 val actor=scene.held?.let{scene.actors[it]}
 val handled=actor!=null
 if(actor!=null){
 if(active.isEmpty()||current.charging){android.util.Log.i("DragonGesture","release part=${actor.id} dragging=$dragging position=${actor.anchor.components[TransformComponent::class.java]!!.position}");scene.held=null;active.clear();previous=emptyList();pressOrigin=null;dragging=false}
 else{
 val points=active.values.take(2).map{input->val local=content.convertPosition(Vector3(input.x,input.y,input.z),ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace);owner.convertPositionTo(local,fitted)}
 val at=actor.anchor.components[TransformComponent::class.java]!!
 if(points.size==2&&previous.size==2){
 dragging=true
 val before=previous[1]-previous[0];val after=points[1]-points[0]
 at.setPosition(at.position+((points[0]+points[1])-(previous[0]+previous[1]))*.5f)
 val old=length(before);val new=length(after)
 if(old>.005f&&new>.005f){val scale=(at.scaleVector.x*new/old).coerceIn(.25f,3f);at.setScaleVector(Vector3(scale,scale,scale));at.setQuaternion(rotation(before,after)*at.quaternion)}
 }else if(points.size==1&&previous.size==1){
 val raw=active.values.first();val moved=pressOrigin?.let{length(Vector3(raw.x,raw.y,raw.z)-it)}?:0f
 if(moved>18f)dragging=true
 val delta=points[0]-previous[0]
 if(dragging&&length(delta).isFinite()&&length(delta)<.35f)at.setPosition(at.position+delta)
 }
 if(dragging){actor.rest=at.position;session.poses[actor.id]=DragonPose(at.position,at.scaleVector,at.quaternion)};previous=points
 }
 }
 handled
 }
 ready=true;message=if(ids.isEmpty())"场景为空，重新许愿恢复龙珠"else"";notifyReady()
 android.util.Log.i("DragonScene","loaded parts=${scene.actors.keys} immersive=$immersive blue=${s.summoned}")
 }catch(e:CancellationException){throw e}catch(e:Exception){message="模型加载失败：${e.message}";android.util.Log.e("DragonScene",message,e)}
 })
 if(message.isNotEmpty())Text(message)
 }
 }
}
