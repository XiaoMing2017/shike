import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    command = 'docker exec shike-mysql mysql -uroot -pNizouba12138. --default-character-set=utf8mb4 -e "SELECT id, user_id, record_date, meal_type, food_items, total_calories, created_at FROM db_shike.tb_diet_record ORDER BY id DESC LIMIT 5;"'
    stdin, stdout, stderr = ssh.exec_command(command)
    print("=== MySQL Query Results ===")
    print(stdout.read().decode('utf-8', errors='replace'))
    print("=== ERRORS (if any) ===")
    print(stderr.read().decode('utf-8', errors='replace'))
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
