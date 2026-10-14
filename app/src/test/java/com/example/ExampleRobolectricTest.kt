package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// SDK 33, matching every other Robolectric test here.
//
// sdk = [36] passes locally on JDK 21 and fails on CI's JDK 17 with
// `UnsupportedOperationException at DefaultSdkProvider.java:170` — Robolectric's
// Android 16 image needs a newer JDK than the build runs on. Measured on one CI
// run: all 9 tests at sdk = [33] passed on that runner; the single sdk = [36]
// test was the only non-service failure. Raising the build's JDK is the other
// fix and is a much larger change than this test justifies.
@Config(sdk = [33])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("M. Engine", appName)
  }
}
