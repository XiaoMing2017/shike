import requests

url = "https://api.xiaomimimo.com/v1/models"
api_key = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if"

headers = {
    "Authorization": f"Bearer {api_key}"
}

try:
    response = requests.get(url, headers=headers, timeout=10)
    print("Status Code:", response.status_code)
    if response.status_code == 200:
        models = response.json()
        print("Supported Models:")
        for m in models.get("data", []):
            print("-", m.get("id"))
    else:
        print("Error Response:", response.text)
except Exception as e:
    print("Error:", e)
