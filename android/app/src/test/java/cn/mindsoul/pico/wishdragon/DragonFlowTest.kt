package cn.mindsoul.pico.wishdragon
import org.junit.Test
import org.junit.Assert.*
import cn.mindsoul.pico.wishdragon.domain.model.*
import cn.mindsoul.pico.wishdragon.domain.usecase.GameUseCase
class DragonFlowTest {
 private val rules=GameUseCase()
 @Test fun cannotStartWithoutWish(){val s=rules.reduce(GameState(),GameEvent.Start);assertFalse(s.charging);assertTrue(s.message.isNotEmpty())}
 @Test fun wishIsTrimmedAndBounded(){assertEquals("心愿",rules.reduce(GameState(),GameEvent.Wish(" 心愿 ")).wish);assertEquals(100,rules.reduce(GameState(),GameEvent.Wish("愿".repeat(110))).wish.length)}
 @Test fun cancelPreventsLaterSummon(){var s=rules.reduce(GameState(wish="心愿",focus=90),GameEvent.Start);repeat(20){s=rules.reduce(s,GameEvent.Tick(.25f))};s=rules.reduce(s,GameEvent.CancelCharge);repeat(80){s=rules.reduce(s,GameEvent.Tick(.25f))};assertFalse(s.summoned);assertEquals(0f,s.charge)}
 @Test fun newWishRestoresDeletedDragonBalls(){val empty=rules.reduce(GameState(wish="心愿",creatures=listOf(Creature(0,0)),summoned=true),GameEvent.Delete);val fresh=rules.reduce(empty,GameEvent.Start);assertEquals(1,fresh.creatures.size);assertTrue(fresh.charging);assertFalse(fresh.summoned)}
 @Test fun hideShowKeepsSummonState(){val s=GameState(summoned=true,creatures=listOf(Creature(0,0)));val hidden=rules.reduce(s,GameEvent.Hide);assertTrue(hidden.hidden);val restored=rules.reduce(hidden,GameEvent.Show);assertFalse(restored.hidden);assertTrue(restored.summoned)}
 @Test fun chargeCannotJumpDueToLargeFrame(){val s=rules.reduce(GameState(charging=true,focus=80),GameEvent.Tick(60f));assertEquals(.25f,s.charge);assertFalse(s.summoned)}
 @Test fun normalCountdownDoesNotRequireFocus(){var s=rules.reduce(GameState(wish="平安",focus=0),GameEvent.Start);repeat(40){s=rules.reduce(s,GameEvent.Tick(.25f))};assertTrue(s.summoned)}
 @Test fun individualBallsCanHideAndDelete(){var s=rules.reduce(GameState(),GameEvent.Part(3));s=rules.reduce(s,GameEvent.Hide);assertEquals(setOf(3),s.hiddenParts);assertFalse(s.hidden);s=rules.reduce(s,GameEvent.Part(4));s=rules.reduce(s,GameEvent.Delete);assertEquals(setOf(4),s.removedParts);s=rules.reduce(s,GameEvent.Reset);assertTrue(s.hiddenParts.isEmpty());assertTrue(s.removedParts.isEmpty())}
}
