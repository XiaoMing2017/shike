import requests
import time
import json

url = "https://api.xiaomimimo.com/v1/chat/completions"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"

headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json"
}

payload = {
    "messages": [
        {"role": "user", "content": "Hello, respond with 'OK'."}
    ]
}

def test_model(model_name):
    p = payload.copy()
    p["model"] = model_name
    print(f"Testing model {model_name}...")
    start_time = time.time()
    try:
        response = requests.post(url, headers=headers, json=p, timeout=20)
        elapsed = time.time() - start_time
        print(f"[{model_name}] Status Code: {response.status_code}")
        print(f"[{model_name}] Time taken: {elapsed:.2f} seconds")
        if response.status_code == 200:
            print(f"[{model_name}] Response: {response.json()['choices'][0]['message']['content']}")
        else:
            print(f"[{model_name}] Error response: {response.text}")
    except Exception as e:
        print(f"[{model_name}] Error: {e}")

if __name__ == "__main__":
    test_model("gpt-4o-mini")
    test_model("mimo-v2.5")
