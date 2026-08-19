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
    image: '/images/pets/pet_dragon.png',
    food: '🍎',
    quote: '我不运动，小龙就没饭吃！',
    themeBg: '#ECFDF5',
    themeColor: '#047857',
    defaultName: '木木'
  },
  TOTORO: {
    type: 'TOTORO',
    icon: '🍃',
    eggEmoji: '⚪',
    eggName: '灵木龙猫之卵',
    name: '治愈大龙猫',
    tag: '温馨陪伴',
    image: '/images/pets/pet_totoro.png',
    food: '🥝',
    quote: '吃饱才有力气自律，记得按时吃减脂餐哦～',
    themeBg: '#F1F5F9',
    themeColor: '#334155',
    defaultName: '大龙猫'
  },
  CAT: {
    type: 'CAT',
    icon: '🐱',
    eggEmoji: '🟡',
    eggName: '元气灵猫之卵',
    name: '软萌元气猫',
    tag: '灵动轻盈',
    image: '/images/pets/pet_cat.png',
    food: '🍊',
    quote: '动作要轻盈，体态要挺拔，今天打卡超棒喵～',
    themeBg: '#FFF7ED',
    themeColor: '#C2410C',
    defaultName: '小橘'
  },
  DOG: {
    type: 'DOG',
    icon: '🐶',
    eggEmoji: '🟤',
    eggName: '忠义玄犬之卵',
    name: '忠诚自律狗',
    tag: '户外自律',
    image: '/images/pets/pet_dog.png',
    food: '🍓',
    quote: '主人快走！去公园跑两圈，今天的狗粮就有啦汪！',
    themeBg: '#FEF3C7',
    themeColor: '#B45309',
    defaultName: '旺财'
  },
  QILIN: {
    type: 'QILIN',
    icon: '✨',
    eggEmoji: '🟣',
    eggName: '祥瑞天麟之卵',
    name: '祥瑞小麒麟',
    tag: '祥瑞好运',
    image: '/images/pets/pet_qilin.png',
    food: '🍑',
    quote: '自律者自带祥瑞，坚持打卡，好身材和好运一起来！',
    themeBg: '#F5F3FF',
    themeColor: '#6D28D9',
    defaultName: '瑞瑞'
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
    hatchingStep: 0, // 0=未孵化, 1=轻微裂纹, 2=剧烈金光, 3=破壳诞生

    // 进化形态数据
    petStageRank: 1,       // 1=幼年期, 2=成长期, 3=究极体
    petStageName: '幼年期 · 萌新搭子',
    nextStageGoalText: 'Lv.5 解锁成长期形态',
    petStageProgressText: '1/5',
    showEvolutionModal: false,

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
  },

  onPullDownRefresh() {
    this.checkToggleAndLoad(() => {
      wx.stopPullDownRefresh();
    });
  },

  onRefreshPage() {
    this.checkToggleAndLoad();
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

  /* 计算进化阶段与成就勋章 */
  calculateEvolutionAndBadges(pet) {
    const lvl = pet.level || 1;
    let rank = 1;
    let name = '幼年期 · 萌新搭子';
    let nextGoal = 'Lv.5 解锁成长期形态';
    let progressText = `${lvl}/5`;

    if (lvl >= 10) {
      rank = 3;
      name = '究极体 · 传奇守护神';
      nextGoal = '已达顶级究极形态 ✨';
      progressText = 'MAX';
    } else if (lvl >= 5) {
      rank = 2;
      name = '成长期 · 进阶神兽';
      nextGoal = 'Lv.10 解锁究极形态';
      progressText = `${lvl}/10`;
    }

    // 勋章状态刷新
    const badges = [...this.data.badgeList];
    badges[0].unlocked = true; // 破壳
    badges[1].unlocked = (pet.streakDays || 0) >= 7;
    badges[2].unlocked = (pet.intimacy || 0) >= 200;
    badges[3].unlocked = (pet.level || 1) >= 3;
    badges[4].unlocked = lvl >= 5;
    badges[5].unlocked = lvl >= 10;

    const count = badges.filter(b => b.unlocked).length;

    this.setData({
      petStageRank: rank,
      petStageName: name,
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

  /* 开启神兽蛋破壳仪式 */
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

    // 播放 3 阶段破壳动画
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
        avatarUrl: this.data.currentTypeInfo.image
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

  /* 立即投喂：检测升级与进化 */
  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    if (this.data.pet.foodCount <= 0) {
      wx.showModal({
        title: '食物不足',
        content: '小家伙的饭碗空空啦！今天去完成一次运动打卡（快走/慢跑/力量等）就能免费带回食物哦～',
        confirmText: '去运动',
        cancelText: '稍后再说',
        success: (modalRes) => {
          if (modalRes.confirm) {
            this.onGoExercise();
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

          // 触发形态进化弹窗 (Lv.5 或 Lv.10)
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

  /* 轻触抚摸 */
  onTapPet() {
    if (!this.data.pet || this.data.isFeeding) return;

    this.setData({
      isTouched: true,
      heartAnim: true
    });
    wx.vibrateShort({ type: 'light' });

    const quotes = [
      '吃饱饱，今天陪你一起燃脂！💪',
      '我不运动，小家伙就没饭吃啦！快走两圈～🏃',
      '自律最酷啦，今天也要一起加油哦！🔥',
      '少油少盐多喝水，体态越来越棒啦！💧',
      '你今天超自律！本搭子超级开心～✨',
      '今天又多消耗了卡路里，我们都在变强！🌟',
      '呼噜噜～摸摸头好舒服呀！❤️'
    ];

    const randomQuote = quotes[Math.floor(Math.random() * quotes.length)];
    this.setData({
      'pet.dialogue': randomQuote
    });

    setTimeout(() => {
      this.setData({
        isTouched: false,
        heartAnim: false
      });
    }, 600);
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

  noBubble() {},
  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); }
});
