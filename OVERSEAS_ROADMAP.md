# 🌍 「食刻 (ShiKe)」出海 PWA 商业化全流程进度表与操作备忘录

> **项目定位**：从国内微信小程序全面转型为面向欧美市场的独立移动端 Web App (PWA)，主打 AI 视觉食物热量估算、沙拉油脂微调与自律小队打卡，通过海外订阅制变现，零门槛绕过苹果 30% 抽成与审核。
> **更新时间**：2026-09-15
> **当前状态指示**：`功能开发已就绪` ➡️ `🔥 当前重心：全功能内部实测与真机验收 (Internal QA & Alpha Testing)`

---

## 🧭 当前所处位置速查 (Current Milestone)

```mermaid
graph LR
  P1[阶段一: PWA基础与合规] -->|已完成 100%| P2[阶段二: 身体代谢评估与建档]
  P2 -->|已完成 100%| P3[阶段三: 海外真实AI视觉识别]
  P3 -->|已完成 100%| P5B[阶段五B: 教练督学中枢]
  P5B -->|已完成 100%| P4[阶段四: 公网部署上线]
  P4 -->|已完成 100%| P5[阶段五: 跨境美金订阅收款]
  P5 -->|🔥 当前重心| QA[内部全功能真机实测与体验打磨]
  QA --> P6[阶段六: 海外冷启动与增长]

  style P1 fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style P2 fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style P3 fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style P5B fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style P4 fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style P5 fill:#d1fae5,stroke:#10b981,stroke-width:2px;
  style QA fill:#fef3c7,stroke:#f59e0b,stroke-width:3px;
  style P6 fill:#f1f5f9,stroke:#94a3b8,stroke-width:1px;
```

---

## 📋 详细推进清单 (Master Checklist)

### 阶段一：PWA 架构迁移与海外基础合规【已全部完成 ✅】
- [x] **1.1 独立 PWA 工程搭建 (`shike-pwa/`)**：Vue 3 + Vite + Tailwind CSS + Pinia + `vite-plugin-pwa`，完全脱离微信生态。
- [x] **1.2 移动端全屏与可安装体验**：配置 Web Manifest，支持 iPhone (Safari 添加到主屏幕) 与 Android (Chrome 原生安装应用)。
- [x] **1.3 核心业务页面重构与美化**：
  - [x] 首页看板 (`HomeView.vue`)：剩余热量动态圆环、三大营养素进度、饮水打卡、当日餐次流。
  - [x] 拍照识别与调整 (`ScanView.vue`)：原生相机唤起、营养素拆解卡、油脂/沙拉酱修正滑块 (-10% / 0 / +15%)。
  - [x] 责任自律小队 (`TeamView.vue`)：合规去赌博化，升级为 5 人监督小队与共享能量池。
  - [x] 历史周/月双重视图 (`HistoryView.vue`)：7 天柱状热量差图表与 30 天日历打卡热力图。
  - [x] 美金订阅转化弹窗 (`PaywallModal.vue`)：周付 $4.99、年付 $39.99。
- [x] **1.4 响应式中英文双语系统 (i18n)**：全界面动态切换（无需刷新），包含英文 (`en.js`) 与中文 (`zh.js`)。
- [x] **1.5 计量单位双轨制**：公制 (kg/cm) 与英制 (lbs/inch) 一键无损实时换算。
- [x] **1.6 海外法律合规审查保障**：醒目展示《健康免责声明 (Disclaimer)》与 GDPR 规范的《彻底注销账户并抹除数据 (Delete Account)》。

---

### 阶段二：身体档案与科学代谢评估系统【已全部完成 ✅】
- [x] **2.1 海外标杆 Onboarding 流程设计**：参考 MyFitnessPal 与 Noom，采用 5 步沉浸式入门问卷（目标 -> 性别年龄 -> 身高体重 -> 活动强度与目标体重 -> 专属分析报告与一键保存方案）。
- [x] **2.2 国际公认 Mifflin-St Jeor 代谢公式落地**：
  - 男性/女性差异化基础代谢率 (BMR) 计算。
  - 4 档活动系数 (PAL: 1.2 ~ 1.725) 对应每日总能耗 (TDEE)。
  - 稳健减脂热量赤字 (-500 kcal/天，对应约 0.5kg/周) 与安全底线保护 (女 1200 / 男 1500 kcal)。
- [x] **2.3 评估结果与全站数据联动**：
  - 评估完成后，首页看板目标热量从默认 2150 kcal 自动更新为专属测算值（如 1575 kcal）。
  - 个人中心展示身体基础档案，并提供“重新测算”入口。
  - 普通注册页增设“个性化测算方案”快捷引流卡片。
- [x] **2.4 后端 MySQL 物理表扩充**：`User.java` 增加 `email`, `password`, `gender`, `age`, `height`, `weight`, `target_weight`, `bmr`, `tdee`, `target_calories` 等字段。
- [x] **2.5 真实后端 Web 认证接口**：新增 `WebAuthController.java`，支持 `/register` 和 `/login`，服务器端二次公式验算入库并签发 Token。

---

### 阶段三：真实海外 AI 视觉大模型接入【🔥 当前进行项 / 即将执行 ⏳】
- [ ] **3.1 极低成本海外多模态大模型选型与配置**：
  - 推荐选型：**OpenAI GPT-4o-mini** 或 **Google Gemini 1.5 Flash**（单次图像分析成本约 $0.0001 美金，1 万次仅需 $1 美金）。
  - 配置 API Key 与安全环境变量。
- [x] **3.2 营养学提示词工程 (Prompt Engineering)**：针对全球中西餐、轻食沙拉升级多模态 Prompt，支持英文与中文双语菜名、热量、蛋白质、脂肪、碳水、膳食纤维与净碳水 (Net Carbs)。
- [x] **3.3 后端多模态视觉与日记接口打通**：`DietServiceImpl.java` 与 `DietController.java` (`/diet/recognize` 与 `/diet/record`) 完成国际化解析与数据落库，Maven 编译通过。
- [x] **3.4 前端 `ScanView.vue` 联调拍照识图**：秒级返回营养素拆解卡、油脂沙拉酱修正滑块 (-10% / 0 / +15%) 实时联动卡路里重算。
- [x] **3.5 饮食日记与首页看板闭环落盘**：一键“记入日记”触发彩屑撒花，首页热量环进度条从 820 kcal 自动跳至 1198 kcal，剩余热量动态刷新为 377 kcal 并完成本地/云端持久化。

---

### 阶段四：零成本公网部署与免费域名上线【待启动 ⏳】
- [ ] **4.1 前端全球免费托管 (Vercel / Cloudflare Pages)**：
  - 连接 GitHub 仓库，一键构建部署 `shike-pwa`。
  - 自动分配全球 CDN 加速节点与免费 HTTPS 域名（例如 `shike.vercel.app`），满足全球移动设备对 PWA 安装及摄像头调用的 HTTPS 强制要求。
- [ ] **4.2 后端与数据库轻量出海部署 (Railway / Render)**：
  - 编写生产环境轻量 `Dockerfile`。
  - 免费/低成本托管 Spring Boot 后端与 MySQL 数据库，配置生产环境变量与时区。
- [ ] **4.3 跨域 (CORS) 与生产网络打通**：
  - 配置前端向后端生产域名的 API 请求转发。
- [ ] **4.4 全球公网真机全面验收**：
  - 手机无需连接电脑局域网，脱机全网访问体验。

---

### 阶段五：跨境美金订阅与变现闭环【已全部完成闭环 ✅】
> 详细设计与运维手册请查阅：[06_Lemon_Squeezy跨境美金订阅与支付系统设计与运维手册.md](文档/03_系统架构与数据库/06_Lemon_Squeezy跨境美金订阅与支付系统设计与运维手册.md)

- [x] **5.1 跨境收款账户选型与配置**：
  - 选定并接入海外独立开发者首选的 **Lemon Squeezy**（Merchant of Record 模式，自动代扣全球消费税 VAT，支持国内个人/个体户入驻）。
- [x] **5.2 订阅商品定义**：
  - 定义 **Weekly Pro** ($4.99/周，含 3 天免费试用) 与 **Annual Pro** ($39.99/年，立省 65%)。
- [x] **5.3 前端收银台对接与内嵌体验**：
  - 引入 `lemon.js` 官方 SDK，支持在移动端 PWA 中无感唤起半透明浮层收银台（支持 Apple Pay / Google Pay / 全球信用卡）。
  - 实现 4 步状态机：套餐选择 -> 游客拦截与防飞单绑定 -> 收银台调用 -> 全屏撒花与 PRO 权益激活。
- [x] **5.4 后端支付 Webhook 与会员权益发放**：
  - 新增订单表 `tb_payment_order`，实现 HMAC-SHA256 签名鉴权防篡改。
  - 自动履约：更新用户 `vip_type = 'PRO'`，顺延 `vip_expire_time`（周卡+7天/年卡+365天），置 `ai_unlimited = true`。
  - 提供沙箱调试接口 `/payment/test-complete/{orderNo}` 方便随时无门槛验收。
- [x] **5.5 会员用量与商业化拦截闭环**：
  - 非会员每日限额 3 次 AI 拍照扫描，超额自动唤起 Paywall 升级弹窗；会员享有无限次拍照分析。
  - 个人中心实时拉取并展示 `[Pro 会员有效 ✨]` 与到期时间。

---

### 阶段五（B）：B2B2C 健身教练督学中枢与多学员席位扩容【已完成闭环 ✅】
- [x] **5.B.1 教练小队席位扩容架构（突破 5 人限制）**：
  - 双模分层架构：顶栏秒级切换普通好友小队（5人轻量打卡）与教练督学特训营（Coach Hub，支持 10、30、100 人大班席位）。
- [x] **5.B.2 状态管理与数据联动 (`coachStore.js`)**：
  - 接入特训营周期（第 2/8 周）、席位占用进度条（24/30 席位）、学员专属入营邀请码 (`SHRED-30X`) 与一键复制。
- [x] **5.B.3 教练全局中枢大盘 (Coach Dashboard)**：
  - 一页纵览学员实时热量与蛋白质达标双进度条。
  - 智能红黄绿警报：🔴 今日摄入超标报警（如 Sarah 超出 340 kcal）、🟡 缺卡/未记录预警（如 David 待打晚餐）、🟢 蛋白质高分达标（如 Emma 超额完成）。
- [x] **5.B.4 轻量高效督促工具箱**：
  - 一键 **Praise 👍（点赞高光）**、一键 **Nudge 🔔（单人/全员一键催卡并弹窗提醒）**、**Comment 💬（展开教练专属饮食指导输入框并持久化）**。
- [x] **5.B.5 教练高客单价商业化套餐展示 (Coach SaaS Pricing)**：
  - 底部嵌入 Coach Pro（30席位）至 Studio 机构方案（100席位，$129.99/月）的扩容入口。
- [x] **5.B.6 全站中英文双语适配与真机验证**：
  - `en.js` 与 `zh.js` 完整注入教练中枢所有词条，真机双语渲染与交互已全面通过。

---

### 阶段六：海外冷启动与增长裂变【规划待执行 ⏳】
- [ ] **6.1 PWA 搜索引擎优化 (SEO) 与社交分享卡片 (OpenGraph)**。
- [ ] **6.2 自律小队（Squad）裂变机制**：
  - 用户生成小队专属邀请链接/海报，每成功邀请 1 位好友，双方各自获赠 7 天 Pro 体验或额外扫描次数。
- [ ] **6.3 海外社媒低成本冷启动运营**：
  - 在 TikTok、Instagram Reels、Reddit (r/loseit, r/fitness) 进行前后对比与减脂打卡分享。

---

### 🔥 核心关键阶段：全功能内部实测与真机验收 (Internal QA & Alpha Testing)【当前进行中 ⏳】
> **核心原则**：上线前必须保证所有用户流程（Onboarding、拍照识别、记账联动、小队互动、教练督导、语言单位切换）在真机上 100% 顺畅无死角，消除所有体验瑕疵。

- [ ] **QA.1 局域网真机环境连通性验收**：
  - 电脑启动 `npm run dev -- --host`（当前局域网 IP：`172.19.20.61:5173`）。
  - 手机在同一 Wi-Fi 下直连，验证首屏加载速度与移动端全屏视口高度适配。
- [ ] **QA.2 身体代谢评估与建档全流程测试 (Onboarding Flow)**：
  - 测试性别、年龄、身高、体重、活动系数与减重目标输入。
  - 验证基于 Mifflin-St Jeor 公式计算的 BMR 与 TDEE 准确度。
  - 验证保存后首页目标热量（如 1575 kcal）是否即时刷新同步。
- [ ] **QA.3 AI 视觉识图与油脂微调测试 (Vision & Oil Flow)**：
  - 测试手机调用原生相机拍照 / 相册选图上传。
  - 测试沙拉/中餐油脂微调滑块（-10% / 标准 / +15%）实时重算卡路里。
  - 点击“记入日记”，验证彩屑特效及首页当日摄入进度条累加。
- [ ] **QA.4 习惯誓约小队互助测试 (Buddy Squad Flow)**：
  - 验证 5 人微型小队宝石能量池显示、连胜天数统计。
  - 测试小队邀请码复制与剪贴板权限。
- [ ] **QA.5 健身教练督学中枢大盘测试 (Coach Hub Flow)**：
  - 切换至「教练督学中枢」，验证 30 人特训营席位占用率。
  - 验证红黄绿智能三色警报（🔴 摄入超标、🟡 缺卡、🟢 达标）。
  - 验证一键批量催卡 (Nudge All)、单人点赞 (Praise) 与教练专属指导 (Coach Note) 输入保存。
- [ ] **QA.6 个人中心与多语言/计量单位测试 (Settings & Compliance)**：
  - 测试 English / 中文 无刷新秒级切换。
  - 测试 Metric (kg/cm) 与 Imperial (lbs/inch) 实时数值换算。
  - 检查《健康免责声明》展示与《彻底注销账户》弹窗。

---

## 🛠️ 本地开发环境常用指令备忘

```bash
# 1. 启动前端 PWA 本地热更新服务（对外监听局域网）
cd d:\heming\shike\shike-pwa
npm run dev -- --host

# 2. 前端代码构建检验
npm run build

# 3. 后端编译检验（JDK 17）
$env:JAVA_HOME = "D:\Program Files\Java\jdk17"
cd d:\heming\shike\shike-backend
mvn compile -DskipTests
```

> **当前电脑局域网真机调试地址**：`http://192.168.0.3:5173`
> **身体评估测算地址**：`http://192.168.0.3:5173/onboarding`
> **教练督学中枢地址**：`http://192.168.0.3:5173/team`

