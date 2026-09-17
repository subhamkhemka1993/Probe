package com.dev.probe.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.ProbePlatformHolder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareTextAndroidTest {
    @After
    fun tearDown() {
        ProbePlatformHolder.clear()
    }

    @Test
    fun shareText_doesNotRequireKoin() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        ProbePlatformHolder.init(ProbePlatformContext(context))
        shareText("curl https://example.com")
    }
}
