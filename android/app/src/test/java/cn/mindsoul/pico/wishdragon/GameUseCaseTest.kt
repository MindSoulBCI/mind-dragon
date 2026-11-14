package cn.mindsoul.pico.wishdragon
import org.junit.Test
import org.junit.Assert.*
import cn.mindsoul.pico.wishdragon.domain.model.*
import cn.mindsoul.pico.wishdragon.domain.usecase.GameUseCase
class GameUseCaseTest {
 val rules=GameUseCase()
 @Test fun chargeRequiresContinuousThreshold(){var s=GameState(kind="dragon",bciMode=true,charging=true,focus=70);repeat(39){s=rules.reduce(s,GameEvent.Tick(.25f))};assertFalse(s.summoned);s=rules.reduce(s,GameEvent.Focus(69));assertEquals(0f,s.charge);s=rules.reduce(s,GameEvent.Tick(.25f));assertEquals(0f,s.charge)}
 @Test fun summonsAfterTenSeconds(){var s=GameState(kind="dragon",bciMode=true,charging=true,focus=70);repeat(40){s=rules.reduce(s,GameEvent.Tick(.25f))};assertTrue(s.summoned);assertFalse(s.charging)}
 @Test fun cannotExceedCatLimit(){var s=GameState(kind="cat");repeat(20){s=rules.reduce(s,GameEvent.Add)};assertEquals(9,s.creatures.size)}
 @Test fun pageResetsWhenChangingChannels(){val s=rules.reduce(GameState(channels=32,page=3),GameEvent.Channels);assertEquals(3,s.channels);assertEquals(0,s.page)}
 @Test fun aquariumCanBecomeEmpty(){val s=rules.reduce(GameState(kind="aquarium",creatures=listOf(Creature(0,0))),GameEvent.Remove);assertTrue(s.creatures.isEmpty());assertTrue(rules.reduce(s,GameEvent.Remove).creatures.isEmpty())}
}
