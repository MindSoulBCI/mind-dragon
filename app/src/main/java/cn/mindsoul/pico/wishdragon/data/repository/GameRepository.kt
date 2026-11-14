package cn.mindsoul.pico.wishdragon.data.repository
import cn.mindsoul.pico.wishdragon.domain.model.*
interface GameRepository { fun initial():GameState }
class LocalGameRepository:GameRepository {
 override fun initial():GameState = GameState().let { s -> s.copy(creatures=when(s.kind){
 "cat"->listOf(Creature(0,0),Creature(1,1));"aquarium"->listOf(Creature(0,0),Creature(1,3),Creature(2,4));else->listOf(Creature(0,0))}) }
}
