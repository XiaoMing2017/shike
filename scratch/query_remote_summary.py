import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect('117.72.61.18', username='root', password='Nizouba12138.', timeout=15)

    cmd = """docker exec -i shike-mysql mysql -uroot -pNizouba12138. db_shike --default-character-set=utf8mb4 -e "
        SELECT 'TOTAL_USERS' AS metric, COUNT(*) AS val FROM tb_user
        UNION ALL
        SELECT 'TODAY_DAU', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE DATE(created_at) = CURDATE()
            UNION
            SELECT user_id FROM tb_team_checkin WHERE DATE(created_at) = CURDATE()
        ) t
        UNION ALL
        SELECT 'ACTIVE_7D', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        ) t
        UNION ALL
        SELECT 'ACTIVE_30D', COUNT(DISTINCT user_id) FROM (
            SELECT user_id FROM tb_diet_record WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            UNION
            SELECT user_id FROM tb_team_checkin WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        ) t
        UNION ALL
        SELECT 'TOTAL_DIET_RECORDS', COUNT(*) FROM tb_diet_record
        UNION ALL
        SELECT 'TOTAL_TEAMS', COUNT(*) FROM tb_team
        UNION ALL
        SELECT 'TOTAL_CHECKINS', COUNT(*) FROM tb_team_checkin;
    " """

    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    print(out)

finally:
    ssh.close()
