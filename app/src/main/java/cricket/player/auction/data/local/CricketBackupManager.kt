package cricket.player.auction.data.local

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileNotFoundException

object CricketBackupManager {

    private fun getCricketDirectory(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val cricketFolder = File(documentsDir, "Cricket")
        if (!cricketFolder.exists()) {
            cricketFolder.mkdirs()
        }
        return cricketFolder
    }

    private fun getDownloadCricketDirectory(): File {
        val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val cricketFolder = File(dlDir, "Cricket")
        if (!cricketFolder.exists()) {
            cricketFolder.mkdirs()
        }
        return cricketFolder
    }

    fun backupToCricketFolder(context: Context, jsonContent: String): Result<File> {
        return try {
            val mainFolder = getCricketDirectory()
            val backupFile = File(mainFolder, "cricket_backup.json")
            backupFile.writeText(jsonContent)

            // Mirror to Downloads/Cricket so it appears under Downloads in all file manager apps
            try {
                val dlFolder = getDownloadCricketDirectory()
                val dlFile = File(dlFolder, "cricket_backup.json")
                dlFile.writeText(jsonContent)
            } catch (_: Exception) {}

            Result.success(backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to internal storage if external documents failed
            try {
                val fallbackFolder = File(context.getExternalFilesDir(null) ?: context.filesDir, "Cricket")
                fallbackFolder.mkdirs()
                val file = File(fallbackFolder, "cricket_backup.json")
                file.writeText(jsonContent)
                Result.success(file)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    fun restoreFromCricketFolder(context: Context): Result<String> {
        return try {
            // 1. Try Documents/Cricket/cricket_backup.json
            val docFile = File(getCricketDirectory(), "cricket_backup.json")
            if (docFile.exists() && docFile.length() > 0) {
                return Result.success(docFile.readText())
            }

            // 2. Try Downloads/Cricket/cricket_backup.json
            val dlFile = File(getDownloadCricketDirectory(), "cricket_backup.json")
            if (dlFile.exists() && dlFile.length() > 0) {
                return Result.success(dlFile.readText())
            }

            // 3. Try any .json in Documents/Cricket
            val docJson = getCricketDirectory().listFiles { file -> file.extension.equals("json", ignoreCase = true) }
                ?.maxByOrNull { it.lastModified() }
            if (docJson != null && docJson.exists() && docJson.length() > 0) {
                return Result.success(docJson.readText())
            }

            // 4. Try any .json in Downloads/Cricket
            val dlJson = getDownloadCricketDirectory().listFiles { file -> file.extension.equals("json", ignoreCase = true) }
                ?.maxByOrNull { it.lastModified() }
            if (dlJson != null && dlJson.exists() && dlJson.length() > 0) {
                return Result.success(dlJson.readText())
            }

            // 5. Try internal fallback
            val internalFile = File(File(context.getExternalFilesDir(null) ?: context.filesDir, "Cricket"), "cricket_backup.json")
            if (internalFile.exists() && internalFile.length() > 0) {
                return Result.success(internalFile.readText())
            }

            Result.failure(FileNotFoundException("No 'cricket_backup.json' file found in the 'Cricket' folder of your File Manager. Please make a backup first!"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

