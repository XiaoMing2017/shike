import paramiko
import sys
import time

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

def run_cmd(cmd):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    max_retries = 3
    for attempt in range(max_retries):
        try:
            ssh.connect(ip, username=user, password=pwd, timeout=10)
            print(f"Running command: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            out = stdout.read().decode('utf-8', errors='replace')
            err = stderr.read().decode('utf-8', errors='replace')
            if out:
                print("[STDOUT]")
                print(out)
            if err:
                print("[STDERR]")
                print(err)
            return
        except Exception as e:
            print(f"Attempt {attempt + 1} failed: {e}")
            if attempt < max_retries - 1:
                time.sleep(2)
            else:
                print("All connection attempts failed.")
        finally:
            ssh.close()

if __name__ == "__main__":
    cmd = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "docker ps -a"
    run_cmd(cmd)
