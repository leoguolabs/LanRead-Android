package com.lanread.app.util

import com.lanread.app.data.local.entity.BookEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Book summary + skimming generation, caching into BookEntity fields (mirrors iOS AISummaryService/SkimmingModeService). */
class AISummaryService(private val isAiAllowed: () -> Boolean = { true }) {

    data class BookSummary(val summary: String, val keyPoints: List<String>)

    sealed class SummaryState {
        object Idle : SummaryState()
        data class Generating(val progress: Double, val partialText: String) : SummaryState()
        data class Ready(val summary: String, val keyPoints: List<String>, val generatedAt: Long) : SummaryState()
        data class Failed(val message: String) : SummaryState()
    }

    val state = MutableStateFlow<SummaryState>(SummaryState.Idle)
    val statePublic: StateFlow<SummaryState> get() = state
    val isGenerating: Boolean get() = state.value is SummaryState.Generating

    fun cachedSummary(book: BookEntity): SummaryState.Ready? {
        val summary = book.aiSummary ?: return null
        val generatedAt = book.aiSummaryGeneratedAt ?: return null
        val thirtyDaysMs = 30L * 24 * 3600 * 1000
        if (System.currentTimeMillis() - generatedAt > thirtyDaysMs) return null
        val keyPoints = book.aiKeyPoints?.let {
            try { JSONArray(it).let { arr -> (0 until arr.length()).map { idx -> arr.getString(idx) } } } catch (_: Exception) { emptyList() }
        } ?: emptyList()
        return SummaryState.Ready(summary, keyPoints, generatedAt)
    }

    suspend fun generateBookSummary(
        book: BookEntity,
        chapters: List<ParsedChapter>,
        onProgress: (suspend (SummaryState) -> Unit)? = null
    ): BookSummary {
        if (!isAiAllowed()) throw AIException("permission_required")
        val initState = SummaryState.Generating(0.05, "")
        state.value = initState
        onProgress?.invoke(initState)

        val context = buildSummaryContext(chapters, knowsBook = false)
        val prompt = buildBookSummaryPrompt(book, chapters, context)

        var accumulated = ""
        val response = try {
            AIService.chatStream(prompt, temperature = 0.7, maxTokens = 2000, timeoutSeconds = 60) { chunk ->
                accumulated += chunk
                val progress = (0.05 + (accumulated.length.toDouble() / 500.0) * 0.9).coerceIn(0.05, 0.95)
                val genState = SummaryState.Generating(progress, accumulated)
                state.value = genState
                onProgress?.invoke(genState)
            }
        } catch (e: Exception) {
            val fallbackResp = AIService.chat(prompt, temperature = 0.7, maxTokens = 2000, timeoutSeconds = 60)
            simulateStream(fallbackResp, onProgress)
            fallbackResp
        }

        val (summary, keyPoints) = AIService.parseSummaryResponse(response)
        val readyState = SummaryState.Ready(summary, keyPoints, System.currentTimeMillis())
        state.value = readyState
        onProgress?.invoke(readyState)
        return BookSummary(summary, keyPoints)
    }

    private suspend fun probeKnowledge(title: String, author: String?): Boolean {
        return try {
            val prompt = """
                You are doing a binary knowledge check for a book title.

                Book title: $title
                Author: ${author ?: "Unknown"}

                Reply with exactly one uppercase word:
                YES
                or
                NO

                Rules:
                - If uncertain, reply NO.
                - Do not output anything else.
            """.trimIndent()
            val response = AIService.chat(prompt, temperature = 0.0, maxTokens = 8, timeoutSeconds = 30)
            response.trim().uppercase().startsWith("YES")
        } catch (_: Exception) {
            false
        }
    }

    private data class SummaryContext(val tableOfContents: String, val content: String, val policyText: String)

    private val summaryLikeKeywords = listOf(
        "前言", "序言", "导言", "摘要", "概述", "结语", "后记",
        "preface", "introduction", "abstract", "overview", "conclusion", "afterword", "epilogue"
    )

    private fun buildSummaryContext(chapters: List<ParsedChapter>, knowsBook: Boolean): SummaryContext {
        val toc = chapters.mapIndexed { index, chapter -> "${index + 1}. ${chapter.title}" }.joinToString("\n")
        if (knowsBook) {
            return SummaryContext(toc, "", "The model reported that it already knows this book. Generate the summary directly from prior knowledge. No book excerpt is provided. If uncertain, stay high-level and avoid fabricated details.")
        }
        val summaryLike = chapters.filter { chapter ->
            val lower = chapter.title.lowercase()
            summaryLikeKeywords.any { lower.contains(it) }
        }
        val selected: List<ParsedChapter>
        val policyText: String
        if (summaryLike.isNotEmpty()) {
            selected = summaryLike
            policyText = "The model reported that it does not know this book. Use only the provided table of contents and summary-like chapters (preface/introduction/abstract/conclusion/afterword). Do not invent details that are not grounded in the provided context."
        } else {
            val n = chapters.size
            val indices = linkedSetOf<Int>()
            if (n > 0) { indices.add(0); if (n > 1) indices.add(1) }
            if (n >= 5) { indices.add(n / 2); if (n >= 6) indices.add(n / 2 + 1) }
            if (n > 2) { indices.add(n - 2); indices.add(n - 1) }
            selected = indices.filter { it < n }.map { chapters[it] }
            policyText = "The model reported that it does not know this book and no summary-like chapters were found. Use only the provided table of contents and sampled excerpts (first 2 + middle 2 + last 2 chapters, each compressed). Do not invent details that are not grounded in the provided context."
        }
        val content = selected.joinToString("\n\n") { chapter ->
            val cleaned = cleanAndCompressText(chapter.content)
            val maxChars = 700
            val excerpt = if (cleaned.length > maxChars) cleaned.take(maxChars) + "..." else cleaned
            "## ${chapter.title}\n$excerpt"
        }
        return SummaryContext(toc, content, policyText)
    }

    private fun buildBookSummaryPrompt(
        book: BookEntity,
        chapters: List<ParsedChapter>,
        context: SummaryContext
    ): String {
        val bookInfo = "书名: ${book.title.ifEmpty { "未知书籍" }}\n作者: ${book.author ?: "未知作者"}\n章节数: ${chapters.size}"
        return """
            请为以下书籍生成一份导读摘要：

            书籍信息：
            $bookInfo

            Context Policy:
            ${context.policyText}

            Table of Contents (cleaned):
            ${context.tableOfContents}

            书籍内容节选：
            ${context.content}

            请生成：
            1. 一份100-150字的全书导读摘要，包含主要内容、核心观点和阅读价值
            2. 3-5个关键要点
            3. 简洁明了，适合快速了解书籍内容

            格式要求：
            - 摘要部分用自然段落形式
            - 关键要点用 • 开头的列表形式
            请使用简体中文回复。
        """.trimIndent()
    }

    private suspend fun simulateStream(
        text: String,
        onProgress: (suspend (SummaryState) -> Unit)? = null
    ) {
        val words = text.split(" ")
        val chunks = if (words.size > 1) words else text.chunked(4)
        var current = ""
        for ((index, chunk) in chunks.withIndex()) {
            current += if (words.size > 1) "$chunk " else chunk
            val genState = SummaryState.Generating(
                progress = (index + 1).toDouble() / chunks.size,
                partialText = current
            )
            state.value = genState
            onProgress?.invoke(genState)
            kotlinx.coroutines.delay(20)
        }
    }

    // MARK: - Skimming

    fun skimmingCacheKey(bookId: String, chapterOrder: Int, lang: String) = "$bookId-$chapterOrder-$lang"

    fun loadSkimmingSummaries(book: BookEntity): MutableMap<String, SkimmingChapterSummary> {
        val map = mutableMapOf<String, SkimmingChapterSummary>()
        val raw = book.skimmingSummaries ?: return map
        try {
            val obj = JSONObject(raw)
            for (key in obj.keys()) {
                AIService.parseSkimmingJson(obj.getString(key))?.let { map[key] = it }
            }
        } catch (_: Exception) {
        }
        return map
    }

    fun storeSkimmingSummaries(bookId: String, summaries: Map<String, SkimmingChapterSummary>): String {
        val obj = JSONObject()
        summaries.forEach { (key, value) ->
            try {
                obj.put(key, JSONObject(kotlinx.serialization.json.Json.encodeToString(SkimmingChapterSummary.serializer(), value)))
            } catch (_: Exception) {
            }
        }
        return obj.toString()
    }

    suspend fun generateSkimmingSummary(
        book: BookEntity,
        chapter: ParsedChapter,
        languageName: String
    ): SkimmingChapterSummary {
        if (!isAiAllowed()) throw AIException("permission_required")
        val knowsChapter = probeChapterKnowledge(book.title, book.author, chapter.title, chapter.order)
        val contextPolicy = if (knowsChapter) {
            "Strategy: The model reported it already knows this chapter from training knowledge.\nGenerate the skimming summary directly using prior knowledge and keep the current output JSON format.\nIf uncertain, stay high-level and avoid fabricated details."
        } else {
            "Strategy: The model reported it does not know this chapter.\nUse the full chapter content provided below after local cleaning/compression.\nDo not invent details that are not grounded in the chapter content."
        }
        val chapterContentSection = if (knowsChapter) {
            "CHAPTER CONTENT:\nNot provided (knowledge-first path)."
        } else {
            "CHAPTER CONTENT START\n${cleanAndCompressText(chapter.content).take(12000)}\nCHAPTER CONTENT END"
        }

        val prompt = """
            You are IslaBooks' inspectional reading coach.
            Follow Mortimer J. Adler's "How to Read a Book" skimming method to help readers quickly grasp the structure, turning points, and key questions of a chapter without diving into full detail.

            TASK CONTEXT:
            - Book Title: ${book.title}
            - Chapter Title: ${chapter.title}
            - Chapter Order: ${chapter.order}
            - Reading Goal: expose architecture, thesis, transitions, and entry questions for deeper study.

            CONTEXT POLICY:
            $contextPolicy

            OUTPUT FORMAT: respond ONLY with valid JSON matching this schema:
            {
              "chapterTitle": string,
              "readingGoal": string,
              "structure": [{"label": string, "insight": string} x3-5],
              "keySentences": [string x3],
              "keywords": [string x5 with conceptual importance],
              "inspectionQuestions": [string x2-3],
              "aiNarrative": "markdown paragraphs summarizing chapter skeleton, transitions, and quick takeaways",
              "estimatedMinutes": integer between 2 and 6
            }

            RENDERING RULES:
            - Use $languageName.
            - structure entries should surface chapter spine: opening hook, argumentative peaks, closing signal.
            - keySentences must be original sentences extracted or paraphrased from the chapter to illustrate turning points.
            - aiNarrative must stay concise (<220 words) but vivid, referencing keywords inline.
            - Align tone with LanRead docs/reading_interaction_design.md guidance on "略读模式".
            - Respect How to Read a Book inspectional reading mindset: emphasize skeleton first, questions second, details last.

            $chapterContentSection
        """.trimIndent()

        val response = AIService.chat(prompt, temperature = 0.4, maxTokens = 1200, timeoutSeconds = 60)
        return AIService.parseSkimmingJson(response) ?: fallbackSkimmingSummary(chapter)
    }

    private suspend fun probeChapterKnowledge(bookTitle: String, author: String?, chapterTitle: String, order: Int): Boolean {
        return try {
            val prompt = """
                You are doing a binary knowledge check for a book chapter.

                Book title: $bookTitle
                Author: ${author ?: "Unknown"}
                Chapter title: $chapterTitle
                Chapter order: $order

                Reply with exactly one uppercase word:
                YES
                or
                NO

                Rules:
                - If uncertain, reply NO.
                - Do not output anything else.
            """.trimIndent()
            val response = AIService.chat(prompt, temperature = 0.0, maxTokens = 8, timeoutSeconds = 30)
            response.trim().uppercase().startsWith("YES")
        } catch (_: Exception) {
            false
        }
    }

    private fun fallbackSkimmingSummary(chapter: ParsedChapter): SkimmingChapterSummary =
        SkimmingChapterSummary(
            chapterTitle = chapter.title,
            readingGoal = "聚焦章节主旨与论证框架",
            structure = listOf(SkimmingStructurePoint("章节概览", "AI 暂无法提供结构点，请稍后重试。")),
            keySentences = listOf("AI 暂无法提供关键句，请稍后重试。"),
            keywords = listOf("结构"),
            inspectionQuestions = listOf("这章试图回答的核心问题是什么？"),
            aiNarrative = "",
            estimatedMinutes = 3
        )

    // MARK: - Inline actions (exact iOS prompts)

    suspend fun translate(text: String, languageName: String): String {
        val prompt = """
            You are a professional translator inside an eBook reader. Translate the following passage into $languageName with clear, natural phrasing. Keep the meaning faithful and avoid extra commentary.

            Passage:
            $text
        """.trimIndent()
        return AIService.chat(prompt, system = "You are an assistant embedded in an eBook reader. Keep answers concise and reader-friendly.", temperature = 0.35, maxTokens = 480, timeoutSeconds = 45)
    }

    suspend fun explain(text: String, languageName: String): String {
        val prompt = """
            You help readers understand tricky passages. In $languageName, briefly explain the following excerpt in under 120 words. Start with a one-sentence summary, then list 2-3 concise bullet insights only if helpful. Keep tone clear and neutral.

            Excerpt:
            $text
        """.trimIndent()
        return AIService.chat(prompt, system = "You are an assistant embedded in an eBook reader. Keep answers concise and reader-friendly.", temperature = 0.5, maxTokens = 520, timeoutSeconds = 45)
    }

    suspend fun ask(question: String, text: String, languageName: String): String {
        val prompt = """
            You are an assistant embedded in an eBook reader. In $languageName, answer the reader's question based only on the excerpt when possible. Keep the answer concise, clear, and practical.

            Question:
            $question

            Excerpt:
            $text
        """.trimIndent()
        return AIService.chat(prompt, system = "You are an assistant embedded in an eBook reader. Keep answers concise and reader-friendly.", temperature = 0.45, maxTokens = 620, timeoutSeconds = 45)
    }

    // MARK: - Text cleaning (mirrors iOS cleanAndCompressText)

    fun cleanAndCompressText(text: String): String {
        var normalized = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\u00A0", " ")
            .replace("\u200B", "")
        normalized = normalized.replace(Regex("[ \t]+"), " ")
        val cleanedLines = mutableListOf<String>()
        var previousWasEmpty = false
        for (rawLine in normalized.lines()) {
            var line = rawLine.trim()
            if (line.isEmpty()) {
                if (!previousWasEmpty) {
                    cleanedLines.add("")
                    previousWasEmpty = true
                }
                continue
            }
            if (shouldDropNoiseLine(line)) continue
            line = line.replace(Regex("[-=*~_•·]{6,}"), " ")
                .replace(Regex("[ \t]{2,}"), " ")
                .trim()
            if (line.isEmpty()) continue
            cleanedLines.add(line)
            previousWasEmpty = false
        }
        return cleanedLines.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun shouldDropNoiseLine(line: String): Boolean {
        if (Regex("^[-=*~_•·]{4,}$").containsMatchIn(line)) return true
        if (Regex("^\\d{1,4}$").containsMatchIn(line)) return true
        if (Regex("^(?i:page)\\s*\\d+(\\s*/\\s*\\d+)?$").containsMatchIn(line)) return true
        if (Regex("^(?i:p\\.)\\s*\\d+$").containsMatchIn(line)) return true
        if (Regex("^第\\s*\\d+\\s*页$").containsMatchIn(line)) return true
        return false
    }
}
