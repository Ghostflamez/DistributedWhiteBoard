import os

EXTS = {'.java'}    # 想导出的文件类型
OUT_PATH = 'repo_export.txt'

with open(OUT_PATH, 'w', encoding='utf-8') as OUT:
    for root, dirs, files in os.walk('.'):
        # 跳过 .git 等目录
        if '.git' in root.split(os.sep):
            continue
        for fn in files:
            if os.path.splitext(fn)[1] in EXTS:
                path = os.path.join(root, fn)
                OUT.write(f"\n\n===== {path} =====\n")
                try:
                    with open(path, encoding='utf-8', errors='ignore') as f:
                        OUT.write(f.read())
                except Exception as e:
                    OUT.write(f"[Error reading {path}: {e}]\n")
print(f"All done! Exported to {OUT_PATH}")
