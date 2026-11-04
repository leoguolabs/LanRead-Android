import os
import io
import glob
import re

base = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res"))
for f in glob.glob(base + r"\values*\strings.xml"):
    content = io.open(f, encoding="utf-8").read()
    lines = content.splitlines(keepends=True)
    out = []
    changed = 0
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("<string") and "</string>" in line:
            start = line.index(">", line.index("<string")) + 1
            end = line.rindex("</string>")
            value = line[start:end]
            # escape unescaped apostrophes
            new_value = re.sub(r"(?<!\\)'", r"\\'", value)
            if new_value != value:
                line = line[:start] + new_value + line[end:]
                changed += 1
        out.append(line)
    io.open(f, "w", encoding="utf-8").write("".join(out))
    print(f, "escaped", changed, "apostrophes")
