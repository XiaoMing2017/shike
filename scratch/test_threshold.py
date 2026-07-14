import urllib.request
import urllib.parse
import json
import paramiko
import time

def run_mysql_cmd(ssh, sql):
    cmd = f'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; {sql}"'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    err = stderr.read().decode('utf-8', errors='replace')
    return out, err

def trigger_settle():
    url = "http://117.72.61.18:8081/api/v1/test/settle/challenge?teamId=1"
    req = urllib.request.Request(url, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            return response.read().decode("utf-8")
    except Exception as e:
        return f"Failed: {e}"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.')
    
    print("=== SCENARIO 1: maxSuccessCount = 4 (< 5 required) ===")
    # Reset user points to 900
    run_mysql_cmd(ssh, "UPDATE tb_user SET points = 900 WHERE id IN (2, 3);")
    # Set User 2's successes to 4 (days 9, 10, 11, 12) and 3 failed days (13, 14, 15)
    run_mysql_cmd(ssh, "UPDATE tb_team_checkin SET is_success = 0 WHERE team_id = 1 AND user_id = 2 AND checkin_date IN ('2026-06-13', '2026-06-14', '2026-06-15');")
    
    # Trigger settlement
    res1 = trigger_settle()
    print("Settle Trigger 1:", res1)
    
    # Query points
    out1, _ = run_mysql_cmd(ssh, "SELECT id, nickname, points FROM tb_user WHERE id IN (2,3);")
    print("User points (Expected 900 for both):")
    print(out1)
    
    print("=== SCENARIO 2: maxSuccessCount = 6 (>= 5 required) ===")
    # Reset user points to 900
    run_mysql_cmd(ssh, "UPDATE tb_user SET points = 900 WHERE id IN (2, 3);")
    # Restore User 2's successes to 6 (days 9, 10, 11, 12, 14, 15)
    run_mysql_cmd(ssh, "UPDATE tb_team_checkin SET is_success = 1 WHERE team_id = 1 AND user_id = 2 AND checkin_date IN ('2026-06-14', '2026-06-15');")
    
    # Trigger settlement
    res2 = trigger_settle()
    print("Settle Trigger 2:", res2)
    
    # Query points
    out2, _ = run_mysql_cmd(ssh, "SELECT id, nickname, points FROM tb_user WHERE id IN (2,3);")
    print("User points (Expected 1100 for User 2, 900 for User 3):")
    print(out2)

except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
