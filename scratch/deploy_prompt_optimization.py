import os
import paramiko
import time

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect(ip, username=user, password=pwd, timeout=15)
    sftp = ssh.open_sftp()
    
    print("="*40)
    print(" 1. Uploading optimized DietServiceImpl.java ")
    print("="*40)
    
    # Upload only the modified file
    files_to_upload = [
        ("shike-backend/src/main/java/com/shike/service/impl/DietServiceImpl.java", "/root/shike/shike-backend/src/main/java/com/shike/service/impl/DietServiceImpl.java"),
    ]
    for local_rel, remote_path in files_to_upload:
        local_path = os.path.abspath(local_rel)
        print(f"Uploading {local_path} -> {remote_path}")
        sftp.put(local_path, remote_path)
    sftp.close()
    
    print("\n" + "="*40)
    print(" 2. Rebuilding shike-app container ")
    print("="*40)
    
    ssh.exec_command("> /tmp/compose_build.log")
    command = "cd /root/shike && docker compose up -d --build shike-app > /tmp/compose_build.log 2>&1"
    print(f"Running remote command: {command}")
    
    transport = ssh.get_transport()
    channel = transport.open_session()
    channel.exec_command(command)
    
    print("Polling build log...")
    last_pos = 0
    while not channel.exit_status_ready():
        time.sleep(2)
        read_ssh = paramiko.SSHClient()
        read_ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            read_ssh.connect(ip, username=user, password=pwd, timeout=5)
            stdin, stdout, stderr = read_ssh.exec_command("cat /tmp/compose_build.log")
            log_content = stdout.read().decode('utf-8', errors='replace')
            if len(log_content) > last_pos:
                print(log_content[last_pos:], end="")
                last_pos = len(log_content)
        except Exception:
            pass
        finally:
            read_ssh.close()
            
    # Print remaining log
    stdin, stdout, stderr = ssh.exec_command("cat /tmp/compose_build.log")
    log_content = stdout.read().decode('utf-8', errors='replace')
    if len(log_content) > last_pos:
        print(log_content[last_pos:], end="")
        
    print("\n" + "="*40)
    print(" 3. Verifying Container Status ")
    print("="*40)
    stdin, stdout, stderr = ssh.exec_command("docker ps -a | grep shike-app")
    print(stdout.read().decode('utf-8', errors='replace'))
    
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
