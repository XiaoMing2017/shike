import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

# 模拟队友（例如：阿强 或 小美）催当前用户 (User 2) 打卡
sender_id = 15  # 健身狂人·阿强
target_id = 2   # 当前测试用户
team_id = 10

url = f"http://127.0.0.1:8081/api/v1/team/nudge?senderId={sender_id}&targetUserId={target_id}&teamId={team_id}"
req = urllib.request.Request(url, data=b'', method='POST')

try:
    with urllib.request.urlopen(req) as res:
        data = json.loads(res.read().decode('utf-8'))
        print(f"成功模拟队友催打卡: {data}")
        print("现在进入微信小程序「对赌小队」页面，将立即弹出【🔔 小队打卡提醒】模态窗口！")
except Exception as e:
    print(f"请求失败: {e}")
