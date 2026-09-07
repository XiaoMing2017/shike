import pymysql
from datetime import date, timedelta

conn = pymysql.connect(host='localhost', port=3306, user='root', password='123456', database='db_shike', charset='utf8mb4', autocommit=True)
with conn.cursor() as cur:
    yesterday = date.today() - timedelta(days=1)
    # 为当前测试用户 (id=2) 重置或插入一份未开启的每日盲盒
    cur.execute("""
        INSERT INTO tb_team_loot_record 
        (user_id, team_id, settlement_date, base_reward, multiplier, final_reward, item_reward, status, created_at, updated_at)
        VALUES (2, 10, %s, 30, 1.0, 30, 'NONE', 'UNCLAIMED', NOW(), NOW())
        ON DUPLICATE KEY UPDATE status = 'UNCLAIMED', claimed_at = NULL, multiplier = 1.0, final_reward = base_reward, item_reward = 'NONE', updated_at = NOW()
    """, (yesterday,))
    print(f"Successfully reset UNCLAIMED loot box for user 2 on date {yesterday}!")
conn.close()
