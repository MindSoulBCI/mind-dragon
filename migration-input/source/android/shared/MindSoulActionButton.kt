package com.pmtalk.mindsoul.xr.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun MindSoulActionButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true){
 val source=remember{MutableInteractionSource()};val hover by source.collectIsHoveredAsState()
 OutlinedButton(onClick,modifier.heightIn(min=44.dp),enabled=enabled,interactionSource=source,
  shape=RoundedCornerShape(14.dp),border=BorderStroke(if(hover&&enabled)2.dp else 1.5.dp,if(enabled)Color(0xFFA6D6C7) else Color(0xFF77767E)),
  colors=ButtonDefaults.outlinedButtonColors(containerColor=if(hover)Color(0xFF38554E) else Color(0xFF293D3A),contentColor=Color.White,disabledContainerColor=Color(0xFF29282F),disabledContentColor=Color(0xFFB8B7BF))){Text(text)}
}
