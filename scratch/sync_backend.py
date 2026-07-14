import paramiko
import os
import stat

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."
local_base = r"D:\heming\shike\shike-backend"
remote_base = "/root/shike/shike-backend"

def upload_dir(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
        print(f"Created remote dir: {remote_dir}")
    except IOError:
        pass  # already exists

    for item in os.listdir(local_dir):
        local_path = os.path.join(local_dir, item)
        remote_path = remote_dir + "/" + item
        
        # Skip target and other build directories
        if item in ["target", ".git", ".idea", ".settings", ".classpath", ".project"]:
            continue
            
        if os.path.isdir(local_path):
            upload_dir(sftp, local_path, remote_path)
        else:
            print(f"Uploading: {local_path} -> {remote_path}")
            sftp.put(local_path, remote_path)

def deploy():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(ip, username=user, password=pwd, timeout=10)
        sftp = ssh.open_sftp()
        
        # Upload shike-backend files
        print("Starting file upload to remote server...")
        upload_dir(sftp, local_base, remote_base)
        sftp.close()
        print("Upload completed successfully.")
        
        # Build and restart container on remote server
        print("Building and restarting docker container on remote server...")
        cmd = "cd /root/shike && docker compose up --build -d shike-app"
        stdin, stdout, stderr = ssh.exec_command(cmd)
        
        out = stdout.read().decode('utf-8', errors='replace')
        err = stderr.read().decode('utf-8', errors='replace')
        if out:
            print("[STDOUT]")
            print(out)
        if err:
            print("[STDERR]")
            print(err)
            
        print("Deployment finished.")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        ssh.close()

if __name__ == "__main__":
    deploy()
