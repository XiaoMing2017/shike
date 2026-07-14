import requests
import time
import base64
import os

url = "https://api.xiaomimimo.com/v1/chat/completions"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"
image_path = r"D:\heming\shike\shike-frontend\images\home.png"

if not os.path.exists(image_path):
    # Try finding any image
    img_dir = r"D:\heming\shike\shike-frontend\images"
    for file in os.listdir(img_dir):
        if file.endswith((".png", ".jpg", ".jpeg")):
            image_path = os.path.join(img_dir, file)
            break

print("Using test image:", image_path)

with open(image_path, "rb") as image_file:
    base64_image = base64.b64encode(image_file.read()).decode('utf-8')

headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

prompt = "判断这张图片是什么，只用中文回复一句话。"

def test_model(model_name):
    payload = {
        "model": model_name,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/png;base64,{base64_image}"
                        }
                    }
                ]
            }
        ],
        "max_tokens": 100
    }
    
    print(f"\nTesting vision on {model_name}...")
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        elapsed = time.time() - start_time
        print(f"[{model_name}] Status Code: {response.status_code}")
        print(f"[{model_name}] Time taken: {elapsed:.2f} seconds")
        if response.status_code == 200:
            res_json = response.json()
            answer = res_json['choices'][0]['message']['content']
            print(f"[{model_name}] Response: {answer}")
        else:
            print(f"[{model_name}] Error response: {response.text}")
    except Exception as e:
        print(f"[{model_name}] Error: {e}")

if __name__ == "__main__":
    models = ["mimo-v2.5", "mimo-v2-flash", "mimo-v2-omni", "mimo-v2-pro"]
    for m in models:
        test_model(m)
