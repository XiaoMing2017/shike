import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.')
    
    # Query teams
    cmd1 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SET NAMES utf8mb4; SELECT id, team_name, target_days, deposit_points, status, created_at FROM tb_team;"'
    stdin, stdout, stderr = ssh.exec_command(cmd1)
    print("--- Teams ---")
    print(stdout.read().decode('utf-8', errors='replace'))
    
    # Query members
    cmd2 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT team_id, user_id, nickname FROM tb_team_member tm JOIN tb_user u ON tm.user_id = u.id;"'
    stdin, stdout, stderr = ssh.exec_command(cmd2)
    print("--- Members ---")
    print(stdout.read().decode('utf-8', errors='replace'))
    
    # Query checkins
    cmd3 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT team_id, user_id, checkin_date, is_success FROM tb_team_checkin ORDER BY checkin_date DESC;"'
    stdin, stdout, stderr = ssh.exec_command(cmd3)
    print("--- Checkins ---")
    print(stdout.read().decode('utf-8', errors='replace'))

except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
