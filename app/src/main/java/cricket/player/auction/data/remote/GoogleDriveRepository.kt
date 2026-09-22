package cricket.player.auction.data.remote

import cricket.player.auction.model.Player
import cricket.player.auction.model.Team
import cricket.player.auction.model.Tournament
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GoogleDriveRepository {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Creates a new Google Sheet directly in Google Drive using Drive v3 API
     */
    suspend fun createGoogleSheetInDrive(
        sheetTitle: String = "Cricket_Auction_Database",
        accessToken: String? = null
    ): String = withContext(Dispatchers.IO) {
        // Direct OAuth Access Token via Google Drive REST API
        if (!accessToken.isNullOrBlank()) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files"
                val bodyJson = JSONObject().apply {
                    put("name", sheetTitle)
                    put("mimeType", "application/vnd.google-apps.spreadsheet")
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(bodyJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string()
                        val jsonResp = JSONObject(respStr ?: "{}")
                        val fileId = jsonResp.optString("id")
                        if (fileId.isNotBlank()) {
                            return@withContext "https://docs.google.com/spreadsheets/d/$fileId/edit"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Direct Fallback: sheets.new creates a fresh spreadsheet directly in user's Google account
        return@withContext "https://sheets.new"
    }

    /**
     * Creates a tournament folder in user's Google Drive using Drive v3 API
     */
    suspend fun createDriveFolder(folderName: String, accessToken: String? = null): String = withContext(Dispatchers.IO) {
        if (!accessToken.isNullOrBlank()) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files"
                val bodyJson = JSONObject().apply {
                    put("name", folderName)
                    put("mimeType", "application/vnd.google-apps.folder")
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(bodyJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string()
                        val jsonResp = JSONObject(respStr ?: "{}")
                        val fileId = jsonResp.optString("id")
                        if (fileId.isNotBlank()) {
                            return@withContext "https://drive.google.com/drive/folders/$fileId"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext "https://drive.google.com/drive/my-drive"
    }

    data class ExportResult(
        val driveUrl: String,
        val localFileUri: android.net.Uri? = null,
        val fileName: String = ""
    )

    /**
     * Converts tournament, teams, and players into clean CSV spreadsheet format compatible with Google Sheets
     */
    fun generateGoogleSheetsCsv(
        tournament: Tournament,
        teams: List<Team>,
        players: List<Player>
    ): String {
        val sb = StringBuilder()

        sb.append("=== TOURNAMENT METADATA ===\n")
        sb.append("ID,Name,Default Purse (Cr),Max Slots,Max Overseas,Created At\n")
        sb.append("\"${tournament.id}\",\"${tournament.name}\",${tournament.defaultPurse},${tournament.maxSlots},${tournament.maxOverseas},\"${tournament.createdAt}\"\n\n")

        sb.append("=== TEAMS DATA ===\n")
        sb.append("Team ID,Team Name,Short Code,Primary Color,Total Purse (Cr),Spent Purse (Cr),Remaining Purse (Cr),Captain Email,Invite Code\n")
        teams.forEach { team ->
            sb.append("\"${team.id}\",\"${team.name}\",\"${team.shortCode}\",\"${team.primaryColorHex}\",${team.totalPurse},${team.spentPurse},${team.remainingPurse},\"${team.captainEmail ?: ""}\",\"${team.inviteCode}\"\n")
        }
        sb.append("\n")

        sb.append("=== PLAYERS DATABASE ===\n")
        sb.append("Player ID,Player Name,Role,Country,Is Overseas,Base Price (Cr),Current Bid (Cr),Highest Bidder Team ID,Status,Sold Price (Cr),Sold To Team ID,Set Name\n")
        players.forEach { p ->
            sb.append("\"${p.id}\",\"${p.name}\",\"${p.role.name}\",\"${p.country}\",${p.isOverseas},${p.basePrice},${p.currentBid},\"${p.highestBidderTeamId ?: ""}\",\"${p.status.name}\",${p.soldPrice ?: 0.0},\"${p.soldToTeamId ?: ""}\",\"${p.setName}\"\n")
        }

        return sb.toString()
    }

    /**
     * Serializes tournament, teams, and players into Google Sheets CSV database, saves locally via FileProvider, and converts to Google Sheet on Google Drive
     */
    suspend fun saveTournamentToUserDrive(
        context: android.content.Context? = null,
        tournament: Tournament,
        teams: List<Team>,
        players: List<Player>,
        accessToken: String? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        val fileName = "Cricket_Auction_${tournament.name.replace("\\s+".toRegex(), "_")}_Database.csv"
        var localUri: android.net.Uri? = null
        var resultUrl = "https://drive.google.com/drive/my-drive"

        try {
            val csvContent = generateGoogleSheetsCsv(tournament, teams, players)

            // 1. Create Local Physical CSV Google Sheets File & FileProvider URI
            if (context != null) {
                try {
                    val exportDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                    if (!exportDir.exists()) exportDir.mkdirs()

                    val localFile = java.io.File(exportDir, fileName)
                    localFile.writeText(csvContent)

                    localUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        localFile
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Direct Google Drive REST API v3 Multipart Upload to create Native Google Sheet
            if (!accessToken.isNullOrBlank()) {
                try {
                    val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                    val boundary = "Boundary_" + System.currentTimeMillis()
                    val multipartMediaType = "multipart/related; boundary=$boundary".toMediaType()

                    val metadataJson = JSONObject().apply {
                        put("name", fileName.removeSuffix(".csv"))
                        put("mimeType", "application/vnd.google-apps.spreadsheet")
                    }.toString()

                    val multipartBody = StringBuilder().apply {
                        append("--$boundary\r\n")
                        append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                        append(metadataJson)
                        append("\r\n--$boundary\r\n")
                        append("Content-Type: text/csv\r\n\r\n")
                        append(csvContent)
                        append("\r\n--$boundary--\r\n")
                    }.toString()

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", "Bearer $accessToken")
                        .post(multipartBody.toRequestBody(multipartMediaType))
                        .build()

                    client.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val respStr = resp.body?.string()
                            val jsonResp = JSONObject(respStr ?: "{}")
                            val fileId = jsonResp.optString("id").takeIf { it.isNotBlank() }
                            if (fileId != null) {
                                resultUrl = "https://docs.google.com/spreadsheets/d/$fileId/edit"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext ExportResult(resultUrl, localUri, fileName)
    }
}
