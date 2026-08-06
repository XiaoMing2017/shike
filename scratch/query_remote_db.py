import paramiko, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print("Connecting to server 117.72.61.18...")
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.', timeout=15)
    print("Connected SSH successfully!")

    cmd = """docker exec -i shike-mysql mysql -uroot -pNizouba12138. db_shike -e "
        SELECT '=== USER COUNT ===' AS section;
        SELECT COUNT(*) AS total_registered_users FROM tb_user;

        SELECT '=== USER LIST ===' AS section;
        SELECT id, openid, nickname, avatar_url, created_at, updated_at FROM tb_user;

        SELECT '=== TODAY DAU ===' AS section;
        SELECT COUNT(DISTINCT user_id) AS dau_today FROM (
            SELECT user_id FROM tb_diet_record WHERE DATE(created_at) = CURDATE()
            UNION
            SELECT user_id FROM tb_team_checkin WHERE DATE(created_at) = CURDATE()
        ) t;

        SELECT '=== ACTIVE USERS 7 DAYS ===' AS section;
        SELECT COUNT(DISTINCT user_id) AS active_7d FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        ) t;

        SELECT '=== ACTIVE USERS 30 DAYS ===' AS section;
        SELECT COUNT(DISTINCT user_id) AS active_30d FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        ) t;

        SELECT '=== DIET RECORDS STATS ===' AS section;
        SELECT COUNT(*) AS total_diet_records FROM tb_diet_record;
        SELECT id, user_id, meal_type, record_date, created_at FROM tb_diet_record ORDER BY id DESC LIMIT 10;

        SELECT '=== TEAMS STATS ===' AS section;
        SELECT COUNT(*) AS total_teams FROM tb_team;
        SELECT id, team_name, creator_id, invite_code, status, created_at FROM tb_team;

        SELECT '=== TEAM CHECKINS STATS ===' AS section;
        SELECT COUNT(*) AS total_checkins FROM tb_team_checkin;
        SELECT * FROM tb_team_checkin ORDER BY id DESC LIMIT 10;
    " """

    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    err = stderr.read().decode('utf-8', errors='replace')

    print(out)
    if err:
        print("=== STDERR / WARNINGS ===")
        print(err)

finally:
    ssh.close()
