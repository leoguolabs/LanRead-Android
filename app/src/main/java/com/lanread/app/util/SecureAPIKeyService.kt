package com.lanread.app.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class SecureAIConfiguration(
    val apiKey: String,
    val endpoint: String,
    val model: String
)

/**
 * Fetches AI credentials from the secure key server, mirroring iOS SecureAPIKeyService:
 * POST {baseURL}/v1/keys/ai with { client_id, nonce, timestamp, signature }
 * where signature = HMAC-SHA256(clientSecret, "clientId.nonce.timestamp") as lowercase hex.
 */
object SecureAPIKeyService {

    // Mirrors iOS Config/Base.xcconfig defaults; override here for local server testing.
    private const val BASE_URL = "https://isla-reader.top"
    private const val CLIENT_ID = "ios-LanRead"
    private const val CLIENT_SECRET = "a8ba4aec4fb3f043c5054a354cd14422b201830f8ccdc93fe299be1cd5551b2e"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cached: SecureAIConfiguration? = null

    sealed class SecureKeyException(message: String) : Exception(message) {
        class ServerConfig : SecureKeyException("secure server not configured")
        class RequestFailed(reason: String) : SecureKeyException(reason)
    }

    fun isConfigured(): Boolean = CLIENT_SECRET.isNotBlank()

    suspend fun fetchAIConfiguration(): SecureAIConfiguration {
        cached?.let { return it }
        if (CLIENT_SECRET.isBlank()) {
            throw SecureKeyException.ServerConfig()
        }

        val nonce = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis() / 1000
        val signature = sign(CLIENT_ID, CLIENT_SECRET, nonce, timestamp)
        val payload = "{\"client_id\":\"$CLIENT_ID\",\"nonce\":\"$nonce\"," +
            "\"timestamp\":$timestamp,\"signature\":\"$signature\"}"

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = okhttp3.Request.Builder()
            .url("$BASE_URL/v1/keys/ai")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBodyJson())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: throw SecureKeyException.RequestFailed("empty response")
                if (!response.isSuccessful) {
                    throw SecureKeyException.RequestFailed("HTTP ${response.code}: ${body.take(200)}")
                }
                val root = json.parseToJsonElement(body).let { it as? kotlinx.serialization.json.JsonObject }
                    ?: throw SecureKeyException.RequestFailed("invalid response shape")
                fun str(key: String): String =
                    (root[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim() ?: ""

                val apiKey = str("api_key")
                val endpoint = str("api_endpoint")
                val model = str("model")
                val missing = buildList {
                    if (apiKey.isEmpty()) add("api_key")
                    if (endpoint.isEmpty()) add("api_endpoint")
                    if (model.isEmpty()) add("model")
                }
                if (missing.isNotEmpty()) {
                    throw SecureKeyException.RequestFailed("missing fields: ${missing.joinToString()}")
                }
                val config = SecureAIConfiguration(apiKey = apiKey, endpoint = endpoint, model = model)
                cached = config
                return config
            }
        } catch (e: SecureKeyException) {
            throw e
        } catch (e: Exception) {
            throw SecureKeyException.RequestFailed("network: ${e.message}")
        }
    }

    private fun sign(clientId: String, clientSecret: String, nonce: String, timestamp: Long): String {
        val message = "$clientId.$nonce.$timestamp"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(clientSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun String.toRequestBodyJson(): okhttp3.RequestBody =
        okhttp3.RequestBody.create("application/json".toMediaType(), this)
}
