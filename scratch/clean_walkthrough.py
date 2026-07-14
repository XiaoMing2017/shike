import re

path = r"C:\Users\19836\.gemini\antigravity\brain\1d00d338-66d2-4f6a-b2a2-b06521b5073a\walkthrough.md"

with open(path, "r", encoding="utf-8", errors="replace") as f:
    content = f.read()

# Replace any occurrence of the old settlement section
old_section_pattern = r"## 🏆 对赌结算规则优化与积分分发修复 \(2026-06-16\).*?(?=## 💡|$)"
# Wait, let's just replace from "## 🏆 对赌结算规则优化与积分分发修复" to the end of the file!
pos = content.find("## 🏆 对赌结算规则优化与积分分发修复")
if pos != -1:
    content = content[:pos]

new_section = """## 🏆 对赌结算规则优化与积分分发修复 (2026-06-16)

为了解决用户反馈的“对赌结束后积分没有到赢的人身上”这一痛点问题，我们对后台结算逻辑进行了分析与优化。

### 1. 发现的瓶颈与逻辑偏差
- **原逻辑**：在终期对赌结算中，系统会严格要求成员的打卡成功次数必须达到目标天数（例如 7 天挑战必须打满 7 天，即 `successCount == 7`）才判定为成功（`successMembers`）。如果有人没有打满 7 天，便会被判定为失败（`failedMembers`）。
- **导致的问题**：在 2 人对赌中，若用户 `?sir` 坚持打卡 6 天（中间漏了 1 天），而队友只打卡了 2 天，根据原逻辑两者均未达到 7 天，系统判定为**全员失败**（`successMembers` 为空），所有积分被扣留没收。然而，对于用户而言，在此类 PvP 对赌中，打卡天数多的人（6 天）显然才是相对的**赢家**。

### 2. 优化方案：基于最大打卡次数与 60% 最低天数门槛判定赢家
我们在 [TeamSettleScheduler.java](file:///d:/heming/shike/shike-backend/src/main/java/com/shike/scheduler/TeamSettleScheduler.java) 中重构了 `performFinalSettlement` 逻辑：
1. **寻找最大打卡次数**：首先统计出小队中所有人实际打卡成功的最高天数 `maxSuccessCount`。
2. **计算 60% 最低达标门槛**：`minRequiredDays = Math.ceil(targetDays * 0.6)`（向上取整，例如 7 天对赌最少需打卡 5 天；14 天对赌最少需打卡 9 天；21 天对赌最少需打卡 13 天）。
3. **动态划分赢家与输家**：
   - 若最高天数 `maxSuccessCount >= minRequiredDays`，则所有实际打卡成功天数等于 `maxSuccessCount` 的成员直接列为 **赢家 (`successMembers`)**；打卡天数少于此数的成员为 **输家 (`failedMembers`)**。
   - 若最高天数 `maxSuccessCount < minRequiredDays`（即大家都不太打卡，哪怕打卡最多的人天数也没达到 60% 的门槛要求，例如 7 天对赌只打了 3 天或 4 天），则同样判定为**全员失败**，没收全部本金。
4. **分配逻辑完全复用**：输家的契约金继续平分给赢家。

### 3. 真机与数据库状态验证
我们在远程服务器上重新部署了后台服务，并针对这套 60% 比例门槛的判定逻辑，编写测试脚本并执行了验证：
* **对赌小队配置**：7 天挑战（60% 最低达标限制为 `Math.ceil(7 * 0.6) = 5` 天）
* **验证场景一：最高打卡天数未过 60% 门槛 (全员失败)**
  - 用户 2 设定为 4 次成功打卡，用户 3 设定为 2 次成功打卡。最高成功天数 4 未达到 5 天的最低限制。
  - 执行对赌结算：结果显示全员失败，双方可用积分保持在初始扣除后的 **900 pts**（本金没收不予退还）。
* **验证场景二：最高打卡天数达到 60% 门槛 (胜者得奖)**
  - 用户 2 恢复为 6 次成功打卡，用户 3 保持 2 次成功打卡。最高成功天数 6 超过了 5 天的最低门槛。
  - 执行对赌结算：系统判定用户 2 (`?sir`) 为唯一的赢家。其可用积分成功提升至 **1100 pts**（返还本金 100 + 赢取用户 3 的本金 100），队友由于未达标保持在 900 pts。

这证明 60% 达标天数门槛的判定逻辑完美生效，有效防御了所有人都不打卡或打卡太少时的漏洞！
"""

with open(path, "w", encoding="utf-8") as f:
    f.write(content + new_section)

print("walkthrough.md cleaned and updated successfully!")
