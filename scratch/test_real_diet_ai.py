import requests
import time
import base64

# Download a tiny food image (200x200) from Unsplash
print("Downloading food image...")
img_url = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200"
img_data = requests.get(img_url).content
base64_image = base64.b64encode(img_data).decode('utf-8')

url = "https://api.xiaomimimo.com/v1/chat/completions"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"

headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

prompt = """你是一个专业的中国膳食营养分析师，擅长通过视觉特征精确识别食物。请分析图片并估算营养数据。
输出格式：你必须只返回一个 JSON 数组，不要包含任何 markdown 代码块标记，不要包含任何其他文字。
JSON 数组的每个对象包含以下字段：
- name: 食物名称
- weight: 估算重量克数
- calories: 估算热量 kcal
- protein: 蛋白质克数
- fat: 脂肪克数
- carbs: 碳水化合物克数"""

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
                            "url": f"data:image/jpeg;base64,{base64_image}"
                        }
                    }
                ]
            }
        ],
        "max_tokens": 500
    }
    
    print(f"\nTesting {model_name} with food image...")
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=45)
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
    models = ["mimo-v2.5", "mimo-v2-omni", "mimo-v2-pro"]
    for m in models:
        test_model(m)
