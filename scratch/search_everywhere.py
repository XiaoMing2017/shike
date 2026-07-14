import os

search_folders = [r"d:\heming\shike", r"d:\heming\weChatProjectcts", r"d:\heming\新建文件夹"]
for folder in search_folders:
    if not os.path.exists(folder):
        continue
    for root, dirs, files in os.walk(folder):
        # Skip node_modules, target, .git
        dirs[:] = [d for d in dirs if d not in ['node_modules', 'target', '.git', '.idea']]
        for file in files:
            if file.endswith(('.txt', '.md', '.json', '.yml', '.yaml', '.xml', '.java', '.js', '.ts')):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        c = f.read()
                        if '7544108' in c or 's0unOk' in c or 'xcRFoy' in c:
                            print(f"Found in {path}")
                except Exception:
                    try:
                        with open(path, 'r', encoding='gbk') as f:
                            c = f.read()
                            if '7544108' in c or 's0unOk' in c or 'xcRFoy' in c:
                                print(f"Found in {path} (gbk)")
                    except Exception:
                        pass
