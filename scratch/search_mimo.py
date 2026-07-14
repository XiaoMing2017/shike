import json
import sys
with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        if 'sk-cz4l' in line or 'xiaomimimo' in line or 'mimo' in line:
            try:
                data = json.loads(line)
                idx = data.get('step_index', 0)
                content = data.get('content') or ""
                # Print tool calls and content
                msg = f"--- STEP {idx} ---\n{content[:200]}\nToolCalls: {data.get('tool_calls')}\n"
                sys.stdout.buffer.write(msg.encode('utf-8', errors='replace'))
            except Exception as e:
                pass
