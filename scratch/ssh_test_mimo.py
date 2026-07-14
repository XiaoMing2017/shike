import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    # Python script to run on remote host
    remote_py_code = """
import urllib.request
import json

url = 'https://api.xiaomimimo.com/v1/chat/completions'
headers = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if'
}
data = {
    'model': 'mimo-v2-pro',
    'messages': [{'role': 'user', 'content': 'Hello, are you working?'}],
    'temperature': 0.2
}
req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers=headers, method='POST')
try:
    with urllib.request.urlopen(req, timeout=10) as response:
        print("Status Code:", response.getcode())
        print("Response Body:", response.read().decode('utf-8'))
except Exception as e:
    print("Request failed:", e)
"""
    # Write remote script and run it
    sftp = ssh.open_sftp()
    with sftp.file('/tmp/test_mimo_pro.py', 'w') as f:
        f.write(remote_py_code)
    sftp.close()
    
    print("=== Running Mimo Pro Connection Test on Remote ===")
    stdin, stdout, stderr = ssh.exec_command("python3 /tmp/test_mimo_pro.py")
    print(stdout.read().decode('utf-8'))
    print(stderr.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
