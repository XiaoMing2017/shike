import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.')
    
    # Query users
    print("=== Users ===")
    cmd1 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT id, nickname, points, target_calories FROM tb_user;"'
    stdin, stdout, stderr = ssh.exec_command(cmd1)
    print(stdout.read().decode('utf-8', errors='replace'))
    
    # Query teams
    print("=== Teams ===")
    cmd2 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT id, team_name, target_days, deposit_points, status, created_at FROM tb_team;"'
    stdin, stdout, stderr = ssh.exec_command(cmd2)
    print(stdout.read().decode('utf-8', errors='replace'))
    
    # Query diet records summed by date and user
    print("=== Daily Diet Sum ===")
    cmd3 = 'docker exec -i shike-mysql mysql -uroot -pNizouba12138. -e "USE db_shike; SELECT user_id, record_date, SUM(total_calories) as sum_cal, COUNT(*) as meal_count FROM tb_diet_record GROUP BY user_id, record_date ORDER BY record_date, user_id;"'
    stdin, stdout, stderr = ssh.exec_command(cmd3)
    print(stdout.read().decode('utf-8', errors='replace'))

except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
