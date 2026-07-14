import paramiko

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect(ip, username=user, password=pwd, timeout=10)
    print("="*60)
    print(" Fetching latest shike-app container logs ")
    print("="*60)
    
    stdin, stdout, stderr = ssh.exec_command("docker logs --since 5m shike-app")
    print(stdout.read().decode('utf-8', errors='replace'))
    
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
