package cn.mindsoul.pico.wishdragon.domain.model
import java.security.SecureRandom
/** Simulation only: this code is displayed locally and never verifies mailbox ownership. */
class LocalVerification(private val now:()->Long={System.nanoTime()/1_000_000},private val generate:()->String={"%06d".format(SecureRandom().nextInt(1000000))}) {
 private data class Challenge(val email:String,val purpose:String,val code:String,val expires:Long,var attempts:Int=0)
 private var challenge:Challenge?=null
 private var lastIssued:Long?=null
 fun issue(email:String,purpose:String):String {
 val time=now()
 lastIssued?.let{if(time-it<60000)return "请等待 ${((60000-(time-it)+999)/1000)} 秒后重新获取"}
 val code=generate();challenge=Challenge(email,purpose,code,time+300000);lastIssued=time
 return "本机模拟验证码：$code（5分钟有效，不发送邮件）"
 }
 fun verify(email:String,purpose:String,code:String):String {
 val c=challenge?:return "请先获取本机模拟验证码"
 if(c.email!=email||c.purpose!=purpose)return "邮箱或验证用途已变化，请重新获取验证码"
 if(now()>=c.expires){challenge=null;return "验证码已过期，请重新获取"}
 if(c.attempts>=5)return "错误次数过多，请重新获取验证码"
 if(c.code!=code.trim()){c.attempts++;return "验证码不正确"}
 challenge=null
 return ""
 }
}
