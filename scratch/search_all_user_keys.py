import json
import re
import sys

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get('type') == 'USER_INPUT':
                content = data.get('content') or ""
                keys = re.findall(r'sk-[a-zA-Z0-9]{20,50}', content)
                if keys:
                    msg = f"User input at step {data.get('step_index')}: {content[:100]}... Keys: {keys}\n"
                    sys.stdout.buffer.write(msg.encode('utf-8'))
        except Exception as e:
            pass
