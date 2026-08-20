// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
    eggEmoji: '🟢',
    eggName: '青玉龙灵之卵',
    name: '青玉小幼龙',
    tag: '燃脂蜕变',
    food: '🍎',
    quote: '我不自律，小龙就没饭吃！',
    themeBg: '#ECFDF5',
    themeColor: '#047857',
    defaultName: '木木',
    stages: [
      {
        rank: 1,
        name: '青玉小幼龙',
        stageTitle: '幼年期 · 萌新搭子',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dragon_stage1.png',
        quote: '刚破壳的幼龙宝宝，圆滚滚毛茸茸，最喜欢吃红苹果！',
        desc: '初始幼龙形态，体态小巧软萌，每天陪你开启运动打卡！'
      },
      {
        rank: 2,
        name: '碧霄灵风龙',
        stageTitle: '成长期 · 进阶神兽',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dragon_stage2.png',
        quote: '长出翡翠龙角与修长羽翼，自律让我更有力量！',
        desc: '进阶为敏捷的碧霄灵龙，长出坚硬的翡翠龙角与羽翼，周身环绕翡翠符文！'
      },
      {
        rank: 3,
        name: '青天应龙神',
        stageTitle: '究极体 · 传奇守护神',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dragon_stage3.png',
        quote: '达成终极进化！身披金鳞流光，成为顶级自律守护神！',
        desc: '自律蜕变巅峰！身披紫青流云与黄金龙鳞，手握神圣宝珠，掌控祥瑞神力！'
      }
    ]
  },
  TOTORO: {
    type: 'TOTORO',
    icon: '🍃',
    eggEmoji: '⚪',
    eggName: '灵木龙猫之卵',
    name: '治愈大龙猫',
    tag: '温馨陪伴',
    food: '🥝',
    quote: '吃饱才有力气自律，记得按时吃减脂餐哦～',
    themeBg: '#F1F5F9',
    themeColor: '#334155',
    defaultName: '大龙猫',
    stages: [
      {
        rank: 1,
        name: '灵木小幼兽',
        stageTitle: '幼年期 · 萌新搭子',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_totoro_stage1.png',
        quote: '圆滚滚毛茸茸，最爱吃新鲜奇异果～',
        desc: '可爱的灵木幼兽，喜欢趴在草坡上打盹晒太阳。'
      },
      {
        rank: 2,
        name: '苍林守护使',
        stageTitle: '成长期 · 进阶神兽',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_totoro_stage2.png',
        quote: '披上绿叶披风，手握水晶法杖守护你的健康！',
        desc: '进阶为森林守护者，身披橡木绿叶斗篷，手握发光水晶法杖！'
      },
      {
        rank: 3,
        name: '神域龙猫尊',
        stageTitle: '究极体 · 传奇守护神',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_totoro_stage3.png',
        quote: '头戴百花神冠，掌控自然与治愈的奇迹之力！',
        desc: '龙猫之神！头戴水晶花冠，手捧森罗万象神球，庇佑你的自律之路！'
      }
    ]
  },
  CAT: {
    type: 'CAT',
    icon: '🐱',
    eggEmoji: '🟡',
    eggName: '元气灵猫之卵',
    name: '软萌元气猫',
    tag: '灵动轻盈',
    food: '🍊',
    quote: '动作要轻盈，体态要挺拔，今天打卡超棒喵～',
    themeBg: '#FFF7ED',
    themeColor: '#C2410C',
    defaultName: '小橘',
    stages: [
      {
        rank: 1,
        name: '软萌元气猫',
        stageTitle: '幼年期 · 萌新搭子',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_cat_stage1.png',
        quote: '今天也要轻盈起跳，体态棒棒喵～',
        desc: '活泼好动的小橘猫，抱着大甜橙满地打滚。'
      },
      {
        rank: 2,
        name: '赤焰灵猫使',
        stageTitle: '成长期 · 进阶神兽',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_cat_stage2.png',
        quote: '系上赤红金铃，步态如风敏捷轻盈！',
        desc: '进阶为赤焰灵猫，系上招财赤红金铃，周身环绕金色星火！'
      },
      {
        rank: 3,
        name: '九尾天焰神猫',
        stageTitle: '究极体 · 传奇守护神',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_cat_stage3.png',
        quote: '九尾金焰绽放！神装加身，成为体态与优雅的化身！',
        desc: '究极神兽！九条华丽金焰尾羽展开，身穿鎏金战甲，威风凛凛！'
      }
    ]
  },
  DOG: {
    type: 'DOG',
    icon: '🐶',
    eggEmoji: '🟤',
    eggName: '忠义玄犬之卵',
    name: '忠诚自律狗',
    tag: '户外自律',
    food: '🍓',
    quote: '主人快走！去公园跑两圈，今天的狗粮就有啦汪！',
    themeBg: '#FEF3C7',
    themeColor: '#B45309',
    defaultName: '旺财',
    stages: [
      {
        rank: 1,
        name: '忠诚自律狗',
        stageTitle: '幼年期 · 萌新搭子',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dog_stage1.png',
        quote: '去公园跑两圈，今天也有大草莓吃汪！',
        desc: '忠厚可爱的柴犬幼崽，最喜欢陪主人户外慢跑。'
      },
      {
        rank: 2,
        name: '疾风御行犬',
        stageTitle: '成长期 · 进阶神兽',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dog_stage2.png',
        quote: '系上疾风领巾，陪你跑赢每一个自律清晨！',
        desc: '进阶为健壮潇洒的疾风猎犬，系着飘扬的红色领巾，奔跑如风！'
      },
      {
        rank: 3,
        name: '天威烈焰神犬',
        stageTitle: '究极体 · 传奇守护神',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dog_stage3.png',
        quote: '身披太阳神铠，掌控烈焰与奔腾神威！',
        desc: '哮天神威圣犬！身披炽金太阳重铠，神威凛凛，无坚不摧！'
      }
    ]
  },
  QILIN: {
    type: 'QILIN',
    icon: '✨',
    eggEmoji: '🟣',
    eggName: '祥瑞天麟之卵',
    name: '祥瑞小麒麟',
    tag: '祥瑞好运',
    food: '🍑',
    quote: '自律者自带祥瑞，坚持打卡，好身材和好运一起来！',
    themeBg: '#F5F3FF',
    themeColor: '#6D28D9',
    defaultName: '瑞瑞',
    stages: [
      {
        rank: 1,
        name: '祥瑞小麒麟',
        stageTitle: '幼年期 · 萌新搭子',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_qilin_stage1.png',
        quote: '自律者自带祥瑞，今天也吃甜桃子～',
        desc: '天生灵秀的紫曜幼麟，踏云而生，带来健康与好运。'
      },
      {
        rank: 2,
        name: '踏云紫灵麟',
        stageTitle: '成长期 · 进阶神兽',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_qilin_stage2.png',
        quote: '犄角生辉、彩云流转，自律之光愈发耀眼！',
        desc: '进阶为踏云仙麟，水晶金角璀璨生辉，踏祥云而行！'
      },
      {
        rank: 3,
        name: '乾坤紫曜麒麟圣皇',
        stageTitle: '究极体 · 传奇守护神',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_qilin_stage3.png',
        quote: '星河为鬃、神龙为甲！以至高祥瑞守护每一位坚持自律的勇者！',
        desc: '顶级麒麟圣皇！身穿真龙金甲，鬃毛如星云银河流动，至高无上！'
      }
    ]
  }
};

Page({
  data: {
    loading: true,
    petSystemEnabled: true,
    hasPet: false,
    pet: null,
    selectedType: 'DRAGON',
    currentTypeInfo: TYPE_CONFIG['DRAGON'],
    petName: '木木',
    adopting: false,
    isFeeding: false,
    isTouched: false,
    heartAnim: false,
    foodIcon: '🍎',

    // 破壳孵化仪式状态
    hatchingStep: 0,

    // 3 阶形态进化系统
    petStageRank: 1,
    currentStageInfo: null,
    activePetImage: '/images/pets/pet_dragon_stage1.png',
    nextStageGoalText: 'Lv.5 解锁成长期形态',
    petStageProgressText: '1/5',
    showEvolutionModal: false,
    showDexModal: false,

    // 🌟 AI 动态交互与自律树洞
    aiThinking: false,
    petDialogue: '',
    chatInputText: '',

    // 勋章馆
    showBadgeModal: false,
    selectedBadge: null,
    unlockedBadgeCount: 1,
    badgeList: [
      { id: 'hatch', icon: '🥚', name: '破壳启航', req: '领养搭子', desc: '成功孵化破壳属于你的第一只 3D 自律神兽！', unlocked: true },
      { id: 'streak_7', icon: '🔥', name: '自律之星', req: '连续 7 天', desc: '连续陪伴打卡满 7 天，养成自律生活好习惯！', unlocked: false },
      { id: 'feed_20', icon: '🥣', name: '合格铲屎官', req: '投喂 20 次', desc: '累计为搭子投喂 20 次营养健康餐，爱意满满！', unlocked: false },
      { id: 'calorie_5k', icon: '🏃', name: '燃脂达人', req: '消耗 5000kcal', desc: '通过自律运动累计为身体燃脂 5000 大卡！', unlocked: false },
      { id: 'evo_stage2', icon: '👑', name: '神兽进阶', req: '达到 Lv.5', desc: '搭子成长蜕变，成功解锁成长期高阶神兽形态！', unlocked: false },
      { id: 'evo_stage3', icon: '🌟', name: '传奇守护神', req: '达到 Lv.10', desc: '搭子达成 Lv.10 究极进化，身披祥瑞光芒！', unlocked: false }
    ],

    // 每日赚粮任务状态
    foodTasks: {
      checkin: false,
      exercise: false,
      diet: false,
      water: false,
      weight: false
    },
    earnedFoodCount: 0,

    candidateNames: ['木木', '小燃', '豆豆', '卡卡', '饭团'],
    types: [
      { type: 'DRAGON', icon: '🐉', eggEmoji: '🟢', name: '小幼龙' },
      { type: 'TOTORO', icon: '🍃', eggEmoji: '⚪', name: '大龙猫' },
      { type: 'CAT', icon: '🐱', eggEmoji: '🟡', name: '元气猫' },
      { type: 'DOG', icon: '🐶', eggEmoji: '🟤', name: '自律狗' },
      { type: 'QILIN', icon: '✨', eggEmoji: '🟣', name: '小麒麟' }
    ]
  },

  onLoad(options) {
    this.checkToggleAndLoad();
  },

  onShow() {
    this.checkToggleAndLoad();
    this.updateCustomTabBar();
    this.fetchFoodTasks();
  },

  onPullDownRefresh() {
    this.checkToggleAndLoad(() => {
      this.fetchFoodTasks();
      wx.stopPullDownRefresh();
    });
  },

  onRefreshPage() {
    this.checkToggleAndLoad();
    this.fetchFoodTasks();
  },

  updateCustomTabBar() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateTabs('pages/pet/pet', (app && app.globalData && app.globalData.features));
    }
  },

  checkToggleAndLoad(callback) {
    let env = 'release';
    try {
      const accountInfo = wx.getAccountInfoSync();
      env = (accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.envVersion) || 'release';
    } catch (e) {}

    wx.request({
      url: `${app.globalData.baseUrl}/config/features?env=${env}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const enabled = res.data.data.pet_system !== false;
          this.setData({ petSystemEnabled: enabled });
          if (app.globalData) { app.globalData.features = res.data.data; }
          this.updateCustomTabBar();
          if (!enabled) {
            this.setData({ loading: false });
            if (callback) callback();
            return;
          }
        }
        app.login((user) => {
          this.fetchPetInfo(callback);
        });
      },
      fail: (err) => {
        app.login((user) => {
          this.fetchPetInfo(callback);
        });
      }
    });
  },

  fetchPetInfo(callback) {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      this.setData({ loading: false });
      if (callback) callback();
      return;
    }

    wx.request({
      url: `${app.globalData.baseUrl}/pet/my?userId=${user.id}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          this.setData({
            hasPet: true,
            pet: pet,
            petDialogue: pet.dialogue || '',
            currentTypeInfo: info,
            foodIcon: info.food || '🍎',
            loading: false
          });
          this.calculateEvolutionAndBadges(pet);
        } else if (res.data && res.data.code === 403) {
          this.setData({
            petSystemEnabled: false,
            loading: false
          });
        } else {
          this.setData({
            hasPet: false,
            pet: null,
            loading: false
          });
        }
      },
      fail: (err) => {
        console.error('Fetch pet failed', err);
        this.setData({ loading: false });
      },
      complete: () => {
        if (callback) callback();
      }
    });
  },

  /* 查询今日赚粮任务进度 */
  fetchFoodTasks() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) return;

    wx.request({
      url: `${app.globalData.baseUrl}/pet/food-tasks?userId=${user.id}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const tasks = res.data.data.tasks || {};
          let count = 0;
          Object.values(tasks).forEach(v => { if (v) count++; });
          this.setData({
            foodTasks: tasks,
            earnedFoodCount: count
          });
        }
      }
    });
  },

  /* 每日一键签到 */
  onDailyCheckin() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '正在签到...' });
    wx.request({
      url: `${app.globalData.baseUrl}/pet/checkin?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          const data = res.data.data;
          wx.showToast({ title: data.message || '签到成功！', icon: 'none', duration: 2500 });
          wx.vibrateShort({ type: 'medium' });
          this.fetchPetInfo();
          this.fetchFoodTasks();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '签到失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  /* 计算 3 阶形态进化与成就勋章 */
  calculateEvolutionAndBadges(pet) {
    const lvl = pet.level || 1;
    const typeInfo = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
    const stages = typeInfo.stages || [];

    let rank = 1;
    let nextGoal = 'Lv.5 解锁成长期形态';
    let progressText = `${lvl}/5`;

    if (lvl >= 10) {
      rank = 3;
      nextGoal = '已达顶级究极形态 ✨';
      progressText = 'MAX';
    } else if (lvl >= 5) {
      rank = 2;
      nextGoal = 'Lv.10 解锁究极形态';
      progressText = `${lvl}/10`;
    }

    const currentStage = stages[rank - 1] || stages[0];

    const badges = [...this.data.badgeList];
    badges[0].unlocked = true;
    badges[1].unlocked = (pet.streakDays || 0) >= 7;
    badges[2].unlocked = (pet.intimacy || 0) >= 200;
    badges[3].unlocked = (pet.level || 1) >= 3;
    badges[4].unlocked = lvl >= 5;
    badges[5].unlocked = lvl >= 10;

    const count = badges.filter(b => b.unlocked).length;

    this.setData({
      petStageRank: rank,
      currentStageInfo: currentStage,
      activePetImage: currentStage.image,
      nextStageGoalText: nextGoal,
      petStageProgressText: progressText,
      badgeList: badges,
      unlockedBadgeCount: count
    });
  },

  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['DRAGON'];
    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      petName: info.defaultName,
      foodIcon: info.food || '🍎',
      hatchingStep: 0
    });
    wx.vibrateShort({ type: 'light' });
  },

  onTapEgg() {
    let nextStep = this.data.hatchingStep + 1;
    if (nextStep > 2) nextStep = 2;
    this.setData({ hatchingStep: nextStep });
    wx.vibrateShort({ type: 'medium' });
  },

  onInputName(e) {
    this.setData({ petName: e.detail.value });
  },

  onSelectCandidateName(e) {
    const name = e.currentTarget.dataset.name;
    this.setData({ petName: name });
    wx.vibrateShort({ type: 'light' });
  },

  onRandomName() {
    const candidates = ['木木', '小燃', '豆豆', '卡卡', '饭团', '元宝', '可乐', '泡泡', '嘟嘟'];
    const randomIdx = Math.floor(Math.random() * candidates.length);
    this.setData({ petName: candidates[randomIdx] });
    wx.vibrateShort({ type: 'light' });
  },

  onStartHatchCeremony() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const name = this.data.petName ? this.data.petName.trim() : '小搭子';
    if (!name) {
      wx.showToast({ title: '请为搭子起个名字', icon: 'none' });
      return;
    }

    this.setData({ adopting: true, hatchingStep: 1 });
    wx.vibrateShort({ type: 'medium' });

    setTimeout(() => {
      this.setData({ hatchingStep: 2 });
      wx.vibrateShort({ type: 'heavy' });
    }, 600);

    setTimeout(() => {
      this.submitAdopt(user.id, name);
    }, 1200);
  },

  submitAdopt(userId, name) {
    wx.request({
      url: `${app.globalData.baseUrl}/pet/create`,
      method: 'POST',
      data: {
        userId: userId,
        name: name,
        petType: this.data.selectedType,
        avatarUrl: this.data.currentTypeInfo.stages[0].image
      },
      success: (res) => {
        this.setData({ adopting: false });
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '破壳成功！🎉', icon: 'success' });
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          this.setData({
            hasPet: true,
            pet: pet,
            currentTypeInfo: info,
            foodIcon: info.food || '🍎',
            hatchingStep: 0
          });
          this.calculateEvolutionAndBadges(pet);
          this.fetchFoodTasks();
          wx.vibrateShort({ type: 'heavy' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '孵化失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ adopting: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  /* 立即投喂：检测升级与 3 阶形态进化 */
  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    if (this.data.pet.foodCount <= 0) {
      wx.showModal({
        title: '食物不足',
        content: '小家伙的饭碗空空啦！完成下方「赚粮任务」（每日签到/运动/饮食打卡）就能免费获得食物哦～',
        confirmText: '立即签到',
        cancelText: '稍后再说',
        success: (modalRes) => {
          if (modalRes.confirm && !this.data.foodTasks.checkin) {
            this.onDailyCheckin();
          }
        }
      });
      return;
    }

    const oldLevel = this.data.pet.level || 1;
    this.setData({ isFeeding: true });
    wx.vibrateShort({ type: 'medium' });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/feed?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const updated = res.data.data;
          const newLevel = updated.level || 1;

          this.setData({ pet: updated });
          this.calculateEvolutionAndBadges(updated);

          // 触发形态进化庆祝弹窗 (突破 Lv.5 或 Lv.10)
          if ((oldLevel < 5 && newLevel >= 5) || (oldLevel < 10 && newLevel >= 10)) {
            setTimeout(() => {
              this.setData({ showEvolutionModal: true });
              wx.vibrateShort({ type: 'heavy' });
            }, 600);
          } else {
            wx.showToast({ title: '投喂成功！+10 经验 ✨', icon: 'none' });
          }
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '投喂失败', icon: 'none' });
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      },
      complete: () => {
        setTimeout(() => {
          this.setData({ isFeeding: false });
        }, 1200);
      }
    });
  },

  /* 🌟 轻触抚摸：触发点击物理弹跳 + 异步 AI 动态拟人搭话 */
  onTapPet() {
    if (!this.data.pet || this.data.isFeeding) return;

    this.setData({
      isTouched: true,
      heartAnim: true
    });
    wx.vibrateShort({ type: 'light' });

    setTimeout(() => {
      this.setData({
        isTouched: false,
        heartAnim: false
      });
    }, 600);

    // 触发 AI 交互
    this.callAiInteraction('TOUCH', '');
  },

  /* 🎋 自律树洞：快捷倾诉胶囊点击 */
  onTapQuickPrompt(e) {
    const prompt = e.currentTarget.dataset.prompt;
    if (!prompt) return;
    this.callAiInteraction('CHAT', prompt);
  },

  onInputChatText(e) {
    this.setData({ chatInputText: e.detail.value });
  },

  /* 🎋 自律树洞：发送自定义消息 */
  onSendChatMessage() {
    const text = this.data.chatInputText ? this.data.chatInputText.trim() : '';
    if (!text) {
      wx.showToast({ title: '请输入你想对搭子说的话', icon: 'none' });
      return;
    }
    this.setData({ chatInputText: '' });
    this.callAiInteraction('CHAT', text);
  },

  /* 核心：调用后端 AI 动态拟人互动 API */
  callAiInteraction(actionType, userMessage) {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    this.setData({ aiThinking: true });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/interact`,
      method: 'POST',
      data: {
        userId: user.id,
        actionType: actionType,
        userMessage: userMessage
      },
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const vo = res.data.data;
          this.setData({
            petDialogue: vo.dialogue,
            aiThinking: false
          });
          wx.vibrateShort({ type: 'light' });
        } else {
          this.setData({ aiThinking: false });
        }
      },
      fail: (err) => {
        console.warn('AI interact request failed, using fallback', err);
        this.setData({ aiThinking: false });
      }
    });
  },

  /* 形态图鉴弹窗 */
  onOpenDexModal() {
    this.setData({ showDexModal: true });
  },

  onCloseDexModal() {
    this.setData({ showDexModal: false });
  },

  /* 勋章馆弹窗 */
  onOpenBadgeModal() {
    const first = this.data.badgeList[0];
    this.setData({
      showBadgeModal: true,
      selectedBadge: first
    });
  },

  onCloseBadgeModal() {
    this.setData({ showBadgeModal: false });
  },

  onTapBadgeItem(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({ selectedBadge: item });
    wx.vibrateShort({ type: 'light' });
  },

  onCloseEvolutionModal() {
    this.setData({ showEvolutionModal: false });
  },

  /* 任务快捷跳转 */
  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoDiet() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWater() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWeight() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); },
  noBubble() {}
});
