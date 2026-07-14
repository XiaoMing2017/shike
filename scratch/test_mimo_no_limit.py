import requests
import json
import time

url = "https://api.xiaomimimo.com/v1/chat/completions"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"

headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

dish_name = "韭菜"
hint = "egg bun"

prompt = "你是一个专业的膳食营养估算模型。用户通过图片识别出吃了一餐：【" + dish_name + "】。"
if hint:
    prompt += "用户补充提示是：\"" + hint + "\"。"
prompt += "\n请进行以下处理：\n" + \
        "1. 判断【" + dish_name + "】是否是食物或饮料。如果它明显不是食物（例如属于电子产品、人物、风景、车辆、普通物件等），或者无法确定是食物，请必须返回空数组 []。\n" + \
        "2. 如果是食物，结合常识 and 用户提示估算这餐的分量。请只返回一个包含单个食物对象的 JSON 数组（不要包含任何 markdown 代码块标记，如 ```json，不要包含 any 额外文字），格式必须严格为：\n" + \
        "[\n" + \
        "  {\n" + \
        "    \"name\": \"" + dish_name + "\",\n" + \
        "    \"weight\": 估算克数,\n" + \
        "    \"calories\": 估算热量kcal,\n" + \
        "    \"protein\": 蛋白质克数,\n" + \
        "    \"fat\": 脂肪克数,\n" + \
        "    \"carbs\": 碳水化合物克数\n" + \
        "  }\n" + \
        "]"

payload = {
    "model": "mimo-v2-pro",
    "messages": [
        {"role": "user", "content": prompt}
    ],
    "temperature": 0.2
}

print("Testing mimo-v2-pro with no max_tokens limit...")
start = time.time()
try:
    response = requests.post(url, headers=headers, json=payload, timeout=90)
    elapsed = time.time() - start
    print(f"[mimo-v2-pro] Status Code: {response.status_code}")
    print(f"[mimo-v2-pro] Time taken: {elapsed:.2f} seconds")
    if response.status_code == 200:
        res_json = response.json()
        print(f"[mimo-v2-pro] Usage: {res_json.get('usage', {})}")
        print(f"[mimo-v2-pro] Content:\n{res_json['choices'][0]['message'].get('content')}")
    else:
        print(f"[mimo-v2-pro] Error: {response.text}")
except Exception as e:
    print(f"[mimo-v2-pro] Failed: {e}")
