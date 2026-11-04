import os
import io
import glob

base = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res"))
for f in glob.glob(base + r"\values*\strings.xml"):
    content = io.open(f, encoding="utf-8").read()
    # \" -> &quot; ; \n -> &#10; (valid XML escapes for Android strings)
    fixed = content.replace('\\"', "&quot;").replace("\\n", "&#10;")
    if fixed != content:
        io.open(f, "w", encoding="utf-8").write(fixed)
        print("fixed", f)
    else:
        print("ok", f)
