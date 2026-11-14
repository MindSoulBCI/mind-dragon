package cn.mindsoul.pico.wishdragon.ui.game.components
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.*
import cn.mindsoul.pico.wishdragon.R
@Composable fun MindSoulSupport(){
 val context=LocalContext.current
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
 Link(onClick={try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://mindsoul.cn")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:android.content.ActivityNotFoundException){Toast.makeText(context,"请使用浏览器访问 mindsoul.cn",Toast.LENGTH_SHORT).show()}}){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
 Image(painterResource(R.drawable.mindsoul_support),"MindSoul 官网",Modifier.size(36.dp),colorFilter=ColorFilter.tint(Color.White)) // design-style: fixed-figma-color 规范指定白色品牌Logo
 Text("由 MindSoul 脑机接口基座大模型提供支持",style=PicoTheme.typography.bodySmall)
 }
 }
 }
}
