import os
import subprocess
import sys

epubs_dir = os.path.dirname(os.path.abspath(__file__))

books = [
    {
        "name": "accessible_epub_3.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/accessible_epub_3.epub",
        "desc": "W3C 标准 EPUB 3.0 基准测试样本"
    },
    {
        "name": "childrens-literature.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/childrens-literature.epub",
        "desc": "W3C 高密度图文排版与内联样式"
    },
    {
        "name": "linear-algebra.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/linear-algebra.epub",
        "desc": "W3C 科学公式 (MathML) 与矩阵排版"
    },
    {
        "name": "kusamakura-japanese-vertical-writing.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/kusamakura-japanese-vertical-writing.epub",
        "desc": "W3C 日文竖排 (vertical-rl) 与假名注音 (Ruby)"
    },
    {
        "name": "regime-anticancer-arabic.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/regime-anticancer-arabic.epub",
        "desc": "W3C 阿拉伯语从右向左 (RTL) 镜像排版"
    },
    {
        "name": "israelsailing.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/israelsailing.epub",
        "desc": "W3C 希伯来语 RTL 与双向文本 (BiDi)"
    },
    {
        "name": "wasteland.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/wasteland.epub",
        "desc": "W3C 复杂双向尾注与学术注释 (epub:type=noteref)"
    },
    {
        "name": "moby-dick.epub",
        "url": "https://github.com/IDPF/epub3-samples/releases/download/20230704/moby-dick.epub",
        "desc": "W3C 经典名著多章节连续排版"
    },
    {
        "name": "Accessibility-Tests-Mathematics-v1.1.1.epub",
        "url": "https://github.com/daisy/epub-accessibility-tests/releases/download/math-extdesc-1.1.1/Accessibility-Tests-Mathematics-v1.1.1.epub",
        "desc": "DAISY 辅助功能与复杂表格/数学测试集"
    },
    {
        "name": "Fundamental-Accessibility-Tests-Non-Visual-Reading-v2.0.1.epub",
        "url": "https://github.com/daisy/epub-accessibility-tests/releases/download/non-visual-reading-2.0.1/Fundamental-Accessibility-Tests-Non-Visual-Reading-v2.0.1.epub",
        "desc": "DAISY 复杂跨行合并表格 (rowspan/colspan)"
    },
    {
        "name": "pg1342.epub",
        "url": "https://www.gutenberg.org/ebooks/1342.epub3.images",
        "desc": "Project Gutenberg 英文长篇《傲慢与偏见》"
    }
]

print("=" * 65)
print(f"LanRead (澜阅) EPUB 测试样本下载器 (curl --ssl-no-revoke)")
print(f"目标目录: {epubs_dir}")
print("=" * 65)

for b in books:
    target_path = os.path.join(epubs_dir, b["name"])
    if os.path.exists(target_path) and os.path.getsize(target_path) > 1024:
        size_kb = round(os.path.getsize(target_path) / 1024, 1)
        print(f"[已存在] {b['name']} ({size_kb} KB) - {b['desc']}")
        continue

    print(f"[正在下载] {b['name']} - {b['desc']}...")
    cmd = [
        "curl.exe",
        "--ssl-no-revoke",
        "-L",
        "--connect-timeout", "15",
        "--max-time", "60",
        "-A", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "-o", target_path,
        b["url"]
    ]
    try:
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if os.path.exists(target_path) and os.path.getsize(target_path) > 1024:
            size_kb = round(os.path.getsize(target_path) / 1024, 1)
            print(f"  -> 下载成功: {size_kb} KB")
        else:
            size = os.path.getsize(target_path) if os.path.exists(target_path) else 0
            print(f"  -> 下载结果异常: {size} bytes")
    except Exception as e:
        print(f"  -> 发生异常: {e}")

print("\n" + "=" * 65)
print("当前 EPUB 目录列表:")
for f in sorted(os.listdir(epubs_dir)):
    if f.endswith(".epub"):
        p = os.path.join(epubs_dir, f)
        print(f"  {f:<45} {os.path.getsize(p):>10} bytes")
print("=" * 65)
