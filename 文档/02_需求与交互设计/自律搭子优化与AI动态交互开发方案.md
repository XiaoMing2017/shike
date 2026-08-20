# 食刻自律搭子系统：成年人心理减负优化与 AI 动态交互开发方案

**文档版本**：v1.0  
**更新日期**：2026-08-20  
**适用对象**：全栈开发人员、前端小程序开发、后端 Java 工程师  

---

## 1. 方案背景与核心目标

### 1.1 业务背景
食刻小程序的“自律搭子”（宠物养成）定位为**健康减脂行为的“正向情绪激励层”**。  
根据成年人心理与行为调研，成年人（18~35岁青年/白领）虽然极度渴望情绪陪伴与即时反馈，但对**“高心理负担、上班式打卡、死板机械重复”**的养成系统极其容易产生抵触与弃坑。

### 1.2 改造目标
1. **去惩罚化（心理减负）**：移除导致负罪感的“生病虚弱、倒扣经验”机制，将断更惩罚转为“休眠待机与温暖回归”，提升长期留存。
2. **AI 动态拟人互动**：接入大语言模型（通义千问 Qwen-Turbo / Qwen3.6-Plus），让搭子具备性格人设、时段感知、健康数据联动与情绪共鸣能力。
3. **自律闭环强化**：将用户的实际运动大卡、饮水量、饮食打卡状态注入搭子大脑，实现千人千面的“懂你的自律搭子”。

---

## 2. 现有模块修改清单（消除成年人弃坑痛点）

### 2.1 后端修改：弱化怠惰惩罚，消除负罪感
- **目标文件**：[`PetServiceImpl.java`](file:///d:/heming/shike/shike-backend/src/main/java/com/shike/service/impl/PetServiceImpl.java)
- **修改要点**：
  1. **移除经验/亲密度倒扣**：在 `applyLifeCycleAndSlackPenalty` 中，注释或删除断更扣除 EXP 和扣除 Intimacy 的逻辑。用户的努力永久保留，杜绝挫败感。
  2. **替换 `SICK` 负面状态**：当用户超过 48 小时未打开时，状态置为 `WAITING`（思念休眠）而非 `SICK`（生病求救）。
  3. **增加回归关怀机制**：当断更用户重新打开并投喂时，触发“回归温暖欢迎语”并赠送额外“回归元气便当”（`foodCount + 1`）。

### 2.2 前端修改：剥离死板静态台词
- **目标文件**：[`pet.js`](file:///d:/heming/shike/shike-frontend/pages/pet/pet.js)
- **修改要点**：
  1. `onTapPet()` 逻辑改造：不再单纯在本地静态 `quotes` 数组中随机取词，而是调用后端 `/pet/interact` 接口获取 AI 动态台词。
  2. 增加轻量防抖与本地缓存：用户快速连续点击时，展示本地缓存的个性化台词，避免重复发起网络请求；单次点击触发异步 AI 刷新。

---

## 3. 新增功能一：AI 动态交互引擎（后端实现）

### 3.1 接口契约定义

#### 接口：搭子 AI 动态交互
- **URL**：`POST /api/v1/pet/interact`
- **Content-Type**：`application/json`

**请求参数 (PetInteractDTO)**：
```json
{
  "userId": 10001,
  "actionType": "TOUCH", 
  "userMessage": "" 
}
```
*参数说明*：
* `actionType` 枚举：
  * `TOUCH`：轻触抚摸搭子
  * `FEED`：投喂食物
  * `EXERCISE_DONE`：刚完成运动打卡
  * `DIET_RECORDED`：刚记录完饮食
  * `WATER_RECORDED`：刚记录完饮水
  * `CHAT`：用户在树洞主动打字倾诉
* `userMessage`：用户输入的对话文本（`actionType == CHAT` 时必填，其余可选）

**响应结果 (ResultDTO<PetInteractVO>)**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dialogue": "呼噜噜～刚跑完3公里超厉害！本小龙感觉全身龙鳞都在发光，快喂我吃个红苹果吧！🍎",
    "mood": "HAPPY",
    "actionAnim": "bounce",
    "soundEffect": "purr"
  }
}
```

---

### 3.2 动态 Prompt 组装与上下文注入

后端在接收到请求后，聚合以下上下文后拼接 System Prompt：

```java
// 上下文聚合逻辑
LocalDate today = LocalDate.now();
int waterMl = waterRecordRepository.sumTodayWater(userId, today);
double burnedCal = exerciseRecordRepository.sumTodayCalories(userId, today);
boolean hasDiet = dietRecordRepository.existsToday(userId, today);
int streak = pet.getStreakDays() != null ? pet.getStreakDays() : 1;

// 时段判断
int hour = LocalTime.now().getHour();
String timeSlot = hour < 9 ? "清晨" : (hour < 13 ? "上午" : (hour < 18 ? "下午" : (hour < 23 ? "晚上" : "深夜")));
```

#### 5 大神兽性格偏置矩阵 (Persona Definition)
```java
public static String getPetPersona(String petType) {
    return switch (petType) {
        case "DRAGON" -> "热血傲娇的青玉幼龙，嘴硬心软，最关注主人的运动燃脂，口癖是‘嗷呜’、‘哼’、‘冲鸭’。";
        case "TOTORO" -> "温吞治愈的大龙猫，说话慢条斯理，极度关心主人吃得健不健康、喝水够不够，充满治愈与包容。";
        case "CAT"    -> "灵动傲娇的元气小橘猫，注重体态与轻盈感，爱撒娇，口癖是‘喵呜’、‘喵~’。";
        case "DOG"    -> "忠诚阳光的自律柴犬，精力充沛，最喜欢户外慢跑，口癖是‘汪汪！’、‘主人最棒！’。";
        case "QILIN"  -> "优雅仙气的小天麟，相信自律会吸引好运与祥瑞，给主人送上温暖的祝福与正向能量。";
        default       -> "元气可爱的自律搭子。";
    };
}
```

#### System Prompt 模板
```text
你现在是用户的专属自律搭子【{{petName}}】。
【性格特征】：{{petPersona}}
【当前阶段】：等级 Lv.{{level}}，形态【{{stageTitle}}】
【当前时段】：{{timeSlot}}
【用户今日真实健康数据】：
- 今日饮水：{{waterMl}} ml
- 运动消耗：{{burnedCal}} kcal
- 连续自律陪伴：{{streak}} 天
- 触发动作：{{actionType}}
- 用户对你说的话：{{userMessage}}

【输出要求】：
1. 必须完全符合你的性格人设和口癖。
2. 敏锐结合用户今日的自律数据进行回应（如喝水达标给予赞扬、深夜时劝主人早点睡、刚运动完给予极致的情绪激励）。
3. 如果用户表达疲惫、嘴馋或减脂压力大，给予充满心理学包容感的治愈与引导，绝不要批评或讲大道理。
4. 字数严格控制在 25 ~ 45 个汉字以内，适合展示在移动端小程序气泡中。
5. 只输出宠物说的话，不要带任何前缀、引号或解释。
```

---

### 3.3 大模型调用与性能保障（极速响应）

1. **模型选型**：推荐使用阿里云 DashScope 的 `qwen-turbo`（响应仅 200~400ms，每千 Token 仅 0.0003 元）或 `qwen3.6-plus`。
2. **Redis 缓存与频次优化**：
   - 抚摸/打卡类常用搭话：同一用户 10 分钟内相同自律状态的抚摸，优先命中 Redis 预生成缓存池。
   - 限制每位用户每日实时 AI 交互上限（如 30 次），超出后从该宠物历史高质量对话池中随机抽取。
3. **网络超时降级（Fallback）**：
   - 设置 HttpClient 超时为 `1500ms`。
   - 若超时或 DashScope 异常，自动无缝返回内置精品台词库，前端零报错感知。

---

## 4. 新增功能二：前端交互与 UI 呈现

### 4.1 核心交互 A：3D 浮岛抚摸即时搭话
- **交互流程**：
  1. 用户点击 3D 浮岛上的宠物模型；
  2. 触发点击微震动（`wx.vibrateShort`）与爱心喷发动画；
  3. 右上角悬浮气泡展示 Loading 动效（“💭...”）；
  4. 请求 `/pet/interact` 成功后，气泡文字平滑淡入展示 AI 动态回复。

### 4.2 核心交互 B：跨页面自律打卡的主动夸夸（Event-Driven）
- 在首页完成**运动记录、饮水打卡、饮食拍照**后，可弹窗轻量 Toast 或在下次进入宠物页时直接展示**针对该动作的专属表扬气泡**。
  - 例如：用户刚记录了 300ml 饮水，进入搭子页时搭子主动说：“*咕嘟咕嘟～收到 300ml 纯净能量！小橘的毛发也变得水灵灵喵～*”

### 4.3 核心交互 C：“自律树洞”轻量对话栏（前端组件）
- 在 `pet.wxml` 底部增加折叠式/快捷对话面板：
  - **快捷气泡标签**：`[🥺 今天好累呀]` `[🍗 好想吃炸鸡怎么办]` `[✨ 夸夸我]` `[💧 提醒我喝水]`
  - **自定义输入框**：支持用户打字发送给搭子。
  - **交互体验**：发送后搭子作为贴心减脂闺蜜/搭子给出高情商回复，释放成年人减脂心理压力。

---

## 5. 文件变更与开发任务拆解清单

| 模块 | 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- | :--- |
| **Backend** | `src/main/java/com/shike/model/dto/PetInteractDTO.java` | **[NEW]** | 定义 AI 交互输入 DTO（动作类型、用户消息等） |
| **Backend** | `src/main/java/com/shike/model/vo/PetInteractVO.java` | **[NEW]** | 定义 AI 交互输出 VO（气泡文案、表情动画等） |
| **Backend** | `src/main/java/com/shike/controller/PetController.java` | **[MODIFY]** | 新增 `POST /pet/interact` 端点 |
| **Backend** | `src/main/java/com/shike/service/PetService.java` | **[MODIFY]** | 声明 `interactWithAi(Long userId, PetInteractDTO dto)` 方法 |
| **Backend** | `src/main/java/com/shike/service/impl/PetServiceImpl.java` | **[MODIFY]** | 1. 移除 48h 掉经验/扣亲密度惩罚；<br>2. 接入健康数据聚合与 Qwen LLM 极速调用与 Fallback 逻辑 |
| **Frontend** | `pages/pet/pet.js` | **[MODIFY]** | 接入 `/pet/interact` 接口，支持抚摸、打卡联动与树洞对话 |
| **Frontend** | `pages/pet/pet.wxml` | **[MODIFY]** | 增加动态气泡加载态、树洞对话入口与快捷交互胶囊 |
| **Frontend** | `pages/pet/pet.wxss` | **[MODIFY]** | 增加对话输入栏、快捷胶囊、气泡微动效等样式 |

---

## 6. 测试与验收标准

1. **去惩罚验证**：
   - 模拟用户 72 小时未上线，调用 `GET /pet/my`，确认用户搭子的等级和经验未减少，状态为温和的休眠/等待状态，重新投喂能正常恢复。
2. **AI 交互时效与质量验证**：
   - 点击搭子，AI 气泡在 800ms 内成功渲染展示；
   - 文案中必须包含该宠物的专属口癖（如龙的“嗷呜”、猫的“喵”）；
   - 在用户记录 2000ml 水后点击，AI 能够识别并在台词中提及饮水充足。
3. **异常与降级验证**：
   - 断开外网或模拟 LLM API 超时（>1.5s），系统平滑降级为本地预设台词库，页面无报错白屏。
