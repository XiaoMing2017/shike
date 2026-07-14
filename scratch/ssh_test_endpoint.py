import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    remote_py_code = """
import urllib.request
import mimetypes
import uuid

# Helper to build multipart/form-data
def build_multipart(fields, files):
    boundary = uuid.uuid4().hex.encode('utf-8')
    body = bytearray()
    
    # Add fields
    for key, value in fields.items():
        body.extend(b'--' + boundary + b'\\r\\n')
        body.extend(f'Content-Disposition: form-data; name="{key}"\\r\\n\\r\\n'.encode('utf-8'))
        body.extend(value.encode('utf-8') + b'\\r\\n')
        
    # Add files
    for key, filepath in files.items():
        body.extend(b'--' + boundary + b'\\r\\n')
        filename = filepath.split('/')[-1]
        mimetype = mimetypes.guess_type(filepath)[0] or 'application/octet-stream'
        body.extend(f'Content-Disposition: form-data; name="{key}"; filename="{filename}"\\r\\n'.encode('utf-8'))
        body.extend(f'Content-Type: {mimetype}\\r\\n\\r\\n'.encode('utf-8'))
        with open(filepath, 'rb') as f:
            body.extend(f.read())
        body.extend(b'\\r\\n')
        
    body.extend(b'--' + boundary + b'--\\r\\n')
    content_type = f'multipart/form-data; boundary={boundary.decode("utf-8")}'
    return body, content_type

url = 'http://localhost:8081/api/v1/diet/recognize'
fields = {'hint': 'egg bun'}
files = {'file': '/root/shike/uploads/261321f3-afa8-4640-8885-26f2e1a693b8.jpeg'}

body, content_type = build_multipart(fields, files)
req = urllib.request.Request(url, data=body, headers={'Content-Type': content_type}, method='POST')

try:
    with urllib.request.urlopen(req, timeout=30) as response:
        print("Status Code:", response.getcode())
        print("Response Body:", response.read().decode('utf-8'))
except Exception as e:
    # Print detailed HTTP error if possible
    if hasattr(e, 'read'):
        print("HTTP Error Status:", e.code)
        print("HTTP Error Body:", e.read().decode('utf-8'))
    else:
        print("Request failed:", e)
"""
    sftp = ssh.open_sftp()
    with sftp.file('/tmp/test_recognize_endpoint.py', 'w') as f:
        f.write(remote_py_code)
    sftp.close()
    
    print("=== Running Backend Recognize Endpoint Test on Remote ===")
    stdin, stdout, stderr = ssh.exec_command("python3 /tmp/test_recognize_endpoint.py")
    print(stdout.read().decode('utf-8'))
    print(stderr.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
