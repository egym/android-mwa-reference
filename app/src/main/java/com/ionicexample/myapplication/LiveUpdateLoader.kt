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

/** Syncs the live update of an MWA before its Portal shows, as the BMA apps do, so the first open is never blank. */
object LiveUpdateLoader {
    private const val SYNC_TIMEOUT_MS = 30_000L
    private const val BUILD_CHECK_ATTEMPTS = 30
    private const val BUILD_CHECK_DELAY_MS = 100L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newCachedThreadPool()

    // The SDK skips a sync while one runs for the same app, so a second load waits for the running one. Main thread only.
    private val runningSyncs = mutableMapOf<String, RunningSync>()

    private class RunningSync {
        @Volatile var previousBuild: File? = null
        @Volatile var hasNewBuild = false
        val waiters = mutableListOf<(RunningSync) -> Unit>()
    }

    /**
     * Calls [onResult] once on the main thread: true when a valid build is on the device after the sync, after a failed
     * sync or after 30 seconds. A build in the APK assets under [assetDir] also counts. Call it on the main thread. The
     * app id must already be registered, as `PortalBuilder.setLiveUpdateConfig` does.
     */
    fun load(context: Context, appId: String, assetDir: String, onResult: (hasBuild: Boolean) -> Unit) {
        val appContext = context.applicationContext
        var isFinished = false
        lateinit var timeout: Runnable

        fun finish(sync: RunningSync?) {
            if (isFinished) return
            isFinished = true
            mainHandler.removeCallbacks(timeout)
            worker.execute {
                if (sync?.hasNewBuild == true) awaitNewBuild(appContext, appId, sync.previousBuild)
                val hasBuild = hasDownloadedBuild(appContext, appId) || hasAssetBuild(appContext, assetDir)
                mainHandler.post { onResult(hasBuild) }
            }
        }

        timeout = Runnable { finish(sync = null) }
        mainHandler.postDelayed(timeout, SYNC_TIMEOUT_MS)
        runningSyncs[appId]?.let { runningSync ->
            runningSync.waiters += ::finish
            return
        }
        val sync = RunningSync()
        sync.waiters += ::finish
        runningSyncs[appId] = sync
        worker.execute {
            sync.previousBuild = LiveUpdateManager.getLatestAppDirectory(appContext, appId)
            LiveUpdateManager.sync(appContext, appId, object : SyncCallback {
                override fun onAppComplete(syncResult: SyncResult) {
                    sync.hasNewBuild = syncResult.latestAppDirectoryChanged
                }

                override fun onAppComplete(failResult: FailResult) = Unit

                override fun onSyncComplete() {
                    mainHandler.post {
                        runningSyncs.remove(appId)
                        sync.waiters.forEach { it(sync) }
                    }
                }
            })
        }
    }

    private fun isValidBuild(directory: File?): Boolean = directory != null && File(directory, "index.html").isFile

    private fun hasDownloadedBuild(context: Context, appId: String): Boolean =
        isValidBuild(LiveUpdateManager.getLatestAppDirectory(context, appId))

    // After a sync with a new build, the SDK can report the new folder shortly after its callback.
    private fun awaitNewBuild(context: Context, appId: String, previousBuild: File?) {
        repeat(BUILD_CHECK_ATTEMPTS) {
            val latestBuild = LiveUpdateManager.getLatestAppDirectory(context, appId)
            if (latestBuild != previousBuild && isValidBuild(latestBuild)) return
            Thread.sleep(BUILD_CHECK_DELAY_MS)
        }
    }

    // A build that app/build.gradle packed into the APK, which the Portal opens from its start directory.
    private fun hasAssetBuild(context: Context, assetDir: String): Boolean =
        runCatching { context.assets.open("$assetDir/index.html").close() }.isSuccess
}
