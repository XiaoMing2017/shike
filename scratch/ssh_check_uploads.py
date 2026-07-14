import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    command = 'ls -l /root/shike/uploads/'
    stdin, stdout, stderr = ssh.exec_command(command)
    print("=== Uploaded files ===")
    print(stdout.read().decode('utf-8', errors='replace'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
