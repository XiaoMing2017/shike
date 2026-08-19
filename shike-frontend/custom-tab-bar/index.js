// custom-tab-bar/index.js
const app = getApp();

const ALL_TABS = [
  {
    pagePath: "pages/index/index",
    text: "首页看板",
    iconPath: "/images/home.png",
    selectedIconPath: "/images/home_active.png"
  },
  {
    key: "pet_system",
    pagePath: "pages/pet/pet",
    text: "自律搭子",
    iconPath: "/images/pet.png",
    selectedIconPath: "/images/pet_active.png"
  },
  {
    pagePath: "pages/team/team",
    text: "减脂对赌",
    iconPath: "/images/team.png",
    selectedIconPath: "/images/team_active.png"
  },
  {
    pagePath: "pages/profile/profile",
    text: "个人档案",
    iconPath: "/images/profile.png",
    selectedIconPath: "/images/profile_active.png"
  }
];

Component({
  data: {
    selectedPath: "pages/index/index",
    color: "#94A3B8",
    selectedColor: "#10B981",
    list: ALL_TABS
  },

  methods: {
    updateTabs(currentRoute, features) {
      const feat = features || (app && app.globalData && app.globalData.features) || {};
      const isPetEnabled = feat.pet_system !== false;

      const filteredList = ALL_TABS.filter(tab => {
        if (tab.key === 'pet_system' && !isPetEnabled) {
          return false;
        }
        return true;
      });

      this.setData({
        selectedPath: currentRoute || "pages/index/index",
        list: filteredList
      });
    },

    switchTab(e) {
      const path = e.currentTarget.dataset.path;
      wx.switchTab({
        url: '/' + path
      });
    }
  }
});
