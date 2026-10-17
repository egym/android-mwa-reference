package com.ionicexample.myapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.ionic.liveupdates.LiveUpdateManager
import io.ionic.liveupdates.data.model.FailResult
import io.ionic.liveupdates.data.model.SyncResult
import io.ionic.liveupdates.network.SyncCallback
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Syncs the live update of an MWA before its Portal shows, as the BMA apps do, so the first open is never blank. */
object LiveUpdateLoader {
    private const val SYNC_TIMEOUT_MS = 30_000L
    private const val BUILD_CHECK_ATTEMPTS = 30
    private const val BUILD_CHECK_DELAY_MS = 100L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val syncWorker = Executors.newSingleThreadExecutor()
    private val checkWorker = Executors.newSingleThreadExecutor()

    /**
     * Calls [onResult] once on the main thread: true when a valid build is on the device after the sync, after a failed
     * sync or after 30 seconds. A build in the APK assets under [assetDir] also counts. The app id must already be
     * registered, as `PortalBuilder.setLiveUpdateConfig` does.
     */
    fun load(context: Context, appId: String, assetDir: String, onResult: (hasBuild: Boolean) -> Unit) {
        val appContext = context.applicationContext
        val isFinished = AtomicBoolean(false)
        val hasFailed = AtomicBoolean(false)

        fun finish(waitForBuild: Boolean) {
            if (!isFinished.compareAndSet(false, true)) return
            checkWorker.execute {
                val hasDownloadedBuild =
                    if (waitForBuild) awaitDownloadedBuild(appContext, appId) else hasDownloadedBuild(appContext, appId)
                val hasBuild = hasDownloadedBuild || hasAssetBuild(appContext, assetDir)
                mainHandler.post { onResult(hasBuild) }
            }
        }

        val timeout = Runnable { finish(waitForBuild = false) }
        mainHandler.postDelayed(timeout, SYNC_TIMEOUT_MS)
        syncWorker.execute {
            LiveUpdateManager.sync(appContext, appId, object : SyncCallback {
                override fun onAppComplete(syncResult: SyncResult) = Unit

                override fun onAppComplete(failResult: FailResult) {
                    hasFailed.set(true)
                }

                override fun onSyncComplete() {
                    mainHandler.removeCallbacks(timeout)
                    finish(waitForBuild = !hasFailed.get())
                }
            })
        }
    }

    private fun hasDownloadedBuild(context: Context, appId: String): Boolean =
        LiveUpdateManager.getLatestAppDirectory(context, appId)?.let { File(it, "index.html").isFile } == true

    // The SDK can report the new folder shortly after its callback.
    private fun awaitDownloadedBuild(context: Context, appId: String): Boolean {
        repeat(BUILD_CHECK_ATTEMPTS) {
            if (hasDownloadedBuild(context, appId)) return true
            Thread.sleep(BUILD_CHECK_DELAY_MS)
        }
        return hasDownloadedBuild(context, appId)
    }

    // A build that app/build.gradle packed into the APK, which the Portal opens from its start directory.
    private fun hasAssetBuild(context: Context, assetDir: String): Boolean =
        runCatching { context.assets.open("$assetDir/index.html").close() }.isSuccess
}
