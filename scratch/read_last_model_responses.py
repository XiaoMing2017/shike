import json
import sys
with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    lines = list(f)
    print(f"Total lines: {len(lines)}")
    for line in lines:
        try:
            data = json.loads(line)
            idx = data.get('step_index', 0)
            if 1000 <= idx <= 1078:
                if data.get('source') == 'MODEL' and data.get('type') in ('PLANNER_RESPONSE', 'USER_INPUT'):
                    content = data.get('content') or ""
                    msg = f"--- STEP {idx} ({data.get('type')}) ---\n{content[:500]}...\n"
                    sys.stdout.buffer.write(msg.encode('utf-8', errors='replace'))
        except Exception as e:
            pass
