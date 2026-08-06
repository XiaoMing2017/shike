# 食刻 (Shike) 系统核心算法与架构技术文档

本文档全面梳理并记录了【食刻 (Shike)】智能健身与膳食管理系统中应用的所有**核心算法**、**模型引擎**与**计算逻辑**。

---

## 目录
1. [基础代谢与总消耗计算算法 (BMR / TDEE Calculation)](#1-基础代谢与总消耗计算算法-bmr--tdee-calculation)
2. [三大营养素动态分配算法 (Macronutrient Allocation)](#2-三大营养素动态分配算法-macronutrient-allocation)
3. [RP 运动容量与渐进超载算法 (RP Landmarks & Progressive Overload)](#3-rp-运动容量与渐进超载算法-rp-landmarks--progressive-overload)
4. [大模型提示词工程与 JSON 严格校验算法 (LLM Prompt & JSON Parsing)](#4-大模型提示词工程与-json-严格校验算法-llm-prompt--json-parsing)
5. [Qwen-VL 多模态视觉识图与烹饪油修正算法 (AI Vision & Oil Adjustment)](#5-qwen-vl-多模态视觉识图与烹饪油修正算法-ai-vision--oil-adjustment)
6. [临床营养全景诊断规则引擎算法 (Clinical Diet Diagnosis Engine)](#6-临床营养全景诊断规则引擎算法-clinical-diet-diagnosis-engine)
7. [AI 深度膳食复盘与积分审计算法 (LLM Diet Diagnosis & Audit)](#7-ai-深度膳食复盘与积分审计算法-llm-diet-diagnosis--audit)
8. [Canvas 2D 动态热量环形渲染算法 (Dynamic Calorie Canvas Ring)](#8-canvas-2d-动态热量环形渲染算法-dynamic-calorie-canvas-ring)
9. [海报离屏模板合成与文本自动换行算法 (Multi-template Offscreen Poster Canvas)](#9-海报离屏模板合成与文本自动换行算法-multi-template-offscreen-poster-canvas)
10. [悬浮气泡物理边界避让与版本化防打扰算法 (FAB Boundary & Modal Guard)](#10-悬浮气泡物理边界避让与版本化防打扰算法-fab-boundary--modal-guard)

---

### 1. 基础代谢与总消耗计算算法 (BMR / TDEE Calculation)
* **算法来源**：Mifflin-St Jeor 公式（国际临床营养学公认最高精度公式）。
* **计算逻辑**：
  $$\text{BMR}_{\text{male}} = 10 \times \text{weight(kg)} + 6.25 \times \text{height(cm)} - 5 \times \text{age} + 5$$
  $$\text{BMR}_{\text{female}} = 10 \times \text{weight(kg)} + 6.25 \times \text{height(cm)} - 5 \times \text{age} - 161$$
* **TDEE 乘数**：根据 PAL (Physical Activity Level) 运动系数设定：
  - 久坐不动：$\text{TDEE} = \text{BMR} \times 1.2$
  - 轻度活动（1-3天/周）：$\text{TDEE} = \text{BMR} \times 1.375$
  - 中度活动（3-5天/周）：$\text{TDEE} = \text{BMR} \times 1.55$
  - 高强度活动（6-7天/周）：$\text{TDEE} = \text{BMR} \times 1.725$
* **赤字/盈余控制**：
  - 减脂期：每日目标热量 $= \text{TDEE} - (500 \sim 700 \text{ kcal})$（控制每周减重 0.5%~1%）
  - 增肌期：每日目标热量 $= \text{TDEE} + (300 \sim 500 \text{ kcal})$

---

### 2. 三大营养素动态分配算法 (Macronutrient Allocation)
根据用户健康目标（减脂、增肌、腹肌塑形、生理期、维持）动态计算三大营养素目标克数：
* **蛋白质 (Protein)**：
  - 减脂/维持：$1.4\text{g} \sim 1.6\text{g} / \text{kg}$ 体重
  - 增肌/腹肌塑形：$2.0\text{g} / \text{kg}$ 体重
* **脂肪 (Fat)**：
  - 占总热量目标的 $20\% \sim 25\%$（$1\text{g} \text{脂肪} = 9\text{ kcal}$）
* **碳水化合物 (Carbohydrates)**：
  - 剩余热量由碳水补足（$1\text{g} \text{蛋白质/碳水} = 4\text{ kcal}$）
  - $\text{Carbs(g)} = \frac{\text{TargetCalories} - (\text{Protein} \times 4 + \text{Fat} \times 9)}{4}$

---

### 3. RP 运动容量与渐进超载算法 (RP Landmarks & Progressive Overload)
* **容量法则 (Volume Landmarks)**：
  - 大肌群（胸、背、腿）：10-14 组 / 周
  - 小肌群（肩、手臂、腹）：6-10 组 / 周
* **强度与保留次数 (RIR - Reps in Reserve)**：
  - 复合动作：75-85% 1RM，RIR 2（保留2次极限）
  - 孤立动作：70-80% 1RM，RIR 1-2
* **渐进超载 (Progressive Overload)**：
  - 上肢动作：完成目标次数且 RIR $\ge 2$ 时，下周负荷 $+1 \sim 2\text{kg}$
  - 下肢动作：完成目标次数且 RIR $\ge 2$ 时，下周负荷 $+2.5 \sim 5\text{kg}$
* **疲劳修正**：睡眠时间 $< 6$ 小时或连续训练 3 天时，单次训练容量自动衰减 $30\%$。

---

### 4. 大模型提示词工程与 JSON 严格校验算法 (LLM Prompt & JSON Parsing)
* **大模型驱动引擎**：阿里云通义千问 `Qwen3.8-Max` / `Qwen3.6-Flash`。
* **Prompt 结构**：10 项强约束规则 + 固定 JSON 结构模版（`user_summary`, `weekly_training`, `weekly_diet`）。
* **格式化与容错处理 (`parseAndCleanJson`)**：
  1. 正则剥离 Markdown 代码块包裹（` ```json ... ``` `）；
  2. 自动修正单引号、非标逗号与未闭合的大括号；
  3. 结构兼容映射：动态将 `weekly_training` 映射为 `workoutPlan`，`weekly_diet` 映射为 `dietPlan`，保障前端无缝兼容；
  4. 超时/截断降级：当输出 Tokens 超过上限触发截断时，平滑降级至每日 3~4 个具体动作的专家预设模板。

---

### 5. Qwen-VL 多模态视觉识图与烹饪油修正算法 (AI Vision & Oil Adjustment)
* **模型**：`Qwen-VL-Plus` 多模态大模型。
* **图片预处理**：自动缩放与 Base64 编码，带压缩保护；
* **烹饪用油修正**：
  - 清淡蒸煮 (Light)：热量乘数 $\times 1.0$
  - 中度炒菜 (Moderate)：热量乘数 $\times 1.15$
  - 重油煎炸 (Heavy)：热量乘数 $\times 1.30$，脂肪系数额外增加 $10\text{g} \sim 15\text{g}$。

---

### 6. 临床营养全景诊断规则引擎算法 (Clinical Diet Diagnosis Engine)
* **毫秒级算法**：前端 `calculateNutritionDiagnosis(data)` 本地离线计算引擎。
* **多维判定规则**：
  1. **热量超标/赤字**：超过目标线自动提醒超标 kcal，并算出需增加的 Zone2 有氧时长；
  2. **蛋白质保肌线**：低于目标 $75\%$ 时触发黄牌预警，按缺口克数具体推荐食材（如缺 $\ge 30\text{g}$ 推荐 200g 鸡胸肉/150g 牛肉）；
  3. **碳水与脂肪占比**：识别精制碳水偏高或重油油脂超标，给出换算代替建议。

---

### 7. AI 深度膳食复盘与积分审计算法 (LLM Diet Diagnosis & Audit)
* **功能**：由 `Qwen` 大模型根据用户今日实际吃的具体菜品生成 150 字专家级复盘点评与 3 条落地策略；
* **积分扣减与审计逻辑**：
  1. **后台开关拦截**：校验 `AdminService.getPublicFeatureToggles("release")` 中的 `diet_diagnosis` 开关，未开启直接拦截；
  2. **首次免费**：查询 `PointsRecord` 是否包含 `DIET_DIAGNOSIS`，无记录则免积分扣除；
  3. **二次使用扣费**：每次消耗 15 积分，账户余额不及 15 积分时抛出 `BizException` 异常保护。

---

### 8. Canvas 2D 动态热量环形渲染算法 (Dynamic Calorie Canvas Ring)
* **图形计算**：
  - 动态计算起始角度（`startAngle = -0.54 * Math.PI`）与总弧度（`totalAngle = 1.62 * Math.PI`）；
  - 进度弧度：$\text{currentAngle} = \text{startAngle} + (\text{progressPercent} \times \text{totalAngle})$；
* **超标色系控制**：未超标呈现 `#6366F1` 紫色渐变，超标后平滑过度为 `#EF4444` 警示红。

---

### 9. 海报离屏模板合成与文本自动换行算法 (Multi-template Offscreen Poster Canvas)
* **模板引擎**：四套高颜值风格（莫兰迪极简、复古日记、杂志风、暗黑霓虹）；
* **文本自动折行计算**：计算字符 `measureText` 宽度，超过海报边界（如 $315\text{px}$）自动分行与增加行距；
* **离屏绘制与导出**：离屏隐藏 Canvas (`left: -9999px`) 绘制完成后导出临时图片文件 `wx.canvasToTempFilePath`。

---

### 10. 悬浮气泡物理边界避让与版本化防打扰算法 (FAB Boundary & Modal Guard)
* **饮水气泡边界计算**：
  - 获取设备屏幕宽高 `windowWidth` 与 `windowHeight`；
  - 悬浮位置公式：$X = \text{windowWidth} - \text{fabSize} - 17\text{px}$，$Y = \text{windowHeight} - \text{fabSize} - 100\text{px}$（精准避开底栏 TabBar）；
* **重磅引导弹窗版本化防打扰**：
  - 利用本地存储 `has_seen_v2_new_feature_modal` 控制每个版本仅对普通用户弹窗 1 次；
  - **测试账号特权模式**：用户 ID 为 `2` 时跳过写入，保障每次进入均可稳定重现弹窗。
