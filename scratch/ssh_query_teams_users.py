import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    
    # Check users
    print("=== Users ===")
    command1 = 'docker exec shike-mysql mysql -uroot -pNizouba12138. -e "SELECT id, openid, nickname, points, created_at FROM db_shike.tb_user;"'
    stdin, stdout, stderr = ssh.exec_command(command1)
    print(stdout.read().decode('utf-8'))
    
    # Check teams
    print("=== Teams ===")
    command2 = 'docker exec shike-mysql mysql -uroot -pNizouba12138. -e "SELECT id, team_name, creator_id, invite_code, target_days, deposit_points, status FROM db_shike.tb_team;"'
    stdin, stdout, stderr = ssh.exec_command(command2)
    print(stdout.read().decode('utf-8'))

    # Check team members
    print("=== Team Members ===")
    command3 = 'docker exec shike-mysql mysql -uroot -pNizouba12138. -e "SELECT id, team_id, user_id FROM db_shike.tb_team_member;"'
    stdin, stdout, stderr = ssh.exec_command(command3)
    print(stdout.read().decode('utf-8'))

except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
