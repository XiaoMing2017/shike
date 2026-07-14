import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

transcript_path = r"C:\Users\19836\.gemini\antigravity\brain\1d00d338-66d2-4f6a-b2a2-b06521b5073a\.system_generated\logs\transcript_full.jsonl"

print("=== ALL USER INPUTS IN HISTORY ===")
with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        try:
            data = json.loads(line)
            source = data.get("source")
            step_type = data.get("type")
            content = data.get("content")
            step_index = data.get("step_index")
            
            if source == "USER_EXPLICIT" or step_type == "USER_INPUT":
                clean_content = content.encode('utf-8', errors='replace').decode('utf-8')
                print(f"\n[Step {step_index}] USER: {clean_content}")
        except Exception as e:
            pass
