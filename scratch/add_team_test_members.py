import json
import sys
from datetime import datetime, date, timedelta
import pymysql

sys.stdout.reconfigure(encoding='utf-8')

def main():
    conn = pymysql.connect(
        host='localhost',
        port=3306,
        user='root',
        password='123456',
        database='db_shike',
        charset='utf8mb4',
        autocommit=True
    )
    cur = conn.cursor(pymysql.cursors.DictCursor)

    team_id = 10
    my_user_id = 2
    today = date.today()
    yesterday = today - timedelta(days=1)

    print(f"Adding test members to Team {team_id} (My User ID: {my_user_id})...")

    # 1. 确保当前用户 (User 2) 拥有充足积分和测试道具
    cur.execute("UPDATE tb_user SET points = 1000 WHERE id = %s", (my_user_id,))
    
    # 为当前用户补全昨日打卡成功
    cur.execute("""
        INSERT INTO tb_team_checkin (team_id, user_id, checkin_date, is_success, created_at)
        VALUES (%s, %s, %s, 1, NOW())
        ON DUPLICATE KEY UPDATE is_success = 1
    """, (team_id, my_user_id, yesterday))

    # 给当前用户发放战术道具
    items = [
        ("CHEAT_SHIELD", 2),
        ("SERUM_REVIVAL", 2),
        ("SNIPER_AUDIT", 2),
        ("MIRROR_DEFLECT", 2),
    ]
    for it_type, qty in items:
        cur.execute("""
            INSERT INTO tb_user_item (user_id, item_type, quantity, created_at, updated_at)
            VALUES (%s, %s, %s, NOW(), NOW())
            ON DUPLICATE KEY UPDATE quantity = %s, updated_at = NOW()
        """, (my_user_id, it_type, qty, qty))

    # 给当前用户插入一份待开启的每日分红盲盒
    cur.execute("DELETE FROM tb_team_loot_record WHERE user_id = %s AND team_id = %s AND settlement_date = %s",
                (my_user_id, team_id, yesterday))
    cur.execute("""
        INSERT INTO tb_team_loot_record (user_id, team_id, settlement_date, base_reward, multiplier, final_reward, item_reward, status, created_at, updated_at)
        VALUES (%s, %s, %s, 30, 1.0, 30, 'NONE', 'UNCLAIMED', NOW(), NOW())
    """, (my_user_id, team_id, yesterday))

    # 2. 准备 4 位各具特色的测试队员
    test_users = [
        {
            "openid": "mock_user_qiang",
            "nickname": "健身狂人·阿强",
            "avatar_url": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100",
            "gender": 1,
            "goal": "GAIN_MUSCLE",
            "target_calories": 2200.0,
            "points": 1200,
            "yesterday_success": 1,
            "today_diet": [
                {
                    "meal_type": "LUNCH",
                    "total_calories": 650.0,
                    "food_items": json.dumps([{"name": "香煎鸡胸肉", "weight": 200}, {"name": "水煮西兰花", "weight": 150}, {"name": "糙米饭", "weight": 150}], ensure_ascii=False)
                },
                {
                    "meal_type": "BREAKFAST",
                    "total_calories": 400.0,
                    "food_items": json.dumps([{"name": "燕麦片", "weight": 60}, {"name": "水煮蛋", "weight": 100}], ensure_ascii=False)
                }
            ],
            "is_spy": False
        },
        {
            "openid": "mock_user_mei",
            "nickname": "奶茶刺客·小美",
            "avatar_url": "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
            "gender": 2,
            "goal": "LOSE_WEIGHT",
            "target_calories": 1600.0,
            "points": 850,
            "yesterday_success": 1,
            "today_diet": [], # 今日未打卡 -> 可测试催TA打卡、查岗
            "is_spy": False
        },
        {
            "openid": "mock_user_liu",
            "nickname": "夜市吃神·大刘",
            "avatar_url": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100",
            "gender": 1,
            "goal": "MAINTAIN",
            "target_calories": 2000.0,
            "points": 920,
            "yesterday_success": 0,
            "today_diet": [
                {
                    "meal_type": "LUNCH",
                    "total_calories": 2200.0,
                    "food_items": json.dumps([{"name": "特辣牛油九宫格火锅", "weight": 600}, {"name": "肥牛卷", "weight": 300}], ensure_ascii=False)
                },
                {
                    "meal_type": "DINNER",
                    "total_calories": 1100.0,
                    "food_items": json.dumps([{"name": "烤羊肉串", "weight": 200}, {"name": "冰镇扎啤", "weight": 500}], ensure_ascii=False)
                }
            ], # 今日摄入 3300 kcal > 2000*1.25 -> 超标！可测试 AI 毒舌战报与催打卡
            "is_spy": True # 设为卧底！
        },
        {
            "openid": "mock_user_yuanyuan",
            "nickname": "自律达人·圆圆",
            "avatar_url": "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=100",
            "gender": 2,
            "goal": "LOSE_WEIGHT",
            "target_calories": 1500.0,
            "points": 1350,
            "yesterday_success": 1,
            "today_diet": [
                {
                    "meal_type": "BREAKFAST",
                    "total_calories": 350.0,
                    "food_items": json.dumps([{"name": "全麦欧包", "weight": 80}, {"name": "美式黑咖啡", "weight": 250}], ensure_ascii=False)
                },
                {
                    "meal_type": "LUNCH",
                    "total_calories": 450.0,
                    "food_items": json.dumps([{"name": "清蒸龙利鱼", "weight": 180}, {"name": "蔬菜沙拉", "weight": 200}], ensure_ascii=False)
                }
            ],
            "is_spy": False
        }
    ]

    spy_id = None
    first_member_id = None

    for m in test_users:
        # 插入或更新用户
        cur.execute("SELECT id FROM tb_user WHERE openid = %s", (m["openid"],))
        row = cur.fetchone()
        if row:
            uid = row["id"]
            cur.execute("""
                UPDATE tb_user 
                SET nickname = %s, avatar_url = %s, gender = %s, goal = %s, target_calories = %s, points = %s, updated_at = NOW()
                WHERE id = %s
            """, (m["nickname"], m["avatar_url"], m["gender"], m["goal"], m["target_calories"], m["points"], uid))
        else:
            cur.execute("""
                INSERT INTO tb_user (openid, nickname, avatar_url, gender, goal, target_calories, points, created_at, updated_at, status, training_level, vip_type)
                VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), NOW(), 'ENABLED', 'BEGINNER', 'NORMAL')
            """, (m["openid"], m["nickname"], m["avatar_url"], m["gender"], m["goal"], m["target_calories"], m["points"]))
            uid = cur.lastrowid

        if first_member_id is None:
            first_member_id = uid

        if m["is_spy"]:
            spy_id = uid

        # 加入队伍
        cur.execute("""
            INSERT INTO tb_team_member (team_id, user_id, joined_at)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE joined_at = %s
        """, (team_id, uid, yesterday, yesterday))

        # 积分明细记录
        cur.execute("""
            INSERT INTO tb_points_record (user_id, amount, type, remark, created_at)
            VALUES (%s, -100, 'TEAM_DEPOSIT', '加入契约小队 [11] 冻结保证金', %s)
        """, (uid, yesterday))

        # 昨日打卡
        cur.execute("""
            INSERT INTO tb_team_checkin (team_id, user_id, checkin_date, is_success, created_at)
            VALUES (%s, %s, %s, %s, NOW())
            ON DUPLICATE KEY UPDATE is_success = %s
        """, (team_id, uid, yesterday, m["yesterday_success"], m["yesterday_success"]))

        # 今日饮食记录
        cur.execute("DELETE FROM tb_diet_record WHERE user_id = %s AND record_date = %s", (uid, today))
        for d in m["today_diet"]:
            cur.execute("""
                INSERT INTO tb_diet_record (user_id, record_date, meal_type, food_items, total_calories, total_protein, total_fat, total_carbs, oil_level, created_at)
                VALUES (%s, %s, %s, %s, %s, 30.0, 15.0, 45.0, 'LIGHT', NOW())
            """, (uid, today, d["meal_type"], d["food_items"], d["total_calories"]))

        print(f"  + Added/Updated member: {m['nickname']} (User ID: {uid})")

    # 更新小队设置（卧底指派为大刘，目标天数 7 天，保证金 100）
    cur.execute("""
        UPDATE tb_team 
        SET spy_user_id = %s, target_days = 7, deposit_points = 100, status = 'ACTIVE'
        WHERE id = %s
    """, (spy_id, team_id))

    # 更新昨日每日瓜分流水，让流水更真实丰富
    cur.execute("""
        UPDATE tb_team_daily_settlement
        SET total_members = 5, success_members = 4, failed_members = 1, penalty_pool = 7, per_person_reward = 2,
            summary_text = '全队 5 人中 4 人自律达标，1 人违约。违约积分池已均分！'
        WHERE team_id = %s AND settlement_date = %s
    """, (team_id, yesterday))

    # 插入一个模拟被队友突击查岗的任务（发起人：阿强，目标：当前用户）
    cur.execute("DELETE FROM tb_team_audit_task WHERE target_id = %s AND status = 'PENDING'", (my_user_id,))
    cur.execute("""
        INSERT INTO tb_team_audit_task (team_id, sender_id, target_id, status, expire_at, reward_points, created_at)
        VALUES (%s, %s, %s, 'PENDING', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 20, NOW())
    """, (team_id, first_member_id, my_user_id))

    print("Successfully populated test members and gamification state!")

    # 验证查询
    cur.execute("SELECT count(*) as cnt FROM tb_team_member WHERE team_id = %s", (team_id,))
    print(f"Total members in Team {team_id}: {cur.fetchone()['cnt']}")

    cur.close()
    conn.close()

if __name__ == '__main__':
    main()
