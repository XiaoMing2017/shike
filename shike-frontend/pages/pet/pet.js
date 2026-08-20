// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🍃',
    eggEmoji: '🟢',
    eggName: '薄荷生机之卵',
    name: '薄荷小啾',
    tag: '元气燃脂',
    food: '🍎',
    quote: '今天每走1000步，小啾碗里就多添一颗甜苹果🍎～',
    themeBg: '#ECFDF5',
    themeColor: '#047857',
    defaultName: '木木',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 薄荷小啾',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dragon_stage1.png',
        quote: '圆滚滚的薄荷绿小肥啾，两颊粉扑扑，最爱吃红苹果！',
        desc: '软萌纯净的芬奇小肥啾，小巧可爱，每天陪你开启自律打卡！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 探险小啾',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dragon_stage2.png',
        quote: '戴上探险遮阳帽，背上小黄包，今天也要元气满满！',
        desc: '进阶为探险小啾，戴上遮阳帽背起行囊，探索更自律的自己！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 优雅小啾',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dragon_stage3.png',
        quote: '戴上法式贝雷帽与爱心暖围巾，做你永远最贴心的闺蜜！',
        desc: '终极形态！身披优雅法式贝雷帽与柔和围巾，温暖陪伴你的日常！'
      }
    ]
  },
  TOTORO: {
    type: 'TOTORO',
    icon: '🌾',
    eggEmoji: '⚪',
    eggName: '燕麦温润之卵',
    name: '燕麦小啾',
    tag: '治愈松弛',
    food: '🥝',
    quote: '吃饱睡好才是正经事，慢慢来，宝宝超棒的～',
    themeBg: '#F1F5F9',
    themeColor: '#334155',
    defaultName: '呼噜噜',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 燕麦小啾',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_totoro_stage1.png',
        quote: '软糯暖灰色的毛绒小啾，乖乖待在小窝里～',
        desc: '温润治愈的小肥啾，喜欢安静陪伴你度过每个自律时刻。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 森系小啾',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_totoro_stage2.png',
        quote: '戴上鼠尾草绿小帽，今天也要好好喝水、按时记餐哦！',
        desc: '进阶为森系小啾，清爽自然，守护你的饮食与作息！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 暖冬小啾',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_totoro_stage3.png',
        quote: '系上温暖明黄围巾，给你无条件的爱与情绪价值！',
        desc: '终极形态！戴上复古贝雷帽与暖冬围巾，带来满满的治愈感！'
      }
    ]
  },
  CAT: {
    type: 'CAT',
    icon: '🍑',
    eggEmoji: '🟡',
    eggName: '蜜桃元气之卵',
    name: '蜜桃小啾',
    tag: '轻盈体态',
    food: '🍊',
    quote: '动作要轻盈，体态要挺拔，今天也超级好看啾～',
    themeBg: '#FFF7ED',
    themeColor: '#C2410C',
    defaultName: '小橘',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 蜜桃小啾',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_cat_stage1.png',
        quote: '杏橙色软萌小啾，大眼睛眨呀眨求夸夸啾～',
        desc: '灵动可爱的小蜜桃，最懂女孩子的身材焦虑与体态美。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 草莓小啾',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_cat_stage2.png',
        quote: '戴上草莓遮阳帽，步态轻盈美美运动打卡啾！',
        desc: '进阶为草莓小啾，戴上甜美遮阳帽，活力加倍！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 雏菊小啾',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_cat_stage3.png',
        quote: '雏菊贝雷帽加身！做你专属的轻盈与优雅化身啾！',
        desc: '终极形态！戴上优雅雏菊贝雷帽与薰衣草小围巾，体态轻盈！'
      }
    ]
  },
  DOG: {
    type: 'DOG',
    icon: '☀️',
    eggEmoji: '🟤',
    eggName: '暖阳活力之卵',
    name: '暖阳小啾',
    tag: '活力户外',
    food: '🍓',
    quote: '扑棱小翅膀给主人充充电，随时陪你散步吹晚风啾！',
    themeBg: '#FEF3C7',
    themeColor: '#B45309',
    defaultName: '旺财',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 暖阳小啾',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dog_stage1.png',
        quote: '蜜糖金黄的小肥啾，精力充沛的小太阳！',
        desc: '阳光忠诚的小搭子，陪伴你度过每一个自律清晨。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 运动小啾',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dog_stage2.png',
        quote: '戴上运动遮阳帽，陪你跑出满满的多巴胺啾！',
        desc: '进阶为运动小啾，戴上遮阳帽背起小包，活力满满！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 暖阳守护啾',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dog_stage3.png',
        quote: '红白围巾加身，做永远守护你、给你无限正能量的小太阳！',
        desc: '终极形态！身披暖阳贝雷帽与热烈围巾，给你源源不断的动力！'
      }
    ]
  },
  QILIN: {
    type: 'QILIN',
    icon: '🌸',
    eggEmoji: '🟣',
    eggName: '粉樱甜梦之卵',
    name: '粉樱小啾',
    tag: '甜美治愈',
    food: '🍓',
    quote: '扑棱小翅膀甩掉烦恼，今天也是开心自律的一天🌸～',
    themeBg: '#FFF1F2',
    themeColor: '#E11D48',
    defaultName: '糖糖',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 粉樱小啾',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_qilin_stage1.png',
        quote: '圆滚滚的淡粉紫小肥啾，两颊粉扑扑超治愈！',
        desc: '甜美温柔的小肥啾，最喜欢陪你跳操、喝水与健康轻食！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 甜心小啾',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_qilin_stage2.png',
        quote: '戴上奶油黄遮阳帽，背上小黄包，今天也是元气满满！',
        desc: '进阶为甜心小啾，戴着明亮小帽子与小背包，充满活力！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 仙境小啾',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_qilin_stage3.png',
        quote: '粉樱贝雷帽加身！用无条件的爱守护你的身心健康！',
        desc: '终极形态！戴上粉樱法式贝雷帽与粉嫩爱心围巾，甜美至极！'
      }
    ]
  }
};

const POLAROID_QUOTES = [
  '今日份轻盈已到账 🍃',
  '慢慢来，每一次坚持都在发光 ✨',
  '自律是爱自己的最高形式 ❤️',
  '好好吃饭，好好喝水，好好生活 🌸',
  '今天也是体态轻盈、心情美好的一天 🌟',
  '和搭子一起变轻变好的日常 🍃'
];

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

    // 破壳仪式
    hatchingStep: 0,

    // 3 阶成长系统
    petStageRank: 1,
    currentStageInfo: null,
    activePetImage: '/images/pets/pet_dragon_stage1.png',
    nextStageGoalText: 'Lv.5 解锁元气陪伴形态',
    petStageProgressText: '1/5',
    showEvolutionModal: false,
    showDexModal: false,

    // 📸 自律拍立得小红书分享
    showPolaroidModal: false,
    polaroidQuote: '今日份轻盈已到账 🍃',
    polaroidDateText: '',

    // AI 动态交互
    aiThinking: false,
    petDialogue: '',

    // 勋章馆
    showBadgeModal: false,
    selectedBadge: null,
    unlockedBadgeCount: 1,
    badgeList: [
      { id: 'hatch', icon: '🥚', name: '破壳启航', req: '领养搭子', desc: '成功孵化唤醒属于你的第一只自律闺蜜搭子！', unlocked: true },
      { id: 'streak_7', icon: '🔥', name: '自律之星', req: '连续 7 天', desc: '连续陪伴打卡满 7 天，养成自律生活好习惯！', unlocked: false },
      { id: 'feed_20', icon: '🥣', name: '贴心投喂', req: '投喂 20 次', desc: '累计为搭子投喂 20 次健康零食，爱意满满！', unlocked: false },
      { id: 'calorie_5k', icon: '🏃', name: '燃脂达人', req: '消耗 5000kcal', desc: '通过自律运动累计为身体燃脂 5000 大卡！', unlocked: false },
      { id: 'evo_stage2', icon: '🌸', name: '元气陪伴', req: '达到 Lv.5', desc: '搭子成长蜕变，成功解锁元气陪伴高阶形态！', unlocked: false },
      { id: 'evo_stage3', icon: '💖', name: '蜕变闺蜜', req: '达到 Lv.10', desc: '搭子达成 Lv.10 蜕变闺蜜，身披光芒守护！', unlocked: false }
    ],

    // 每日赚粮任务
    foodTasks: {
      checkin: false,
      exercise: false,
      diet: false,
      water: false,
      weight: false
    },
    earnedFoodCount: 0,

    candidateNames: ['木木', '小燃', '豆豆', '卡卡', '饭团', '泡泡', '嘟嘟'],
        types: [
      { type: 'DRAGON', icon: '🍃', eggEmoji: '🟢', name: '薄荷小啾' },
      { type: 'TOTORO', icon: '🌾', eggEmoji: '⚪', name: '燕麦小啾' },
      { type: 'CAT', icon: '🍑', eggEmoji: '🟡', name: '蜜桃小啾' },
      { type: 'DOG', icon: '☀️', eggEmoji: '🟤', name: '暖阳小啾' },
      { type: 'QILIN', icon: '🌸', eggEmoji: '🟣', name: '粉樱小啾' }
    ]
  },

  onLoad(options) {
    this.checkToggleAndLoad();
    this.initPolaroidDate();
  },

  onShow() {
    this.checkToggleAndLoad();
    this.updateCustomTabBar();
    this.fetchFoodTasks();
  },

  initPolaroidDate() {
    const d = new Date();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    this.setData({ polaroidDateText: `${m}月${day}日` });
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

  calculateEvolutionAndBadges(pet) {
    const lvl = pet.level || 1;
    const typeInfo = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
    const stages = typeInfo.stages || [];

    let rank = 1;
    let nextGoal = 'Lv.5 解锁元气陪伴形态';
    let progressText = `${lvl}/5`;

    if (lvl >= 10) {
      rank = 3;
      nextGoal = '已达终极蜕变闺蜜 ✨';
      progressText = 'MAX';
    } else if (lvl >= 5) {
      rank = 2;
      nextGoal = 'Lv.10 解锁蜕变闺蜜形态';
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

  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    if (this.data.pet.foodCount <= 0) {
      wx.showModal({
        title: '零食袋空空啦',
        content: '小家伙的零食袋空空啦！完成下方「赚粮任务」（运动/记餐/喝水打卡）就能免费获得食物哦～',
        confirmText: '去签到',
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

          if ((oldLevel < 5 && newLevel >= 5) || (oldLevel < 10 && newLevel >= 10)) {
            setTimeout(() => {
              this.setData({ showEvolutionModal: true });
              wx.vibrateShort({ type: 'heavy' });
            }, 600);
          } else {
            wx.showToast({ title: '投喂成功！+10 能量 ✨', icon: 'none' });
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

    this.callAiInteraction('TOUCH', '');
  },

  onTapQuickPrompt(e) {
    const prompt = e.currentTarget.dataset.prompt;
    if (!prompt) return;
    this.callAiInteraction('CHAT', prompt);
  },

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

  /* 📸 自律拍立得小红书分享 */
  onOpenPolaroidModal() {
    const randomQuote = POLAROID_QUOTES[Math.floor(Math.random() * POLAROID_QUOTES.length)];
    this.setData({
      showPolaroidModal: true,
      polaroidQuote: randomQuote
    });
    wx.vibrateShort({ type: 'medium' });
  },

  onClosePolaroidModal() {
    this.setData({ showPolaroidModal: false });
  },

  onSavePolaroidToAlbum() {
    wx.showLoading({ title: '正在生成拍立得...' });
    setTimeout(() => {
      wx.hideLoading();
      wx.showModal({
        title: '📸 拍立得已就绪',
        content: '拍立得卡片已生成！您可以直接截图保存，分享至小红书或微信朋友圈，晒出你的松弛感自律日常～✨',
        showCancel: false,
        confirmText: '知道啦 💖'
      });
      wx.vibrateShort({ type: 'heavy' });
    }, 600);
  },

  onOpenDexModal() {
    this.setData({ showDexModal: true });
  },

  onCloseDexModal() {
    this.setData({ showDexModal: false });
  },

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

  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoDiet() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWater() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWeight() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); },
  noBubble() {}
});
