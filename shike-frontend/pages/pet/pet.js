// pages/pet/pet.js — 《Pocket Love》风格 2.5D 微缩自律生活系统
const app = getApp();

const TYPE_CONFIG = {
  BUNNY: {
    type: 'BUNNY',
    icon: '🐰',
    eggEmoji: '🌸',
    eggName: '灵花萌兔之卵',
    name: '棉花糖兔',
    tag: '治愈甜心',
    food: '🥕',
    quote: '在露台草坪上晒晒太阳闻闻花香，今天也要开开心心自律哦～🌸',
    themeBg: '#FFF1F2',
    themeColor: '#E11D48',
    defaultName: '糖糖',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 糖糖',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/sprite_bunny.png',
        quote: '雪白软萌长耳兔，在小花园草坪上蹦蹦跳跳超治愈！',
        desc: '软萌雪白的小兔子，两只长耳朵会灵动摇晃，最喜欢在小花园晒太阳。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 花环灵兔',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/sprite_bunny.png',
        quote: '粉色小花环+小斜挎包，今天也是轻盈元气美少女！',
        desc: '进阶为花环灵兔，头戴粉嫩花环，活泼可爱！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 心愿仙兔',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/sprite_bunny.png',
        quote: '星芒灵羽+手捧热香茶，永远陪你健康轻盈！',
        desc: '终极形态！身披柔和光芒，做你一辈子的贴心自律闺蜜！'
      }
    ]
  },
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
    quote: '吃饱睡好才是正经事，慢慢来，宝宝超棒的～🍃',
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
    quote: '动作要轻盈，体态要挺拔，今天也超级美喵～🍊',
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
    quote: '甩甩尾巴给主人充充电，随时陪你散步吹晚风汪！🍓',
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
  '慢慢来，每一次自律坚持都在发光 ✨',
  '自律是爱自己的最高形式 ❤️',
  '好好吃饭，好好喝水，好好生活 🌸',
  '今天也是体态轻盈、心情美好的一天 🌟',
  '和搭子一起变轻变好的日常 🍃'
];

const SCENE_LIST = [
  {
    id: 'diorama',
    name: '2.5D 治愈微缩小屋',
    tag: '手绘等轴测微缩',
    icon: '🏡',
    image: '/images/pets/scene_cozy_diorama_clean.jpg',
    nightImage: '/images/pets/scene_cozy_diorama_clean_night.jpg',
    desc: '沙发阅读角、复古木雕大床、绿意小花园与黑胶唱机，温馨满分。'
  },
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

// 🛋️ 《Pocket Love》风格 6 大打卡驱动生活点位
const ROOM_SPOTS = {
  GARDEN: {
    id: 'GARDEN',
    name: '露台小花园',
    icon: '🪴',
    bottom: '22%',
    left: '36%',
    scale: 1.05,
    tag: '跳绳燃脂',
    habitType: 'EXERCISE',
    actionState: 'EXERCISING',
    emoteBadge: '🔥 露台跳绳中',
    quote: '刚陪你做完燃脂运动，露台跳绳出出汗太爽啦！身体超轻盈！🏃‍♀️🔥'
  },
  SOFA: {
    id: 'SOFA',
    name: '客厅软沙发',
    icon: '🛋️',
    bottom: '39%',
    left: '24%',
    scale: 0.95,
    tag: '享用轻食',
    habitType: 'DIET',
    actionState: 'EATING_MEAL',
    emoteBadge: '🥗 享用轻食中',
    quote: '健康减脂餐好好吃！按时吃饭才是维持好体态的秘诀呢～🥗✨'
  },
  WATER_BAR: {
    id: 'WATER_BAR',
    name: '水吧补水角',
    icon: '💧',
    bottom: '46%',
    left: '44%',
    scale: 0.90,
    tag: '咕嘟喝水',
    habitType: 'WATER',
    actionState: 'DRINKING_WATER',
    emoteBadge: '💧 健康补水中',
    quote: '喝足 8 大杯温水，皮肤水嫩嫩，代谢加速全靠它啦！💧🌸'
  },
  BED: {
    id: 'BED',
    name: '雕花暖被窝',
    icon: '🛏️',
    bottom: '36%',
    left: '73%',
    scale: 0.96,
    tag: '早睡安眠',
    habitType: 'SLEEP',
    actionState: 'DEEP_SLEEP',
    emoteBadge: '🌙 甜美安睡中',
    quote: '晚安宝宝，美容觉时间到，今晚也要做个甜甜的好梦 🌙💤'
  },
  DESK: {
    id: 'DESK',
    name: '专注学习书桌',
    icon: '📝',
    bottom: '52%',
    left: '60%',
    scale: 0.88,
    tag: '手账打卡',
    habitType: 'CHECKIN',
    actionState: 'STUDYING',
    emoteBadge: '✍️ 专注打卡中',
    quote: '坐在书桌前整理手账，今天的小目标逐个搞定超有成就感！✍️✨'
  },
  DRESSER: {
    id: 'DRESSER',
    name: '梳妆穿衣镜',
    icon: '🪞',
    bottom: '55%',
    left: '32%',
    scale: 0.86,
    tag: '身材管理',
    habitType: 'WEIGHT',
    actionState: 'CHECKING_MIRROR',
    emoteBadge: '🪞 体态管理中',
    quote: '对着大穿衣镜伸个懒腰，晨起记录体重，腰线越来越美啦！🪞💖'
  }
};

Page({
  data: {
    // 🛋️ 2.5D 等轴测家具点位与活体动画状态
    roomSpots: Object.values(ROOM_SPOTS),
    currentSpotId: 'GARDEN',
    currentSpot: ROOM_SPOTS['GARDEN'],
    isMovingSpot: false,
    petFacing: 'right', // 'left' | 'right'
    petAnimState: 'EXERCISING',
    showDustPuff: false,

    // 🎨 场景切换系统
    sceneList: SCENE_LIST,
    currentSceneId: 'diorama',
    currentSceneBg: '/images/pets/scene_cozy_diorama_clean.jpg',
    showSceneModal: false,

    // ☀️ 昼夜与光照系统 ('DAY' | 'SUNSET' | 'NIGHT')
    lightingMode: 'DAY',

    // 🎵 黑胶唱片机控制
    isPlayingMusic: false,
    currentSongName: 'Flower',

    loading: true,
    petSystemEnabled: true,
    hasPet: false,
    pet: null,
    selectedType: 'BUNNY',
    currentTypeInfo: TYPE_CONFIG['BUNNY'],
    petName: '糖糖',
    adopting: false,
    isFeeding: false,
    isTouched: false,
    heartAnim: false,
    foodIcon: '🥕',

    // 破壳仪式
    hatchingStep: 0,

    // 3 阶成长系统
    petStageRank: 1,
    currentStageInfo: null,
    activePetImage: '/images/pets/sprite_bunny.png',
    nextStageGoalText: 'Lv.5 解锁元气陪伴形态',
    petStageProgressText: '1/5',
    showEvolutionModal: false,
    showDexModal: false,
    selectedDexType: 'BUNNY',
    selectedDexInfo: TYPE_CONFIG['BUNNY'],

    // 📸 拍立得瞬间相册
    showPolaroidModal: false,
    polaroidDate: '',
    polaroidQuote: '',
    polaroidSaving: false,

    // 🏆 自律勋章系统
    showBadgeModal: false,
    selectedBadge: null,
    badgeList: [
      { id: 'checkin_3', icon: '🌱', name: '自律发芽', req: '连续签到 3 天', desc: '迈出自律第一步，搭子陪你茁壮成长！', unlocked: true },
      { id: 'checkin_7', icon: '🌿', name: '习惯养成', req: '连续签到 7 天', desc: '坚持一周自律打卡，生活规律更轻盈！', unlocked: true },
      { id: 'checkin_21', icon: '🌳', name: '蜕变新生', req: '连续签到 21 天', desc: '21天自律形成潜意识，好体态伴你左右！', unlocked: false },
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

    candidateNames: ['糖糖', '木木', '小燃', '豆豆', '卡卡', '饭团', '泡泡', '嘟嘟'],
    types: [
      { type: 'BUNNY', icon: '🐰', eggEmoji: '🌸', name: '棉花糖兔' },
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

  onHide() {
    if (this._roamTimer) {
      clearInterval(this._roamTimer);
      this._roamTimer = null;
    }
  },

  onUnload() {
    if (this.audioCtx) {
      this.audioCtx.destroy();
      this.audioCtx = null;
    }
    if (this._roamTimer) {
      clearInterval(this._roamTimer);
      this._roamTimer = null;
    }
  },

  updateCustomTabBar() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 });
    }
  },

  initPolaroidDate() {
    const d = new Date();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
    this.setData({
      polaroidDate: `${m}月${day}日 ${week}`
    });
  },

  checkToggleAndLoad() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      this.setData({ loading: false });
      return;
    }
    this.fetchMyPet();
  },

  fetchMyPet(callback) {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      this.setData({ loading: false });
      return;
    }

    wx.request({
      url: `${app.globalData.baseUrl}/pet/my-pet?userId=${user.id}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const pet = res.data.data;
          this.setData({
            hasPet: true,
            pet: pet,
            selectedType: pet.petType || 'BUNNY',
            petName: pet.name || '糖糖',
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
    const saved = wx.getStorageSync('shike_pet_scene') || 'diorama';
    const found = SCENE_LIST.find(s => s.id === saved) || SCENE_LIST[0];
    this.setData({
      currentSceneId: found.id,
      currentSceneBg: found.image
    });
  },

  // ============ 🛋️ Pocket Love 平滑抛物线生活漫步 ============
  onSelectRoomSpot(e) {
    const spotId = e.currentTarget.dataset.id;
    this.moveToSpot(spotId);
  },

  moveToSpot(spotId) {
    if (spotId === this.data.currentSpotId || !ROOM_SPOTS[spotId] || this.data.isMovingSpot) return;

    const targetSpot = ROOM_SPOTS[spotId];
    const currSpot = this.data.currentSpot || ROOM_SPOTS['GARDEN'];
    
    // 计算移动朝向
    const currLeft = parseInt(currSpot.left) || 36;
    const targetLeft = parseInt(targetSpot.left) || 36;
    const facing = targetLeft >= currLeft ? 'right' : 'left';

    this.setData({
      isMovingSpot: true,
      showDustPuff: true,
      petFacing: facing,
      petAnimState: 'HOPPING',
      petDialogue: '一蹦一跳去' + targetSpot.name + '啦～🐾'
    });
    wx.vibrateShort({ type: 'medium' });

    setTimeout(() => {
      this.setData({
        currentSpotId: spotId,
        currentSpot: targetSpot,
        showDustPuff: false
      });
    }, 150);

    setTimeout(() => {
      this.setData({
        isMovingSpot: false,
        petAnimState: targetSpot.actionState || 'IDLE',
        petDialogue: targetSpot.quote
      });
    }, 650);
  },

  // 🌟 真实自律打卡数据驱动房间生活状态机
  syncSpotWithHabits(tasks) {
    const hour = new Date().getHours();
    let targetId = 'DESK'; // 默认在书桌整理手账

    if (hour >= 22 || hour < 7) {
      targetId = 'BED'; // 🌙 夜间自动钻进暖被窝早睡
    } else if (tasks && tasks.exercise) {
      targetId = 'GARDEN'; // 🏃 运动锻炼打卡 ➔ 露台跳绳燃脂
    } else if (tasks && tasks.diet) {
      targetId = 'SOFA'; // 🥗 记餐打卡 ➔ 客厅沙发享用轻食
    } else if (tasks && tasks.water) {
      targetId = 'WATER_BAR'; // 💧 喝水打卡 ➔ 水吧咕嘟补水
    } else if (tasks && tasks.weight) {
      targetId = 'DRESSER'; // 🪞 体重打卡 ➔ 梳妆穿衣镜前记录体态
    } else if (hour >= 8 && hour <= 18) {
      targetId = 'DESK'; // 📝 白天专注打卡
    }

    if (ROOM_SPOTS[targetId] && targetId !== this.data.currentSpotId) {
      this.moveToSpot(targetId);
    }
  },

  // ============ 🎵 治愈黑胶唱片机控制 ============
  onToggleMusicPlayer() {
    const nextState = !this.data.isPlayingMusic;
    this.setData({ isPlayingMusic: nextState });
    wx.vibrateShort({ type: 'light' });
    if (nextState) {
      wx.showToast({ title: '🎵 正在播放:《Flower》· 治愈 Lo-Fi', icon: 'none', duration: 2000 });
      if (!this.audioCtx) {
        this.audioCtx = wx.createInnerAudioContext();
        this.audioCtx.loop = true;
        this.audioCtx.src = 'https://assets.mixkit.co/music/preview/mixkit-dreaming-big-31.mp3';
      }
      this.audioCtx.play();
    } else {
      if (this.audioCtx) {
        this.audioCtx.pause();
      }
      wx.showToast({ title: '已暂停音乐 ⏸️', icon: 'none', duration: 1200 });
    }
  },

  // ============ ☀️ 昼夜光影切换 ============
  onToggleLightingMode() {
    const modes = ['DAY', 'SUNSET', 'NIGHT'];
    const currIdx = modes.indexOf(this.data.lightingMode || 'DAY');
    const nextMode = modes[(currIdx + 1) % modes.length];
    
    let dialogue = '阳光正好，元气满满！☀️';
    if (nextMode === 'SUNSET') dialogue = '晚霞染红了窗台，今天辛苦啦 🌅';
    if (nextMode === 'NIGHT') dialogue = '夜幕降临，床头暖灯已亮起，早点休息哦 🌙';

    const isNight = nextMode === 'NIGHT';
    const cleanBg = isNight 
      ? '/images/pets/scene_cozy_diorama_clean_night.jpg' 
      : '/images/pets/scene_cozy_diorama_clean.jpg';

    this.setData({
      lightingMode: nextMode,
      currentSceneBg: this.data.currentSceneId === 'diorama' ? cleanBg : this.data.currentSceneBg,
      petDialogue: dialogue
    });
    wx.vibrateShort({ type: 'light' });
    wx.showToast({ 
      title: nextMode === 'NIGHT' ? '🌙 夜晚暖光模式' : (nextMode === 'SUNSET' ? '🌅 黄金黄昏模式' : '☀️ 明媚日光模式'),
      icon: 'none',
      duration: 1800
    });
  },

  // ============ 🎨 场景选择弹窗 ============
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

    wx.setStorageSync('shike_pet_scene', sceneId);
    this.setData({
      currentSceneId: found.id,
      currentSceneBg: found.image,
      showSceneModal: false,
      petDialogue: `换到了新场景「${found.name}」，心情超好！🌸`
    });
    wx.vibrateShort({ type: 'medium' });
    wx.showToast({ title: `已换景：${found.name}`, icon: 'none' });
  },

  // ============ 📸 拍立得瞬间相册 ============
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

  onRefreshPolaroidQuote() {
    const randomQuote = POLAROID_QUOTES[Math.floor(Math.random() * POLAROID_QUOTES.length)];
    this.setData({ polaroidQuote: randomQuote });
    wx.vibrateShort({ type: 'light' });
  },

  onSavePolaroidCard() {
    this.setData({ polaroidSaving: true });
    setTimeout(() => {
      this.setData({ polaroidSaving: false });
      wx.showToast({ title: '📸 拍立得卡片已保存到相册！', icon: 'success' });
    }, 1000);
  },

  // ============ 💖 领养与喂养互动 ============
  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['BUNNY'];
    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      petName: info.defaultName,
      foodIcon: info.food
    });
    wx.vibrateShort({ type: 'light' });
  },

  onInputName(e) {
    this.setData({ petName: e.detail.value });
  },

  onPickCandidateName(e) {
    const name = e.currentTarget.dataset.name;
    this.setData({ petName: name });
    wx.vibrateShort({ type: 'light' });
  },

  onDirectAdopt() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const type = this.data.selectedType || 'BUNNY';
    const name = (this.data.petName || '').trim() || TYPE_CONFIG[type].defaultName;

    this.setData({ adopting: true });
    wx.request({
      url: `${app.globalData.baseUrl}/pet/adopt?userId=${user.id}&petType=${type}&petName=${encodeURIComponent(name)}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const newPet = res.data.data;
          this.setData({
            hasPet: true,
            pet: newPet,
            adopting: false
          });
          this.calculateEvolutionAndBadges(newPet);
          wx.showToast({ title: `恭喜领养 ${name} 💖`, icon: 'success' });
          wx.vibrateShort({ type: 'heavy' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '领养失败', icon: 'none' });
          this.setData({ adopting: false });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络异常', icon: 'none' });
        this.setData({ adopting: false });
      }
    });
  },

  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet || this.data.isFeeding) return;

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
      fail: () => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      },
      complete: () => {
        setTimeout(() => {
          this.setData({ isFeeding: false });
        }, 1200);
      }
    });
  },

  onDailyCheckin() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) return;

    wx.request({
      url: `${app.globalData.baseUrl}/pet/checkin?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '签到成功！粮仓+1 🍎', icon: 'success' });
          this.fetchMyPet();
          this.fetchFoodTasks();
        }
      }
    });
  },

  // 🌟 触碰互动：开心高跳与闺蜜对话
  onTapPet() {
    if (!this.data.pet || this.data.isFeeding || this.data.isMovingSpot) return;

    this.setData({
      isTouched: true,
      heartAnim: true,
      petAnimState: 'JOY'
    });
    wx.vibrateShort({ type: 'heavy' });

    setTimeout(() => {
      this.setData({
        isTouched: false,
        heartAnim: false,
        petAnimState: this.data.currentSpot.actionState || 'IDLE'
      });
    }, 950);

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
        actionType: actionType || 'TOUCH',
        userMessage: userMessage || '',
        petType: this.data.pet.petType,
        petLevel: this.data.pet.level,
        petMood: this.data.pet.mood
      },
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const resp = res.data.data;
          this.setData({
            petDialogue: resp.dialogue,
            aiThinking: false
          });
        } else {
          this.setData({ aiThinking: false });
        }
      },
      fail: () => {
        this.setData({ aiThinking: false });
      }
    });
  },

  calculateEvolutionAndBadges(pet) {
    const type = pet.petType || 'BUNNY';
    const cfg = TYPE_CONFIG[type] || TYPE_CONFIG['BUNNY'];
    const level = pet.level || 1;

    let rank = 1;
    let nextGoal = 'Lv.5 解锁元气陪伴形态';
    let progress = `${level}/5`;

    if (level >= 10) {
      rank = 3;
      nextGoal = '已达成终极形态 ✨';
      progress = 'MAX';
    } else if (level >= 5) {
      rank = 2;
      nextGoal = 'Lv.10 解锁蜕变闺蜜终极形态';
      progress = `${level}/10`;
    }

    const currentStage = cfg.stages[rank - 1] || cfg.stages[0];

    this.setData({
      petStageRank: rank,
      currentStageInfo: currentStage,
      activePetImage: currentStage.image,
      nextStageGoalText: nextGoal,
      petStageProgressText: progress,
      foodIcon: cfg.food
    });
  },

  // ============ 弹窗控制 ============
  onOpenDexModal() {
    const type = (this.data.pet && this.data.pet.petType) || this.data.selectedType || 'BUNNY';
    this.setData({
      showDexModal: true,
      selectedDexType: type,
      selectedDexInfo: TYPE_CONFIG[type]
    });
  },

  onSelectDexTab(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({
      selectedDexType: type,
      selectedDexInfo: TYPE_CONFIG[type]
    });
    wx.vibrateShort({ type: 'light' });
  },

  onCloseDexModal() {
    this.setData({ showDexModal: false });
  },

  onOpenBadgeModal() {
    this.setData({
      showBadgeModal: true,
      selectedBadge: this.data.badgeList[0]
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

  // 快捷自律打卡跳转
  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoDiet() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWater() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWeight() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); },
  noBubble() {}
});
