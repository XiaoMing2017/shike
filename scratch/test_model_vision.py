import requests
import json
import base64

url = "https://api.xiaomimimo.com/v1/chat/completions"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"
image_path = r"D:\heming\shike\shike-frontend\images\home.png"

with open(image_path, "rb") as f:
    base64_image = base64.b64encode(f.read()).decode('utf-8')

def test_model(model_name):
    print(f"\nTesting model: {model_name}...")
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    payload = {
        "model": model_name,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "Is this a food image? Answer yes or no."},
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/png;base64,{base64_image}"
                        }
                    }
                ]
            }
        ]
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=15)
        print("Status Code:", response.status_code)
        print("Response:", response.text[:300])
        return response.status_code == 200
    except Exception as e:
        print("Error:", e)
        return False

# Test the candidate models
test_model("mimo-v2.5")
test_model("mimo-v2.5-pro")
test_model("mimo-v2-omni")
test_model("mimo-v2-pro")
