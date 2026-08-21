// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
    eggEmoji: '🟢',
    eggName: '青玉龙灵之卵',
    name: '木木小龙',
    tag: '燃脂闺蜜',
    food: '🍎',
    quote: '今天每走1000步，小龙饭碗里就多添一颗甜苹果🍎～',
    themeBg: '#ECFDF5',
    themeColor: '#047857',
    defaultName: '木木',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 木木',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dragon_stage1.png',
        quote: '薄荷绿软萌小奶龙抱苹果，圆滚滚超治愈！',
        desc: '初始软萌形态，小巧可爱，每天陪你开启运动燃脂与轻盈生活！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 碧霄灵龙',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dragon_stage2.png',
        quote: '粉色蝴蝶结发带+小背包，今天也要美美冲鸭！',
        desc: '进阶为元气灵龙，长出漂亮的翡翠龙角与修长羽翼，活力满满！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 青天应龙',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dragon_stage3.png',
        quote: '手绘樱花花环+手捧热香茶，做你永远最贴心的自律闺蜜！',
        desc: '终极形态！身披紫青流云与金辉光芒，守护你的好体态！'
      }
    ]
  },
  TOTORO: {
    type: 'TOTORO',
    icon: '🍃',
    eggEmoji: '⚪',
    eggName: '灵木龙猫之卵',
    name: '呼噜龙猫',
    tag: '治愈松弛',
    food: '🥝',
    quote: '吃饱睡好才是正经事，慢慢来，宝宝超棒的～',
    themeBg: '#F1F5F9',
    themeColor: '#334155',
    defaultName: '呼噜噜',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 龙猫仔',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_totoro_stage1.png',
        quote: '毛茸茸圆球小兽捧星星，抱着新鲜奇异果打盹～',
        desc: '纯真可爱的小龙猫，最喜欢趴在草坡上晒太阳打呼噜。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 森林使者',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_totoro_stage2.png',
        quote: '绿叶小斗篷+雨靴+四叶草，守护你的好心情！',
        desc: '进阶为森林守护者，身披橡木绿叶斗篷，充满治愈力量！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 治愈神鹿',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_totoro_stage3.png',
        quote: '浆果贝雷帽+爱心毛衣+暖灯，给你无条件的爱！',
        desc: '终极形态！头戴浆果贝雷帽，手提温暖小灯，庇佑你的身心平衡！'
      }
    ]
  },
  CAT: {
    type: 'CAT',
    icon: '🐱',
    eggEmoji: '🟡',
    eggName: '元气灵猫之卵',
    name: '元气小橘',
    tag: '轻盈体态',
    food: '🍊',
    quote: '动作要轻盈，体态要挺拔，今天也超级美喵～',
    themeBg: '#FFF7ED',
    themeColor: '#C2410C',
    defaultName: '小橘',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 奶橘',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_cat_stage1.png',
        quote: '奶橘圆球大眼萌猫抱橙子，满地打滚求摸头喵～',
        desc: '活泼好动的小猫咪，最懂女孩子的身材焦虑与体态美。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 灵猫使',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_cat_stage2.png',
        quote: '草莓遮阳帽+小红铃铛，步态如风轻盈美美喵！',
        desc: '进阶为赤焰灵猫，系上赤红小铃铛，周身环绕金色星火！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 天焰金猫',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_cat_stage3.png',
        quote: '雏菊贝雷帽+小围巾+水壶，做你专属的优雅化身喵！',
        desc: '终极形态！戴上优雅雏菊贝雷帽与小围巾，体态轻盈！'
      }
    ]
  },
  DOG: {
    type: 'DOG',
    icon: '🐶',
    eggEmoji: '🟤',
    eggName: '忠义玄犬之卵',
    name: '旺财柴柴',
    tag: '户外元气',
    food: '🍓',
    quote: '甩甩尾巴给主人充充电，随时陪你散步吹晚风汪！',
    themeBg: '#FEF3C7',
    themeColor: '#B45309',
    defaultName: '旺财',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 柴柴幼崽',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dog_stage1.png',
        quote: '吐舌憨萌柴犬宝宝抱草莓，最爱陪主人慢跑！',
        desc: '忠诚可爱的柴犬幼崽，陪伴你度过每一个自律清晨。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 疾风柴柴',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dog_stage2.png',
        quote: '红白格子领巾+运动跑鞋，陪你跑出满满的多巴胺汪！',
        desc: '进阶为健壮潇洒的疾风小猎犬，系着飘扬的红色领巾，奔跑如风！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 烈焰圣犬',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dog_stage3.png',
        quote: '花环头饰+金光闪闪奖章，做永远守护你的小太阳！',
        desc: '终极形态！身披暖阳花环与金色奖章，给你无限元气！'
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

const SCENE_LIST = [
  {
    id: 'island',
    name: '云端浮空仙岛',
    tag: '奇幻治愈',
    icon: '☁️',
    image: '/images/pets/scene_island_bg.jpg',
    desc: '漂浮在云海之上的梦幻仙境岛屿，花树清泉环绕。'
  },
  {
    id: 'room',
    name: '阳光原木小屋',
    tag: '日式温馨',
    icon: '🌿',
    image: '/images/pets/scene_room_bg.jpg',
    desc: '落地阳光窗、原木地板与软糯米白地毯，温馨治愈。'
  }
];

const ROOM_SPOTS = {
  RUG: {
    id: 'RUG',
    name: '软糯地毯',
    icon: '🧶',
    bottom: '22%',
    left: '50%',
    scale: 1.0,
    tag: '阳光小憩',
    quote: '坐在浮空岛软乎乎的草坪上晒太阳，感觉整个人都被治愈了～'
  },
  SOFA: {
    id: 'SOFA',
    name: '原木小桥',
    icon: '🌉',
    bottom: '28%',
    left: '68%',
    scale: 0.95,
    tag: '惬意漫步',
    quote: '坐在小木桥边吹吹微风，享受不被打扰的自律时光！'
  },
  FITNESS: {
    id: 'FITNESS',
    name: '石板小径',
    icon: '🧘',
    bottom: '18%',
    left: '38%',
    scale: 1.02,
    tag: '燃脂漫步',
    quote: '在小径上慢跑拉伸一下，多巴胺分泌满满，体态越来越轻盈！'
  },
  POND: {
    id: 'POND',
    name: '清泉瀑布',
    icon: '🌊',
    bottom: '36%',
    left: '42%',
    scale: 0.90,
    tag: '补水解渴',
    quote: '瀑布潺潺，清泉叮咚，今天也要喝足八杯水哦～'
  },
  FLOWER: {
    id: 'FLOWER',
    name: '盛开花丛',
    icon: '🌸',
    bottom: '25%',
    left: '26%',
    scale: 0.96,
    tag: '驻足闻花',
    quote: '走在花丛小道上，闻一闻粉红花朵的清香，心情大好！'
  }
};

Page({
  data: {
    // 🛋️ 2.5D 等轴测家具点位系统
    roomSpots: Object.values(ROOM_SPOTS),
    currentSpotId: 'RUG',
    currentSpot: ROOM_SPOTS['RUG'],
    isMovingSpot: false,

    // 🎨 3D 沉浸式场景切换系统
    sceneList: SCENE_LIST,
    currentSceneId: 'island',
    currentSceneBg: '/images/pets/scene_island_bg.jpg',
    showSceneModal: false,

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
      { type: 'DRAGON', icon: '🐉', eggEmoji: '🟢', name: '木木小龙' },
      { type: 'TOTORO', icon: '🍃', eggEmoji: '⚪', name: '呼噜龙猫' },
      { type: 'CAT', icon: '🐱', eggEmoji: '🟡', name: '元气小橘' },
      { type: 'DOG', icon: '🐶', eggEmoji: '🟤', name: '旺财柴柴' }
    ]
  },

  onLoad(options) {
    this.checkToggleAndLoad();
    this.initPolaroidDate();
    this.initSavedScene();
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

  onUnload() {
    if (this.world3D) {
      this.world3D.destroy();
      this.world3D = null;
    }
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
          this.syncSpotWithHabits(tasks);
        }
      }
    });
  },


  initSavedScene() {
    const saved = wx.getStorageSync('shike_pet_scene') || 'island';
    const found = SCENE_LIST.find(s => s.id === saved) || SCENE_LIST[0];
    this.setData({
      currentSceneId: found.id,
      currentSceneBg: found.image
    });
  },


  onSelectRoomSpot(e) {
    const spotId = e.currentTarget.dataset.id;
    if (spotId === this.data.currentSpotId || !ROOM_SPOTS[spotId]) return;

    const targetSpot = ROOM_SPOTS[spotId];
    this.setData({
      currentSpotId: spotId,
      currentSpot: targetSpot,
      petDialogue: targetSpot.quote
    });
    wx.vibrateShort({ type: 'medium' });

    if (this.world3D) {
      this.world3D.navigateToSpot(spotId);
    }
  },

  syncSpotWithHabits(tasks) {
    const hour = new Date().getHours();
    let targetId = 'RUG';

    if (hour >= 22 || hour < 7) {
      targetId = 'SOFA'; // 夜间在沙发休憩
    } else if (tasks && tasks.exercise) {
      targetId = 'FITNESS'; // 完成运动后在瑜伽垫
    } else if (tasks && tasks.diet) {
      targetId = 'DINING'; // 完成饮食记录后在轻食角
    } else if (hour >= 7 && hour <= 10) {
      targetId = 'WINDOW'; // 早晨在阳光窗边
    }

    if (ROOM_SPOTS[targetId]) {
      this.setData({
        currentSpotId: targetId,
        currentSpot: ROOM_SPOTS[targetId],
        petDialogue: ROOM_SPOTS[targetId].quote
      });
    }
  },


  

  onOpenSceneModal() {
    this.setData({ showSceneModal: true });
    wx.vibrateShort({ type: 'light' });
  },

  onCloseSceneModal() {
    this.setData({ showSceneModal: false });
  },

  onSelectScene(e) {
    const sceneId = e.currentTarget.dataset.id;
    const found = SCENE_LIST.find(s => s.id === sceneId);
    if (!found) return;

    this.setData({
      currentSceneId: found.id,
      currentSceneBg: found.image,
      showSceneModal: false
    });
    wx.setStorageSync('shike_pet_scene', found.id);
    wx.vibrateShort({ type: 'medium' });
    wx.showToast({ title: `已切换至「${found.name}」✨`, icon: 'none', duration: 2000 });
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
      petName: info.defaultName || '小搭子',
      foodIcon: info.food || '🍎',
      hatchingStep: 0
    });
    wx.vibrateShort({ type: 'medium' });
  },

  onTapPreviewPet() {
    wx.vibrateShort({ type: 'light' });
    wx.showToast({ title: '准备好领养我了吗？💖', icon: 'none' });
  },

  onDirectAdopt() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const name = this.data.petName ? this.data.petName.trim() : (this.data.currentTypeInfo.defaultName || '小搭子');
    this.setData({ adopting: true });
    wx.vibrateShort({ type: 'medium' });
    this.submitAdopt(user.id, name);
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
          wx.showToast({ title: '领养成功！🎉', icon: 'success' });
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
    if (this.world3D) this.world3D.triggerPetFeed();
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
    wx.vibrateShort({ type: 'medium' });

    setTimeout(() => {
      this.setData({
        isTouched: false,
        heartAnim: false
      });
    }, 850);

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
    // 🛋️ 2.5D 等轴测家具点位系统
    roomSpots: Object.values(ROOM_SPOTS),
    currentSpotId: 'GARDEN',
    currentSpot: ROOM_SPOTS['GARDEN'],
    isMovingSpot: false,

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
