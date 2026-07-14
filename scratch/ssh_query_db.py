import paramiko

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect(ip, username=user, password=pwd, timeout=10)
    print("="*60)
    print(" Checking MySQL table schema charset ")
    print("="*60)
    
    cmd = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "show create table db_shike.tb_diet_record\\G"'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    print(stdout.read().decode('utf-8', errors='replace'))
    
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
