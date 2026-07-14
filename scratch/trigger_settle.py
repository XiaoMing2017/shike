import urllib.request
import urllib.parse
import json
import paramiko

# 1. Trigger final settlement
url = "http://117.72.61.18:8081/api/v1/test/settle/challenge?teamId=1"
req = urllib.request.Request(url, method="POST")
try:
    with urllib.request.urlopen(req) as response:
        res_body = response.read().decode("utf-8")
        print("Settle response:", res_body)
except Exception as e:
    print("Trigger settlement failed:", e)

# 2. Check points and team status in DB
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.')
    
    print("\n=== Users and Points after settlement ===")
    cmd1 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT id, nickname, points FROM tb_user;"'
    stdin, stdout, stderr = ssh.exec_command(cmd1)
    print(stdout.read().decode('utf-8', errors='replace'))
    
    print("=== Team Status ===")
    cmd2 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT id, team_name, status FROM tb_team;"'
    stdin, stdout, stderr = ssh.exec_command(cmd2)
    print(stdout.read().decode('utf-8', errors='replace'))

except Exception as e:
    print(f"Error querying DB: {e}")
finally:
    ssh.close()
