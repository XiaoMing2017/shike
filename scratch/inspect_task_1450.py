import json
import os
import sys

log_file = r"C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef\.system_generated\tasks\task-1450.log"
if os.path.exists(log_file):
    with open(log_file, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()
        match = content.find('{"code":200')
        if match != -1:
            json_str = content[match:].strip()
            try:
                data = json.loads(json_str)
                food_items = data['data']['foodItems']
                items = json.loads(food_items)
                for item in items:
                    name = item.get('name')
                    unicode_escape = name.encode('unicode_escape').decode('ascii') if name else ''
                    msg = f"Food Name: {name} (unicode: {unicode_escape})\n"
                    sys.stdout.buffer.write(msg.encode('utf-8'))
            except Exception as e:
                print("Failed to parse:", e)
        else:
            print("Could not find json in content:", repr(content[:200]))
else:
    print("Log file does not exist")
