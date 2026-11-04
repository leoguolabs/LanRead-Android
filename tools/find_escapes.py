import os
import re
import io

pat = re.compile(r'\\\\u[0-9a-fA-F]{4}')
base = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res"))
for f in ['values/strings.xml', 'values-zh-rCN/strings.xml', 'values-ja/strings.xml', 'values-ko/strings.xml']:
    content = io.open(base + '\\' + f.replace('/', '\\'), encoding='utf-8').read()
    hits = pat.findall(content)
    if hits:
        print(f, set(hits))
        for i, line in enumerate(content.splitlines(), 1):
            if pat.search(line):
                print(' ', i, line.strip()[:130])
    else:
        print(f, 'clean')
