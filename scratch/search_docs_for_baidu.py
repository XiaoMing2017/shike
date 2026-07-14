import os

doc_dir = r"d:\heming\shike\文档"
for root, dirs, files in os.walk(doc_dir):
    for file in files:
        if file.endswith('.md') or file.endswith('.txt') or file.endswith('.json'):
            path = os.path.join(root, file)
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if 'baidu' in content.lower() or '7544108' in content or 's0un' in content:
                        print(f"Found in {path}")
            except Exception as e:
                pass
