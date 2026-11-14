package cn.mindsoul.pico.wishdragon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.mindsoul.pico.wishdragon.account.ForumAuthClient
import cn.mindsoul.pico.wishdragon.ui.account.AccountViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalAccountTest {
    @Test
    fun forumClientConfiguredAndRulesMatchServer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(ForumAuthClient.from(context))
        assertTrue(ForumAuthClient.validEmail("user@example.com"))
        assertFalse(ForumAuthClient.validEmail("bad"))
        assertTrue(ForumAuthClient.validPassword("Pass12"))
        assertFalse(ForumAuthClient.validPassword("短"))
        assertTrue(ForumAuthClient.validCode("123456"))
        AccountViewModel(context)
    }
}
