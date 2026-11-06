package com.lanread.app.ui.reader

import com.lanread.app.data.local.entity.HighlightEntity
import org.json.JSONObject
import com.lanread.app.data.settings.ReadingFontOption

/**
 * Builds the paginated reading HTML with exact CSS from iOS ReaderWebView.getMobileOptimizedCSS
 * and the pagination/selection/highlight/touch JS engine.
 */
object ReaderHtmlBuilder {

    fun lineHeight(fromLineSpacing: Double): Double =
        if (fromLineSpacing <= 1.0) 0.9 + (fromLineSpacing * 0.55)
        else 1.45 + ((fromLineSpacing - 1.0) * 0.34)

    fun buildFullHtml(
        htmlContent: String,
        fontSizePx: Int,
        lineSpacing: Double,
        isDarkMode: Boolean,
        pageMarginsPx: Int,
        fontOption: ReadingFontOption,
        initialPageIndex: Int = 0
    ): String {
        val css = mobileOptimizedCss(fontSizePx, lineSpacing, isDarkMode, pageMarginsPx, fontOption)
        val js = readerJavaScript(isDarkMode, initialPageIndex)
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>$css</style>
            </head>
            <body>
            <div class="reader-content"><div class="reader-book-root">$htmlContent</div></div>
            <script>$js</script>
            </body>
            </html>
        """.trimIndent()
    }

    fun mobileOptimizedCss(
        fontSize: Int,
        lineSpacing: Double,
        isDarkMode: Boolean,
        pageMargins: Int,
        fontOption: ReadingFontOption
    ): String {
        val lineHeight = lineHeight(lineSpacing)
        val backgroundColor = if (isDarkMode) "#0d0d12" else "#fafafa"
        val textColor = if (isDarkMode) "rgba(255, 255, 255, 0.85)" else "rgba(0, 0, 0, 0.86)"
        val linkColor = if (isDarkMode) "#5aadff" else "#0066cc"
        val fontFamily = when (fontOption) {
            ReadingFontOption.SYSTEM -> "-apple-system, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Noto Sans CJK SC', sans-serif"
            ReadingFontOption.SERIF -> "Georgia, 'Times New Roman', 'Songti SC', 'Noto Serif CJK SC', serif"
            ReadingFontOption.MONOSPACE -> "'SF Mono', Menlo, Monaco, 'Courier New', monospace"
        }

        return """
        :root {
            --reader-page-margin: ${pageMargins}px;
            --reader-vertical-padding: 8px;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            -webkit-tap-highlight-color: transparent;
        }

        html {
            width: 100%;
            height: 100%;
            overflow-x: hidden;
            overflow-y: hidden;
            overscroll-behavior: none;
            touch-action: pan-y;
        }

        body {
            overflow: hidden;
            background-color: $backgroundColor;
            color: $textColor;
            font-family: $fontFamily;
            -webkit-font-smoothing: antialiased;
            font-feature-settings: 'kern' 1, 'liga' 1;
            text-rendering: optimizeLegibility;
            text-size-adjust: 100%;
            -webkit-text-size-adjust: 100%;
            overscroll-behavior: none;
            touch-action: pan-y;
        }

        body {
            font-size: ${fontSize}px;
            line-height: $lineHeight;
            margin: 0;
            width: 100vw;
            height: 100vh;
            padding: 0;
            -webkit-hyphenate-character: auto;
            -webkit-hyphens: auto;
            hyphens: auto;
        }

        .reader-content {
            max-width: 100%;
            height: 100%;
            margin: 0;
            padding: var(--reader-vertical-padding) 0;
            word-wrap: break-word;
            overflow-wrap: break-word;
            word-break: break-word;
            -webkit-column-width: 100vw;
            column-width: 100vw;
            -webkit-column-gap: 0;
            column-gap: 0;
            -webkit-column-fill: auto;
            column-fill: auto;
        }

        .reader-book-root {
            width: 100%;
            max-width: 100%;
            min-height: 100%;
            margin: 0;
            padding-left: var(--reader-page-margin);
            padding-right: var(--reader-page-margin);
        }

        .reader-generated-continuation {
            text-indent: 0 !important;
            margin-top: 1em;
            -webkit-column-break-inside: avoid;
            break-inside: avoid;
        }

        .reader-book-root div,
        .reader-book-root article,
        .reader-book-root section,
        .reader-book-root aside,
        .reader-book-root nav,
        .reader-book-root header,
        .reader-book-root footer,
        .reader-book-root main,
        .reader-book-root figure,
        .reader-book-root table {
            width: auto;
            max-width: 100%;
            margin-left: 0;
            margin-right: 0;
            padding-left: 0;
            padding-right: 0;
            box-sizing: border-box;
        }

        p {
            margin: 0 0 0.85em 0;
            text-align: justify;
            text-indent: 2em;
            letter-spacing: 0.01em;
            orphans: 2;
            widows: 2;
        }

        h1, h2, h3, h4, h5, h6 {
            font-weight: 700;
            margin: 1.4em 0 0.6em 0;
            line-height: 1.25;
            text-indent: 0;
            letter-spacing: -0.01em;
            -webkit-column-break-after: avoid;
            break-after: avoid;
        }

        h1 { font-size: 1.65em; padding-bottom: 0.25em; }
        h2 { font-size: 1.4em; }
        h3 { font-size: 1.2em; }
        h4 { font-size: 1.08em; }
        h5, h6 { font-size: 1em; }

        a {
            color: $linkColor;
            text-decoration: none;
            word-break: break-all;
        }

        a:active { opacity: 0.7; }

        img {
            max-width: 100% !important;
            height: auto !important;
            display: block;
            margin: 0.8em auto;
            border-radius: 4px;
            -webkit-column-break-inside: avoid;
            break-inside: avoid;
        }

        ul, ol {
            margin: 0.5em 0 0.5em 1.5em;
            padding-left: 1em !important;
        }

        li { margin: 0.25em 0; }

        blockquote {
            margin: 0.8em 0;
            padding: 0.6em 1em;
            border-left: 3px solid ${if (isDarkMode) "rgba(255,255,255,0.15)" else "rgba(0,0,0,0.12)"};
            background-color: ${if (isDarkMode) "rgba(255,255,255,0.03)" else "rgba(0,0,0,0.02)"};
            font-style: italic;
            -webkit-column-break-inside: avoid;
            break-inside: avoid;
        }

        code {
            font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
            font-size: 0.88em;
            padding: 0.15em 0.35em;
            background-color: ${if (isDarkMode) "rgba(255,255,255,0.06)" else "rgba(0,0,0,0.04)"};
            border-radius: 4px;
        }

        pre {
            margin: 0.8em 0;
            padding: 0.8em 1em;
            background-color: ${if (isDarkMode) "rgba(255,255,255,0.05)" else "rgba(0,0,0,0.03)"};
            border-radius: 6px;
            overflow-x: auto;
            -webkit-column-break-inside: avoid;
            break-inside: avoid;
        }

        pre code { padding: 0; background-color: transparent; }

        table {
            width: 100%;
            max-width: 100%;
            border-collapse: collapse;
            margin: 0.8em 0;
            display: block;
            overflow-x: auto;
            -webkit-column-break-inside: avoid;
            break-inside: avoid;
        }

        th, td {
            padding: 0.45em 0.6em;
            border: 1px solid ${if (isDarkMode) "rgba(255,255,255,0.1)" else "rgba(0,0,0,0.08)"};
            text-align: left;
        }

        th {
            background-color: ${if (isDarkMode) "rgba(255,255,255,0.05)" else "rgba(0,0,0,0.03)"};
            font-weight: 600;
        }

        hr {
            border: none;
            border-top: 1px solid ${if (isDarkMode) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.06)"};
            margin: 1.8em 0;
        }

        strong, b { font-weight: 600; }
        em, i { font-style: italic; }
        del, s { text-decoration: line-through; }

        u {
            text-decoration: underline;
            text-underline-offset: 0.15em;
        }

        small { font-size: 0.85em; }

        div, article, section, aside, nav, header, footer, main {
            max-width: 100%;
            word-wrap: break-word;
        }

        pre, code, kbd, samp {
            white-space: pre-wrap;
            word-wrap: break-word;
        }

        svg { max-width: 100%; height: auto; }

        ::selection {
            background-color: ${if (isDarkMode) "rgba(50, 130, 246, 0.35)" else "rgba(0, 122, 255, 0.2)"};
            color: inherit;
        }

        mark.reader-highlight {
            border-radius: 2px;
            padding: 0 1px !important;
            -webkit-box-decoration-break: clone;
            box-decoration-break: clone;
        }

        iframe { max-width: 100%; }

        .pagebreak {
            page-break-after: always;
            margin: 2em 0;
        }

        .footnote {
            font-size: 0.85em;
            vertical-align: super;
        }

        .no-select {
            -webkit-user-select: none;
            user-select: none;
        }
        """.trimIndent()
    }

    fun readerJavaScript(isDarkMode: Boolean, initialPageIndex: Int = 0): String = """
        var __initialPage = $initialPageIndex;
        var __isRestoring = __initialPage !== 0;
        var __jsCurrentPage = __initialPage < 0 ? 0 : __initialPage;
        var __justDragged = false;
        var __justDraggedTimer = null;
        var __activeScrollAnimationId = null;

        function resolveInitialTargetPage() {
            if (__initialPage < 0) {
                return Math.max(0, getTotalPageCount() - 1);
            }
            return __initialPage;
        }

        function markJustDragged() {
            __justDragged = true;
            if (__justDraggedTimer) clearTimeout(__justDraggedTimer);
            __justDraggedTimer = setTimeout(function() {
                __justDragged = false;
            }, 300);
        }

        function getScroller() {
            if (document.body && document.body.scrollWidth > document.body.clientWidth) {
                return document.body;
            }
            if (document.documentElement && document.documentElement.scrollWidth > document.documentElement.clientWidth) {
                return document.documentElement;
            }
            return document.scrollingElement || document.body || document.documentElement;
        }

        function normalizeColor(colorHex) {
            if (!colorHex) return '#ffe38f';
            if (colorHex.startsWith('#')) { return colorHex; }
            return '#' + colorHex;
        }

        function nativePost(name, payload) {
            try { LanReadBridge.postMessage(JSON.stringify({ name: name, payload: payload })); } catch (e) {}
        }

        function getPerPage() {
            var v = (window.visualViewport && window.visualViewport.width) || document.documentElement.clientWidth;
            return (v && v > 0) ? v : (window.innerWidth || 393);
        }
        function getScrollLeft() {
            var s = getScroller();
            return s ? s.scrollLeft : 0;
        }
        function getTotalWidth() {
            var s = getScroller();
            return s ? s.scrollWidth : Math.max(document.documentElement.scrollWidth, document.body.scrollWidth, 0);
        }
        function getTotalPageCount() {
            var perPage = getPerPage();
            if (perPage <= 0) return 1;
            return Math.max(1, Math.ceil(getTotalWidth() / perPage));
        }

        var isApplyingPaginationLayout = false;

        function computePageCount(shouldNotify) {
            var pages = getTotalPageCount();
            if (shouldNotify !== false) {
                nativePost('pageMetrics', { type: 'pageCount', value: pages });
            }
            return pages;
        }

        function splitCrossPageParagraphsIfNeeded() { return 0; }

        function applyPagination() {
            if (isApplyingPaginationLayout) {
                return computePageCount(true);
            }
            isApplyingPaginationLayout = true;
            var finalPages = 1;
            try {
                finalPages = computePageCount(true);
            } catch (e) {
                finalPages = computePageCount(true);
            } finally {
                isApplyingPaginationLayout = false;
            }
            var target = resolveInitialTargetPage();
            if (target > 0) {
                scrollToPage(target, false);
            }
            return finalPages;
        }

        function clampVisualViewport() {
            try {
                if (window.visualViewport && (Math.abs(window.visualViewport.offsetLeft) > 0.5 || Math.abs(window.visualViewport.offsetTop) > 0.5)) {
                    var curPage = getCurrentPage();
                    var perPage = getPerPage();
                    var targetX = Math.max(0, curPage * perPage);
                    var anchor = document.getElementById('__vv_anchor__');
                    if (!anchor) {
                        anchor = document.createElement('div');
                        anchor.id = '__vv_anchor__';
                        anchor.style.position = 'absolute';
                        anchor.style.top = '0px';
                        anchor.style.width = '1px';
                        anchor.style.height = '1px';
                        anchor.style.pointerEvents = 'none';
                        anchor.style.opacity = '0';
                        document.body.appendChild(anchor);
                    }
                    anchor.style.left = targetX + 'px';
                    anchor.scrollIntoView({ block: 'start', inline: 'start' });
                }
            } catch (e) {}
        }

        function cancelScrollAnimation() {
            if (__activeScrollAnimationId) {
                cancelAnimationFrame(__activeScrollAnimationId);
                __activeScrollAnimationId = null;
            }
        }

        function setScrollLeft(x, animated, onComplete) {
            cancelScrollAnimation();
            var perPage = getPerPage();
            var totalPages = getTotalPageCount();
            var page = perPage > 0 ? Math.max(0, Math.round(x / perPage)) : 0;
            if (totalPages > 0 && page >= totalPages) page = totalPages - 1;
            __jsCurrentPage = page;
            var scroller = getScroller();

            if (!animated) {
                if (scroller) scroller.scrollLeft = x;
                try { document.documentElement.scrollLeft = x; } catch (e) {}
                try { document.body.scrollLeft = x; } catch (e) {}
                clampVisualViewport();
                if (onComplete) onComplete();
                return;
            }

            var startX = scroller ? scroller.scrollLeft : 0;
            var distance = x - startX;
            if (Math.abs(distance) < 0.5) {
                if (scroller) scroller.scrollLeft = x;
                clampVisualViewport();
                if (onComplete) onComplete();
                return;
            }

            var duration = Math.min(280, Math.max(160, Math.abs(distance) * 0.55));
            var startTime = performance.now();

            function step(now) {
                var elapsed = now - startTime;
                var progress = Math.min(1, elapsed / duration);
                var ease = 1 - Math.pow(1 - progress, 3);
                var cur = startX + distance * ease;
                if (scroller) scroller.scrollLeft = cur;
                if (progress < 1) {
                    __activeScrollAnimationId = requestAnimationFrame(step);
                } else {
                    if (scroller) scroller.scrollLeft = x;
                    __activeScrollAnimationId = null;
                    clampVisualViewport();
                    if (onComplete) onComplete();
                }
            }
            __activeScrollAnimationId = requestAnimationFrame(step);
        }

        function scrollToPage(index, animated, onComplete) {
            var perPage = getPerPage();
            var totalPages = getTotalPageCount();
            var target = (totalPages > 0) ? Math.max(0, Math.min(Math.floor(index), totalPages - 1)) : Math.max(0, Math.floor(index));
            var x = target * perPage;
            setScrollLeft(x, animated, onComplete);
        }

        function scrollToLastPage(animated) {
            var target = Math.max(0, getTotalPageCount() - 1);
            __jsCurrentPage = target;
            scrollToPage(target, animated || false, function() {
                nativePost('pageMetrics', { type: 'currentPage', value: target });
            });
            nativePost('pageMetrics', { type: 'currentPage', value: target });
        }
        window.scrollToLastPage = scrollToLastPage;

        function currentPageIndex() {
            var perPage = getPerPage();
            if (perPage <= 0) return 0;
            return Math.max(0, Math.round(getScrollLeft() / perPage));
        }
        function getCurrentPage() { return __jsCurrentPage; }

        // ---- Touch-driven 1:1 Page Turn & Fast Fling ----
        var touchState = {
            startX: 0,
            startY: 0,
            lastX: 0,
            startTime: 0,
            lastTime: 0,
            velocityX: 0,
            startScroll: 0,
            isDragging: false,
            isHorizontal: null,
            startPage: 0
        };

        document.addEventListener('touchstart', function(e) {
            if (e.touches.length !== 1) return;

            var sel = window.getSelection ? window.getSelection() : null;
            if (sel && !sel.isCollapsed && String(sel).trim().length > 0) {
                return;
            }

            cancelScrollAnimation();

            var touch = e.touches[0];
            var perPage = getPerPage();
            var curScroll = getScrollLeft();

            touchState.startX = touch.clientX;
            touchState.startY = touch.clientY;
            touchState.lastX = touch.clientX;
            touchState.startTime = Date.now();
            touchState.lastTime = touchState.startTime;
            touchState.velocityX = 0;
            touchState.startScroll = curScroll;
            touchState.isDragging = false;
            touchState.isHorizontal = null;
            touchState.startPage = perPage > 0 ? Math.round(curScroll / perPage) : 0;
        }, { passive: true });

        document.addEventListener('touchmove', function(e) {
            if (e.touches.length !== 1) return;

            var sel = window.getSelection ? window.getSelection() : null;
            if (sel && !sel.isCollapsed && String(sel).trim().length > 0) {
                return;
            }

            var touch = e.touches[0];
            var dx = touch.clientX - touchState.startX;
            var dy = touch.clientY - touchState.startY;

            if (touchState.isHorizontal === null) {
                var absDx = Math.abs(dx);
                var absDy = Math.abs(dy);
                if (absDx > 8 || absDy > 8) {
                    if (absDx >= absDy) {
                        touchState.isHorizontal = true;
                        touchState.isDragging = true;
                    } else {
                        touchState.isHorizontal = false;
                    }
                }
            }

            if (!touchState.isHorizontal) return;

            if (e.cancelable) e.preventDefault();

            var now = Date.now();
            var dt = now - touchState.lastTime;
            if (dt > 8) {
                touchState.velocityX = (touch.clientX - touchState.lastX) / dt;
                touchState.lastX = touch.clientX;
                touchState.lastTime = now;
            }

            var perPage = getPerPage();
            var totalPages = getTotalPageCount();
            var maxScroll = Math.max(0, (totalPages - 1) * perPage);
            var targetScroll = touchState.startScroll - dx;

            var scroller = getScroller();
            if (targetScroll < 0) {
                if (scroller) scroller.scrollLeft = 0;
                var overscroll = -targetScroll * 0.35;
                document.body.style.transform = 'translateX(' + overscroll + 'px)';
            } else if (targetScroll > maxScroll) {
                if (scroller) scroller.scrollLeft = maxScroll;
                var overshoot = (targetScroll - maxScroll) * 0.35;
                document.body.style.transform = 'translateX(' + (-overshoot) + 'px)';
            } else {
                document.body.style.transform = '';
                if (scroller) scroller.scrollLeft = targetScroll;
            }
        }, { passive: false });

        document.addEventListener('touchend', function(e) {
            if (!touchState.isDragging) return;

            markJustDragged();

            var touch = e.changedTouches[0];
            var totalDx = touch.clientX - touchState.startX;
            var totalDt = Math.max(1, Date.now() - touchState.startTime);
            var avgVelocityX = totalDx / totalDt;
            var releaseVelocityX = touchState.velocityX;

            if (document.body.style.transform) {
                document.body.style.transition = 'transform 0.22s cubic-bezier(0.25, 1, 0.5, 1)';
                document.body.style.transform = '';
                setTimeout(function() { document.body.style.transition = ''; }, 240);
            }

            var perPage = getPerPage();
            var totalPages = getTotalPageCount();
            var startPage = touchState.startPage;
            var targetPage = startPage;

            // Fast fling: velocity > 0.28 px/ms and distance > 25px
            var isFling = (Math.abs(releaseVelocityX) > 0.28 || Math.abs(avgVelocityX) > 0.28) && Math.abs(totalDx) > 25;

            if (isFling) {
                var flingDir = releaseVelocityX !== 0 ? releaseVelocityX : avgVelocityX;
                if (flingDir < 0) {
                    targetPage = startPage + 1;
                } else {
                    targetPage = startPage - 1;
                }
            } else {
                // Interactive slow drag: threshold is 20% of page width
                var threshold = perPage * 0.20;
                if (totalDx < -threshold) {
                    targetPage = startPage + 1;
                } else if (totalDx > threshold) {
                    targetPage = startPage - 1;
                } else {
                    targetPage = startPage;
                }
            }

            touchState.isDragging = false;
            touchState.isHorizontal = null;

            if (targetPage >= 0 && targetPage < totalPages) {
                __jsCurrentPage = targetPage;
                scrollToPage(targetPage, true, function() {
                    nativePost('pageMetrics', { type: 'currentPage', value: targetPage });
                });
            } else if (targetPage < 0) {
                scrollToPage(0, true);
                nativePost('pageTap', { zone: 'prev' });
            } else if (targetPage >= totalPages) {
                scrollToPage(totalPages - 1, true);
                nativePost('pageTap', { zone: 'next' });
            }
        }, { passive: false });

        document.addEventListener('touchcancel', function(e) {
            if (touchState.isDragging) {
                markJustDragged();
                document.body.style.transform = '';
                scrollToPage(touchState.startPage, true);
                touchState.isDragging = false;
                touchState.isHorizontal = null;
            }
        }, { passive: true });

        window.addEventListener('load', function() {
            var total = applyPagination();
            nativePost('pageMetrics', { type: 'pageCount', value: total });
            var target = resolveInitialTargetPage();
            if (target > 0) {
                scrollToPage(target, false);
                setTimeout(function() {
                    scrollToPage(target, false);
                    __isRestoring = false;
                    nativePost('pageMetrics', { type: 'currentPage', value: getCurrentPage() });
                }, 150);
            } else {
                __isRestoring = false;
                nativePost('pageMetrics', { type: 'currentPage', value: 0 });
            }
        });

        var resizeTimer = null;
        window.addEventListener('resize', function() {
            var page = getCurrentPage();
            if (resizeTimer) clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function() {
                applyPagination();
                scrollToPage(page, false);
            }, 120);
        });

        function handleScroll() {
            var scroller = getScroller();
            if (scroller && scroller.scrollTop !== 0) { scroller.scrollTop = 0; }
            if (window.scrollY !== 0) { try { window.scrollTo(window.scrollX, 0); } catch (e) {} }
            if (document.documentElement.scrollTop !== 0) { try { document.documentElement.scrollTop = 0; } catch (e) {} }
            __jsCurrentPage = currentPageIndex();
            if (!__isRestoring && !touchState.isDragging) {
                nativePost('pageMetrics', { type: 'currentPage', value: __jsCurrentPage });
            }
            clampVisualViewport();
        }
        window.addEventListener('scroll', handleScroll, { passive: true });
        document.addEventListener('scroll', handleScroll, { passive: true, capture: true });

        // ---- Text offset mapping ----
        function buildTextSegments() {
            var segments = [];
            var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
            var current = 0;
            var node;
            while ((node = walker.nextNode())) {
                var len = (node.textContent || '').length;
                if (len <= 0) continue;
                segments.push({ node: node, start: current, end: current + len, length: len });
                current += len;
            }
            return segments;
        }

        function locateOffsetBoundary(offset, segments, preferNextOnBoundary) {
            if (!segments || segments.length === 0) return null;
            var target = Math.max(0, offset);
            for (var i = 0; i < segments.length; i++) {
                var seg = segments[i];
                if (target < seg.end) return { node: seg.node, offset: Math.max(0, target - seg.start) };
                if (target === seg.end) {
                    if (preferNextOnBoundary && i + 1 < segments.length) return { node: segments[i + 1].node, offset: 0 };
                    return { node: seg.node, offset: seg.length };
                }
            }
            var last = segments[segments.length - 1];
            return { node: last.node, offset: last.length };
        }

        function buildRangeFromOffsets(startOffset, endOffset, segments) {
            if (typeof startOffset !== 'number' || typeof endOffset !== 'number') return null;
            if (startOffset >= endOffset) return null;
            var startBoundary = locateOffsetBoundary(startOffset, segments, true);
            var endBoundary = locateOffsetBoundary(endOffset, segments, false);
            if (!startBoundary || !endBoundary) return null;
            if (startBoundary.node === endBoundary.node && startBoundary.offset >= endBoundary.offset) return null;
            var range = document.createRange();
            range.setStart(startBoundary.node, startBoundary.offset);
            range.setEnd(endBoundary.node, endBoundary.offset);
            return range;
        }

        function serializeSelection() {
            var selection = window.getSelection ? window.getSelection() : null;
            if (!selection || selection.rangeCount === 0) return null;
            var text = selection.toString();
            if (!text || !text.trim()) return null;
            var range = selection.getRangeAt(0);
            var segments = buildTextSegments();
            var start = null;
            var end = null;
            for (var i = 0; i < segments.length; i++) {
                var seg = segments[i];
                if (seg.node === range.startContainer) start = seg.start + range.startOffset;
                if (seg.node === range.endContainer) end = seg.start + range.endOffset;
            }
            if (start === null || end === null || end <= start) return null;
            var rect = range.getBoundingClientRect();
            return {
                text: text,
                cleanedText: text,
                start: start,
                end: end,
                pageIndex: currentPageIndex(),
                rect: { x: rect.left, y: rect.top, width: rect.width, height: rect.height }
            };
        }

        function notifySelectionChange() {
            var payload = serializeSelection();
            if (payload) {
                nativePost('textSelection', payload);
            } else {
                nativePost('textSelection', { text: '', cleanedText: '', start: 0, end: 0, pageIndex: currentPageIndex(), rect: null, cleared: true });
            }
        }

        document.addEventListener('selectionchange', function() {
            clearTimeout(window.__selTimer);
            window.__selTimer = setTimeout(function() {
                notifySelectionChange();
                var s = window.getSelection ? window.getSelection() : null;
                if (!s || s.rangeCount === 0 || !s.toString().trim()) {
                    clampVisualViewport();
                }
            }, 120);
        });

        // ---- Highlight application ----
        function findHighlightAncestor(node) {
            var current = node;
            while (current) {
                if (current.nodeType === Node.ELEMENT_NODE && current.classList && current.classList.contains('reader-highlight')) return current;
                current = current.parentNode;
            }
            return null;
        }

        function clearExistingHighlights() {
            var marks = document.querySelectorAll('mark.reader-highlight');
            var parents = [];
            marks.forEach(function(mark) {
                var parent = mark.parentNode;
                if (parent) {
                    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
                    parent.removeChild(mark);
                    if (parents.indexOf(parent) === -1) parents.push(parent);
                }
            });
            parents.forEach(function(p) {
                if (p && p.normalize) p.normalize();
            });
        }

        function normalizeRangeToTextNodeBoundaries(range) {
            if (!range || range.collapsed) return range;

            var startContainer = range.startContainer;
            var endContainer = range.endContainer;
            var startOffset = range.startOffset;
            var endOffset = range.endOffset;

            if (startContainer === endContainer && startContainer.nodeType === Node.TEXT_NODE) {
                var textNode = startContainer;
                var length = textNode.length;
                if (length === 0 || startOffset >= endOffset) return range;

                if (endOffset > 0 && endOffset < length) {
                    textNode.splitText(endOffset);
                }

                if (startOffset > 0 && startOffset < textNode.length) {
                    textNode = textNode.splitText(startOffset);
                }

                range.setStart(textNode, 0);
                range.setEnd(textNode, textNode.length);
                return range;
            }

            if (endContainer.nodeType === Node.TEXT_NODE) {
                var endLength = endContainer.length;
                if (endOffset > 0 && endOffset < endLength) {
                    endContainer.splitText(endOffset);
                    range.setEnd(endContainer, endContainer.length);
                } else if (endOffset === endLength) {
                    range.setEnd(endContainer, endLength);
                }
            }

            if (startContainer.nodeType === Node.TEXT_NODE) {
                if (startOffset > 0 && startOffset < startContainer.length) {
                    var splitStart = startContainer.splitText(startOffset);
                    range.setStart(splitStart, 0);
                } else if (startOffset === 0) {
                    range.setStart(startContainer, 0);
                }
            }

            return range;
        }

        function collectTextNodesInRange(range) {
            var nodes = [];
            if (!range || range.collapsed) return nodes;

            var root = range.commonAncestorContainer;
            if (root.nodeType === Node.TEXT_NODE) {
                if (!findHighlightAncestor(root)) {
                    nodes.push(root);
                }
                return nodes;
            }

            var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
            var node;
            while ((node = walker.nextNode())) {
                var content = node.textContent || '';
                if (!content.length) continue;
                if (findHighlightAncestor(node)) continue;
                try {
                    if (range.intersectsNode(node)) {
                        if (range.comparePoint(node, content.length) < 0) continue;
                        if (range.comparePoint(node, 0) > 0) continue;
                        nodes.push(node);
                    }
                } catch (e) {}
            }

            return nodes;
        }

        function wrapTextNodeWithHighlight(node, colorHex, highlightId) {
            if (!node || !node.parentNode) return false;
            var content = node.textContent || '';
            if (!content.length || content.trim().length === 0) return false;
            var wrapper = document.createElement('mark');
            wrapper.className = 'reader-highlight';
            wrapper.style.backgroundColor = normalizeColor(colorHex);
            if (highlightId) wrapper.dataset.highlightId = highlightId;
            node.parentNode.insertBefore(wrapper, node);
            wrapper.appendChild(node);
            return true;
        }

        function wrapRangeWithHighlights(range, colorHex, highlightId) {
            if (!range || range.collapsed) return false;
            var workingRange = range.cloneRange();
            normalizeRangeToTextNodeBoundaries(workingRange);
            var textNodes = collectTextNodesInRange(workingRange);
            if (textNodes.length === 0) return false;
            var wrapped = 0;
            textNodes.forEach(function(node) {
                if (wrapTextNodeWithHighlight(node, colorHex, highlightId)) wrapped += 1;
            });
            return wrapped > 0;
        }

        function highlightByOffsets(start, end, colorHex, highlightId) {
            if (typeof start !== 'number' || typeof end !== 'number' || start >= end) return false;
            var segments = buildTextSegments();
            if (segments.length === 0) return false;
            var range = buildRangeFromOffsets(start, end, segments);
            if (!range) return false;
            return wrapRangeWithHighlights(range, colorHex, highlightId);
        }

        function applyNativeHighlights(items) {
            clearExistingHighlights();
            if (items && items.forEach) {
                items.forEach(function(item) {
                    highlightByOffsets(item.start, item.end, item.colorHex || '#ffe38f', item.id || '');
                });
            }
        }

        function applyHighlightForSelection(colorHex, highlightId) {
            var payload = serializeSelection();
            if (!payload) return false;
            var ok = highlightByOffsets(payload.start, payload.end, colorHex, highlightId || '');
            if (ok && window.getSelection) {
                var s = window.getSelection();
                if (s && s.removeAllRanges) s.removeAllRanges();
            }
            return ok;
        }

        // ---- Highlight tap ----
        document.addEventListener('click', function(event) {
            if (__justDragged) return;
            var w = getPerPage();
            var x = event.clientX;
            if (x < w * 0.24 || x > w * 0.76) return;
            var node = event.target;
            var target = null;
            while (node) {
                if (node.classList && node.classList.contains('reader-highlight')) { target = node; break; }
                node = node.parentElement;
            }
            if (!target) return;
            var highlightId = (target.dataset && target.dataset.highlightId) ? target.dataset.highlightId : '';
            if (!highlightId) return;
            var text = target.textContent || '';
            var grouped = document.querySelectorAll('mark.reader-highlight[data-highlight-id="' + highlightId + '"]');
            if (grouped && grouped.length > 1) text = '';
            nativePost('highlightTap', { id: highlightId, text: text });
            if (event.preventDefault) event.preventDefault();
            if (window.getSelection) { var s = window.getSelection(); if (s && s.removeAllRanges) s.removeAllRanges(); }
        }, true);

        // ---- Tap zones (prev/next/toolbar) - WebView consumes touches, so handle here ----
        var lastTapTime = 0;
        document.addEventListener('click', function(event) {
            if (__justDragged) return;
            var sel = window.getSelection ? window.getSelection() : null;
            if (sel && !sel.isCollapsed && String(sel).trim().length > 0) return;

            var w = getPerPage();
            var x = event.clientX;

            // Left edge: previous page
            if (x < w * 0.24) {
                clampVisualViewport();
                nativePost('pageTap', { zone: 'prev' });
                return;
            }

            // Right edge: next page
            if (x > w * 0.76) {
                clampVisualViewport();
                nativePost('pageTap', { zone: 'next' });
                return;
            }

            // Center area: do not toggle toolbar if tapping an interactive element (link or highlight)
            var target = event.target;
            if (target && target.closest && target.closest('a, mark.reader-highlight, .reader-highlight')) return;

            var now = Date.now();
            if (now - lastTapTime < 180) return;
            lastTapTime = now;

            nativePost('toggleToolbar', {});
        }, false);

        // ---- Content link tap ----
        document.addEventListener('click', function(event) {
            if (__justDragged) return;
            var w = getPerPage();
            var x = event.clientX;
            if (x < w * 0.24 || x > w * 0.76) return;
            var target = event ? event.target : null;
            if (!target) return;
            var current = target;
            var inHighlight = false;
            while (current) {
                if (current.classList && current.classList.contains('reader-highlight')) { inHighlight = true; break; }
                current = current.parentElement;
            }
            if (inHighlight) return;
            var linkNode = null;
            current = target;
            while (current) {
                if (current.tagName && String(current.tagName).toLowerCase() === 'a') { linkNode = current; break; }
                current = current.parentElement;
            }
            if (!linkNode) return;
            var href = '';
            try { href = (linkNode.getAttribute('href') || '').trim(); } catch (e) {}
            if (!href) return;
            if (href.toLowerCase().startsWith('javascript:')) return;
            var isExternal = /^(https?:|mailto:|tel:|sms:)/i.test(href);
            var text = '';
            try { text = (linkNode.textContent || '').trim(); } catch (e) {}
            nativePost('contentLinkTap', { href: href, text: text, isExternal: !!isExternal });
            if (event.preventDefault) event.preventDefault();
            if (event.stopPropagation) event.stopPropagation();
        }, true);

    """.trimIndent()

    data class HighlightRange(val id: String, val start: Int, val end: Int, val colorHex: String)

    fun highlightsToJs(highlights: List<HighlightEntity>, anchors: Map<String, Pair<Int, Int>>): String {
        val items = highlights.mapNotNull { highlight ->
            val (start, end) = anchors[highlight.id] ?: return@mapNotNull null
            """{"id":"${highlight.id}","start":$start,"end":$end,"colorHex":"${highlight.colorHex}"}"""
        }
        return "[${items.joinToString(",")}]"
    }

    fun buildAnchor(startPosition: String, offset: Int, chapterIndex: Int, pageIndex: Int): String = try {
        JSONObject(startPosition).apply {
            put("chapterIndex", chapterIndex)
            put("pageIndex", pageIndex)
            put("offset", offset)
        }.toString()
    } catch (_: Exception) {
        JSONObject().apply {
            put("chapterIndex", chapterIndex)
            put("pageIndex", pageIndex)
            put("offset", offset)
        }.toString()
    }

    fun parseAnchor(position: String?): Triple<Int, Int, Int>? {
        if (position.isNullOrEmpty()) return null
        return try {
            val obj = JSONObject(position)
            Triple(
                obj.optInt("chapterIndex", 0),
                obj.optInt("pageIndex", 0),
                if (obj.has("offset")) obj.optInt("offset") else -1
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isMergedAnchor(position: String?): Boolean {
        if (position.isNullOrEmpty()) return false
        return try {
            JSONObject(position).optBoolean("mergedFromHighlights", false)
        } catch (_: Exception) {
            false
        }
    }
}
