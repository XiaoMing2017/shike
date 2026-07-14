import json
import re

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            content = data.get('content') or ""
            # Find any 24-character alphanumeric string or 32-character
            keys24 = re.findall(r'\b[a-zA-Z0-9]{24}\b', content)
            keys32 = re.findall(r'\b[a-zA-Z0-9]{32}\b', content)
            if keys24 or keys32:
                print(f"Step {data.get('step_index')}: {keys24} / {keys32}")
        except Exception as e:
            pass
