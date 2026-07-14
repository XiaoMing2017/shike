import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    remote_py_code = """
import urllib.request
import urllib.error
import json

client_id = 'sQunOkSArZbRwVCks3pXfwGK'
client_secret = 'xcRFoyqDTCwwKVKEZDCJc0iav67YhVw0'
url = f'https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id={client_id}&client_secret={client_secret}'
req = urllib.request.Request(url, method='POST')
try:
    with urllib.request.urlopen(req, timeout=10) as response:
        print("Status Code:", response.getcode())
        body = response.read().decode('utf-8')
        data = json.loads(body)
        if 'access_token' in data:
            print("Access Token fetched successfully!")
        else:
            print("Response Body:", body)
except urllib.error.HTTPError as e:
    print("HTTPError Status Code:", e.code)
    print("HTTPError Body:", e.read().decode('utf-8'))
except Exception as e:
    print("Request failed:", e)
"""
    sftp = ssh.open_sftp()
    with sftp.file('/tmp/test_baidu_new.py', 'w') as f:
        f.write(remote_py_code)
    sftp.close()
    
    print("=== Running New Baidu Connection Test on Remote ===")
    stdin, stdout, stderr = ssh.exec_command("python3 /tmp/test_baidu_new.py")
    print(stdout.read().decode('utf-8'))
    print(stderr.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
