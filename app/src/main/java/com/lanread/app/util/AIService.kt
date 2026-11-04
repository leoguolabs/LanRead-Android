package com.lanread.app.util

import com.lanread.app.data.settings.AppLanguageOption
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class SkimmingStructurePoint(val label: String, val insight: String)

@Serializable
data class SkimmingChapterSummary(
    val chapterTitle: String = "",
    val readingGoal: String = "",
    val structure: List<SkimmingStructurePoint> = emptyList(),
    val keySentences: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val inspectionQuestions: List<String> = emptyList(),
    val aiNarrative: String = "",
    val estimatedMinutes: Int = 3
)

data class AIConfiguration(
    val endpoint: String,
    val model: String,
    val apiKey: String
)

/** Single AI gateway shared by summary/skimming/inline actions. */
object AIService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Mirrors iOS AIConfig.current(): secure server first, local fallback second.
    var configProvider: suspend () -> AIConfiguration = {
        try {
            val secure = SecureAPIKeyService.fetchAIConfiguration()
            AIConfiguration(endpoint = secure.endpoint, model = secure.model, apiKey = secure.apiKey)
        } catch (e: Exception) {
            AIConfiguration(
                endpoint = "https://api.openai.com/v1",
                model = "gpt-4o-mini",
                apiKey = ""
            )
        }
    }

    fun isConfigured(): Boolean = true

    suspend fun chat(
        prompt: String,
        system: String? = null,
        temperature: Double,
        maxTokens: Int,
        timeoutSeconds: Long = 60
    ): String {
        val config = configProvider()
        val url = config.endpoint.trimEnd('/') + "/chat/completions"
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(timeoutSeconds + 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val messages = mutableListOf<Map<String, String>>()
        if (system != null) messages.add(mapOf("role" to "system", "content" to system))
        messages.add(mapOf("role" to "user", "content" to prompt))

        // Build JSON manually to avoid extra serialization deps
        val messagesJson = messages.joinToString(",") { m ->
            "{\"role\":\"${m["role"]}\",\"content\":${jsonString(m["content"]!!)}}"
        }
        val isDashScope = config.endpoint.contains("dashscope", ignoreCase = true) ||
            config.endpoint.contains("aliyun", ignoreCase = true)
        val thinkingField = if (isDashScope) ",\"enable_thinking\":false" else ""
        val payload = "{\"model\":${jsonString(config.model)},\"messages\":[$messagesJson]," +
            "\"temperature\":$temperature,\"max_tokens\":$maxTokens$thinkingField}"

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: throw AIException("empty_response")
            if (!response.isSuccessful) throw AIException("http_${response.code}: ${text.take(200)}")
            val root = json.parseToJsonElement(text).jsonObject
            val content = root["choices"]?.jsonArray?.get(0)?.jsonObject?.get("message")
                ?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: throw AIException("parse_error")
            return content
        }
    }

    fun parseSummaryResponse(response: String): Pair<String, List<String>> {
        val lines = response.lines()
        val summaryLines = mutableListOf<String>()
        val keyPoints = mutableListOf<String>()
        var inKeyPoints = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val isBullet = trimmed.startsWith("•") || trimmed.startsWith("- ") ||
                trimmed.startsWith("* ") || trimmed.startsWith("·")
            val lower = trimmed.lowercase()
            if (lower.startsWith("key points") || lower.startsWith("关键要点") ||
                lower.startsWith("要点") || lower.startsWith("キーポイント") || lower.startsWith("핵심")) {
                inKeyPoints = true
                continue
            }
            if (isBullet) {
                inKeyPoints = true
                keyPoints.add(trimmed.trimStart('•', '-', '*', '·', ' ').trim())
                continue
            }
            if (trimmed.startsWith("#")) {
                inKeyPoints = false
                summaryLines.add(trimmed.removePrefix("#").trim())
                continue
            }
            if (inKeyPoints) {
                keyPoints.add(trimmed)
            } else {
                summaryLines.add(trimmed)
            }
        }
        return summaryLines.joinToString("\n") to keyPoints.filter { it.isNotBlank() }
    }

    private fun jsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    fun parseSkimmingJson(response: String): SkimmingChapterSummary? {
        val cleaned = response.trim()
        val candidates = mutableListOf(cleaned)
        Regex("\\{[\\s\\S]*\\}").find(cleaned)?.let { candidates.add(it.value) }
        for (candidate in candidates) {
            try {
                return json.decodeFromString<SkimmingChapterSummary>(candidate)
            } catch (_: Exception) {
            }
        }
        return null
    }
}

class AIException(message: String) : Exception(message)

/** Language name used in AI prompts, mirroring iOS aiOutputLanguageName. */
fun aiOutputLanguageName(appLanguage: AppLanguageOption, systemLanguage: AppLanguageOption): String {
    val resolved = if (appLanguage == AppLanguageOption.SYSTEM) systemLanguage else appLanguage
    return when (resolved) {
        AppLanguageOption.EN, AppLanguageOption.SYSTEM -> "English"
        AppLanguageOption.ZH_HANS -> "Simplified Chinese"
        AppLanguageOption.JA -> "Japanese"
        AppLanguageOption.KO -> "Korean"
    }
}
