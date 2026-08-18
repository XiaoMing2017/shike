# 🚀 食刻项目 Git 分支与多设备协作指南

> 更新时间：2026-08-18  
> 当前版本：v2.1.0 研发周期（宠物养成系统大版本）

---

## 📌 当前分支全景状态

| 分支 / 标签 | 分支类型 | 状态 / 说明 |
| :--- | :--- | :--- |
| **`main`** | 生产主分支 | 🟢 **线上稳定运行代码**（生产环境部署以此分支为准） |
| **`feature/pet-system`** | **当前研发大分支** | 🔥 **宠物养成系统（自律搭子、运动打卡、投喂升级）核心开发分支** |
| **`v2.0.0-stable`** | 历史里程碑 Tag | 🏷️ 宠物系统开发前的稳定版快照（随时可回滚） |

---

## 💻 换到其他电脑开发（多设备协作流程）

当你在**另一台电脑**或**新拉取的代码库**中继续开发时，请按以下步骤操作：

### 1. 首次在其他电脑拉取并切换到宠物分支
```bash
# 1. 拉取远端所有最新分支与标签
git fetch origin

# 2. 切换到宠物养成开发大分支（并关联远端）
git checkout feature/pet-system

# 3. 确保本地代码为最新
git pull origin feature/pet-system
```

---

### 2. 日常开发与提交规范
在 `feature/pet-system` 分支上开发时，建议按模块小步提交：

```bash
# 查看改动状态
git status

# 暂存并提交代码（建议使用语义化前缀）
git add .
git commit -m "feat(pet-backend): 新增宠物实体与运动打卡数据表"

# 推送代码到 GitHub
git push origin feature/pet-system
```

常用 Commit 前缀：
- `feat(pet-xxx)`: 宠物新功能
- `fix(pet-xxx)`: 宠物功能 Bug 修复
- `style(pet-xxx)`: 样式与 UI 优化
- `docs(...)`: 文档更新

---

### 3. 如果线上主分支出现紧急 Bug 需要修复
若开发进行到一半，需要紧急修复线上 `main` 的问题：

```bash
# 1. 暂存当前未写完的代码
git stash

# 2. 切回主分支
git checkout main
git pull origin main

# 3. 在 main 上修好 Bug 并推送到线上
git add .
git commit -m "fix: 修复线上紧急问题"
git push origin main

# 4. 切回宠物分支，继续写代码并把刚才修复的 Bug 同步过来
git checkout feature/pet-system
git merge main
git stash pop
```

---

### 4. 宠物系统开发完毕，大版本正式合并与发布
当宠物养成系统在本地和测试环境全部验证通过，准备正式发布到生产环境时：

```bash
# 1. 切换到 main 分支
git checkout main
git pull origin main

# 2. 合并宠物系统分支（使用 --no-ff 记录清晰的发布节点）
git merge --no-ff feature/pet-system -m "feat: 正式发布 v2.1.0 宠物养成系统（自律搭子与运动投喂）"

# 3. 推送到线上生产分支
git push origin main

# 4. 打上正式发布 Tag 标签
git tag -a v2.1.0 -m "Release v2.1.0: 宠物养成系统全面上线"
git push origin v2.1.0
```
