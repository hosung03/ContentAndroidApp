package com.visang.mathalive.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.visang.mathalive.BuildConfig
import com.visang.mathalive.R
import com.visang.mathalive.eventbus.EventBus
import com.visang.mathalive.eventbus.HideProgress
import com.visang.mathalive.eventbus.HideSplash
import java.io.File
import java.util.*
import kotlin.concurrent.schedule


class DownloadController(
        private val context: Context,
        private val url: String,
        private val target: String,
        private val version: String
) {

    companion object {
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }

    fun enqueueDownload() {
//        var destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/MATHALIVE/update.apk"
//        var destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/MATHALIVE/update_${version}.tmp"
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/MATHALIVE/update_" + target + ".apk"
        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        showInstallOption(destination, uri)

        val file = File(destination)
        if (file.exists()) {
            file.delete()
        }

//        val newDest = destination.replace(".tmp", ".apk")
//        if(File(newDest).exists()) {
//            context.sendBroadcast(Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE).apply {
//            })
//            return
//        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle(context.getString(R.string.title_file_download))
        request.setDescription(context.getString(R.string.downloading))

        // set destination
        request.setDestinationUri(uri)

        // Enqueue a new download and same the referenceId
        val downloadId = downloadManager.enqueue(request)
//        Toast.makeText(context, context.getString(R.string.downloading), Toast.LENGTH_LONG).show()

        Thread {
            var completed = false
            while (!completed) {
                val q = DownloadManager.Query()
                q.setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(q)
                cursor.moveToFirst()
                val bytesTotal: Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if(bytesTotal > 0) {
                    val bytesDownloaded: Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
//                UiThreadStatement.runOnUiThread(Runnable { progressBar.setProgress(progress) })

                    context.sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                        putExtra("CMD", "DownloadProgress")
                        putExtra("progress", progress)
                        putExtra("target", target)
                        putExtra("downloadedfilesize", bytesDownloaded)
                        putExtra("filesize", bytesTotal)
                    })

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) === DownloadManager.STATUS_SUCCESSFUL) {
                        completed = true
//                    break
                    }
                } else {
                    context.sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                        putExtra("CMD", "DownloadProgress")
                        putExtra("progress", 0)
                        putExtra("target", target)
                        putExtra("downloadedfilesize", 0)
                        putExtra("filesize", 0)
                    })
                }

                cursor.close()
            }
        }.start()
    }

    private fun showInstallOption(
            destination: String,
            uri: Uri
    ) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val file = File(destination)
//                    val newDest = destination.replace(".tmp", ".apk")
//                    if (file.exists()) {
//                        val from = File(destination)
//                        val to = File(newDest)
//                        from.renameTo(to)
//                    }

                    if(file.exists()) {
//                        Toast.makeText(context, "다운로드 완료!!!", Toast.LENGTH_LONG).show()
                        Timer("InstallApp", false).schedule(1 * 1000) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val contentUri = FileProvider.getUriForFile(
                                    context,
                                    BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                                    file
                                )
                                val install = Intent(Intent.ACTION_VIEW)
                                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                install.data = contentUri
                                context.startActivity(install)
                            } else {
                                val install = Intent(Intent.ACTION_VIEW)
                                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                install.setDataAndType(
                                    uri,
                                    APP_INSTALL_PATH
                                )
                                context.startActivity(install)
                            }
                            EventBus.send(HideProgress())
                        }
                    } else {
                    }
                    context.unregisterReceiver(this)

                    context.sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                        putExtra("CMD", "CompleteUpdateApp")
                        putExtra("target", target)
                    })

                    if(target == "me") {
                        context.sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "onDestroy")
                            putExtra("Quit", true)
                        })
                    }
                } catch (e: Exception) {
                    Log.e("DownloadController", "Error APK File Install...")
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    class MyDownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
        }
    }
}
