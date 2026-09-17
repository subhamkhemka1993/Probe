package com.dev.probe.platform

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.ProbePlatformHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File

/**
 * Robolectric can't parse [FileProvider]'s `<meta-data>` path-strategy XML (see
 * https://github.com/robolectric/robolectric/issues/2199), so [ShadowFileProvider] stubs
 * [FileProvider.getUriForFile] to keep this test focused on [shareFile]'s own behaviour: writing
 * the export to the cache directory before handing off to the share intent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShareFileAndroidTest.ShadowFileProvider::class])
class ShareFileAndroidTest {
    @After
    fun tearDown() {
        ProbePlatformHolder.clear()
    }

    @Test
    fun shareFile_writesContentToCacheExportDir() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        ProbePlatformHolder.init(ProbePlatformContext(context))

        shareFile(fileName = "session.json", content = "[]", mimeType = "application/json")

        val exportedFile = File(File(context.cacheDir, "probe_exports"), "session.json")
        assert(exportedFile.exists()) { "Expected exported file to be written to cache" }
        assert(exportedFile.readText() == "[]")
    }

    @Implements(FileProvider::class)
    class ShadowFileProvider {
        companion object {
            @JvmStatic
            @Implementation
            fun getUriForFile(
                context: Context,
                authority: String,
                file: File,
            ): Uri = Uri.parse("content://$authority/${file.name}")
        }
    }
}
