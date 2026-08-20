package com.urunkarpm.pingpin.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val publishedAt: String
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val apkFile: File, val updateInfo: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_RELEASE_URL = "https://api.github.com/repos/urunkarpm/pingpin/releases/latest"
        private const val APK_FILENAME = "pingpin-update.apk"
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "PingPin-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateState.Error("GitHub API returned HTTP ${connection.responseCode}")
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)

            val rawTagName = json.optString("tag_name", "").trim()
            val versionName = rawTagName.removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "No release notes provided.")
            val publishedAt = json.optString("published_at", "")

            val assets = json.optJSONArray("assets")
            var downloadUrl = ""
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (versionName.isBlank()) {
                return@withContext UpdateState.Error("Invalid release version from GitHub.")
            }

            val isNewer = isNewerVersion(versionName, currentVersion)
            if (isNewer && downloadUrl.isNotBlank()) {
                val info = UpdateInfo(
                    versionName = versionName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    apkSize = apkSize,
                    publishedAt = publishedAt
                )
                UpdateState.UpdateAvailable(info)
            } else {
                UpdateState.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateState.Error(e.localizedMessage ?: "Failed to check for updates.")
        }
    }

    suspend fun downloadApk(
        updateInfo: UpdateInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val apkFile = File(context.cacheDir, APK_FILENAME)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var currentUrl = updateInfo.downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            // Follow HTTP redirects (GitHub release asset downloads redirect to AWS S3/CDN)
            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "PingPin-Android-App")
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    currentUrl = connection.getHeaderField("Location")
                    redirects++
                    if (redirects > 5) {
                        return@withContext Result.failure(Exception("Too many redirects downloading update."))
                    }
                    continue
                }
                break
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Failed to download update: HTTP ${connection.responseCode}"))
            }

            val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: updateInfo.apkSize

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (contentLength > 0) {
                            downloadedBytes.toFloat() / contentLength.toFloat()
                        } else 0f

                        onProgress(progress.coerceIn(0f, 1f), downloadedBytes, contentLength)
                    }
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("Downloaded APK file not found."))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteClean = remote.trim().removePrefix("v").removePrefix("V")
        val currentClean = current.trim().removePrefix("v").removePrefix("V")

        val remoteParts = remoteClean.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
