import subprocess
import sys

try:
    res = subprocess.run(['git', 'log', '-p', '-n', '3', '--', 'shike-backend/src/main/java/com/shike/service/impl/DietServiceImpl.java'], capture_output=True, timeout=10)
    stdout_decoded = res.stdout.decode('utf-8', errors='replace')
    sys.stdout.buffer.write(stdout_decoded.encode('utf-8'))
except Exception as e:
    print("Error:", e)
