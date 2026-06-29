package com.liferlighdow.device

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.HashMap

object HardwareProvider {

    suspend fun getCpuInfo(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = HashMap<String, String>()
        try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val parts = line!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (parts.size > 1) {
                        map[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } catch (ignored: IOException) {
        }
        map
    }

    suspend fun getMemInfo(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = HashMap<String, String>()
        try {
            BufferedReader(FileReader("/proc/meminfo")).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val parts = line!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (parts.size > 1) {
                        map[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } catch (ignored: IOException) {
        }
        map
    }

    suspend fun getSystemProperty(key: String): String = withContext(Dispatchers.IO) {
        try {
            val p = Runtime.getRuntime().exec("getprop $key")
            p.inputStream.bufferedReader().use { it.readLine() ?: "" }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun readFileLine(path: String): String = withContext(Dispatchers.IO) {
        try {
            BufferedReader(FileReader(path)).use { it.readLine() ?: "" }
        } catch (e: Exception) {
            ""
        }
    }

    fun isRooted(): Boolean {
        // 1. Check Build Tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        // 2. Check common su paths
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        if (paths.any { java.io.File(it).exists() }) return true

        // 3. Try to execute su command (most reliable but slightly slower)
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val br = BufferedReader(java.io.`InputStreamReader`(process.inputStream))
            br.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
}
