package com.pmtalk.mindsoul.xr.core.xr

import androidx.xr.scenecore.GltfAnimationStartOptions
import androidx.xr.scenecore.GltfModelEntity

fun GltfModelEntity.playAnimation(animationName: String? = null, loop: Boolean = true) {
    val animation = animationName?.let { name -> animations.firstOrNull { it.name == name } }
        ?: animations.firstOrNull()
        ?: return
    animations.forEach { if (it != animation) runCatching { it.stop() } }
    animation.start(GltfAnimationStartOptions(shouldLoop = loop))
}
