import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    query = (
        "SELECT 'tb_user' as table_name, count(*) as count from db_shike.tb_user "
        "UNION ALL SELECT 'tb_diet_record', count(*) from db_shike.tb_diet_record "
        "UNION ALL SELECT 'tb_team', count(*) from db_shike.tb_team "
        "UNION ALL SELECT 'tb_team_member', count(*) from db_shike.tb_team_member "
        "UNION ALL SELECT 'tb_team_checkin', count(*) from db_shike.tb_team_checkin;"
    )
    command = f'docker exec shike-mysql mysql -uroot -pNizouba12138. -e "{query}"'
    stdin, stdout, stderr = ssh.exec_command(command)
    print("=== Database Table Stats ===")
    print(stdout.read().decode('utf-8'))
    print("=== Errors ===")
    print(stderr.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
