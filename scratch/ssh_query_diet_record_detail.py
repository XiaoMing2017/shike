import paramiko
import json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect("117.72.61.18", username="root", password="Nizouba12138.", timeout=15)
    command = 'docker exec shike-mysql mysql -uroot -pNizouba12138. --default-character-set=utf8mb4 -e "SELECT id, food_items FROM db_shike.tb_diet_record ORDER BY id DESC LIMIT 5;"'
    stdin, stdout, stderr = ssh.exec_command(command)
    raw = stdout.read()
    
    decoded = raw.decode('utf-8', errors='replace')
    lines = decoded.splitlines()
    for line in lines[1:]:
        parts = line.split('\t')
        if len(parts) >= 2:
            idx = parts[0]
            food_items_str = parts[1]
            print(f"Record ID: {idx}")
            print("  Raw string repr:", repr(food_items_str))
            try:
                items = json.loads(food_items_str)
                for item in items:
                    name = item.get('name')
                    unicode_escape = name.encode('unicode_escape').decode('ascii') if name else ''
                    print(f"    Item name unicode: {unicode_escape}")
            except Exception as e:
                print("    Failed to parse JSON:", e)
except Exception as e:
    print(f"Error: {e}")
finally:
    ssh.close()
