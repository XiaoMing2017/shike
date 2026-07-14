import paramiko
import os

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

local_path = r"D:\heming\shike\docker-compose.yml"
remote_path = "/root/shike/docker-compose.yml"

def sync_and_restart():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(ip, username=user, password=pwd, timeout=10)
        sftp = ssh.open_sftp()
        
        print(f"Uploading: {local_path} -> {remote_path}")
        sftp.put(local_path, remote_path)
        sftp.close()
        print("Upload completed successfully.")
        
        print("Restarting docker containers to apply environment changes...")
        cmd = "cd /root/shike && docker compose down && docker compose up -d"
        stdin, stdout, stderr = ssh.exec_command(cmd)
        
        out = stdout.read().decode('utf-8', errors='replace')
        err = stderr.read().decode('utf-8', errors='replace')
        if out:
            print("[STDOUT]")
            print(out)
        if err:
            print("[STDERR]")
            print(err)
            
        print("Restart finished.")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        ssh.close()

if __name__ == "__main__":
    sync_and_restart()
