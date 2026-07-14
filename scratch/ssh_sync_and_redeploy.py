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
    print(" 1. Uploading modified DietController.java ")
    print("="*40)
    
    # Upload new and modified backend files
    files_to_upload = [
        ("shike-backend/src/main/resources/db/schema.sql", "/root/shike/shike-backend/src/main/resources/db/schema.sql"),
        ("shike-backend/src/main/java/com/shike/controller/DietController.java", "/root/shike/shike-backend/src/main/java/com/shike/controller/DietController.java"),
        ("shike-backend/src/main/java/com/shike/service/impl/DietServiceImpl.java", "/root/shike/shike-backend/src/main/java/com/shike/service/impl/DietServiceImpl.java"),
        ("shike-backend/src/main/java/com/shike/model/entity/WaterRecord.java", "/root/shike/shike-backend/src/main/java/com/shike/model/entity/WaterRecord.java"),
        ("shike-backend/src/main/java/com/shike/repository/WaterRecordRepository.java", "/root/shike/shike-backend/src/main/java/com/shike/repository/WaterRecordRepository.java"),
        ("shike-backend/src/main/java/com/shike/service/WaterService.java", "/root/shike/shike-backend/src/main/java/com/shike/service/WaterService.java"),
        ("shike-backend/src/main/java/com/shike/service/impl/WaterServiceImpl.java", "/root/shike/shike-backend/src/main/java/com/shike/service/impl/WaterServiceImpl.java"),
        ("shike-backend/src/main/java/com/shike/controller/WaterController.java", "/root/shike/shike-backend/src/main/java/com/shike/controller/WaterController.java")
    ]
    for local_rel, remote_path in files_to_upload:
        local_path = os.path.abspath(local_rel)
        print(f"Uploading {local_path} -> {remote_path}")
        sftp.put(local_path, remote_path)
    sftp.close()
    
    print("\n" + "="*40)
    print(" 2. Rebuilding in background ")
    print("="*40)
    
    # Run the build command redirecting both stdout and stderr to a file
    # We clear the log file first
    ssh.exec_command("> /tmp/compose_build.log")
    command = "cd /root/shike && docker compose up -d --build shike-app > /tmp/compose_build.log 2>&1"
    print(f"Running remote command: {command}")
    
    # Start it asynchronously
    transport = ssh.get_transport()
    channel = transport.open_session()
    channel.exec_command(command)
    
    # Poll the log file
    print("Polling build log...")
    last_pos = 0
    while not channel.exit_status_ready():
        time.sleep(2)
        # Read the file from remote
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
            
    # Print the remaining log
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
