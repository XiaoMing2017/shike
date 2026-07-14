import os
import paramiko

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect(ip, username=user, password=pwd, timeout=15)
    sftp = ssh.open_sftp()
    
    print("="*40)
    print(" 1. Uploading modified TeamSettleScheduler.java ")
    print("="*40)
    
    local_file = r"d:\heming\shike\shike-backend\src\main\java\com\shike\scheduler\TeamSettleScheduler.java"
    remote_file = "/root/shike/shike-backend/src/main/java/com/shike/scheduler/TeamSettleScheduler.java"
    print(f"Uploading {local_file} -> {remote_file}")
    sftp.put(local_file, remote_file)

    local_file2 = r"d:\heming\shike\shike-backend\src\main\java\com\shike\service\impl\TeamServiceImpl.java"
    remote_file2 = "/root/shike/shike-backend/src/main/java/com/shike/service/impl/TeamServiceImpl.java"
    print(f"Uploading {local_file2} -> {remote_file2}")
    sftp.put(local_file2, remote_file2)
    
    sftp.close()
    
    print("\n" + "="*40)
    print(" 2. Rebuilding and restarting shike-app container ")
    print("="*40)
    
    command = "cd /root/shike && docker compose up -d --build shike-app"
    print(f"Running command: {command}")
    stdin, stdout, stderr = ssh.exec_command(command)
    
    for line in stdout:
        print(line.strip())
    for line in stderr:
        print(line.strip())
        
    print("\n" + "="*40)
    print(" 3. Verifying Container Status ")
    print("="*40)
    stdin, stdout, stderr = ssh.exec_command("docker ps -a | grep shike-app")
    print(stdout.read().decode('utf-8', errors='replace'))
    
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
