package cn.mindsoul.pico.wishdragon.domain.model

data class Creature(val id:Int,val species:Int)
data class GameState(
 val kind:String="dragon",val title:String="意念神龙",val creatures:List<Creature> = emptyList(),
 val bciMode:Boolean=false,val selectedPart:Int?=null,val hiddenParts:Set<Int> = emptySet(),val removedParts:Set<Int> = emptySet(),val wish:String="",val selected:Boolean=false,val hidden:Boolean=false,val choice:Int=0,val coat:Int=0,val breathing:Boolean=false,val paused:Boolean=false,
 val pets:Int=0,val revision:Int=0,val nextId:Int=3,val focus:Int=50,
 val charge:Float=0f,val charging:Boolean=false,val summoned:Boolean=false,
 val channels:Int=3,val page:Int=0,val waves:Boolean=true,val device:String="未选择",
 val immersive:Boolean=false,val message:String=""
)
sealed interface GameEvent {
 data class Bci(val enabled:Boolean):GameEvent
 data class Part(val id:Int):GameEvent
 data class Wish(val text:String):GameEvent
 data class Skin(val index:Int):GameEvent
 data object CancelCharge:GameEvent
 data object Select:GameEvent
 data object Hide:GameEvent
 data object Show:GameEvent
 data object Delete:GameEvent

 data object Add:GameEvent; data object Next:GameEvent;data object Play:GameEvent
 data object Reset:GameEvent;data object Remove:GameEvent;data object Pet:GameEvent
 data object Start:GameEvent;data class Focus(val value:Int):GameEvent
 data class Tick(val seconds:Float):GameEvent;data object Channels:GameEvent
 data object Page:GameEvent;data object Waves:GameEvent;data class Device(val name:String):GameEvent
 data class Immersive(val active:Boolean):GameEvent;data class Message(val text:String):GameEvent
}
