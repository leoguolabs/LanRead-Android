# LanRead EPUB Test Corpus Download Script
$ErrorActionPreference = "Continue"

$epubsDir = $PSScriptRoot
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LanRead (澜阅) EPUB 端到端测试样本库自动下载" -ForegroundColor Cyan
Write-Host " 目标目录: $epubsDir" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$books = @(
    @{
        Name = "accessible_epub_3.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/accessible_epub_3.epub"
        Desc = "W3C 标准 EPUB 3.0 基准测试样本"
    },
    @{
        Name = "childrens-literature.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/childrens-literature.epub"
        Desc = "W3C 高密度图文排版与内联样式"
    },
    @{
        Name = "linear-algebra.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/linear-algebra.epub"
        Desc = "W3C 科学公式 (MathML) 与矩阵排版"
    },
    @{
        Name = "kusamakura-japanese-vertical-writing.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/kusamakura-japanese-vertical-writing.epub"
        Desc = "W3C 日文竖排 (vertical-rl) 与假名注音 (Ruby)"
    },
    @{
        Name = "regime-anticancer-arabic.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/regime-anticancer-arabic.epub"
        Desc = "W3C 阿拉伯语从右向左 (RTL) 镜像排版"
    },
    @{
        Name = "israelsailing.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/israelsailing.epub"
        Desc = "W3C 希伯来语 RTL 与双向文本 (BiDi)"
    },
    @{
        Name = "wasteland.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/wasteland.epub"
        Desc = "W3C 复杂双向尾注与学术注释 (epub:type=noteref)"
    },
    @{
        Name = "moby-dick.epub"
        Url  = "https://github.com/IDPF/epub3-samples/releases/download/20230704/moby-dick.epub"
        Desc = "W3C 经典名著多章节连续排版"
    },
    @{
        Name = "jane-austen_pride-and-prejudice_advanced.epub"
        Url  = "https://standardebooks.org/ebooks/jane-austen/pride-and-prejudice/downloads/jane-austen_pride-and-prejudice_advanced.epub"
        Desc = "Standard Ebooks 现代精排版 (傲慢与偏见)"
    },
    @{
        Name = "Accessibility-Tests-Mathematics-v1.1.1.epub"
        Url  = "https://github.com/daisy/epub-accessibility-tests/releases/download/math-extdesc-1.1.1/Accessibility-Tests-Mathematics-v1.1.1.epub"
        Desc = "DAISY 辅助功能与复杂表格/数学测试集"
    },
    @{
        Name = "Fundamental-Accessibility-Tests-Non-Visual-Reading-v2.0.1.epub"
        Url  = "https://github.com/daisy/epub-accessibility-tests/releases/download/non-visual-reading-2.0.1/Fundamental-Accessibility-Tests-Non-Visual-Reading-v2.0.1.epub"
        Desc = "DAISY 复杂跨行合并表格 (rowspan/colspan)"
    },
    @{
        Name = "pg25256_sanguo.epub"
        Url  = "https://www.gutenberg.org/ebooks/25256.epub3.images"
        Desc = "Project Gutenberg 中文经典《三国演义》"
    },
    @{
        Name = "pg1342.epub"
        Url  = "https://www.gutenberg.org/ebooks/1342.epub3.images"
        Desc = "Project Gutenberg 英文长篇《傲慢与偏见》"
    }
)

$headers = @{
    "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    "Accept" = "*/*"
}

foreach ($b in $books) {
    $outFile = Join-Path $epubsDir $b.Name
    if (Test-Path $outFile) {
        $len = (Get-Item $outFile).Length
        if ($len -gt 1024) {
            Write-Host "[已存在] $($b.Name) ($([math]::Round($len/1KB, 1)) KB) - $($b.Desc)" -ForegroundColor Green
            continue
        } else {
            Write-Host "[重试] $($b.Name) 尺寸异常 ($len bytes)，重新下载..." -ForegroundColor Yellow
            Remove-Item $outFile -Force
        }
    }

    Write-Host "[正在下载] $($b.Name) -> $($b.Desc) ..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri $b.Url -OutFile $outFile -Headers $headers -TimeoutSec 30
        $newLen = (Get-Item $outFile).Length
        Write-Host "  -> 成功: $([math]::Round($newLen/1KB, 1)) KB" -ForegroundColor Green
    } catch {
        Write-Host "  -> Invoke-WebRequest 失败: $($_.Exception.Message)，尝试 curl.exe ..." -ForegroundColor Yellow
        & curl.exe -s -L -A "Mozilla/5.0" -o $outFile $b.Url
        if (Test-Path $outFile) {
            $cLen = (Get-Item $outFile).Length
            if ($cLen -gt 1024) {
                Write-Host "  -> curl 成功: $([math]::Round($cLen/1KB, 1)) KB" -ForegroundColor Green
            } else {
                Write-Host "  -> curl 结果尺寸较小: $cLen bytes" -ForegroundColor Magenta
            }
        } else {
            Write-Host "  -> 下载失败: $($b.Url)" -ForegroundColor Red
        }
    }
}

Write-Host "`n全部下载完成，当前 epubs 目录列表:" -ForegroundColor Cyan
Get-ChildItem -Path $epubsDir -Filter "*.epub" | Select-Object Name, Length | Format-Table -AutoSize
