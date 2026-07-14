import json
import sys

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        if '1781238772617' in line or 'media__' in line:
            try:
                data = json.loads(line)
                idx = data.get('step_index')
                source = data.get('source')
                type_ = data.get('type')
                print(f"Step {idx} ({source}, {type_})")
            except Exception as e:
                pass
