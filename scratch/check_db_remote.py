import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.')
    cmd = 'curl -s -X POST https://open.bigmodel.cn/api/paas/v4/chat/completions -H "Content-Type: application/json" -H "Authorization: Bearer 3ba6d2913b34455face1735113e4fcdb.oW6qiRCbdAU0hSfH" -d \'{"model": "glm-4v-flash", "messages": [{"role": "user", "content": [{"type": "text", "text": "hello"}]}]}\''
    stdin, stdout, stderr = ssh.exec_command(cmd)
    
    out = stdout.read()
    err = stderr.read()
    
    # Try to decode with utf-8 first, fallback to gbk or latin1 to see if we can read the raw bytes
    print("[STDOUT - decoded as UTF-8]")
    print(out.decode('utf-8', errors='replace'))
    
    print("[STDOUT - repr]")
    print(repr(out))
    
    if err:
        print("[STDERR]")
        print(err.decode('utf-8', errors='replace'))
        
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
