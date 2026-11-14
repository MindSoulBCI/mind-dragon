package cn.mindsoul.pico.wishdragon
import cn.mindsoul.pico.wishdragon.domain.model.LocalVerification
import org.junit.Test
import org.junit.Assert.*
class LocalVerificationTest {
 private var time=0L
 private val service=LocalVerification({time},{"012345"})
 @Test fun codeIsBoundToEmailAndPurpose(){service.issue("a@example.com","register");assertTrue(service.verify("b@example.com","register","012345").isNotEmpty());assertTrue(service.verify("a@example.com","login","012345").isNotEmpty());assertEquals("",service.verify("a@example.com","register","012345"))}
 @Test fun usedCodeCannotBeReplayed(){service.issue("a","login");assertEquals("",service.verify("a","login","012345"));assertTrue(service.verify("a","login","012345").isNotEmpty())}
 @Test fun expiresAtFiveMinutes(){service.issue("a","login");time=300000;assertTrue(service.verify("a","login","012345").contains("过期"))}
 @Test fun resendIsLimitedAndReplacesChallenge(){service.issue("a","login");assertTrue(service.issue("b","login").contains("等待"));time=60000;assertTrue(service.issue("b","login").contains("012345"));assertTrue(service.verify("a","login","012345").isNotEmpty());assertEquals("",service.verify("b","login","012345"))}
 @Test fun fiveWrongAttemptsLockChallenge(){service.issue("a","login");repeat(5){assertTrue(service.verify("a","login","999999").isNotEmpty())};assertTrue(service.verify("a","login","012345").contains("次数过多"))}
}
