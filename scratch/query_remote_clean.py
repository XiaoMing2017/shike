import paramiko, sys

sys.stdout.reconfigure(encoding='utf-8')

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.', timeout=15)

    cmd = """docker exec -i shike-mysql mysql -uroot -pNizouba12138. db_shike --default-character-set=utf8mb4 -e "
        SELECT '=== REAL USER METRICS (EXCLUDING TEST USER 2 & 1) ===' AS section;

        SELECT 'TOTAL_REAL_USERS' AS metric, COUNT(*) AS val FROM tb_user WHERE id NOT IN (1, 2)
        UNION ALL
        SELECT 'TODAY_REAL_DAU', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE DATE(created_at) = CURDATE() AND user_id NOT IN (1, 2)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE DATE(created_at) = CURDATE() AND user_id NOT IN (1, 2)
        ) t
        UNION ALL
        SELECT 'REAL_ACTIVE_7D', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND user_id NOT IN (1, 2)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND user_id NOT IN (1, 2)
        ) t
        UNION ALL
        SELECT 'REAL_ACTIVE_30D', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND user_id NOT IN (1, 2)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND user_id NOT IN (1, 2)
        ) t
        UNION ALL
        SELECT 'REAL_DIET_RECORDS', COUNT(*) FROM tb_diet_record WHERE user_id NOT IN (1, 2)
        UNION ALL
        SELECT 'REAL_TEAMS', COUNT(*) FROM tb_team WHERE creator_id NOT IN (1, 2)
        UNION ALL
        SELECT 'REAL_TEAM_CHECKINS', COUNT(*) FROM tb_team_checkin WHERE user_id NOT IN (1, 2);
    " """

    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    print(out)

finally:
    ssh.close()
