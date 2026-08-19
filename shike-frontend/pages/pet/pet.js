// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
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

const SCENE_CONFIG = {
  ROOM: {
    key: 'ROOM',
    icon: '🏡',
    name: '🌿 日式原木阳光小屋',
    shortName: '阳光小屋',
    image: '/images/scenes/scene_room.jpg',
    motto: '温馨客厅 · 铺上瑜伽垫一起自律打卡',
    waypoints: [
      { x: 48, y: 58 }, // 地毯中心
      { x: 32, y: 52 }, // 窗边绿植
      { x: 64, y: 50 }, // 沙发边
      { x: 68, y: 70 }  // 瑜伽垫
    ],
    foodBowlPos: { x: 74, y: 72 }
  },
  ISLAND: {
    key: 'ISLAND',
    icon: '☁️',
    name: '☁️ 云端仙境浮空岛',
    shortName: '仙境空岛',
    image: '/images/scenes/scene_island.jpg',
    motto: '奇幻空岛 · 沐浴云端阳光与花海',
    waypoints: [
      { x: 42, y: 44 }, // 花坡草坪
      { x: 54, y: 38 }, // 果树下
      { x: 66, y: 58 }, // 泉水边
      { x: 30, y: 68 }  // 石阶木桥
    ],
    foodBowlPos: { x: 32, y: 66 }
  },
  YARD: {
    key: 'YARD',
    icon: '☀️',
    name: '☀️ 阳光运动露台花园',
    shortName: '运动露台',
    image: '/images/scenes/scene_yard.jpg',
    motto: '活力庭院 · 跑步机与喷泉花园',
    waypoints: [
      { x: 58, y: 60 }, // 草坪中心
      { x: 32, y: 66 }, // 跑步机旁
      { x: 72, y: 52 }, // 喷泉花丛
      { x: 38, y: 50 }  // 砖石露台
    ],
    foodBowlPos: { x: 26, y: 56 }
  }
};

let lifeLoopTimer = null;

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
    heartAnim: false,
    foodIcon: '🍎',
    
    // 3D 沉浸式场景与活体漫步坐标系统
    currentScene: 'ROOM',
    currentSceneInfo: SCENE_CONFIG['ROOM'],
    sceneList: [SCENE_CONFIG['ROOM'], SCENE_CONFIG['ISLAND'], SCENE_CONFIG['YARD']],
    showSceneModal: false,
    
    petPosX: 48,
    petPosY: 58,
    facingRight: true,
    motionState: 'idle', // 'idle' | 'walking' | 'eating' | 'happy'
    motionStateText: '悠闲发呆',

    candidateNames: ['木木', '小燃', '豆豆', '卡卡', '饭团'],
    types: [
      { type: 'DRAGON', icon: '🐉', name: '小幼龙' },
      { type: 'TOTORO', icon: '🍃', name: '大龙猫' },
      { type: 'CAT', icon: '🐱', name: '元气猫' },
      { type: 'DOG', icon: '🐶', name: '自律狗' },
      { type: 'QILIN', icon: '✨', name: '小麒麟' }
    ]
  },

  onLoad(options) {
    const savedScene = wx.getStorageSync('user_pet_scene') || 'ROOM';
    const sceneInfo = SCENE_CONFIG[savedScene] || SCENE_CONFIG['ROOM'];
    this.setData({
      currentScene: savedScene,
      currentSceneInfo: sceneInfo,
      petPosX: sceneInfo.waypoints[0].x,
      petPosY: sceneInfo.waypoints[0].y
    });

    this.checkToggleAndLoad();
  },

  onShow() {
    this.checkToggleAndLoad();
    this.updateCustomTabBar();
    this.startLivingMotionLoop();
  },

  onHide() {
    this.stopLivingMotionLoop();
  },

  onUnload() {
    this.stopLivingMotionLoop();
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
            foodIcon: info.food || '🍎',
            loading: false
          });
          this.startLivingMotionLoop();
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

  /* ================= 3D 活体自主漫步与行为状态机 ================= */
  startLivingMotionLoop() {
    this.stopLivingMotionLoop();
    if (!this.data.hasPet) return;

    lifeLoopTimer = setInterval(() => {
      if (this.data.isFeeding || this.data.motionState === 'happy') return;

      const scene = this.data.currentSceneInfo;
      const waypoints = scene.waypoints || [];
      if (waypoints.length === 0) return;

      // 随机挑选下一个漫步路径点
      const nextIdx = Math.floor(Math.random() * waypoints.length);
      const targetPoint = waypoints[nextIdx];

      const currentX = this.data.petPosX;
      const facingRight = targetPoint.x >= currentX;

      // 切换为漫步状态并移动
      this.setData({
        facingRight: facingRight,
        motionState: 'walking',
        motionStateText: '悠闲漫步中 🚶',
        petPosX: targetPoint.x,
        petPosY: targetPoint.y
      });

      // 走动 1.8 秒后停下恢复待机
      setTimeout(() => {
        if (this.data.motionState === 'walking') {
          const idleTexts = ['东张西望 👀', '伸懒腰 🐱', '发呆晒太阳 ☀️', '摇摇尾巴 🐾'];
          const randomIdle = idleTexts[Math.floor(Math.random() * idleTexts.length)];
          this.setData({
            motionState: 'idle',
            motionStateText: randomIdle
          });
        }
      }, 1900);

    }, 8500); // 每 8.5 秒执行一次自主漫步
  },

  stopLivingMotionLoop() {
    if (lifeLoopTimer) {
      clearInterval(lifeLoopTimer);
      lifeLoopTimer = null;
    }
  },

  /* 点击场景地面引导宠物走动 */
  onTapSceneGround(e) {
    if (!this.data.hasPet || this.data.isFeeding) return;

    // 微信小程序点击坐标转百分比
    const query = wx.createSelectorQuery();
    query.select('.virtual-3d-world-scene').boundingClientRect(rect => {
      if (!rect) return;
      const touch = (e.touches && e.touches[0]) || e.detail;
      const clickX = touch.x || touch.clientX;
      const clickY = touch.y || touch.clientY;

      const relX = ((clickX - rect.left) / rect.width) * 100;
      const relY = ((clickY - rect.top) / rect.height) * 100;

      // 限制在安全地面活动范围 (20% ~ 80%)
      const clampedX = Math.max(22, Math.min(78, relX));
      const clampedY = Math.max(40, Math.min(76, relY));

      const facingRight = clampedX >= this.data.petPosX;

      this.setData({
        facingRight: facingRight,
        motionState: 'walking',
        motionStateText: '跑向新地点 🐾',
        petPosX: clampedX,
        petPosY: clampedY
      });

      wx.vibrateShort({ type: 'light' });

      setTimeout(() => {
        if (this.data.motionState === 'walking') {
          this.setData({
            motionState: 'idle',
            motionStateText: '好奇观察中'
          });
        }
      }, 1900);
    }).exec();
  },

  /* 场景切换 */
  onSelectScene(e) {
    const key = e.currentTarget.dataset.key;
    this.applyScene(key);
  },

  onSelectSceneFromModal(e) {
    const key = e.currentTarget.dataset.key;
    this.applyScene(key);
    this.onCloseSceneModal();
  },

  applyScene(key) {
    const sceneInfo = SCENE_CONFIG[key] || SCENE_CONFIG['ROOM'];
    wx.setStorageSync('user_pet_scene', key);
    this.setData({
      currentScene: key,
      currentSceneInfo: sceneInfo,
      petPosX: sceneInfo.waypoints[0].x,
      petPosY: sceneInfo.waypoints[0].y,
      motionState: 'idle',
      motionStateText: '到达新场景 ✨'
    });
    wx.vibrateShort({ type: 'medium' });
  },

  onOpenSceneModal() {
    this.setData({ showSceneModal: true });
  },

  onCloseSceneModal() {
    this.setData({ showSceneModal: false });
  },

  noBubble() {
    // 阻止冒泡
  },

  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['DRAGON'];
    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      petName: info.defaultName,
      foodIcon: info.food || '🍎'
    });
    wx.vibrateShort({ type: 'light' });
  },

  onInputName(e) {
    this.setData({
      petName: e.detail.value
    });
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

  onAdoptPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const name = this.data.petName ? this.data.petName.trim() : '小搭子';
    this.setData({ adopting: true });
    wx.showLoading({ title: '正在唤醒 3D 搭子...' });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/create`,
      method: 'POST',
      data: {
        userId: user.id,
        name: name,
        petType: this.data.selectedType,
        avatarUrl: this.data.currentTypeInfo.image
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ adopting: false });
        if (res.data && res.data.code === 200) {
          wx.showToast({
            title: '领养成功！🎉',
            icon: 'success'
          });
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          this.setData({
            hasPet: true,
            pet: pet,
            foodIcon: info.food || '🍎'
          });
          wx.vibrateShort({ type: 'medium' });
          this.startLivingMotionLoop();
        } else {
          wx.showToast({
            title: (res.data && res.data.message) || '领养失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        this.setData({ adopting: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  /* 立即投喂：跑向食盆 + 欢快咀嚼 */
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

    const bowlPos = this.data.currentSceneInfo.foodBowlPos || { x: 70, y: 70 };
    const facingRight = bowlPos.x >= this.data.petPosX;

    // 1. 跑向食盆
    this.setData({
      isFeeding: true,
      facingRight: facingRight,
      motionState: 'walking',
      motionStateText: '奔向食盆干饭 🥣',
      petPosX: bowlPos.x,
      petPosY: bowlPos.y
    });
    wx.vibrateShort({ type: 'medium' });

    // 2. 到达食盆后开启动态咀嚼
    setTimeout(() => {
      this.setData({
        motionState: 'eating',
        motionStateText: '大口嚼嚼嚼 😋'
      });
    }, 700);

    wx.request({
      url: `${app.globalData.baseUrl}/pet/feed?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const updated = res.data.data;
          this.setData({
            pet: updated
          });
          wx.showToast({
            title: '投喂成功！+10 经验 ✨',
            icon: 'none'
          });
        } else {
          wx.showToast({
            title: (res.data && res.data.message) || '投喂失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      },
      complete: () => {
        setTimeout(() => {
          this.setData({
            isFeeding: false,
            motionState: 'idle',
            motionStateText: '吃饱饱超满足 💖'
          });
        }, 2200);
      }
    });
  },

  /* 点击身体抚摸：原地蹦跳撒欢 */
  onTapPet() {
    if (!this.data.pet || this.data.isFeeding) return;

    this.setData({
      motionState: 'happy',
      motionStateText: '开心蹦跳撒欢 🎉',
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
        heartAnim: false,
        motionState: 'idle',
        motionStateText: '悠闲发呆'
      });
    }, 800);
  },

  onGoExercise() {
    wx.switchTab({
      url: '/pages/index/index'
    });
  },

  onGoHome() {
    wx.switchTab({
      url: '/pages/index/index'
    });
  }
});
