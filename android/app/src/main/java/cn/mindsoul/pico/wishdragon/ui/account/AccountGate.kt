package cn.mindsoul.pico.wishdragon.ui.account
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable
import com.pico.spatial.ui.design.windows.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pico.spatial.ui.design.*
import cn.mindsoul.pico.wishdragon.R
import cn.mindsoul.pico.wishdragon.account.AuthFailure
import cn.mindsoul.pico.wishdragon.account.ForumAuthClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AccountViewModel(context: Context) : ViewModel() {
    private val client = ForumAuthClient.from(context)
    val user = MutableStateFlow<String?>(null)
    fun enterGuest() { user.value = "guest" }

    private fun api(): ForumAuthClient = client ?: throw AuthFailure("账号服务尚未配置。")

    suspend fun requestCode(email: String): String {
        val mail = ForumAuthClient.email(email)
        if (!ForumAuthClient.validEmail(mail)) return "请输入有效邮箱"
        return try {
            api().sendCode(mail)
            "验证码已发送到邮箱，请查收。"
        } catch (e: AuthFailure) {
            e.message ?: "发送失败"
        } catch (_: Exception) {
            "无法连接账号服务，请检查网络"
        }
    }

    suspend fun loginWithCode(email: String, code: String): String {
        val mail = ForumAuthClient.email(email)
        if (!ForumAuthClient.validEmail(mail)) return "请输入有效邮箱"
        if (!ForumAuthClient.validCode(code)) return "请输入 6 位邮箱验证码"
        return try {
            val session = api().loginByCode(mail, code)
            user.value = session.nickname.ifBlank { session.email }
            ""
        } catch (e: AuthFailure) {
            e.message ?: "登录失败"
        } catch (_: Exception) {
            "无法连接账号服务，请检查网络"
        }
    }

    suspend fun submit(email: String, password: String, confirm: String, register: Boolean, code: String = ""): String {
        val mail = ForumAuthClient.email(email)
        if (!ForumAuthClient.validEmail(mail)) return "请输入有效邮箱"
        return try {
            if (register) {
                if (!ForumAuthClient.validPassword(password)) return "密码需为 6–20 位字母或数字"
                if (password != confirm) return "两次密码不一致"
                if (!ForumAuthClient.validCode(code)) return "请输入 6 位邮箱验证码"
                val session = api().register(mail, password, code)
                user.value = session.nickname.ifBlank { session.email }
            } else {
                if (password.isEmpty()) return "请输入密码"
                val session = api().login(mail, password)
                user.value = session.nickname.ifBlank { session.email }
            }
            ""
        } catch (e: AuthFailure) {
            e.message ?: "操作失败"
        } catch (_: Exception) {
            "无法连接账号服务，请检查网络"
        }
    }
}

@Composable fun rememberAccount(): AccountViewModel {
    val context = LocalContext.current
    return remember { AccountViewModel(context) }
}

@Composable fun AccountGate(account: AccountViewModel) {
    var splash by rememberSaveable { mutableStateOf(true) }
    var register by rememberSaveable { mutableStateOf(false) }
    var passwordLogin by rememberSaveable { mutableStateOf(false) }
    var policy by rememberSaveable { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeHint by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    if (splash) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                GuestEntry(account::enterGuest)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Image(painterResource(R.mipmap.ic_spatial_launcher), "意念神龙", Modifier.size(188.dp).clip(RoundedCornerShape(44.dp)))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("意念神龙", style = PicoTheme.typography.headlineLarge, fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary)
                    Text("让专注点亮你的愿望", style = PicoTheme.typography.bodyLarge, fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Normal, color = PicoTheme.colorScheme.labelSecondary, textAlign = TextAlign.Center)
                }
            }
            cn.mindsoul.pico.wishdragon.ui.game.components.MindSoulSupport()
            Button(onClick = { splash = false }, modifier = Modifier.align(Alignment.CenterHorizontally).height(56.dp)) {
                Text("开始找神龙许愿吧！", style = PicoTheme.typography.titleMedium)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Image(painterResource(R.mipmap.ic_spatial_launcher), "意念神龙", Modifier.size(88.dp).clip(RoundedCornerShape(22.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("意念神龙", style = PicoTheme.typography.headlineLarge, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                    Text("让专注点亮你的愿望", style = PicoTheme.typography.bodyMedium, color = PicoTheme.colorScheme.labelSecondary)
                }
                GuestEntry(account::enterGuest, enabled = !busy)
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (register) "注册账号" else "登录 · 开始许愿", modifier = Modifier.weight(1f), style = PicoTheme.typography.headlineSmall)
                    if (!register) Button(enabled = !busy, onClick = { passwordLogin = !passwordLogin; message = ""; password = "" }) {
                        Text(if (passwordLogin) "使用邮箱验证码" else "使用密码登录")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(value = email, onValueChange = { email = it; code = ""; codeHint = ""; message = "" }, placeholder = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), enabled = !busy)
                    if (register || !passwordLogin) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextField(value = code, onValueChange = { code = it.filter { c -> c in '0'..'9' }.take(6) }, placeholder = { Text("邮箱验证码") }, singleLine = true, modifier = Modifier.weight(1f).heightIn(min = 60.dp), enabled = !busy)
                            Button(enabled = !busy, onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        codeHint = account.requestCode(email)
                                        message = ""
                                    } finally {
                                        busy = false
                                    }
                                }
                            }) { Text("获取验证码") }
                        }
                        if (codeHint.isNotEmpty()) Text(codeHint, style = PicoTheme.typography.bodySmall)
                    }
                    if (register || passwordLogin) TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(if (register) "设置密码（6–20 位字母或数字）" else "密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        enabled = !busy,
                    )
                    if (register) TextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        placeholder = { Text("再次输入密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        enabled = !busy,
                    )
                    if (message.isNotEmpty()) Text(message)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(enabled = !busy, modifier = Modifier.height(56.dp), onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    message = if (!register && !passwordLogin) account.loginWithCode(email, code)
                                    else account.submit(email, password, confirm, register, code)
                                    if (message.isEmpty()) {
                                        password = ""; confirm = ""; code = ""
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    message = "账号操作失败，请重试"
                                } finally {
                                    busy = false
                                }
                            }
                        }) { Text(if (busy) "处理中…" else if (register) "完成注册并进入" else "登录并开始许愿") }
                        Button(enabled = !busy, onClick = {
                            register = !register; code = ""; codeHint = ""; message = ""; password = ""; confirm = ""
                        }) { Text(if (register) "已有账号，去登录" else "没有账号，去注册") }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !busy, onClick = { splash = true }) { Text("返回开屏") }
                    Link(onClick = { policy = "terms" }) { Text("用户协议", textDecoration = TextDecoration.Underline) }
                    Link(onClick = { policy = "privacy" }) { Text("隐私政策", textDecoration = TextDecoration.Underline) }
                }
                Text("验证码由服务端发送到邮箱", style = PicoTheme.typography.bodySmall)
                cn.mindsoul.pico.wishdragon.ui.game.components.MindSoulSupport()
            }
        }
    }
    policy?.let { kind ->
        AlertDialog(onDismissRequest = { policy = null }, title = { Text(if (kind == "privacy") "隐私政策" else "用户协议") }, content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (kind == "privacy") {
                        Text("1. 游客模式：无需提供邮箱、密码，不创建账号。")
                        Text("2. 邮箱账号：注册与登录通过脑云矩阵账号服务（https://mindsoul.cn/forum）完成，邮箱验证码由服务端发送。")
                        Text("3. 体验数据：海洋生物外观、互动与脑电演示主要在本机运行；脑电为模拟数据，不采集真实脑电。")
                        Text("喜爱榜与心愿墙、照片与模型素材：保存在本设备应用私有目录，不随账号自动上传。清除应用数据或卸载将删除这些记录。")
                        Text("4. 外部页面：主动点击 MindSoul 官网后，由浏览器打开外部网站，该网站的数据处理规则需另行查看。")
                    } else {
                        Text("1. 意念神龙提供海洋生物陪伴与交互体验。你可以选择游客模式，或使用邮箱注册登录脑云矩阵账号。")
                        Text("2. 脑机接口面板与脑电波形为模拟演示，不代表设备已连接，不用于医疗诊断或治疗。")
                        Text("3. 请在周围空间安全、身体舒适时使用，感到不适时停止体验。")
                    }
                }
                Button(onClick = { policy = null }, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
            }
        })
    }
}

@Composable private fun GuestEntry(onClick: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(24.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PicoTheme.colorScheme.labelPrimary),
    ) {
        Text("游客登录 ↗")
    }
}
