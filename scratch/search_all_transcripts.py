import os
import json

brain_dir = r"C:\Users\19836\.gemini\antigravity\brain"
if os.path.exists(brain_dir):
    for folder in os.listdir(brain_dir):
        log_path = os.path.join(brain_dir, folder, ".system_generated", "logs", "transcript.jsonl")
        if os.path.exists(log_path):
            print(f"Checking transcript in {folder}...")
            try:
                with open(log_path, 'r', encoding='utf-8') as f:
                    for line_num, line in enumerate(f, 1):
                        if 's0unOk' in line or '7544108' in line:
                            data = json.loads(line)
                            print(f"  Match at line {line_num}: source={data.get('source')}, content={repr(data.get('content') or '')[:200]}")
            except Exception as e:
                print(f"  Error reading {folder}: {e}")
else:
    print("brain_dir does not exist")
