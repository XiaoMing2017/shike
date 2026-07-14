import os

root_dir = r"d:\heming"
for file in os.listdir(root_dir):
    path = os.path.join(root_dir, file)
    if os.path.isfile(path) and (file.endswith('.txt') or file.endswith('.md') or file.endswith('.json')):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
                if 'baidu' in content.lower() or '7544108' in content or 's0un' in content:
                    print(f"Found in {path}")
        except Exception as e:
            try:
                with open(path, 'r', encoding='gbk') as f:
                    content = f.read()
                    if 'baidu' in content.lower() or '7544108' in content or 's0un' in content:
                        print(f"Found in {path} (gbk)")
            except Exception as e2:
                pass
