const app = getApp();

Page({
  data: {
    hasTeam: false,
    teamId: null,
    teamName: '',
    currentDay: 1,
    targetDays: 7,
    points: 500,
    inviteCode: '',
    members: [],

    // Form inputs
    inputInviteCode: '',
    createTeamName: '',
    targetDaysIndex: 0,
    targetDaysOptions: [7, 14, 21]
  },

  onShow() {
    this.checkUserAndLoadData();
  },

  checkUserAndLoadData() {
    app.login((user) => {
      this.fetchTeamData(user.id);
    });
  },

  fetchTeamData(userId) {
    wx.request({
      url: `${app.globalData.baseUrl}/team/user/${userId}/active`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const detail = res.data.data;
          this.setData({
            hasTeam: true,
            teamId: detail.teamId,
            teamName: detail.teamName,
            currentDay: detail.currentDay,
            targetDays: detail.targetDays,
            points: detail.points,
            inviteCode: detail.inviteCode,
            members: detail.members
          });
        } else {
          this.setData({
            hasTeam: false
          });
        }
      },
      fail: () => {
        wx.showToast({ title: '同步小队数据失败', icon: 'none' });
      }
    });
  },

  onCopyInviteCode() {
    wx.setClipboardData({
      data: this.data.inviteCode,
      success: () => {
        wx.showToast({ title: '邀请码已复制', icon: 'success' });
      }
    });
  },

  onInviteCodeInput(e) {
    this.setData({ inputInviteCode: e.detail.value.trim().toUpperCase() });
  },

  onTeamNameInput(e) {
    this.setData({ createTeamName: e.detail.value.trim() });
  },

  onTargetDaysChange(e) {
    this.setData({ targetDaysIndex: parseInt(e.detail.value) });
  },

  onJoinTeamByCode() {
    const code = this.data.inputInviteCode;
    if (!code || code.length !== 6) {
      wx.showToast({ title: '请输入6位邀请码', icon: 'none' });
      return;
    }

    const user = app.globalData.userInfo;
    if (!user) return;

    wx.showLoading({ title: '正在加入...' });
    wx.request({
      url: `${app.globalData.baseUrl}/team/join`,
      method: 'POST',
      data: {
        userId: user.id,
        inviteCode: code
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '成功加入小队！', icon: 'success' });
          this.fetchTeamData(user.id);
        } else {
          wx.showToast({ title: res.data.message || '加入失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
      }
    });
  },

  onCreateNewTeam() {
    const name = this.data.createTeamName;
    if (!name) {
      wx.showToast({ title: '请输入队伍名称', icon: 'none' });
      return;
    }

    const user = app.globalData.userInfo;
    if (!user) return;

    const targetDays = this.data.targetDaysOptions[this.data.targetDaysIndex];

    wx.showLoading({ title: '正在创建...' });
    wx.request({
      url: `${app.globalData.baseUrl}/team/create`,
      method: 'POST',
      data: {
        creatorId: user.id,
        teamName: name,
        targetDays: targetDays
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '创建小队成功！', icon: 'success' });
          this.setData({
            createTeamName: ''
          });
          this.fetchTeamData(user.id);
        } else {
          wx.showToast({ title: res.data.message || '创建失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
      }
    });
  },

  onShareAppMessage() {
    return {
      title: `我发起了${this.data.targetDays}天减脂对赌小队，赢取 ${this.data.points} 积分！加入我的队伍吧！`,
      path: `/pages/team/team?inviteCode=${this.data.inviteCode}`
    };
  },

  onGeneratePoster() {
    wx.showLoading({ title: '正在生成高质感海报...' });

    const query = wx.createSelectorQuery();
    query.select('#posterCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) {
          wx.hideLoading();
          wx.showToast({ title: '未找到绘制画布', icon: 'none' });
          return;
        }

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        
        // 必须在 JS 中显式设置 canvas 2D 实例的宽高，否则会默认为宽 300，高 150，导致文字和画面被截断
        canvas.width = 750;
        canvas.height = 1000;
        
        // 1. Clear Canvas
        ctx.clearRect(0, 0, 750, 1000);
        
        // 2. Draw Gradient Background (Indigo -> Purple)
        const gradient = ctx.createLinearGradient(0, 0, 750, 1000);
        gradient.addColorStop(0, '#6366F1'); 
        gradient.addColorStop(1, '#A855F7'); 
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, 750, 1000);
        
        // 3. Draw white semi-transparent rounded card in the center (glassmorphism)
        ctx.fillStyle = 'rgba(255, 255, 255, 0.92)';
        ctx.shadowColor = 'rgba(15, 23, 42, 0.1)';
        ctx.shadowBlur = 30;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 15;
        
        ctx.beginPath();
        this.drawRoundedRect(ctx, 50, 80, 650, 840, 32);
        ctx.fill();

        // Cancel shadow for text drawing
        ctx.shadowBlur = 0;
        ctx.shadowOffsetY = 0;

        // 4. Draw Header text
        ctx.font = 'bold 38px sans-serif';
        ctx.fillStyle = '#0F172A';
        ctx.fillText('食刻 · 契约自律挑战', 90, 170);

        ctx.font = '24px sans-serif';
        ctx.fillStyle = '#64748B';
        ctx.fillText('今日挑战已达标，契约持续生效中', 90, 215);

        // 5. Separator Line
        ctx.strokeStyle = '#F1F5F9';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(90, 255);
        ctx.lineTo(660, 255);
        ctx.stroke();

        // 6. Draw Team metadata
        ctx.font = 'bold 28px sans-serif';
        ctx.fillStyle = '#334155';
        ctx.fillText('小队: ' + (this.data.teamName || '契约挑战组'), 90, 310);
        ctx.fillText('进度: 第 ' + this.data.currentDay + ' / ' + this.data.targetDays + ' 天', 90, 360);

        // 7. Draw Calorie Circular Progress Ring
        // Ring center: x=200, y=530, radius=80
        ctx.strokeStyle = '#F1F5F9';
        ctx.lineWidth = 14;
        ctx.beginPath();
        ctx.arc(200, 530, 80, 0, 2 * Math.PI);
        ctx.stroke();

        ctx.strokeStyle = '#10B981'; // Green
        ctx.lineWidth = 14;
        ctx.lineCap = 'round';
        ctx.beginPath();
        // Visual progress arc representing success
        ctx.arc(200, 530, 80, -0.5 * Math.PI, 1.1 * Math.PI);
        ctx.stroke();

        // Ring inner text
        ctx.font = 'bold 26px sans-serif';
        ctx.fillStyle = '#10B981';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('达标', 200, 530);
        
        // Stats text next to the ring
        ctx.textAlign = 'left';
        ctx.textBaseline = 'alphabetic';
        
        ctx.font = 'bold 30px sans-serif';
        ctx.fillStyle = '#0F172A';
        ctx.fillText('今日契约达标 🏆', 320, 495);
        
        ctx.font = '24px sans-serif';
        ctx.fillStyle = '#64748B';
        ctx.fillText('挑战池: ' + this.data.points + ' 积分', 320, 540);
        ctx.fillText('连续自律天数: ' + (this.data.members[0] ? this.data.members[0].successCount : 1) + ' 天', 320, 580);

        // 8. Draw 7-Day progress grid (Ticks)
        ctx.font = 'bold 26px sans-serif';
        ctx.fillStyle = '#334155';
        ctx.fillText('我的周契约进度:', 90, 680);

        const startX = 90;
        const startY = 715;
        const boxSize = 56;
        const gap = 24;

        for (let i = 0; i < 7; i++) {
          const x = startX + i * (boxSize + gap);
          const isPastChecked = i < this.data.currentDay;
          
          ctx.fillStyle = isPastChecked ? '#E6FDF4' : '#F8FAFC';
          ctx.strokeStyle = isPastChecked ? '#10B981' : '#CBD5E1';
          ctx.lineWidth = 2;
          ctx.beginPath();
          this.drawRoundedRect(ctx, x, startY, boxSize, boxSize, 10);
          ctx.fill();
          ctx.stroke();

          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          if (isPastChecked) {
            ctx.font = 'bold 26px sans-serif';
            ctx.fillStyle = '#10B981';
            ctx.fillText('✓', x + boxSize/2, startY + boxSize/2);
          } else {
            ctx.font = '20px sans-serif';
            ctx.fillStyle = '#94A3B8';
            ctx.fillText((i + 1).toString(), x + boxSize/2, startY + boxSize/2);
          }
        }
        ctx.textAlign = 'left';
        ctx.textBaseline = 'alphabetic';

        // 9. Brand Quote
        ctx.font = 'italic bold 26px sans-serif';
        ctx.fillStyle = '#6366F1';
        ctx.fillText('“自律食刻，每一口都算数”', 90, 850);

        // 10. QR code block (bottom right)
        ctx.fillStyle = '#FFFFFF';
        ctx.strokeStyle = '#F1F5F9';
        ctx.lineWidth = 4;
        ctx.beginPath();
        this.drawRoundedRect(ctx, 510, 775, 110, 110, 16);
        ctx.fill();
        ctx.stroke();

        // Mini QR pattern
        ctx.fillStyle = '#6366F1';
        ctx.fillRect(525, 790, 30, 30);
        ctx.fillRect(575, 790, 30, 30);
        ctx.fillRect(525, 840, 30, 30);
        ctx.fillRect(575, 840, 10, 10);
        ctx.fillRect(595, 850, 10, 10);
        ctx.fillRect(565, 830, 10, 10);
        
        ctx.font = '16px sans-serif';
        ctx.fillStyle = '#94A3B8';
        ctx.fillText('长按加入我们', 520, 905);

        // Save image file
        this.saveCanvasToAlbum(canvas);
      });
  },

  drawRoundedRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.arcTo(x + width, y, x + width, y + radius, radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.arcTo(x + width, y + height, x + width - radius, y + height, radius);
    ctx.lineTo(x + radius, y + height);
    ctx.arcTo(x, y + height, x, y + height - radius, radius);
    ctx.lineTo(x, y + radius);
    ctx.arcTo(x, y, x + radius, y, radius);
    ctx.closePath();
  },

  saveCanvasToAlbum(canvas) {
    wx.canvasToTempFilePath({
      canvas: canvas,
      x: 0,
      y: 0,
      width: 750,
      height: 1000,
      destWidth: 750,
      destHeight: 1000,
      success: (tempRes) => {
        const filePath = tempRes.tempFilePath;
        
        wx.saveImageToPhotosAlbum({
          filePath: filePath,
          success: () => {
            wx.hideLoading();
            wx.showModal({
              title: '海报保存成功',
              content: '契约打卡海报已保存到您的手机相册！快发朋友圈/小红书秀出你的今日自律吧！',
              showCancel: false
            });
          },
          fail: (err) => {
            wx.hideLoading();
            // Detect user permission denial
            if (err.errMsg.indexOf('auth deny') > -1 || err.errMsg.indexOf('authorize:fail') > -1) {
              wx.showModal({
                title: '保存失败',
                content: '相册保存权限已被拒绝，请点击确定前往开启，否则无法保存海报。',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.openSetting();
                  }
                }
              });
            } else {
              wx.showToast({ title: '保存图片失败', icon: 'none' });
            }
          }
        });
      },
      fail: (err) => {
        wx.hideLoading();
        console.error(err);
        wx.showToast({ title: '导出海报失败', icon: 'none' });
      }
    });
  },

  onQuitTeam() {
    wx.showModal({
      title: '退出小队对赌',
      content: '中途退出对赌小队将直接扣除100个信用分。确定要放弃承诺退出挑战吗？',
      cancelText: '狠心退出',
      confirmText: '坚守承诺',
      success: (res) => {
        if (res.cancel) {
          wx.showToast({ title: '已退出挑战，扣除分值。', icon: 'none' });
          // In real production, this would make an API call to soft-delete membership
          // For MVP, we simply show the action and refresh
          setTimeout(() => {
            this.fetchTeamData(app.globalData.userInfo.id);
          }, 1500);
        }
      }
    });
  }
});
