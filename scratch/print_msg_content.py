import os
import json
import sys

msg_dir = r"C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\messages"
for file in ["e743744b-cff8-415b-87ec-914e9b6f5057.json", "eaa7c95a-fa66-4b43-ac95-2477bd203d08.json"]:
    path = os.path.join(msg_dir, file)
    with open(path, 'r', encoding='utf-8') as f:
        data = json.loads(f.read())
        print(f"=== File: {file} ===")
        print("Sender:", data.get('sender'))
        print("Recipient:", data.get('recipient'))
        print("Content:")
        sys.stdout.buffer.write(data.get('content', '').encode('utf-8'))
        print("\n")
