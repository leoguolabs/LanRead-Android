package com.lanread.app.util

import kotlin.text.RegexOption
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

data class ParsedChapter(
    val title: String,
    val content: String,
    val htmlContent: String,
    val order: Int,
    val sourceHref: String?
)

data class ParsedTocItem(
    val title: String,
    val href: String,
    val level: Int,
    val chapterIndex: Int
) {
    val fragment: String?
        get() {
            val parts = href.split("#")
            if (parts.size < 2) return null
            val raw = parts.drop(1).joinToString("#").trim()
            return raw.ifEmpty { null }
        }
}

data class EPubMetadata(
    val title: String,
    val author: String?,
    val language: String?,
    val coverImageData: ByteArray?,
    val chapters: List<ParsedChapter>,
    val tocItems: List<ParsedTocItem>,
    val totalPages: Int
)

class EPubParser {

    fun parse(file: java.io.File): EPubMetadata {
        ZipFile(file).use { zip ->
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: throw EPubParseException("container.xml missing")
            val containerXml = zip.readEntry(containerEntry)
            val opfPath = parseContainerOpfPath(containerXml)
                ?: throw EPubParseException("OPF path missing")
            val opfEntry = zip.findEntry(opfPath) ?: throw EPubParseException("OPF missing")
            val opfDir = opfPath.substringBeforeLast('/', "")
            val opfXml = zip.readEntry(opfEntry)
            val opf = parseOpf(opfXml)

            val coverData = loadCover(zip, opf, opfDir)

            val spineHrefs = opf.spineItems.mapNotNull { id ->
                opf.manifestMap[id]?.let { href -> resolvePath(opfDir, href) }
            }

            val tocItems = loadToc(zip, opf, opfDir, spineHrefs)

            val chapters = spineHrefs.mapIndexedNotNull { index, href ->
                val entry = zip.findEntry(href) ?: return@mapIndexedNotNull null
                val rawHtml = String(zip.readEntry(entry), Charsets.UTF_8)
                val docTitle = extractTitle(rawHtml) ?: opf.title ?: "Chapter ${index + 1}"
                val processedHtml = processImages(rawHtml, zip, href)
                ParsedChapter(
                    title = docTitle,
                    content = extractPlainText(rawHtml),
                    htmlContent = processedHtml,
                    order = index,
                    sourceHref = href
                )
            }

            if (chapters.isEmpty()) throw EPubParseException("no chapters")

            return EPubMetadata(
                title = opf.title ?: file.nameWithoutExtension,
                author = opf.author,
                language = opf.language,
                coverImageData = coverData,
                chapters = chapters,
                tocItems = tocItems,
                totalPages = chapters.size * 10
            )
        }
    }

    private fun ZipFile.readEntry(entry: ZipEntry): ByteArray =
        getInputStream(entry).use { it.readBytes() }

    private fun ZipFile.findEntry(path: String): ZipEntry? {
        val normalized = normalizePath(path)
        getEntry(normalized)?.let { return it }
        val lower = normalized.lowercase()
        for (entry in entries()) {
            if (normalizePath(entry.name).lowercase() == lower) return entry
        }
        return null
    }

    private fun parseContainerOpfPath(xml: ByteArray): String? {
        val handler = object : DefaultHandler() {
            var fullPath: String? = null
            override fun startElement(
                uri: String?, localName: String?, qName: String?, attributes: Attributes
            ) {
                if (qName?.lowercase()?.endsWith("rootfile") == true) {
                    val media = attributes.getValue("media-type")
                    if (fullPath == null && (media == null || media == "application/oebps-package+xml")) {
                        fullPath = attributes.getValue("full-path")
                    }
                }
            }
        }
        parseXml(xml, handler)
        return handler.fullPath
    }

    private data class OpfResult(
        var title: String? = null,
        var author: String? = null,
        var language: String? = null,
        var coverId: String? = null,
        val manifestMap: MutableMap<String, String> = mutableMapOf(),
        val manifestProperties: MutableMap<String, String> = mutableMapOf(),
        val spineItems: MutableList<String> = mutableListOf(),
        var tocId: String? = null,
        var navId: String? = null
    )

    private fun parseOpf(xml: ByteArray): OpfResult {
        val result = OpfResult()
        val handler = object : DefaultHandler() {
            private var currentElement = ""
            private var textBuffer = StringBuilder()
            private var inTitle = false
            private var inCreator = false

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                currentElement = (qName ?: localName ?: "").lowercase()
                textBuffer = StringBuilder()
                when {
                    currentElement.endsWith("title") -> inTitle = true
                    currentElement.endsWith("creator") -> inCreator = true
                    currentElement.endsWith("manifest") || currentElement.endsWith("spine") -> Unit
                    currentElement.endsWith("item") -> {
                        val id = attributes.getValue("id") ?: return
                        val href = attributes.getValue("href") ?: return
                        val mediaType = attributes.getValue("media-type") ?: ""
                        val properties = attributes.getValue("properties") ?: ""
                        result.manifestMap[id] = href
                        result.manifestProperties[id] = properties
                        if (properties.contains("nav") && mediaType.contains("html")) result.navId = id
                        if (mediaType == "application/x-dtbncx+xml") result.tocId = id
                        if (properties.contains("cover-image")) result.coverId = id
                    }
                    currentElement.endsWith("itemref") -> {
                        val idref = attributes.getValue("idref")
                        if (idref != null) result.spineItems.add(idref)
                    }
                    currentElement.endsWith("meta") -> {
                        if (attributes.getValue("name") == "cover") {
                            result.coverId = attributes.getValue("content")
                        }
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inTitle || inCreator) textBuffer.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val name = (qName ?: localName ?: "").lowercase()
                when {
                    name.endsWith("title") -> {
                        if (result.title == null) result.title = textBuffer.toString().trim()
                        inTitle = false
                    }
                    name.endsWith("creator") -> {
                        if (result.author == null) result.author = textBuffer.toString().trim()
                        inCreator = false
                    }
                    name.endsWith("language") -> {
                        if (result.language == null && textBuffer.isNotBlank()) {
                            result.language = textBuffer.toString().trim()
                        }
                    }
                }
            }
        }
        parseXml(xml, handler)
        if (result.tocId == null && result.navId == null) {
            // fall back to any .ncx manifest item
            result.manifestMap.entries.firstOrNull { it.value.endsWith(".ncx", ignoreCase = true) }
                ?.let { result.tocId = it.key }
        }
        if (result.coverId == null) {
            result.manifestMap.entries.firstOrNull {
                it.value.contains("cover", ignoreCase = true) && it.value.endsWith(".jpg", true) ||
                    it.value.contains("cover", ignoreCase = true) && it.value.endsWith(".png", true)
            }?.let { result.coverId = it.key }
        }
        return result
    }

    private fun loadCover(zip: ZipFile, opf: OpfResult, opfDir: String): ByteArray? {
        val coverId = opf.coverId ?: return null
        val href = opf.manifestMap[coverId] ?: return null
        val entry = zip.findEntry(resolvePath(opfDir, href)) ?: return null
        return zip.readEntry(entry)
    }

    private fun loadToc(
        zip: ZipFile,
        opf: OpfResult,
        opfDir: String,
        spineHrefs: List<String>
    ): List<ParsedTocItem> {
        val entries = mutableListOf<TocEntry>()
        val navId = opf.navId
        val tocId = opf.tocId

        if (navId != null) {
            val href = opf.manifestMap[navId]
            if (href != null) {
                zip.findEntry(resolvePath(opfDir, href))?.let { entry ->
                    parseNavToc(zip.readEntry(entry), entries)
                }
            }
        }
        if (entries.isEmpty() && tocId != null) {
            val href = opf.manifestMap[tocId]
            if (href != null) {
                zip.findEntry(resolvePath(opfDir, href))?.let { entry ->
                    parseNcxToc(zip.readEntry(entry), entries)
                }
            }
        }
        if (entries.isEmpty()) {
            // Fallback: one TOC item per spine chapter titled by chapter title
            return spineHrefs.mapIndexed { index, _ ->
                ParsedTocItem("Chapter ${index + 1}", spineHrefs[index], 0, index)
            }
        }

        val chapterIndexByPath = mutableMapOf<String, Int>()
        spineHrefs.forEachIndexed { index, href -> chapterIndexByPath.putIfAbsent(normalizePath(href), index) }

        return entries.mapNotNull { entry ->
            val targetPath = resolvePath(opfDir, entry.href.substringBefore('#'))
            val chapterIndex = chapterIndexByPath[targetPath]
            if (chapterIndex == null) {
                null
            } else {
                ParsedTocItem(entry.title, entry.href, entry.level, chapterIndex)
            }
        }.ifEmpty {
            spineHrefs.mapIndexed { index, _ -> ParsedTocItem("Chapter ${index + 1}", spineHrefs[index], 0, index) }
        }
    }

    private data class TocEntry(val title: String, val href: String, val level: Int)

    private fun parseNcxToc(xml: ByteArray, out: MutableList<TocEntry>) {
        val handler = object : DefaultHandler() {
            private var depth = 0
            private var currentLabel: StringBuilder? = null
            private var pendingSrc: String? = null
            private var pendingLabel: String? = null

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                val name = (qName ?: localName ?: "").lowercase()
                when (name) {
                    "navpoint" -> { depth += 1 }
                    "text" -> currentLabel = StringBuilder()
                    "content" -> pendingSrc = attributes.getValue("src")
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                currentLabel?.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val name = (qName ?: localName ?: "").lowercase()
                when (name) {
                    "navpoint" -> {
                        val label = pendingLabel
                        val src = pendingSrc
                        if (!label.isNullOrBlank() && !src.isNullOrBlank()) {
                            out.add(TocEntry(label.trim(), src.trim(), (depth - 1).coerceAtLeast(0)))
                        }
                        pendingLabel = null
                        pendingSrc = null
                        depth = (depth - 1).coerceAtLeast(0)
                    }
                    "text" -> {
                        pendingLabel = currentLabel?.toString()
                        currentLabel = null
                    }
                }
            }
        }
        parseXml(xml, handler)
    }

    private fun parseNavToc(xml: ByteArray, out: MutableList<TocEntry>) {
        val handler = object : DefaultHandler() {
            private var navDepth = 0
            private var inAnchor = false
            private var anchorText = StringBuilder()
            private var anchorHref: String? = null
            private var olDepth = 0

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                val name = (qName ?: localName ?: "").lowercase()
                if (name == "nav") navDepth += 1
                if (name == "ol") olDepth += 1
                if (name == "a") {
                    inAnchor = true
                    anchorText = StringBuilder()
                    anchorHref = attributes.getValue("href")
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inAnchor) anchorText.append(ch, start, length)
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val name = (qName ?: localName ?: "").lowercase()
                if (name == "a") {
                    val text = anchorText.toString().trim()
                    val href = anchorHref
                    if (text.isNotEmpty() && !href.isNullOrEmpty()) {
                        out.add(TocEntry(text, href, (olDepth - 1).coerceAtLeast(0)))
                    }
                    inAnchor = false
                }
                if (name == "ol") olDepth -= 1
                if (name == "nav") navDepth -= 1
            }
        }
        parseXml(xml, handler)
    }

    private fun processImages(html: String, zip: ZipFile, chapterHref: String): String {
        // Inline images as data URIs so the WebView can render them without a base URL.
        val regex = Regex("""(src|href)\s*=\s*["']([^"']+\.(?:png|jpe?g|gif|svg|webp))["']""", RegexOption.IGNORE_CASE)
        return regex.replace(html) { match ->
            val attr = match.groupValues[1]
            val rawSrc = match.groupValues[2]
            val resolved = resolvePath(chapterHref.substringBeforeLast('/', ""), rawSrc)
            val entry = zip.findEntry(resolved)
            if (entry == null) {
                match.value
            } else {
                val bytes = zip.readEntry(entry)
                val mime = when (resolved.substringAfterLast('.').lowercase()) {
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "svg" -> "image/svg+xml"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                "$attr=\"data:$mime;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}\""
            }
        }
    }

    private fun extractTitle(html: String): String? {
        val match = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html) ?: return null
        val text = decodeEntities(match.groupValues[1]).trim()
        return text.ifEmpty { null }
    }

    fun extractPlainText(html: String): String {
        var text = html
            .replace(Regex("""<(script|style)[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""</(p|div|h[1-6]|li|tr|blockquote)>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
        text = decodeEntities(text)
        return text
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }

    private fun resolvePath(baseDir: String, href: String): String {
        val clean = href.substringBefore('?').trim()
        if (clean.isEmpty()) return ""
        if (clean.startsWith("/")) return normalizePath(clean)
        val combined = if (baseDir.isEmpty()) clean else "$baseDir/$clean"
        return normalizePath(combined)
    }

    private fun normalizePath(path: String): String {
        var p = path.replace('\\', '/').trim()
        while (p.startsWith("/")) p = p.removePrefix("/")
        val segments = mutableListOf<String>()
        for (segment in p.split("/")) {
            when (segment) {
                "", "." -> continue
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(segment)
            }
        }
        return segments.joinToString("/")
    }

    private fun parseXml(xml: ByteArray, handler: DefaultHandler) {
        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isValidating = false
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        } catch (_: Exception) {
        }
        val parser = factory.newSAXParser()
        val stream: InputStream = ByteArrayInputStream(xml)
        parser.parse(stream, handler)
    }

    companion object {
        fun parse(file: java.io.File): EPubMetadata = EPubParser().parse(file)
    }
}

class EPubParseException(message: String) : Exception(message)
