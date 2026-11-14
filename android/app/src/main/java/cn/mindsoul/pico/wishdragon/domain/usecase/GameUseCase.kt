package cn.mindsoul.pico.wishdragon.domain.usecase
import cn.mindsoul.pico.wishdragon.domain.model.*
class GameUseCase {
 fun reduce(s:GameState,e:GameEvent):GameState=when(e){
 is GameEvent.Bci->s.copy(bciMode=e.enabled,charge=0f,charging=false,message=if(e.enabled)"脑机模拟模式：专注力≥70%持续10秒"else"许愿倒计时模式")
 is GameEvent.Part->s.copy(selected=true,selectedPart=e.id)

 is GameEvent.Wish->s.copy(wish=e.text.trim().take(100),message="")
 is GameEvent.Skin->s.copy(choice=e.index.coerceIn(0,1),selected=false,hidden=false)
 GameEvent.CancelCharge->s.copy(charging=false,charge=0f,message="已取消蓄能，可以重新许愿")
 GameEvent.Select->s.copy(selected=s.creatures.isNotEmpty()&&!s.hidden)
 GameEvent.Hide->if(s.selectedPart!=null)s.copy(hiddenParts=s.hiddenParts+s.selectedPart,selected=false,selectedPart=null)else s.copy(hidden=true,selected=false)
 GameEvent.Show->s.copy(hidden=false,hiddenParts=emptySet(),selected=false,selectedPart=null)
 GameEvent.Delete->if(s.selectedPart!=null)s.copy(removedParts=s.removedParts+s.selectedPart,selected=false,selectedPart=null)else s.copy(creatures=emptyList(),selected=false,hidden=false,charging=false,charge=0f,summoned=false,message="已移出场景，重新许愿可恢复龙珠")

 GameEvent.Add->if(s.creatures.size >= if(s.kind=="cat")9 else 12)s else s.copy(creatures=s.creatures+Creature(s.nextId,s.choice),nextId=s.nextId+1)
 GameEvent.Next->if(s.kind=="cat")s.copy(coat=(s.coat+1)%4) else s.copy(choice=(s.choice+1)%(if(s.kind=="dragon")2 else 5))
 GameEvent.Play->if(s.kind=="cat")s.copy(breathing=!s.breathing)else s.copy(paused=!s.paused)
 GameEvent.Reset->s.copy(revision=s.revision+1,hidden=false,hiddenParts=emptySet(),removedParts=emptySet(),selectedPart=null,selected=false)
 GameEvent.Remove->s.copy(creatures=s.creatures.dropLast(1))
 GameEvent.Pet->s.copy(pets=s.pets+1)
 GameEvent.Start->if(s.wish.isBlank())s.copy(message="请先写下你的心愿")else s.copy(charge=0f,charging=true,summoned=false,revision=s.revision+1,hidden=false,hiddenParts=emptySet(),removedParts=emptySet(),selectedPart=null,selected=false,creatures=listOf(Creature(0,s.choice)),message="")
 is GameEvent.Focus->s.copy(focus=e.value.coerceIn(0,100),charge=if(s.bciMode&&e.value<70)0f else s.charge)
 is GameEvent.Tick->if(s.charging){val charge=if(!s.bciMode||s.focus>=70)(s.charge+e.seconds.coerceIn(0f,.25f)).coerceAtMost(10f)else 0f;s.copy(charge=charge,summoned=charge>=10f,charging=charge<10f)}else s
 GameEvent.Channels->s.copy(channels=if(s.channels==3)32 else 3,page=0)
 GameEvent.Page->s.copy(page=if(s.channels==32)(s.page+1)%4 else 0)
 GameEvent.Waves->s.copy(waves=!s.waves)
 is GameEvent.Device->s.copy(device=e.name)
 is GameEvent.Immersive->s.copy(immersive=e.active)
 is GameEvent.Message->s.copy(message=e.text)
 }
}
