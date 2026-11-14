package cn.mindsoul.pico.wishdragon.data.wishes
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId

data class DragonWish(val id:String,val author:String,val text:String,val shared:Boolean,val date:String,val likes:Set<String>)
class WishRepository(context:Context,storeName:String="dragon_wishes"){
 private val prefs=context.applicationContext.getSharedPreferences(storeName,0)
 val rows=MutableStateFlow(read())
 private fun read():List<DragonWish> = runCatching{
 val array=JSONArray(prefs.getString("wishes","[]"));(0 until array.length()).map{array.getJSONObject(it)}.map{o->val liked=o.optJSONArray("likes")?:JSONArray();DragonWish(o.getString("id"),o.getString("author"),o.getString("text"),o.getBoolean("shared"),o.getString("date"),(0 until liked.length()).map{liked.getString(it)}.toSet())}
 }.getOrDefault(emptyList())
 private fun save(all:List<DragonWish>){val array=JSONArray();all.forEach{r->array.put(JSONObject().put("id",r.id).put("author",r.author).put("text",r.text).put("shared",r.shared).put("date",r.date).put("likes",JSONArray(r.likes.toList())))};prefs.edit().putString("wishes",array.toString()).apply();rows.value=all}
 fun add(author:String,text:String,shared:Boolean):String{
 val clean=text.trim();if(clean.isBlank()||clean.length>100)return "心愿需为1–100字"
 if(rows.value.size>=200)return "本机心愿已达200条，请删除旧记录后再添加"
 save(listOf(DragonWish(java.util.UUID.randomUUID().toString(),author,clean,shared,LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(),emptySet()))+rows.value);return ""
 }
 fun like(id:String,user:String){save(rows.value.map{if(it.id==id&&it.shared)it.copy(likes=if(user in it.likes)it.likes-user else it.likes+user)else it})}
 fun remove(id:String,user:String){save(rows.value.filterNot{it.id==id&&it.author==user})}
 fun visibility(id:String,user:String,shared:Boolean){save(rows.value.map{if(it.id==id&&it.author==user)it.copy(shared=shared)else it})}
 companion object { fun identity(account:String):String=MessageDigest.getInstance("SHA-256").digest(account.toByteArray()).take(8).joinToString(""){"%02x".format(it)} }
}
