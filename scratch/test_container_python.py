import paramiko

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(ip, username=user, password=pwd, timeout=10)
        
        # Install python3 and requests inside shike-app container
        print("Installing python3 and requests in shike-app container...")
        cmd_install = "docker exec shike-app apk add --no-cache python3 py3-requests"
        stdin, stdout, stderr = ssh.exec_command(cmd_install)
        print("[Install STDOUT]\n", stdout.read().decode('utf-8'))
        print("[Install STDERR]\n", stderr.read().decode('utf-8'))
        
        # Copy the test script from host /tmp to container /tmp
        print("Copying test script to container...")
        cmd_cp = "docker exec shike-app tee /tmp/test_mimo.py"
        # Let's read the local test script file first
        with open("d:\\heming\\shike\\scratch\\test_mimo_no_limit.py", "r", encoding="utf-8") as f:
            script_content = f.read()
            
        stdin, stdout, stderr = ssh.exec_command(cmd_cp)
        stdin.write(script_content)
        stdin.close()
        print("[Copy STDOUT]\n", stdout.read().decode('utf-8'))
        
        # Run python test inside container
        print("Running python test inside container...")
        cmd_run = "docker exec shike-app python3 /tmp/test_mimo.py"
        stdin, stdout, stderr = ssh.exec_command(cmd_run)
        print("[Run STDOUT]\n", stdout.read().decode('utf-8'))
        print("[Run STDERR]\n", stderr.read().decode('utf-8'))
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        ssh.close()

if __name__ == "__main__":
    main()
