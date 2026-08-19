// pages/pet/pet.js
const app = getApp();
const NavMesh = require('./engine/NavMesh');
const NeedsEngine = require('./engine/NeedsEngine');
const SMART_OBJECTS = require('./engine/SmartObjects');
const UtilityAIDecision = require('./engine/UtilityAIDecision');
const ActionQueue = require('./engine/ActionQueue');

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
    name: '青玉小幼龙',
    tag: '燃脂蜕变',
    image: '/images/pets/pet_dragon.png',
    walkGif: '/images/pets/pet_dragon_walk.gif',
    idleGif: '/images/pets/pet_dragon_idle.gif',
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
    walkGif: '/images/pets/pet_totoro_walk.gif',
    idleGif: '/images/pets/pet_totoro_idle.gif',
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
    walkGif: '/images/pets/pet_cat_walk.gif',
    idleGif: '/images/pets/pet_cat_idle.gif',
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
    walkGif: '/images/pets/pet_dog_walk.gif',
    idleGif: '/images/pets/pet_dog_idle.gif',
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
    walkGif: '/images/pets/pet_qilin_walk.gif',
    idleGif: '/images/pets/pet_qilin_idle.gif',
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
    foodBowlPos: { x: 74, y: 72 }
  },
  ISLAND: {
    key: 'ISLAND',
    icon: '☁️',
    name: '☁️ 云端仙境浮空岛',
    shortName: '仙境空岛',
    image: '/images/scenes/scene_island.jpg',
    motto: '奇幻空岛 · 沐浴云端阳光与花海',
    foodBowlPos: { x: 32, y: 66 }
  },
  YARD: {
    key: 'YARD',
    icon: '☀️',
    name: '☀️ 阳光运动露台花园',
    shortName: '运动露台',
    image: '/images/scenes/scene_yard.jpg',
    motto: '活力庭院 · 跑步机与喷泉花园',
    foodBowlPos: { x: 26, y: 56 }
  }
};

// 引擎实例
let navMesh = null;
let needsEngine = null;
let aiDecision = null;
let actionQueue = null;
let lifeTicker = null;
let lastActionId = null;

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
    
    // 3D 沉浸式场景
    currentScene: 'ROOM',
    currentSceneInfo: SCENE_CONFIG['ROOM'],
    sceneList: [SCENE_CONFIG['ROOM'], SCENE_CONFIG['ISLAND'], SCENE_CONFIG['YARD']],
    showSceneModal: false,
    
    // 活体坐标与渲染状态
    petPosX: 48,
    petPosY: 58,
    depthScale: 1.0,
    facingRight: true,
    motionState: 'idle',
    activeSpriteUrl: '/images/pets/pet_dragon_idle.gif',
    currentThought: '舒服趴卧中',
    petDialogue: null,
    sleepBubble: false,
    needsMood: { mood: 'HAPPY', text: '超级开心 😄' },

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
    
    // 初始化引擎
    navMesh = new NavMesh(savedScene);
    needsEngine = new NeedsEngine();
    aiDecision = new UtilityAIDecision('DRAGON');
    actionQueue = new ActionQueue(navMesh, (event) => this.handleActionEvent(event));

    this.setData({
      currentScene: savedScene,
      currentSceneInfo: sceneInfo,
      petPosX: 48,
      petPosY: 58,
      depthScale: navMesh.calcDepthScale(58)
    });

    this.checkToggleAndLoad();
  },

  onShow() {
    this.checkToggleAndLoad();
    this.updateCustomTabBar();
    this.startLifeEngine();
  },

  onHide() {
    this.stopLifeEngine();
  },

  onUnload() {
    this.stopLifeEngine();
  },

  /* ================= 引擎事件调度 ================= */
  handleActionEvent(event) {
    const typeInfo = this.data.currentTypeInfo || TYPE_CONFIG['DRAGON'];

    switch (event.type) {
      case 'MOVE_TO_POINT': {
        const nextX = event.point.x;
        const nextY = event.point.y;
        const facingRight = nextX >= this.data.petPosX;

        this.setData({
          facingRight: facingRight,
          petPosX: nextX,
          petPosY: nextY,
          depthScale: event.depthScale,
          motionState: 'walking',
          activeSpriteUrl: typeInfo.walkGif,
          currentThought: event.thoughtText || '漫步中 🐾',
          sleepBubble: false
        });
        break;
      }

      case 'PLAY_ANIMATION': {
        const anim = event.animType;
        const isRest = anim === 'sleep' || anim === 'lie_down' || anim === 'idle';
        
        this.setData({
          motionState: anim,
          activeSpriteUrl: isRest ? typeInfo.idleGif : typeInfo.walkGif,
          petDialogue: event.dialogue || null,
          sleepBubble: !!event.bubble,
          currentThought: event.thoughtText || '享受当下 🌱'
        });

        if (event.dialogue) {
          setTimeout(() => {
            this.setData({ petDialogue: null });
          }, Math.min(event.duration, 4000));
        }
        break;
      }

      case 'JUMP_ANIMATION': {
        if (event.target) {
          const facingRight = event.target.x >= this.data.petPosX;
          this.setData({
            facingRight: facingRight,
            petPosX: event.target.x,
            petPosY: event.target.y,
            depthScale: event.depthScale || this.data.depthScale,
            motionState: 'happy',
            activeSpriteUrl: typeInfo.walkGif
          });
        }
        break;
      }

      case 'ACTION_FINISHED': {
        lastActionId = event.actionId;
        
        // 行为完成后更新内在需求
        if (event.actionId === 'sofa' || event.actionId === 'fruit_tree') {
          needsEngine.onSleep(10);
        } else if (event.actionId === 'plant' || event.actionId === 'window') {
          needsEngine.onExplore();
        } else if (event.actionId === 'rug' || event.actionId === 'yoga_mat' || event.actionId === 'lawn_center' || event.actionId === 'treadmill') {
          needsEngine.onPlay();
        } else if (event.actionId === 'food_bowl') {
          needsEngine.onEat();
        } else if (event.actionId === 'waterfall_pond' || event.actionId === 'fountain') {
          needsEngine.onDrink();
        }

        // 短暂停歇后触发下一次 AI 决策
        setTimeout(() => {
          this.triggerNextAIDecision();
        }, 2200);
        break;
      }
    }
  },

  /* 触发 Utility AI 下一次决策 */
  triggerNextAIDecision() {
    if (!this.data.hasPet || this.data.isFeeding) return;

    const objects = SMART_OBJECTS[this.data.currentScene] || SMART_OBJECTS['ROOM'];
    const nextObject = aiDecision.evaluateNextAction(needsEngine, objects, lastActionId);

    if (nextObject && actionQueue) {
      actionQueue.startActionSequence(nextObject, {
        x: this.data.petPosX,
        y: this.data.petPosY
      });
    }
  },

  /* 启动生命感知循环时钟 */
  startLifeEngine() {
    this.stopLifeEngine();
    if (!this.data.hasPet) return;

    // 1. 启动需求钟 (每秒 tick)
    lifeTicker = setInterval(() => {
      if (needsEngine) {
        needsEngine.tick(1);
        this.setData({
          needsMood: needsEngine.getMoodStatus()
        });
      }
    }, 1000);

    // 2. 启动首次自主决策
    setTimeout(() => {
      this.triggerNextAIDecision();
    }, 1500);
  },

  stopLifeEngine() {
    if (lifeTicker) {
      clearInterval(lifeTicker);
      lifeTicker = null;
    }
    if (actionQueue) {
      actionQueue.stopCurrent();
    }
  },

  /* 点击房间地面：引导宠物走过去 */
  onTapSceneGround(e) {
    if (!this.data.hasPet || this.data.isFeeding) return;

    const query = wx.createSelectorQuery();
    query.select('.virtual-3d-world-scene').boundingClientRect(rect => {
      if (!rect) return;
      const touch = (e.touches && e.touches[0]) || e.detail;
      const clickX = touch.x || touch.clientX;
      const clickY = touch.y || touch.clientY;

      const relX = ((clickX - rect.left) / rect.width) * 100;
      const relY = ((clickY - rect.top) / rect.height) * 100;

      // 通过 NavMesh 寻路走向点击位置
      const target = navMesh.findNearestWalkable(relX, relY);
      
      const customWalkAction = {
        id: 'tap_walk',
        thoughtText: '向你指的方向跑去 🐾',
        actionSteps: [
          { type: 'FAST_WALK', target: target },
          { type: 'LOOK_AROUND', duration: 2000, dialogue: '我到这里啦！有什么好玩的吗？✨' },
          { type: 'IDLE', duration: 1000 }
        ]
      };

      wx.vibrateShort({ type: 'light' });
      actionQueue.interruptWithPlayerAction(customWalkAction, {
        x: this.data.petPosX,
        y: this.data.petPosY
      });
    }).exec();
  },

  /* 点击宠物：轻触抚摸与高优先级打断 */
  onTapPet() {
    if (!this.data.pet || this.data.isFeeding) return;

    if (needsEngine) {
      needsEngine.onPat();
    }

    const quotes = [
      '呼噜噜～摸摸头好舒服呀！❤️',
      '吃饱饱，今天陪你一起燃脂！💪',
      '自律最酷啦，今天也要一起加油哦！🔥',
      '少油少盐多喝水，体态越来越棒啦！💧',
      '你今天超自律！本搭子超级开心～✨',
      '今天又多消耗了卡路里，我们都在变强！🌟'
    ];
    const randomQuote = quotes[Math.floor(Math.random() * quotes.length)];

    const patAction = {
      id: 'player_pat',
      thoughtText: '被主人温柔抚摸中 ❤️',
      actionSteps: [
        { type: 'HAPPY', duration: 1800, dialogue: randomQuote }
      ]
    };

    this.setData({ heartAnim: true });
    wx.vibrateShort({ type: 'medium' });

    actionQueue.interruptWithPlayerAction(patAction, {
      x: this.data.petPosX,
      y: this.data.petPosY
    });

    setTimeout(() => {
      this.setData({ heartAnim: false });
    }, 900);
  },

  /* 投喂干饭 */
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

    this.setData({ isFeeding: true });
    wx.vibrateShort({ type: 'medium' });

    const objects = SMART_OBJECTS[this.data.currentScene] || SMART_OBJECTS['ROOM'];
    const foodBowl = objects.food_bowl;

    // 打断当前行为，直奔食盆
    actionQueue.interruptWithPlayerAction(foodBowl, {
      x: this.data.petPosX,
      y: this.data.petPosY
    });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/feed?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const updated = res.data.data;
          this.setData({ pet: updated });
          wx.showToast({ title: '投喂成功！+10 经验 ✨', icon: 'none' });
        }
      },
      complete: () => {
        setTimeout(() => {
          this.setData({ isFeeding: false });
        }, 3000);
      }
    });
  },

  /* 切换场景 */
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
    
    if (navMesh) navMesh.setScene(key);
    const typeInfo = this.data.currentTypeInfo || TYPE_CONFIG['DRAGON'];

    this.setData({
      currentScene: key,
      currentSceneInfo: sceneInfo,
      petPosX: 48,
      petPosY: 58,
      depthScale: navMesh ? navMesh.calcDepthScale(58) : 1.0,
      motionState: 'idle',
      activeSpriteUrl: typeInfo.idleGif,
      currentThought: '到达新空间 探索中 ✨'
    });

    wx.vibrateShort({ type: 'medium' });
    this.triggerNextAIDecision();
  },

  onOpenSceneModal() { this.setData({ showSceneModal: true }); },
  onCloseSceneModal() { this.setData({ showSceneModal: false }); },
  noBubble() {},

  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['DRAGON'];
    if (aiDecision) aiDecision.setPetType(type);

    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      activeSpriteUrl: info.idleGif,
      petName: info.defaultName,
      foodIcon: info.food || '🍎'
    });
    wx.vibrateShort({ type: 'light' });
  },

  onInputName(e) { this.setData({ petName: e.detail.value }); },
  onSelectCandidateName(e) {
    this.setData({ petName: e.currentTarget.dataset.name });
    wx.vibrateShort({ type: 'light' });
  },
  onRandomName() {
    const candidates = ['木木', '小燃', '豆豆', '卡卡', '饭团', '元宝', '可乐', '泡泡', '嘟嘟'];
    this.setData({ petName: candidates[Math.floor(Math.random() * candidates.length)] });
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
    wx.showLoading({ title: '正在唤醒 3D 自主神兽...' });

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
          wx.showToast({ title: '领养成功！🎉', icon: 'success' });
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          if (aiDecision) aiDecision.setPetType(pet.petType);

          this.setData({
            hasPet: true,
            pet: pet,
            currentTypeInfo: info,
            activeSpriteUrl: info.idleGif,
            foodIcon: info.food || '🍎'
          });
          wx.vibrateShort({ type: 'medium' });
          this.startLifeEngine();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '领养失败', icon: 'none' });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        this.setData({ adopting: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
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
          if (aiDecision) aiDecision.setPetType(pet.petType);

          this.setData({
            hasPet: true,
            pet: pet,
            currentTypeInfo: info,
            activeSpriteUrl: info.idleGif,
            foodIcon: info.food || '🍎',
            loading: false
          });
          this.startLifeEngine();
        } else if (res.data && res.data.code === 403) {
          this.setData({ petSystemEnabled: false, loading: false });
        } else {
          this.setData({ hasPet: false, pet: null, loading: false });
        }
      },
      fail: (err) => {
        this.setData({ loading: false });
      },
      complete: () => {
        if (callback) callback();
      }
    });
  },

  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); }
});
