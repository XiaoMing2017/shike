// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
    name: '青玉小幼龙',
    tag: '燃脂蜕变',
    image: '/images/pets/pet_dragon.png',
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
    quote: '自律者自带祥瑞，坚持打卡，好身材和好运一起来！',
    themeBg: '#F5F3FF',
    themeColor: '#6D28D9',
    defaultName: '瑞瑞'
  }
};

Page({
  data: {
    loading: true,
    hasPet: false,
    pet: null,
    selectedType: 'DRAGON',
    currentTypeInfo: TYPE_CONFIG['DRAGON'],
    petName: '木木',
    adopting: false,
    isFeeding: false,
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
    this.ensureUserAndLoad();
  },

  onShow() {
    if (app.globalData.userInfo) {
      this.fetchPetInfo();
    }
  },

  onPullDownRefresh() {
    this.fetchPetInfo(() => {
      wx.stopPullDownRefresh();
    });
  },

  ensureUserAndLoad() {
    app.login((user) => {
      this.fetchPetInfo();
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
          this.setData({
            hasPet: true,
            pet: pet,
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

  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['DRAGON'];
    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      petName: info.defaultName
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
    if (!name) {
      wx.showToast({ title: '请为搭子起个名字', icon: 'none' });
      return;
    }

    this.setData({ adopting: true });
    wx.showLoading({ title: '正在唤醒搭子...' });

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
          this.setData({
            hasPet: true,
            pet: res.data.data
          });
          wx.vibrateShort({ type: 'medium' });
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

  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    if (this.data.pet.foodCount <= 0) {
      wx.showModal({
        title: '食物不足',
        content: '小家伙的饭碗空空啦！今天完成一次运动打卡（快走/慢跑/力量等）就能免费带回食物哦～',
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
            title: '投喂成功！+10 经验',
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
          this.setData({ isFeeding: false });
        }, 600);
      }
    });
  },

  onTapPet() {
    if (!this.data.pet) return;
    wx.vibrateShort({ type: 'light' });

    const quotes = [
      '吃饱饱，今天陪你一起燃脂！💪',
      '我不运动，小家伙就没饭吃啦！快走两圈～🏃',
      '自律最酷啦，今天也要一起加油哦！🔥',
      '少油少盐多喝水，体态越来越棒啦！💧',
      '你今天超自律！本搭子超级开心～✨',
      '今天又多消耗了卡路里，我们都在变强！🌟'
    ];

    const randomQuote = quotes[Math.floor(Math.random() * quotes.length)];
    this.setData({
      'pet.dialogue': randomQuote
    });
  },

  onGoExercise() {
    wx.switchTab({
      url: '/pages/index/index'
    });
  }
});
