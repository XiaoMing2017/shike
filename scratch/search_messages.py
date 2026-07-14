import os
import json

msg_dir = r"C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\messages"
if os.path.exists(msg_dir):
    for file in os.listdir(msg_dir):
        if file.endswith('.json'):
            path = os.path.join(msg_dir, file)
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if 's0unOk' in content or '7544108' in content:
                        print(f"Found in message: {file}")
                        # print content excerpt
                        data = json.loads(content)
                        # print key paths
                        print("Keys in JSON:", list(data.keys()))
            except Exception as e:
                print(f"Error reading {file}: {e}")
else:
    print("msg_dir does not exist")
