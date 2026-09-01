package com.example.ai.capabilities.ecology

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class AutonomousMetabolismTest {

    @Test
    fun testMetabolismWorkerExecutesSuccessfully() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // CoroutineWorker requires TestListenableWorkerBuilder instead of TestWorkerBuilder
        val worker = TestListenableWorkerBuilder<EcologyMetabolismWorker>(context).build()
        
        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Success)
    }
}
