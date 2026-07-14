import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    
    print("=== Checkins ===")
    command = 'docker exec shike-mysql mysql -uroot -pNizouba12138. -e "SELECT id, team_id, user_id, checkin_date, is_success, created_at FROM db_shike.tb_team_checkin;"'
    stdin, stdout, stderr = ssh.exec_command(command)
    print(stdout.read().decode('utf-8'))

except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
