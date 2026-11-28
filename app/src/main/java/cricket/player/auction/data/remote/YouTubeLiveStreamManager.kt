package cricket.player.auction.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class YouTubeBroadcastResult(
    val success: Boolean,
    val broadcastId: String = "",
    val streamId: String = "",
    val watchUrl: String = "",
    val ingestionAddress: String = "",
    val streamKey: String = "",
    val errorMessage: String = ""
)

object YouTubeLiveStreamManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun isoNowUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(System.currentTimeMillis() + 5000L))
    }

    suspend fun createAndBindLiveBroadcast(
        accessToken: String,
        title: String,
        description: String,
        privacyStatus: String = "public"
    ): YouTubeBroadcastResult = withContext(Dispatchers.IO) {
        try {
            // 1. Create LiveBroadcast
            val broadcastBody = JsonObject().apply {
                add("snippet", JsonObject().apply {
                    addProperty("title", title)
                    addProperty("description", description)
                    addProperty("scheduledStartTime", isoNowUtc())
                })
                add("status", JsonObject().apply {
                    addProperty("privacyStatus", privacyStatus)
                    addProperty("selfDeclaredMadeForKids", false)
                })
                add("contentDetails", JsonObject().apply {
                    addProperty("enableAutoStart", true)
                    addProperty("enableAutoStop", true)
                    addProperty("latencyPreference", "ultraLow")
                })
            }

            val req1 = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/liveBroadcasts?part=snippet,status,contentDetails")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(broadcastBody.toString().toRequestBody(jsonMediaType))
                .build()

            val resp1 = client.newCall(req1).execute()
            val body1 = resp1.body?.string().orEmpty()
            if (!resp1.isSuccessful) {
                val friendly = if (body1.contains("liveStreamingNotEnabled", ignoreCase = true)) {
                    "Your YouTube channel has not enabled Live Streaming yet (YouTube requires 24h activation in YouTube Studio)."
                } else {
                    "YouTube Broadcast error (${resp1.code}): ${extractErrorMessage(body1)}"
                }
                return@withContext YouTubeBroadcastResult(success = false, errorMessage = friendly)
            }

            val json1 = gson.fromJson(body1, JsonObject::class.java)
            val broadcastId = json1.get("id")?.asString.orEmpty()
            if (broadcastId.isBlank()) {
                return@withContext YouTubeBroadcastResult(success = false, errorMessage = "Failed to get YouTube broadcast ID.")
            }

            // 2. Create LiveStream (RTMP ingestion)
            val streamBody = JsonObject().apply {
                add("snippet", JsonObject().apply {
                    addProperty("title", "$title - Mobile Stream")
                })
                add("cdn", JsonObject().apply {
                    addProperty("frameRate", "30fps")
                    addProperty("resolution", "720p")
                    addProperty("ingestionType", "rtmp")
                })
            }

            val req2 = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/liveStreams?part=snippet,cdn")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(streamBody.toString().toRequestBody(jsonMediaType))
                .build()

            val resp2 = client.newCall(req2).execute()
            val body2 = resp2.body?.string().orEmpty()
            if (!resp2.isSuccessful) {
                return@withContext YouTubeBroadcastResult(
                    success = false,
                    errorMessage = "YouTube Stream error (${resp2.code}): ${extractErrorMessage(body2)}"
                )
            }

            val json2 = gson.fromJson(body2, JsonObject::class.java)
            val streamId = json2.get("id")?.asString.orEmpty()
            val ingestionInfo = json2.getAsJsonObject("cdn")?.getAsJsonObject("ingestionInfo")
            val ingestionAddress = ingestionInfo?.get("ingestionAddress")?.asString.orEmpty()
            val streamName = ingestionInfo?.get("streamName")?.asString.orEmpty()

            // 3. Bind Broadcast to Stream
            val req3 = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/liveBroadcasts/bind?id=$broadcastId&part=id,contentDetails&streamId=$streamId")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("".toRequestBody(jsonMediaType))
                .build()

            val resp3 = client.newCall(req3).execute()
            val body3 = resp3.body?.string().orEmpty()
            if (!resp3.isSuccessful) {
                return@withContext YouTubeBroadcastResult(
                    success = false,
                    errorMessage = "YouTube Bind error (${resp3.code}): ${extractErrorMessage(body3)}"
                )
            }

            YouTubeBroadcastResult(
                success = true,
                broadcastId = broadcastId,
                streamId = streamId,
                watchUrl = "https://www.youtube.com/watch?v=$broadcastId",
                ingestionAddress = ingestionAddress,
                streamKey = streamName
            )
        } catch (e: Exception) {
            YouTubeBroadcastResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Network error connecting to YouTube API"
            )
        }
    }

    suspend fun transitionBroadcast(
        accessToken: String,
        broadcastId: String,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/liveBroadcasts/transition?broadcastStatus=$status&id=$broadcastId&part=status")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("".toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun extractErrorMessage(raw: String): String {
        return try {
            val obj = gson.fromJson(raw, JsonObject::class.java)
            obj.getAsJsonObject("error")?.get("message")?.asString ?: raw.take(160)
        } catch (_: Exception) {
            raw.take(160)
        }
    }
}
