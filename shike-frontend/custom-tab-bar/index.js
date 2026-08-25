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
    updateTabs(currentRoute) {
      this.setData({
        selectedPath: currentRoute || "pages/index/index",
        list: ALL_TABS
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
