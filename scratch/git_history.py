import subprocess
import sys

try:
    # Use capture_output=True but do not set text=True so we get raw bytes
    res = subprocess.run(['git', 'log', '-p', '-n', '20', '--', 'shike-backend/src/main/resources/application.yml'], capture_output=True, timeout=10)
    stdout_decoded = res.stdout.decode('utf-8', errors='replace')
    sys.stdout.buffer.write(stdout_decoded.encode('utf-8'))
except Exception as e:
    print("Error:", e)
