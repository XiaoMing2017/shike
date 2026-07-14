import json
import sys

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get('step_index') == 982:
                sys.stdout.buffer.write(json.dumps(data, indent=2).encode('utf-8'))
        except Exception as e:
            pass
