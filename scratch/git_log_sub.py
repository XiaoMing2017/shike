import subprocess
try:
    res = subprocess.run(['git', 'log', '-p', '-S', 's0unOk'], capture_output=True, text=True, timeout=10)
    print("=== Git Log stdout ===")
    print(res.stdout[:2000])
    print("=== Git Log stderr ===")
    print(res.stderr)
except Exception as e:
    print("Failed to run git log:", e)
