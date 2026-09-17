package com.dev.probe.api

import com.dev.probe.Probe
import com.dev.probe.shell.createProbeViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

actual object ProbeLauncher {
    actual fun openHub(ctx: ProbePlatformContext) =
        ProbeViewControllerPresenter.present(screen = ProbeStartScreen.Hub)

    actual fun openInspector(ctx: ProbePlatformContext) =
        ProbeViewControllerPresenter.present(screen = ProbeStartScreen.Inspector)
}

/**
 * Presents the Probe shell full-screen on top of the current key window's view controller
 * stack. There is no hardware back button on iOS, so this also watches [Probe.screen] and
 * auto-dismisses the presented controller once the user closes the hub/inspector from within
 * the shell (screen state returns to `null`).
 */
internal object ProbeViewControllerPresenter {
    private var presented: UIViewController? = null
    private var watcherJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun present(screen: ProbeStartScreen) {
        if (presented != null) {
            routeTo(screen)
            return
        }
        val root = findTopViewController() ?: return
        val viewController = createProbeViewController(screen)
        viewController.modalPresentationStyle = UIModalPresentationFullScreen
        presented = viewController
        root.presentViewController(viewController, animated = true, completion = null)
        watchForClose()
    }

    private fun routeTo(screen: ProbeStartScreen) {
        when (screen) {
            ProbeStartScreen.Hub -> Probe.showHub()
            ProbeStartScreen.Inspector -> Probe.showInspector()
        }
    }

    private fun watchForClose() {
        watcherJob?.cancel()
        var sawScreen = false
        watcherJob = scope.launch {
            Probe.screen.collect { current ->
                if (current != null) {
                    sawScreen = true
                } else if (sawScreen) {
                    dismiss()
                }
            }
        }
    }

    private fun dismiss() {
        watcherJob?.cancel()
        watcherJob = null
        presented?.dismissViewControllerAnimated(true, completion = null)
        presented = null
    }
}

private fun findTopViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow ?: app.windows.firstOrNull() as? UIWindow
    val root = window?.rootViewController ?: return null
    return findTopPresentedViewController(root)
}

private fun findTopPresentedViewController(viewController: UIViewController): UIViewController {
    viewController.presentedViewController?.let { presented ->
        return findTopPresentedViewController(presented)
    }
    if (viewController is UINavigationController) {
        viewController.visibleViewController?.let { visible ->
            return findTopPresentedViewController(visible)
        }
    }
    if (viewController is UITabBarController) {
        viewController.selectedViewController?.let { selected ->
            return findTopPresentedViewController(selected)
        }
    }
    return viewController
}
