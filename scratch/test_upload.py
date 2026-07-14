import requests
import os
import sys

# Ensure stdout encodes to utf-8 to prevent GBK errors on Windows terminal
sys.stdout.reconfigure(encoding='utf-8')

url = "http://117.72.61.18:8081/api/v1/diet/recognize"
image_path = r"D:\heming\shike\shike-frontend\images\home.png"

if not os.path.exists(image_path):
    print(f"Error: File {image_path} does not exist.")
    exit(1)

print(f"Uploading {image_path} to {url}...")
try:
    with open(image_path, "rb") as f:
        files = {"file": ("home.png", f, "image/png")}
        # Let's pass a hint parameter as well to test the full flow
        data = {"hint": "肉包"}
        response = requests.post(url, files=files, data=data, timeout=30)
        print("Status Code:", response.status_code)
        print("Response Body:")
        print(response.text)
except Exception as e:
    print("Error during upload:", e)

