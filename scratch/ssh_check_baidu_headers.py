import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    remote_py_code = """
import urllib.request
import json
import base64

client_id = 'sQunOkSArZbRwVCks3pXfwGK'
client_secret = 'xcRFoyqDTCwwKVKEZDCJc0iav67YhVw0'

# Get Token
url = f'https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id={client_id}&client_secret={client_secret}'
req = urllib.request.Request(url, method='POST')
with urllib.request.urlopen(req, timeout=10) as response:
    token = json.loads(response.read().decode('utf-8'))['access_token']

# Read image
image_path = '/root/shike/uploads/261321f3-afa8-4640-8885-26f2e1a693b8.jpeg'
with open(image_path, 'rb') as f:
    img_data = base64.b64encode(f.read())
    
# Call Dish API
dish_url = f'https://aip.baidubce.com/rest/2.0/image-classify/v2/dish?access_token={token}'
req_body = b'image=' + urllib.parse.quote_plus(img_data).encode('utf-8')
req_dish = urllib.request.Request(dish_url, data=req_body, headers={'Content-Type': 'application/x-www-form-urlencoded'}, method='POST')

with urllib.request.urlopen(req_dish, timeout=15) as response:
    print("Baidu Response Headers:")
    for k, v in response.getheaders():
        print(f"  {k}: {v}")
"""
    sftp = ssh.open_sftp()
    with sftp.file('/tmp/test_baidu_headers.py', 'w') as f:
        f.write(remote_py_code)
    sftp.close()
    
    print("=== Running Baidu Headers Test on Remote ===")
    stdin, stdout, stderr = ssh.exec_command("python3 /tmp/test_baidu_headers.py")
    print(stdout.read().decode('utf-8'))
    print(stderr.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
