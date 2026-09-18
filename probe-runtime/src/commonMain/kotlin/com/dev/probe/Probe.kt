package com.dev.probe

import com.dev.probe.plugin.ProbePlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ProbeScreen {
    HUB,
    NETWORK_MODE,
    INSPECTOR,
    PERMISSIONS,
}

internal object Probe {
    private var _enabled = false
    val isEnabled: Boolean get() = _enabled

    private val _screen = MutableStateFlow<ProbeScreen?>(null)
    val screen: StateFlow<ProbeScreen?> = _screen.asStateFlow()

    private var _plugins: List<ProbePlugin> = emptyList()
    val plugins: List<ProbePlugin> get() = _plugins

    /**
     * Which plugin [ProbeScreen.INSPECTOR] should render — set by [showNetworkModePicker] (the
     * network plugin's own two-mode picker eventually calls [showInspector] without repeating
     * it) or [showPluginInspector] (every other plugin, tapped directly from the hub).
     */
    private var _selectedPluginId: String? = null
    val selectedPluginId: String? get() = _selectedPluginId

    /** When [screen] is [ProbeScreen.INSPECTOR], handles list ↔ detail then exits inspector. */
    private var inspectorBackHandler: (() -> Boolean)? = null

    fun install(plugins: List<ProbePlugin>) {
        _enabled = true
        _plugins = plugins
    }

    fun showHub() {
        _screen.value = ProbeScreen.HUB
    }

    fun showNetworkModePicker() {
        _selectedPluginId = "network"
        _screen.value = ProbeScreen.NETWORK_MODE
    }

    fun showInspector() {
        _screen.value = ProbeScreen.INSPECTOR
    }

    /** Opens [ProbeScreen.INSPECTOR] directly on [pluginId] — every plugin except "network". */
    fun showPluginInspector(pluginId: String) {
        _selectedPluginId = pluginId
        _screen.value = ProbeScreen.INSPECTOR
    }

    fun showPermissions() {
        _screen.value = ProbeScreen.PERMISSIONS
    }

    fun dismissAll() {
        inspectorBackHandler = null
        _selectedPluginId = null
        _screen.value = null
    }

    fun setInspectorBackHandler(handler: (() -> Boolean)?) {
        inspectorBackHandler = handler
    }

    /**
     * Pops one level in the debug UI stack. Returns `true` if handled, `false` if nothing to pop
     * (caller should let the system handle back — e.g. exit the app).
     */
    fun navigateBack(): Boolean = when (_screen.value) {
        null -> false
        ProbeScreen.HUB -> {
            dismissAll()
            true
        }
        ProbeScreen.NETWORK_MODE -> {
            showHub()
            true
        }
        ProbeScreen.PERMISSIONS -> {
            showHub()
            true
        }
        ProbeScreen.INSPECTOR ->
            inspectorBackHandler?.invoke() ?: run {
                dismissAll()
                true
            }
    }
}
