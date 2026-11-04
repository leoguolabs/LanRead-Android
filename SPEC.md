# LanRead (Isla Reader) → Android (Kotlin + Jetpack Compose) Port Specification

Source of truth: iOS SwiftUI source at `D:\workspace\Code\SelfProjAI\Isla-Project\Isla-Reader`. App display name: **澜阅 / LanRead** (bundle internally "Isla_Reader"). Version shown in UI: "1.0". Privacy policy: `https://isla-reader.top/privacy`. Support email: `support@isla-reader.top`. Tagline: “把每本书变成一位可对话的导师：获取、阅读、理解与交流，一站式完成。”

---

## 1. APP MAP

### Root (`ContentView` → `MainTabView`)
- **Phone (compact width):** `TabView` with 3 tabs:
  1. **Library** — icon `books.vertical`, label “书架” (`tab.library`)
  2. **Progress** — icon `chart.line.uptrend.xyaxis`, label “进度” (`tab.progress`)
  3. **Settings** — icon `gearshape`, label “设置” (`tab.settings`)
- **Tablet (regular width):** `NavigationSplitView` with a sidebar `List` of the same 3 destinations; sidebar title = app name, detail starts at Library.
- Global tint: `.blue` (system blue). `preferredColorScheme` follows AppSettings theme.
- On launch: AI consent sheet may present (see §5.1); Notion sync engine starts; reading statuses auto-paused; sample book seeded (§12).

### Navigation edges (how you reach each screen)
- **LibraryView** (tab 1)
  - Toolbar trailing `+` button → **ImportBookView** (sheet, full import flow §3.7). Blocked with alert if an AI summary is generating.
  - Filter icon button (funnel, next to search) → **FilterSheetView** (sheet).
  - Tap a book card → **AISummaryView** (full-screen cover, wrapped in NavigationStack, close via leading `xmark.circle.fill`).
  - Card context menu (long-press):
    - “略读模式” (sparkles.rectangle.stack) → **SkimmingModeView** (full-screen cover)
    - “书签” (bookmark) → **BookmarkListSheet** (sheet)
    - “高亮与笔记” (highlighter) → **HighlightListSheet** (full-screen cover)
    - “书籍信息” (info.circle) → **BookInfoSheet** (sheet)
    - “添加到收藏 / 取消收藏” (heart / heart.slash) — inline action
    - Divider, “从书架移除” (trash, destructive) → confirmation dialog
  - Selecting a bookmark/highlight from lists → launches **ReaderView** (full-screen cover) with `initialLocation`.
- **AISummaryView** → “开始阅读” button (header) → **ReaderView**; bottom “略读模式” orange pill button (or left-swipe ≥100pt) → **SkimmingModeView** (pushed via navigationDestination).
- **SkimmingModeView** → “阅读本章” per chapter → **ReaderView** opened at that chapter; “继续阅读” → ReaderView at saved position; TOC button → **SkimmingTableOfContentsView** (sheet).
- **ReaderView** (full-screen, nav bar hidden, status bar hidden when toolbar hidden)
  - Toolbar (toggle by center tap) → TOC sheet (`TableOfContentsView`), Reader settings sheet (`ReaderSettingsView`), Note editor sheet, AI response sheet, Ask-AI composer sheet, Highlight actions sheet, Highlights list (full-screen cover), Bookmarks list (sheet), Link preview sheet.
- **ReadingProgressView** (tab 2) → Recent reading card tap → **ReaderView** (full-screen cover).
- **SettingsView** (tab 3)
  - Profile row → **ProfileSettingsView** (push)
  - “阅读偏好” → **ReadingSettingsView** (push)
  - “主题外观” → **ThemeSettingsView** (push)
  - “高亮排序” → **HighlightSortSettingsView** (push)
  - “连接 Notion” → **NotionAuthView** (sheet) → **NotionParentPagePickerView** (sheet)
  - “数据管理” → **DataManagementView** (sheet) → **ExportDataView** (sheet)
  - “关于” → **AboutView** (sheet)
  - AI privacy row → re-presents the **AI privacy consent sheet** (same as launch sheet).
- External: `onOpenURL` handles book files (switch to Library tab + import) and deep link `isla-reader://read/last` / `lanread://read/last` (continue-reading request → switches to Library tab). URL schemes registered: `lanread`, `isla-reader`.
- Update prompt alert on launch/foreground (soft/hard update, “立即更新”/“稍后”).

---

## 2. DATA MODEL (Core Data → Room)

Store name: `Isla_Reader`. 10 entities:

### 2.1 `Book`
| Field | Type | Notes |
|---|---|---|
| id | UUID (String PK) | required |
| title | String | required; display fallback “未知书籍” |
| author | String? | display fallback “未知作者” |
| language | String? | |
| coverImageData | Blob? | |
| filePath | String | required; stores **file name only** (e.g. `UUID.epub`), resolved against `Documents/Books/` |
| fileFormat | String | required, always "epub" |
| fileSize | Long | bytes |
| checksum | String | required; lowercase SHA-256 hex of file bytes; used for dedupe & backup matching |
| totalPages | Int | initial estimate = `chapters.count * 10`, later replaced by measured sum |
| createdAt / updatedAt | Date | required |
| metadata | String? | JSON array `[{"title","content","order"}]` of chapter plain text+order |
| aiSummary | String? | cached whole-book summary text |
| aiKeyPoints | String? | JSON array of strings |
| aiSummaryGeneratedAt | Date? | TTL 30 days |
| skimmingSummaries | String? | JSON map key `{bookID}-{chapterOrder}-{lang}` → SkimmingChapterSummary |
| notionPageId | String? | cached Notion page ID |

Relationships (all to `Book`): `readingProgress` (1:0..1, delete rule **Cascade**), `bookmarks` (1:many, **Cascade**), `highlights` (1:many, **Cascade**), `annotations` (1:many, **Cascade**), `libraryItem` (1:0..1, **Cascade**).

### 2.2 `LibraryItem`
| Field | Type | Notes |
|---|---|---|
| id | UUID PK | |
| statusRaw | String | enum `ReadingStatus`: `want_to_read` / `reading` / `finished` / `paused` (default `want_to_read`) |
| tags | String? | JSON string array |
| isFavorite | Boolean | default false |
| rating | Int16 | 1–5 (0 = unrated) |
| addedAt | Date | |
| lastAccessedAt | Date? | |
| sortOrder | Int32 | set to `Int(now.timeIntervalSince1970)` on import |
| book | relation → Book (inverse, Nullify) | required, to-one |

### 2.3 `ReadingProgress`
| Field | Type | Notes |
|---|---|---|
| id | UUID PK | |
| currentPage | Int | legacy: stores chapter index |
| currentChapter | String? | |
| currentPosition | String? | JSON: `{"chapterIndex":Int,"pageIndex":Int,"totalPages":Int,"globalPage":Int,"bookTotalPages":Int}` |
| progressPercentage | Double | 0…1 = globalPage / bookTotalPages |
| lastReadAt | Date | |
| totalReadingTime | Long | seconds |
| detailedReadingTime | Long | seconds, default 0 |
| skimmingReadingTime | Long | seconds, default 0 |
| createdAt / updatedAt | Date | |
| book | relation → Book (Nullify inverse) | required |

### 2.4 `Bookmark`
id UUID PK; chapterIndex Int; pageIndex Int; chapterTitle String?; createdAt Date; colorHex String? (default `"FFFFFF"`); book → Book (Nullify inverse, required).

### 2.5 `Highlight`
id UUID PK; selectedText String; startPosition String (JSON anchor `{chapterIndex,pageIndex,offset,mergedFromHighlights?}`); endPosition String (same anchor JSON); chapter String?; pageNumber Int; colorHex String (required, default yellow); note String?; createdAt/updatedAt Date; book → Book (Nullify inverse, required).

### 2.6 `Annotation`
id UUID PK; content String; position String (JSON); chapter String?; pageNumber Int; associatedText String?; createdAt/updatedAt Date; book → Book (Nullify inverse).

### 2.7 `BookMapping` (legacy Notion mapping)
bookID String; notionPageID String; updatedAt Date.

### 2.8 `NotionSyncConfig`
databaseId String; containerPageId String; workspaceName String; lastSyncedAt Date?.

### 2.9 `SyncQueue` (legacy, unused by current engine)
id UUID PK; operationType String; payload String; statusRaw String; retryCount Int32; nextAttemptAt Date; lastError String?; createdAt/updatedAt Date.

### 2.10 `SyncQueueItem` (active Notion queue)
id UUID PK; targetBookId String (book UUID string); type String (`highlight` | `note`); payload Blob (JSON-encoded `NotionSyncPayload`); status String (`pending` | `inProgress` | `failed`); retryCount Int16; createdAt Date.

`NotionSyncPayload` JSON: `{bookID, bookTitle, bookAuthor, bookReadingStatusRaw?, bookProgressPercentage?, chapter?, highlightText?, noteContent?, eventDate}`.

### Non-DB persisted state (SharedPreferences/DataStore equivalents)
- AppSettings keys: `app_language`, `translation_language`, `app_theme`, `reading_font_size`, `reading_font`, `line_spacing`, `page_margins`, `adRemovalUnlocked`, `highlightSortMode`, `profileDisplayName`, `profileAvatarData`, `aiAdvanceAdNoticeEnabled` (default true), `readingReminderEnabled` (default false), `readingGoalMinutes` (default 20), `readingReminderTimeMinutes` (default `20*60+0=1200`).
- Other keys: AI consent (`aiPrivacyPermissionGranted`, `aiPrivacySuppressLaunchPrompt`, `aiPrivacyConsentVersion`=2), sample seeding (`bundled_sample_book_seeded_v1`), skimming last chapter (`skimming_last_chapter_{bookId}`), skimming ad progress (`skimming_ad_progress_{bookId}` JSON), daily reading stats (`reading_daily_stats_today_start_timestamp`, `reading_daily_stats_today_reading_seconds`).

---

## 3. LIBRARY SCREEN

### 3.1 Layout
- `NavigationStack`, large title “我的书架” (`library.title`).
- Top bar (below nav title): HStack of [search field | filter button].
  - Search field: magnifier icon + placeholder “搜索书籍或作者” (`library.search.placeholder`), horizontal padding 12, vertical 8, glass surface corner radius 10.
  - Filter button: 38×38, funnel icon (`line.3.horizontal.decrease.circle` / filled when active), glass surface radius 12.
- Content: `LazyVGrid` of book cards, **grid spacing 20**, standard padding.
  - Columns / card width / spacing by size class: Phone compact: **2 columns**, card width **140pt**, spacing 12; Regular non-iPad: 3 columns, 140pt, spacing 16; iPad regular: **4 columns**, card width **160pt**, spacing 20.
  - Card height = width × **1.4**. Corner radius **12**.
  - Cover placeholder: `LinearGradient` blue(0.6) → purple(0.8), topLeading→bottomTrailing, with centered `book.closed` icon (40pt, white) + title (caption, white, max 3 lines, h-padding 8).
  - Overlays (padding 8): top row = [progress badge (if >0%) | spacer | status badge]; bottom-left = favorite badge.
    - **Progress badge**: bar-chart icon + `%.0f%%`, caption2, background color by progress: <30% orange, <70% blue, <100% purple, else green (opacity 0.9), white text, corner radius 8.
    - **Status badge**: caption2 text, h-padding 8 v-padding 4, radius 8, white text; colors: want_to_read=gray, reading=blue, finished=green, paused=orange (opacity 0.9). Labels: 想读/在读/已读/暂停.
    - **Favorite badge**: `heart.fill` 12pt white in pink(0.9) circle, padding 6.

### 3.2 Search
Live filter (no debounce) matching title OR author, case-insensitive. Combined with status/favorite filter.

### 3.3 Filter sheet (`FilterSheetView`)
Grouped list: Section (no header): 全部， 收藏 (pink heart icon); Section “阅读状态”： 想读 / 在读 / 已读 / 暂停. Selected row shows blue checkmark; tap selects + dismisses; trailing “完成” button.

### 3.4 Sort
Fetch sorted by `LibraryItem.lastAccessedAt` DESC (most recently accessed first).

### 3.5 Empty state (`EmptyLibraryView`)
Centered: `books.vertical` icon 80pt secondary; title “书架空空如也” (title2, medium); subtitle “导入您的第一本电子书开始阅读之旅”； pill button (accent background, radius **25**, h-padding 24 v-padding 12, white headline text) “+ 导入书籍”.

### 3.6 Context menu order
1. 略读模式 (sparkles.rectangle.stack)
2. 书签 (bookmark)
3. 高亮与笔记 (highlighter)
4. 书籍信息 (info.circle)
5. 添加到收藏 / 取消收藏 (heart / heart.slash)
6. Divider
7. 从书架移除 (trash, destructive)

### 3.7 Import flow (`ImportBookView` + `BookImportService`)
1. Sheet: centered `square.and.arrow.down` icon 60pt accent; title “导入电子书” (title, bold); description “支持 ePub、TXT 等格式\n从文件 App 或其他应用导入”； blue button “从文件 App 选择” (folder icon, full width, radius 12); leading “取消”.
2. System file picker: **EPUB only** (single selection).
3. Progress UI while importing: linear progress (max width 200) + “正在导入书籍...” + percent caption.
4. Pipeline (progress steps): 0.1 verify extension == "epub" → 0.2 read bytes + **SHA-256** checksum → 0.3 duplicate check (fetch Book where checksum == X, limit 1); if found throw “书籍《title》已存在于书架中” → 0.4 parse EPUB (unzip, META-INF/container.xml → OPF → metadata title/author/language/cover, spine chapters with HTML, TOC from NCX/EPUB3 nav; totalPages estimate = chapters × 10) → 0.6 copy file to `Documents/Books/{UUID}.epub` → 0.8 create Book + LibraryItem (status=want_to_read, sortOrder=timestamp) + ReadingProgress (currentPage 0, position JSON init `{chapterIndex:0,pageIndex:0,totalPages:1,globalPage:1,bookTotalPages:max(chapters,1)}`, all timers 0) → 0.9 store chapters JSON into `Book.metadata` → 1.0 save.
5. On success: sheet dismisses, after **0.3–0.5 s** delay the **AISummaryView** full-screen cover opens automatically (`source: import_completion`).
6. Errors surface in alert titled “导入结果”.
7. External import (open-in from other apps): same service; switch to Library tab first; unsupported type → alert “不支持的文件格式，请选择ePub文件”； concurrent import → alert “正在导入书籍...”; blocked while AI summary generating.

### 3.8 Remove book
Confirmation dialog “确认移除”， message “确定将《%@》从书架移除吗？将删除本地文件及本地阅读数据（进度、书签、笔记）。\n\nNotion 中的记录不会删除；若重新加入此书，将创建一条新的同步记录。” Actions: 移除 (destructive) / 取消.
`LibraryRemovalService`: deletes SyncQueueItems for book, legacy BookMappings, progress/bookmarks/highlights/annotations, LibraryItem + Book, then deletes the EPUB file from `Documents/Books/`; clears Notion page mapping and skimming progress keys.

### 3.9 BookInfoSheet
Sheet, inline nav title “书籍信息”， trailing “完成”.
- Header: cover 90×130 radius 12 (placeholder primary 0.08); title (headline, 2 lines), author (subheadline secondary), StatusBadge.
- Section “书籍详情” (rows in card bg primary.opacity(0.03), radius 12): 作者 / 语言 (localized language code) / 阅读状态 / 阅读进度 (`%.0f%%`) / 添加时间 / 上次打开 / 评分 (★☆ string or 未评分).
- Section “文件信息”： 文件格式 (uppercase) / 文件大小 (MB/KB formatter) / 总页数 / 文件位置 (path).

### 3.10 BookmarkListSheet (also reachable from Reader)
- Grouped list sorted by createdAt DESC. Empty state: `bookmark.slash` 28pt + “暂无书签” + book title.
- Row: 10pt color circle; title = chapterTitle or “第 %d 章” (16pt semibold rounded, 2 lines); trailing page chip “第 %d 页” on bookmark color at 0.56 opacity radius 10; second row caption labels chapter/page; third row formatted date. Row background bookmark color at 0.34 opacity radius 12.
- **Leading swipe actions = 4 color swatches**: 白色 FFFFFF (swipe tint D9D9D9), 薄荷绿 D6EFDE (B7DEC3), 蓝色 D4E7FF (B2D0F4), 蜜桃色 FFDCC8 (F2BEA4).
- Swipe-to-delete; EditButton when non-empty; “完成” trailing.
- Tap row → dismiss + open ReaderView at location.

### 3.11 HighlightListSheet (full-screen cover)
- Toolbar: leading back (“‹ 返回”)； trailing (normal mode): 导出 button (format dialog → “Markdown（.md）”)， 选择 (multi-select), sort menu (按章节/按修改时间， persisted `highlightSortMode`, default **modified_time**). In select mode: 取消 + 合并 (enabled when ≥2 selected).
- Sort: modifiedTime → updatedAt DESC then createdAt DESC; chapter → chapterIndex, pageIndex, offset (missing = Int.max), createdAt, updatedAt.
- Row: selection circle; 10pt color dot + chapter label (fallback “未知章节”) + date; highlighted text block (bg = highlight color at 0.48 opacity, radius 10) — tappable to jump (not for merged); note line or “暂无笔记”； inline action buttons: 添加笔记/编辑笔记， 分享笔记， 删除 (red).
- Leading swipe actions = 4 highlight color swatches: 黄色， 绿色 D6EFD6 (B6DBB6), 蓝色 D4E7FF (B2D0F4), 粉色 FFD3E5 (F0B1CB). Trailing full-swipe delete (red).
- Multi-select merge: joins texts with `\n\n`, notes joined `\n\n`, keeps first color/chapter/page/createdAt, sets `mergedFromHighlights=true` in startPosition, deletes originals.
- Note editor: sheet showing quoted text (15pt serif on primary 0.05 bg radius 10) + TextEditor; toolbar 取消/保存.
- Markdown export: YAML front-matter (`title`, `book`, `exported_at` ISO8601, `sort_order`, `total_entries`) + `# Highlights & Notes` + numbered `## n. chapterTitle` sections with `> ` blockquote highlight and note body; file `{sanitizedTitle}-highlights-{yyyyMMdd-HHmmss}.md` shared via system share sheet.
- Share card: rendered image → **HighlightSharePreviewSheet** (preview with frame-style selector, 分享图片 / 下载 buttons; footer “由 LanRead 分享” / "AI for EPUB, synced to Notion"; uses profile display name (fallback “读者”) + avatar).

---

## 4. READER SCREEN

### 4.1 Structure
Full-screen cover; nav bar hidden; status bar hidden while toolbar hidden. Background: light `#FAFAFA`, dark `#0D0D12`. Loading view: spinner + “正在加载书籍...”; error view: warning icon 50pt orange + “加载失败” + error + blue “返回” pill (radius 12).

### 4.2 Rendering approach (ReaderWebView)
- Chapter HTML wrapped in: `<!DOCTYPE html>` + viewport meta + `<style>{css}</style>` + `<body><div class="reader-content"><div class="reader-book-root">{html}</div></div></body>`.
- **Horizontal CSS-column pagination**: `.reader-content { padding: 20px 0; column-width: 100vw; column-gap: 0; column-fill: auto; }`; `.reader-book-root { padding-left/right: var(--reader-page-margin) }`. Page turns are programmatic via JS `scrollToPage(index)`.
- Page count = `ceil(scrollWidth / innerWidth)`; cross-page paragraphs are **split into continuation blocks** (`p.reader-generated-continuation`, up to 3 split rounds).
- Rendered HTML cached (NSCache countLimit 12) keyed by `chapter:{bookID}-{chapterOrder}|font:{sizeInt}|line:{lineSpacing:.3f}|theme:{dark|light}|margin:{int}`; neighboring chapters (−1…+2) pre-rendered.
- Message handlers JS→native: `toggleToolbar`, `textSelection` (text, cleanedText, start/end offsets, pageIndex, rect, canContinueToNextPage), `highlightTap` (id+text), `contentLinkTap` (href, isExternal, footnote/TOC hints), `pageMetrics` (pageCount / currentPage), `interaction` (touch active).

### 4.3 Exact injected CSS (light / dark values)
- bg: `#fafafa` light / `#0d0d12` dark; text: `rgba(0,0,0,0.86)` / `rgba(255,255,255,0.85)`; link: `#0066cc` / `#5aadff`.
- Font stack: `-apple-system, 'PingFang SC', 'Hiragino Sans GB', sans-serif` (Android: system sans + CJK fallbacks); font-feature kern+liga; hyphens auto.
- body: `font-size: {size}px; line-height: {computed}`; **line-height mapping** from slider (0.6–2.4): `ls ≤ 1.0 → 0.9 + ls*0.55`; `ls > 1.0 → 1.45 + (ls−1)*0.34`.
- `p { margin: 0 0 0.85em 0; text-align: justify; text-indent: 2em; letter-spacing: 0.01em; orphans:2; widows:2 }`
- Headings: weight 700, margin `1.4em 0 0.6em`, line-height 1.25, break-after avoid; h1 1.65em (padding-bottom .25em), h2 1.4em, h3 1.2em, h4 1.08em, h5/h6 1em.
- img: max-width 100%, display block, margin 0.8em auto, radius 4. ul/ol margin `0.5em 1.5em`.
- blockquote: margin 0.8em 0, padding 0.6em 1em, border-left 3px `rgba(0,0,0,0.12)`/`rgba(255,255,255,0.15)`, bg `rgba(0,0,0,0.02)`/`rgba(255,255,255,0.03)`, italic.
- code: 0.88em; bg `rgba(0,0,0,0.04)`/`rgba(255,255,255,0.06)`; radius 4. pre: padding 0.8em 1em, bg `rgba(0,0,0,0.03)`/`rgba(255,255,255,0.05)`, radius 6.
- table: th/td padding 0.45em 0.6em, 1px border `rgba(0,0,0,0.08)`/`rgba(255,255,255,0.1)`.
- hr: 1px `rgba(0,0,0,0.06)`/`rgba(255,255,255,0.08)`, margin 1.8em 0. strong 600; small 0.85em.
- `::selection { background: rgba(0,122,255,0.2) light / rgba(50,130,246,0.35) dark; color: inherit }`
- `mark.reader-highlight { border-radius: 2px; padding: 0 1px; box-decoration-break: clone }`.
- `.pagebreak { page-break-after: always; margin: 2em 0 }`; `.footnote { font-size: .85em; vertical-align: super }`.

### 4.4 Interaction model
- **Tap zones**: x < **24%** width = previous page; x > 76% = next page. Center 52%: toolbar toggle, double-tap suppression window **0.18 s**. Tap page-turn uses **fade** animation: crossfade **0.12 s**. Haptic: soft impact.
- **Swipe**: horizontal drag > 8pt; offset = translation × **0.65**, clamped to ±40% width; rubber-band at boundaries; drag hides toolbar. Release: page turn if |translation| > **12% width** OR |velocity.x| > **150**; else spring back (response 0.3, damping 0.86). Turn animation = **slide**: old-page snapshot slides 35% width with gradient shadow, 0.3s spring; one page per swipe (min interval 0.36s).
- Boundary: haptic + toast “已经是本章第一页 / 已经是本章最后一页”.
- Toolbar show/hide: spring(response 0.35, damping 0.86).
- Inline chapter title (when toolbar hidden): 14pt semibold rounded secondary, 1 line, h-padding max(20, margin), followed by one blank line of `fontSize` height; page indicator bottom: “{page+1} / {total}” 11pt secondary 0.7 in a **36pt** bottom strip (only when chapter has >1 page).

### 4.5 Toolbars
- **Top** (translucent material, h-padding 20, v-padding 12): back circle button (44×44, circle primary 0.08 + shadow) — saves progress then dismisses; title block (book title 16pt semibold; chapter title 13pt secondary); TOC button; settings button.
- **Bottom** (v-padding 16): chapter progress row — “{n}” (13pt, minWidth 30) + 4pt capsule track with blue gradient fill width = (chapterIndex+1)/chapterCount + “{total}”; then 3 equal-width 44pt buttons (radius 12): bookmark toggle (active tint blue with blue 0.12 bg), highlights, bookmark list (stacked triple-bookmark icon).

### 4.6 Typography & theme controls (`ReaderSettingsView` sheet)
- Section 字体设置： font size segmented 小/中/大/特大 → **16 / 19 / 22 / 26 px**.
- Section 排版设置： line spacing slider **0.6…2.4 step 0.1** (default 1.0); page margins slider **0…50 step 5** (default 35, shown "{n}pt").
- Section 主题： segmented 跟随系统/浅色/深色.
- (Global Settings → 阅读偏好 additionally offers font family menu: 系统字体/衬线字体/等宽字体.)

### 4.7 TOC sheet (`TableOfContentsView`)
Grouped list of TOC items (fallback = flat chapter list). Row: 36pt circle with number (current = blue gradient, white text; else gray gradient); title 16pt medium serif (2 lines); current row shows “当前章节” blue 12pt + checkmark. Indent = level × 16. Auto-scrolls to current. Trailing “完成”.

### 4.8 Bookmarks
Toolbar bookmark toggles: adds Bookmark(chapterIndex, pageIndex, chapterTitle, colorHex FFFFFF) or deletes existing at same location.

### 4.9 Text selection → highlight/note/AI
- Selection posts offsets (global char offsets across chapter text nodes) + page index + rect. Selection cleared 0.22s after collapse. Haptic selectionChanged.
- **Floating selection toolbar** (radius 26, width = clamp(width−16, 320…820), shadow black 0.12 r18 y8; placed above or below selection):
  - Optional top button “下一页继续选择” (white text on blue→cyan capsule gradient).
  - Horizontal scroll of capsule actions (white 0.65 bg): 高亮标记 (yellow tint) → creates Highlight with default color = system yellow; 添加笔记 (blue); AI 解释 (purple); AI 翻译 (green); 复制 → toast “已复制到剪贴板”.
  - Bottom ask bar: sparkles icon + placeholder “针对这段内容输入你的问题...” + send → **Ask composer sheet** (quoted selection card 16pt serif; bottom input row with 30pt blue send circle).
- Toasts: top overlay, 14pt semibold on translucent radius 16, auto-dismiss **1.6 s**.
- **Tapping an existing highlight** opens **Highlight actions sheet** “高亮详情”： quoted content (16pt serif); note row; bottom grid: AI 解释， AI 翻译， 复制高亮， 问问 AI, 分享笔记， 删除高亮 (red, confirm “确定删除此高亮？”); note deletion “确定删除这条笔记？”.
- AI response sheet: title = 翻译/解释/问问 AI； loading + “AI 正在生成...”; result 16pt serif; “插入到笔记” button appends AI content to highlight note.
- Link taps: internal links → **LinkTargetPreviewSheet** (mini WebView preview with target fragment highlighted; buttons “跳转到目标位置” / “留在当前页”)； external → “在浏览器打开”； unresolvable → toast “当前书籍中无法定位该链接目标。”

### 4.10 Progress persistence & status
- On every page turn / chapter change / dismiss: `ReadingProgress.currentPage = chapterIndex`; `currentPosition` JSON = {chapterIndex, pageIndex, totalPages, globalPage, bookTotalPages}; `progressPercentage = globalPage / bookTotalPages`; `book.totalPages` updated; `lastAccessedAt` updated.
- Status transitions: on open/reading, wantToRead|paused → **reading**; ≥ **99%** or last page → **finished**.
- On app launch / Library appear: books with status reading and no access for **≥ 7 days** → **paused**.
- Reading time: heartbeat every **15 s**; adds to `totalReadingTime` + `detailedReadingTime` (Reader) or `skimmingReadingTime` (Skimming); daily seconds store (resets local midnight).

---

## 5. AI FEATURES

### 5.1 AI privacy consent (launch gate)
- Sheet presented at launch when consent version < 2 or not suppressed; non-dismissable. Title “AI 数据权限提示”. Lines: 用途说明 / 处理位置 / 第三方共享 / 第三方 AI 提供方：{provider} + 地域说明 / 明确授权. Link “查看隐私政策”. Note “你可以稍后在“设置 > AI 数据与隐私”中查看并修改该权限。”
- Controls: Toggle “不再提示”； “同意并继续” (prominent) / “暂不使用”. AI calls blocked with “在你授权数据传输前，AI 功能暂不可用。” until granted.
- Provider disclosure from endpoint host: Azure OpenAI / OpenAI / Anthropic / Google AI / xAI / DeepSeek / Moonshot AI / Zhipu AI / Baidu AI / Alibaba Cloud (DashScope) / Volcengine (Doubao) / Custom AI Provider.

### 5.2 AI config backend
- Preferred: secure server `POST {SecureServerBaseURL}/v1/keys/ai` with HMAC-signed body `{clientId, nonce, timestamp, signature}`. Response: `{apiKey, endpoint, model}`.
- Fallback: local config values (OpenAI-compatible). DashScope endpoints add `"enable_thinking": false`.
- All AI calls: `POST {endpoint}/chat/completions`, Bearer auth, JSON `{model, messages, temperature, max_tokens}`.

### 5.3 Start-reading summary (`AISummaryView` + `AISummaryService`)
- Triggered on book card tap and after import. If cached summary (aiSummary + aiKeyPoints + generatedAt within **30 days**) → show after 1.0s; else generate.
- Flow: (1) **knowledge probe** — `You are doing a binary knowledge check for a book title.\n\nBook title: {t}\nAuthor: {a}\n\nReply with exactly one uppercase word:\nYES\nor\nNO\n\nRules:\n- If uncertain, reply NO.\n- Do not output anything else.` (temp 0.0, maxTokens 8).
- (2) Context strategy: known → no excerpt; unknown → TOC + “summary-like” chapters (前言/序言/导言/摘要/概述/结语/后记/preface/introduction/abstract/overview/conclusion/afterword/epilogue etc.), 700 chars/chapter; else sampled chapters (first 2 + middle 2 + last 2), 500 chars/chapter. Content cleaned locally.
- (3) Prompt (localized; zh): “请为以下书籍生成一份导读摘要：” + 书籍信息 + Context Policy + TOC + 节选 + requirements (100-150字摘要 / 3-5个关键要点 / 简洁明了) + format (段落 / • 列表) + “请使用简体中文回复。” Temp 0.7, maxTokens 2000, timeout 60 s.
- (4) "Streaming" is **simulated**: full response split on spaces, emitted every **50 ms** with progress %.
- UI: header (brain icon + “AI 导读摘要” + subtitle + blue gradient “开始阅读” button → ReaderView); generating view (linear progress + % + partial markdown + “正在分析书籍内容...”); ready view: “内容摘要” + “关键要点” orange bullets + disclaimer “AI 可能生成错误信息” + “生成于 {date}”. Error view with 重试.
- Bottom “略读模式” entry: orange gradient pill (bolt icon + 略读模式 + arrow.left), plus left-swipe ≥100pt gesture.

### 5.4 Skimming mode (`SkimmingModeView` + `SkimmingModeService`)
- Dark theme always: bg gradient `rgb(0.04,0.05,0.10)` → `rgb(0.02,0.02,0.05)`; white text; chapter cards bg white 0.04, radius 24, stroke white 0.08.
- Top bar: close + TOC button. Bottom: “{n}/{total}” caption + white progress capsule (4pt) + “继续阅读” pill (white 0.15 bg radius 20) → ReaderView.
- Pager: page-style; per-chapter card: chapter title (title2 bold), then loading (“AI 正在生成章节骨架...”), error + 重试， placeholder (“暂无略读内容” + “生成当前章节略读”)， or summary sections:
  1. Time badge (hourglass): “略读约 %d 分钟”
  2. 阅读目标 card (blue 0.15 bg, radius 16)
  3. 章节骨架： 3–5 points (label subheadline bold + insight, white 0.05 bg radius 12)
  4. 关键句： numbered
  5. 关键词： chip grid (caption, white 0.1 bg, radius 20)
  6. 检视问题： “• {question}” ×2–3
  7. AI 略读讲解： markdown narrative
  8. Blue button “阅读本章” (radius 16) → ReaderView at that chapter/fragment
- Preload: current + next **3** chapters. Forward swipe blocked while loading (1.5s-cooldown toast “当前章节正在加载，请等待加载完成后继续切换。”); backward allowed.
- Prompt skeleton: `You are IslaBooks' inspectional reading coach.` + Adler framing + TASK CONTEXT + CONTEXT POLICY (knowledge probe per chapter; NO → full cleaned chapter between CHAPTER CONTENT START/END) + OUTPUT FORMAT: ONLY JSON `{chapterTitle, readingGoal, structure:[{label,insight}×3-5], keySentences[×3], keywords[×5], inspectionQuestions[×2-3], aiNarrative (<220 words), estimatedMinutes (2–6)}` + RENDERING RULES (language = app language name). Temp 0.4, maxTokens 1200, timeout 60 s.
- Cache: persisted into `Book.skimmingSummaries` keyed `{bookUUID}-{chapterOrder}-{lang}`; last visited chapter persisted.

### 5.5 Inline AI actions (`ReadingAIService`) — exact prompts
- **Translate**: `You are a professional translator inside an eBook reader. Translate the following passage into {languageName} with clear, natural phrasing. Keep the meaning faithful and avoid extra commentary.\n\nPassage:\n{text}` — temp 0.35, maxTokens 480. Target = `translation_language`.
- **Explain**: `You help readers understand tricky passages. In {languageName}, briefly explain the following excerpt in under 120 words. Start with a one-sentence summary, then list 2-3 concise bullet insights only if helpful. Keep tone clear and neutral.\n\nExcerpt:\n{text}` — temp 0.5, maxTokens 520.
- **Ask**: `You are an assistant embedded in an eBook reader. In {languageName}, answer the reader's question based only on the excerpt when possible. Keep the answer concise, clear, and practical.\n\nQuestion:\n{question}\n\nExcerpt:\n{text}` — temp 0.45, maxTokens 620.
- All include system message: `You are an assistant embedded in an eBook reader. Keep answers concise and reader-friendly.` Timeout 45 s.

---

## 6. PROGRESS SCREEN (`ReadingProgressView`)

- Nav large title “阅读统计”. Segmented picker 时间范围： 本周 / 本月 / 今年 (filters by calendar week/month/year of `lastReadAt`).
- **6 stat cards** in 2-column grid (spacing 16, card: bg systemGray6, radius 12, icon top-left, value title2 bold, label caption secondary):
  1. 细读时长 (clock, blue) — format “{h}h {m}m” or “{m}m”
  2. 略读时长 (bolt.horizontal.circle, teal)
  3. 平均进度 (chart, green) — "%.1f%%"
  4. 阅读书籍 (books.vertical, orange)
  5. 完成阅读 (checkmark.seal, indigo) — status==finished or ≥99%
  6. 目标达成 (target, purple) — `min(100, totalMinutes*100 / (dailyGoal × daysInRange))%`
- **Goal card**: title “阅读目标” + “每日 %d 分钟”； right "{current}/{total} 分钟” (total = goal × days); 8pt progress bar radius 4, blue→purple gradient, animated 0.5s; footer “%.0f%% 完成” and green “✓ 目标达成！” when ≥100%.
- **最近阅读** list (top 5): rows (bg systemGray6, radius 12) with 50×70 cover (placeholder blue→purple gradient), title (2 lines), author caption, blue progress "xx.x%", relative date. Tap → ReaderView.
- Maintenance on appear: delete orphaned ReadingProgress rows; migrate legacy time buckets.

---

## 7. SETTINGS + PROFILE

### 7.1 SettingsView root (grouped list, large title “设置”)
1. **个人资料** section: row with 30pt avatar (photo or initial-letter circle with gradient rgb(0.41,0.57,0.94)→rgb(0.29,0.44,0.84)) + “个人资料” + display name caption → ProfileSettingsView.
2. **语言** section: 语言 menu picker (跟随系统/English/中文/日本語/한국어); 翻译目标语言 menu picker.
3. **阅读设置** section: 阅读偏好 → ReadingSettingsView; 主题外观 → ThemeSettingsView; 高亮排序 → HighlightSortSettingsView (subtitle “当前：%@”, footer “该顺序也会用于”高亮与笔记“同步到 Notion 时的排序。”).
4. **数据与同步** section: 连接 Notion row (workspace icon 20pt; subtitle = workspace name / 连接中... / 未连接； accessory = green check / spinner / warning / chevron) → NotionAuthView; 数据管理 → DataManagementView.
5. **AI 数据与隐私** section: AI 数据权限 row (shield icon; subtitle 状态：已允许/未允许) → consent sheet; 当前第三方 AI 提供方 row; 隐私政策 link.
6. **通知提醒** section: 每日阅读提醒 toggle (bell); if on → 提醒时间 time picker; 每日目标 stepper **10…180 step 5** shown “{n} 分钟”.
7. **关于** section: 关于澜阅 → AboutView; 版本 1.0.
8. **支持** section: 联系我们 (mailto:support@isla-reader.top); 隐私政策 (https://isla-reader.top/privacy).
- Reminder permission: on toggle-on request authorization; denied → toast “请在系统设置中允许通知，才能收到阅读提醒。” and toggle reverts.

### 7.2 ProfileSettingsView
- Avatar 72pt (photo picker; overlay “编辑” capsule); display name TextField placeholder “输入你的显示名称”， **max 24 chars**, counter "{n}/24", hint “留空时，分享卡片会使用默认名称。”; destructive “移除当前头像” if set. Preview hint “该身份将用于分享图片中的笔记评论。” Default name “读者”. Avatar JPEG 0.85, max **512px**.

### 7.3 ReadingSettingsView (global)
Font size menu, font type menu, line spacing slider (0.6–2.4 step 0.1), page margins slider (0–50 step 5), 预览 section with sample text “这是一段示例文本，用于预览当前的字体和排版设置效果。您可以调整上面的设置来获得最佳的阅读体验。”

### 7.4 ThemeSettingsView
List of 跟随系统/浅色/深色 with descriptions, checkmark on selection; preview card showing primary/secondary/accent swatches.

### 7.5 DataManagementView
- **数据导出**： “导出全部阅读数据” → ExportDataView (icon, description, notice “导出的数据不包含书籍文件...”, “开始导出” blue button, then share with file). JSON: `{version:1, exportedAt, readingData:{books:[{checksum, readingProgress, libraryItem, bookmarks[]}]}, notes:{version:1, highlights[], annotations[]}}`.
- **数据导入**： “导入阅读数据” (.json picker) → matches books **by checksum**; result “成功匹配 %d 本书，更新 %d 条进度和 %d 个书签。未匹配的书籍：%d 本。”
- **存储管理**： 缓存大小 row; “清理缓存” (orange) → clears cache dir + nulls aiSummary/aiKeyPoints/aiSummaryGeneratedAt/skimmingSummaries; success “已释放 %@ 缓存”.
- **危险操作**： “清除所有数据” (red) → confirm (“确认清除” / warning “此操作将删除所有书籍、阅读进度、笔记和设置。此操作不可撤销。”) → wipes all + deletes Books dir; success “已清除 %d 本书，释放 %@ 存储空间。”
- **AboutView**: app icon 100pt radius 20, name 澜阅， 版本 1.0, tagline, “© 2025 澜阅”, “用心打造的阅读体验”.

### 7.6 NotionAuthView
- Icon 60pt; “连接到 Notion” title; description “授权 LanRead 访问你的 Notion 工作区，以便同步你的阅读笔记和高亮。”; blue button “开始授权”/“重新授权” (key.fill icon); destructive “断开连接” when connected; privacy notice “我们将只访问你授权的内容，不会访问其他数据。” + privacy link.
- Pre-flight alert “连接 Notion 须知”： “App 将申请访问您的 Notion 空间。我们将在您选择的页面下创建一个'LanRead Library'数据库。您的阅读记录将仅单向同步至该库，App 不会读取或修改您 Notion 中的其他数据。” (同意并继续 / 取消).
- After connect: page picker (“选择父页面” / “你想把书单存在哪个页面下？” / 刷新 / 未命名页面) → create database “📚 LanRead Library” with properties Name (title), Author (rich_text), Reading Progress (number percent), Reading Status (select: Want to Read / Reading / Finished / Paused).

---

## 8. REMINDERS & NOTIFICATIONS
- Notification id `reading_reminder_daily_2000`; default time **20:00**; deep link `isla-reader://read/last` (schemes `isla-reader` and `lanread`, host `read`, path `/last`).
- Scheduling: daily repeating alarm at {hour, minute}. Content: title “阅读时间”， body “给自己 %d 分钟阅读时间。”, deepLink userInfo.
- Tap handling: → switch to **Library** tab (continue reading).
- Disable: cancels pending notification.
- Android: AlarmManager/WorkManager + NotificationChannel (high importance), deep link via intent filter.

---

## 9. NOTION SYNC
- OAuth: Custom Tab → `https://api.notion.com/v1/oauth/authorize?client_id={ID}&response_type=code&owner=user&redirect_uri={URI}&state={random32}`. Redirect URI `lanread://notion/finish`. State validated (CSRF). App does **not** exchange code directly; POSTs `{session_id}` to `{SecureServerBaseURL}/v1/oauth/finalize` returning `{access_token, workspace_name?, workspace_id?, workspace_icon?}`. Token in EncryptedSharedPreferences.
- Workspace change wipes mappings + queue. Disconnect clears all.
- Queue-based sync engine: observer on data saves — Highlight insert/update/delete enqueues **one SyncQueueItem per book** (type `highlight`). Processor: on enqueue debounce **2 s**; loop while network available AND connected; oldest `pending` → `inProgress` → execute → delete on success + update `lastSyncedAt`.
- Task: (1) sync book page — look up via `Book.notionPageId`; if mapped → update properties; else create page with children `[heading_1 "📝 Notes", divider]`. Properties: Name=title, Author=rich_text, Reading Progress=number percent, Reading Status=select. (2) Replace highlights/notes blocks — list all child blocks (pageSize 100, paginate), archive each, append rebuilt blocks in batches (≤ **100 blocks** and ≤ **450,000 bytes**).
- Rebuilt blocks: `heading_1 "📝 Highlights & Notes"` + divider; groups per sort mode (heading_2 per date/chapter group), each highlight = **quote block**, each note = **callout** with 💡, spacer paragraphs; text truncated at **2000 chars** + "...".
- Failure: retry up to **5**. 429 → pause `max(1, retryAfter)` s. Network → backoff `2^(retry-1)` capped 64 s. Unknown → 8 s. In-progress reset to pending on start.

---

## 10. LOCALIZATION
Languages: **English (en), 简体中文 (zh-Hans), 日本語 (ja), 한국어 (ko)** (+ system-follow). In-app language override. Key zh-Hans strings are in `Isla Reader/zh-Hans.lproj/Localizable.strings` (498 lines) — copy all keys into Android `values-zh-rCN/strings.xml`; en/ja/ko from their `.lproj` files.

---

## 11. VISUAL DESIGN TOKENS
- **Global tint: system blue** (Android: `#2196F3`-ish; iOS system blue `#007AFF` for closest match).
- Reader background: light `#FAFAFA`, dark `#0D0D12`. Reader text: `rgba(0,0,0,0.86)` / `rgba(255,255,255,0.85)`. Links `#0066CC` / `#5AADFF`.
- Chrome surfaces: translucent blur (~white/black 0.7–0.8); card fills `#F2F2F7` (systemGray6) / primary 0.03–0.08.
- Corner radii: **4** (img/code), **8** (badges), **10** (search field, chips), **12** (cards, buttons, list rows), **16** (toast, skimming buttons), **20** (keyword chips, pills), **24** (skimming cards), **25** (empty-state import button), **26** (selection toolbar).
- Gradients: book placeholder cover blue(0.6)→purple(0.8) diagonal; goal bar blue→purple; start-reading button blue→blue(0.8); skimming entry orange→orange(0.82); skimming bg vertical; avatar rgb(0.41,0.57,0.94)→rgb(0.29,0.44,0.84); continue-selection capsule blue(0.88)→cyan(0.82).
- Status colors: gray/blue/green/orange; progress badge orange/blue/purple/green by quartile; destructive red; success green.
- Typography: rounded for UI labels, serif for quoted content, monospace for code. Reader body 16/19/22/26px. Buttons ≥44pt touch targets.

---

## 12. SAMPLE BOOKS
- One bundled EPUB: **`SampleBooks/LanRead_Getting_Started_Multilingual.epub`** (“LanRead User Guide”), 8 chapter XHTMLs in **en, ja, zh, ko, es, de, fr, it** — a 10-section tutorial (Welcome / Import / AI Summarize / Skimming / Bookmark-Highlight-Note / Ask AI / Sharing / Notion sync / Theme settings / Closing).
- Seeding: runs on every launch; guarded by flag `bundled_sample_book_seeded_v1`; imports via normal pipeline; marks done on success or "already exists".
