package com.dev.probe.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
internal actual fun shareText(text: String) {
    val presenter = findTopViewController() ?: return
    val shareController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null,
    )
    shareController.popoverPresentationController?.let { popover ->
        popover.sourceView = presenter.view
        popover.sourceRect = presenter.view.bounds
    }
    presenter.presentViewController(
        viewControllerToPresent = shareController,
        animated = true,
        completion = null,
    )
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
