package com.pmtalk.mindsoul.xr.ui.screen.bcidata

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pmtalk.mindsoul.xr.core.di.LocalUserViewModel
import java.io.File
import kotlinx.coroutines.*

internal data class EegDiskUsage(val bytes: Long = 0, val free: Long = 0)
@Composable
internal fun rememberEegDiskUsage(): EegDiskUsage {
    val context = LocalContext.current
    var usage by remember { mutableStateOf(EegDiskUsage()) }
    LaunchedEffect(context) {
        while (true) {
            usage = withContext(Dispatchers.IO) {
                val dirs = listOfNotNull(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), File(context.filesDir,"documents"), File(context.filesDir,"brain-data"))
                val bytes = dirs.distinctBy { it.canonicalPath }.sumOf { dir ->
                    dir.walkTopDown().filter { it.isFile && ((it.name.startsWith("ihnnk_eeg_") && it.extension == "csv") || it.name.endsWith(".eeg.csv")) }.sumOf { it.length() }
                }
                EegDiskUsage(bytes, context.filesDir.usableSpace)
            }
            delay(3000)
        }
    }
    return usage
}
internal fun durationText(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
}

/** Foreground session time; persisted per account and game, independent of panel resets. */
@Composable
internal fun GameSessionSummary(gameId: String) {
    val context = LocalContext.current
    val user by LocalUserViewModel.current.state.collectAsState()
    val owner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("game-duration",0) }
    val key = "${user.userId}:$gameId"
    var elapsed by remember(key) { mutableLongStateOf(0) }
    val previous = remember(key) { prefs.getLong(key,0) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner,key) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!foreground) prefs.edit().putLong(key,previous+elapsed).apply()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); prefs.edit().putLong(key,previous+elapsed).apply() }
    }
    LaunchedEffect(key,foreground) {
        if (foreground) {
            var last = SystemClock.elapsedRealtime()
            try { while (true) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                elapsed += now-last; last=now
                prefs.edit().putLong(key,previous+elapsed).apply()
            } } finally { elapsed += SystemClock.elapsedRealtime()-last; prefs.edit().putLong(key,previous+elapsed).apply() }
        }
    }
    val usage = rememberEegDiskUsage()
    val filled = kotlin.math.ceil(20.0 * usage.bytes / (usage.bytes+usage.free).coerceAtLeast(1)).toInt()
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f)) {
            Text("本机 EEG 存储 ${android.text.format.Formatter.formatFileSize(context,usage.bytes)}",style=MaterialTheme.typography.bodySmall)
            com.pmtalk.mindsoul.xr.ui.component.EegStorageBattery(usage.bytes,usage.free,Modifier.padding(top=5.dp),compact=true)
        }
        Column { Text("本次任务时长",style=MaterialTheme.typography.bodySmall);Text(durationText(elapsed)) }
        Column { Text("游戏累计时长",style=MaterialTheme.typography.bodySmall);Text(durationText(previous+elapsed)) }
    }
}
