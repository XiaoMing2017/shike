import json
import sys

with open(r'C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if data.get('type') == 'SEARCH_WEB' or 'search_web' in str(data.get('tool_calls')):
                idx = data.get('step_index')
                source = data.get('source')
                print(f"Step {idx} ({source}) tool call/result:")
                content = data.get('content') or ""
                # Print any keys or interesting terms in content
                print("  Query/Summary:", content[:300])
        except Exception as e:
            pass
