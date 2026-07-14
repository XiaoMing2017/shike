import json
import sys

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            idx = data.get('step_index')
            if idx in [989, 990, 991, 992]:
                sys.stdout.buffer.write(f"--- STEP {idx} ({data.get('source')}, {data.get('type')}) ---\n".encode('utf-8'))
                content = data.get('content') or ""
                sys.stdout.buffer.write(content.encode('utf-8'))
                sys.stdout.buffer.write(b"\n")
        except Exception as e:
            pass
