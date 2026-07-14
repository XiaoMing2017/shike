import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    command = "docker logs shike-app 2>&1 | grep -iE 'DietServiceImpl|Baidu|OPENAI|mimo' | tail -n 50"
    stdin, stdout, stderr = ssh.exec_command(command)
    print("=== DietServiceImpl Logs ===")
    print(stdout.read().decode('utf-8', errors='replace'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
