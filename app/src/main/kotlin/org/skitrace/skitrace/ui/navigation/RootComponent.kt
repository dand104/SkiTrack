package org.skitrace.skitrace.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.MainTabs,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Config, context: ComponentContext): Child {
        return when (config) {
            is Config.MainTabs -> Child.MainTabs
            is Config.Settings -> Child.Settings
            is Config.Details -> Child.Details(config.runId)
        }
    }

    fun navigateToSettings() = navigation.pushNew(Config.Settings)
    fun navigateToDetails(runId: Long) = navigation.pushNew(Config.Details(runId))
    fun navigateBack() = navigation.pop()

    @Serializable
    sealed class Config {
        @Serializable data object MainTabs : Config()
        @Serializable data object Settings : Config()
        @Serializable data class Details(val runId: Long) : Config()
    }

    sealed class Child {
        data object MainTabs : Child()
        data object Settings : Child()
        data class Details(val runId: Long) : Child()
    }
}