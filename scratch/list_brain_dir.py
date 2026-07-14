import os

brain_dir = r"C:\Users\19836\.gemini\antigravity\brain\9dc9f9ce-fcf2-4a6f-847e-434c7bd966ef"
if os.path.exists(brain_dir):
    for root, dirs, files in os.walk(brain_dir):
        # limit depth to 3
        depth = root[len(brain_dir):].count(os.sep)
        if depth > 3:
            continue
        print(f"{root}:")
        for file in files:
            print(f"  {file} ({os.path.getsize(os.path.join(root, file))} bytes)")
else:
    print("brain_dir does not exist")
