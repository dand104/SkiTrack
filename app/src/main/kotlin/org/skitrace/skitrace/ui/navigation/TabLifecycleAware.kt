package org.skitrace.skitrace.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation

class TabLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    fun updateState(isActive: Boolean, parentState: Lifecycle.State) {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return

        val targetState = if (isActive) {
            parentState
        } else {
            if (parentState.isAtLeast(Lifecycle.State.CREATED)) {
                Lifecycle.State.CREATED
            } else {
                parentState
            }
        }

        if (lifecycleRegistry.currentState != targetState) {
            lifecycleRegistry.currentState = targetState
        }
    }

    fun destroy() {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}

@Composable
fun TabLifecycleAware(
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    val parentLifecycle = LocalLifecycleOwner.current.lifecycle
    val tabLifecycleOwner = remember(parentLifecycle) { TabLifecycleOwner() }

    DisposableEffect(isActive, parentLifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            tabLifecycleOwner.updateState(isActive, parentLifecycle.currentState)
        }
        parentLifecycle.addObserver(observer)

        tabLifecycleOwner.updateState(isActive, parentLifecycle.currentState)

        onDispose {
            parentLifecycle.removeObserver(observer)
            tabLifecycleOwner.updateState(isActive = false, parentState = parentLifecycle.currentState)
        }
    }

    LaunchedEffect(Unit) {
        try {
            awaitCancellation()
        } finally {
            tabLifecycleOwner.destroy()
        }
    }

    CompositionLocalProvider(LocalLifecycleOwner provides tabLifecycleOwner) {
        content()
    }
}