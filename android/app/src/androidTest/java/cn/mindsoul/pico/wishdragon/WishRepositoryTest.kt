package cn.mindsoul.pico.wishdragon
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import cn.mindsoul.pico.wishdragon.data.wishes.WishRepository
@RunWith(AndroidJUnit4::class)
class WishRepositoryTest {
 @Test fun ownershipPrivacyLikesAndPersistence(){
 val context=InstrumentationRegistry.getInstrumentation().targetContext
 val store="wish-test-${System.nanoTime()}"
 try {
 val repo=WishRepository(context,store)
 assertTrue(repo.add("alice"," ",false).isNotEmpty())
 assertEquals("",repo.add("alice"," 私人心愿 ",false))
 val id=repo.rows.value.single().id
 repo.like(id,"bob");assertTrue(repo.rows.value.single().likes.isEmpty())
 repo.visibility(id,"bob",true);assertFalse(repo.rows.value.single().shared)
 repo.remove(id,"bob");assertEquals(1,repo.rows.value.size)
 repo.visibility(id,"alice",true);repo.like(id,"bob");assertEquals(setOf("bob"),repo.rows.value.single().likes)
 repo.like(id,"bob");assertTrue(repo.rows.value.single().likes.isEmpty())
 repo.like(id,"bob")
 val restored=WishRepository(context,store);assertEquals(repo.rows.value,restored.rows.value)
 repo.visibility(id,"alice",false);assertFalse(repo.rows.value.single().shared)
 repo.remove(id,"alice");assertTrue(repo.rows.value.isEmpty())
 }finally{context.getSharedPreferences(store,0).edit().clear().commit()}
 }
}
