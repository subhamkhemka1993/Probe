@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.dev.probe.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

internal actual fun shareFile(fileName: String, content: String, mimeType: String) {
    val presenter = findTopViewControllerForShare() ?: return
    val fileUrl = writeExportFile(fileName, content) ?: return

    val shareController =
        UIActivityViewController(
            activityItems = listOf(fileUrl),
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

private fun writeExportFile(fileName: String, content: String): NSURL? {
    val exportDirPath = NSTemporaryDirectory() + "probe_exports"
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(exportDirPath)) {
        fileManager.createDirectoryAtPath(
            path = exportDirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    val filePath = "$exportDirPath/$fileName"
    val data = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    val written = fileManager.createFileAtPath(path = filePath, contents = data, attributes = null)
    return if (written) NSURL.fileURLWithPath(filePath) else null
}

private fun findTopViewControllerForShare(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow ?: app.windows.firstOrNull() as? UIWindow
    val root = window?.rootViewController ?: return null
    return findTopPresentedViewControllerForShare(root)
}

private fun findTopPresentedViewControllerForShare(viewController: UIViewController): UIViewController {
    viewController.presentedViewController?.let { presented ->
        return findTopPresentedViewControllerForShare(presented)
    }
    if (viewController is UINavigationController) {
        viewController.visibleViewController?.let { visible ->
            return findTopPresentedViewControllerForShare(visible)
        }
    }
    if (viewController is UITabBarController) {
        viewController.selectedViewController?.let { selected ->
            return findTopPresentedViewControllerForShare(selected)
        }
    }
    return viewController
}
