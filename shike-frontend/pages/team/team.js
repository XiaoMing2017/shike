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
    targetDaysOptions: [7, 14, 21, 28, 42, 66, 90],
    showTargetDaysSheet: false,
    showSharePosterModal: false,
    tempPosterPath: '',
    activeTemplate: 'morandi'
  },

  onLoad(options) {
    if (wx.showShareMenu) {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline']
      });
    }

    if (options && options.inviteCode) {
      this.setData({ inputInviteCode: options.inviteCode });
      wx.showToast({ title: '已填入团队邀请码', icon: 'success' });
    }
  },

  onShow() {
    this.checkUserAndLoadData();
    if (app.globalData.pendingInviteCode) {
      this.setData({ inputInviteCode: app.globalData.pendingInviteCode });
      app.globalData.pendingInviteCode = '';
      wx.showToast({ title: '已自动填入邀请码', icon: 'success' });
    }
  },

  checkUserAndLoadData() {
    app.login((user) => {
      this.fetchTeamData(user.id);
      this.checkPendingNudgeAlert(user.id);
    });
  },

  fetchTeamData(userId) {
    wx.request({
      url: `${app.globalData.baseUrl}/team/user/${userId}/active`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const detail = res.data.data;
          const formattedMembers = (detail.members || []).map(m => {
            let avatar = m.avatar || m.avatarUrl;
            avatar = app.formatImageUrl(avatar);
            return {
              ...m,
              avatar: avatar
            };
          });
          this.setData({
            hasTeam: true,
            teamId: detail.teamId,
            teamName: detail.teamName,
            currentDay: detail.currentDay,
            targetDays: detail.targetDays,
            points: detail.points,
            inviteCode: detail.inviteCode,
            members: formattedMembers
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

  showTargetDaysModal() {
    this.setData({ showTargetDaysSheet: true });
  },

  hideTargetDaysModal() {
    this.setData({ showTargetDaysSheet: false });
  },

  selectTargetDays(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      targetDaysIndex: index,
      showTargetDaysSheet: false
    });
  },

  preventBubble() {
    // Prevent scrolling behind modal
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
    const inviteCode = this.data.inviteCode ? `?inviteCode=${this.data.inviteCode}` : '';
    this._rewardSharePoints('SHARE_FRIEND');
    return {
      title: `🔥 我在咔嚓算卡发起了[${this.data.teamName || '契约减脂队'}]对赌减脂，赢取 ${this.data.points || 500} 积分大池！加入我的队伍吧！`,
      path: `/pages/team/team${inviteCode}`,
      imageUrl: this.data.tempPosterPath || ''
    };
  },

  onShareTimeline() {
    const inviteCode = this.data.inviteCode ? `inviteCode=${this.data.inviteCode}` : '';
    this._rewardSharePoints('SHARE_TIMELINE');
    return {
      title: `🔥 咔嚓算卡·契约减脂队[${this.data.teamName || '契约减脂队'}]开赛！邀你一起对赌自律，赢取 ${this.data.points || 500} 积分大池！`,
      query: inviteCode,
      imageUrl: this.data.tempPosterPath || ''
    };
  },

  _rewardSharePoints(shareType) {
    const user = app.globalData.userInfo;
    if (!user || !user.id) return;
    wx.request({
      url: `${app.globalData.baseUrl}/user/share-reward?userId=${user.id}&shareType=${shareType}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const { rewarded, points } = res.data.data;
          if (rewarded) {
            wx.showToast({ title: '分享成功 +10积分', icon: 'none', duration: 2000 });
            if (app.globalData.userInfo) {
              app.globalData.userInfo.points = points;
            }
          }
        }
      }
    });
  },

  onGeneratePoster() {
    wx.showLoading({ title: '正在获取最新餐食...' });

    const myId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    const todayStr = new Date().toISOString().split('T')[0];

    // 1. 先异步查询今日用户的打卡膳食记录，获取用户真实拍摄的食物大图
    wx.request({
      url: `${app.globalData.baseUrl}/diet/daily?userId=${myId}&date=${todayStr}`,
      method: 'GET',
      success: (dietRes) => {
        const dietRecords = dietRes.data && dietRes.data.code === 200 ? dietRes.data.data : [];
        this._dietRecords = dietRecords;

        // 寻找包含上传照片的食物记录
        let foodImgUrl = '';
        if (Array.isArray(dietRecords)) {
          const recordWithImg = dietRecords.find(r => r.imageUrl && r.imageUrl.trim() !== '');
          if (recordWithImg) {
            foodImgUrl = recordWithImg.imageUrl;
          }
        }

        // 如果用户今天上传了真实餐食图片，使用真实图片；否则使用本地沙拉海报图片兜底
        let bgUrl = '/images/poster_bg.png';
        let isRemoteBg = false;
        if (foodImgUrl) {
          if (foodImgUrl.startsWith('/uploads')) {
            bgUrl = `${app.globalData.baseUrl.replace('/api/v1', '')}${foodImgUrl}`;
            isRemoteBg = true;
          } else if (foodImgUrl.startsWith('http')) {
            bgUrl = foodImgUrl;
            isRemoteBg = true;
          }
        }

        wx.showLoading({ title: '正在生成高质感海报...' });

        const qrUrl = `${app.globalData.baseUrl}/team/qrcode?inviteCode=${this.data.inviteCode}`;
        const userInfo = app.globalData.userInfo;
        let avatarUrl = '/images/profile.png';
        if (userInfo && userInfo.avatarUrl) {
          if (userInfo.avatarUrl.startsWith('/uploads')) {
            avatarUrl = `${app.globalData.baseUrl}${userInfo.avatarUrl}`;
          } else {
            avatarUrl = userInfo.avatarUrl;
          }
        }

        // 资源加载超时保护
        const withTimeout = (promise, ms, fallbackValue) => {
          return new Promise((resolve) => {
            const timer = setTimeout(() => {
              console.warn('Promise timed out after ' + ms + 'ms');
              resolve(fallbackValue);
            }, ms);
            promise.then((res) => {
              clearTimeout(timer);
              resolve(res);
            }).catch(() => {
              clearTimeout(timer);
              resolve(fallbackValue);
            });
          });
        };

        const downloadBgPromise = new Promise((resolve) => {
          if (isRemoteBg) {
            wx.downloadFile({
              url: bgUrl,
              success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
              fail: () => resolve('')
            });
          } else {
            wx.getImageInfo({
              src: bgUrl,
              success: (res) => resolve(res.path),
              fail: () => resolve('')
            });
          }
        });

        const downloadQrPromise = new Promise((resolve) => {
          wx.downloadFile({
            url: qrUrl,
            success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
            fail: () => resolve('')
          });
        });

        const downloadAvatarPromise = new Promise((resolve) => {
          if (avatarUrl.startsWith('/')) {
            wx.getImageInfo({
              src: avatarUrl,
              success: (res) => resolve(res.path),
              fail: () => resolve('')
            });
          } else {
            wx.downloadFile({
              url: avatarUrl,
              success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
              fail: () => resolve('')
            });
          }
        });

        // 对远程背景图下载给4秒容错，其它资源3秒限制
        const downloadBg = withTimeout(downloadBgPromise, 4000, '');
        const downloadQr = withTimeout(downloadQrPromise, 3000, '');
        const downloadAvatar = withTimeout(downloadAvatarPromise, 3000, '');

        const loadImage = (canvas, path) => {
          return new Promise((resolve) => {
            if (!path) {
              resolve(null);
              return;
            }
            const img = canvas.createImage();
            const timer = setTimeout(() => {
              console.warn('loadImage timed out for path:', path);
              resolve(null);
            }, 3000);
            img.onload = () => {
              clearTimeout(timer);
              resolve(img);
            };
            img.onerror = () => {
              clearTimeout(timer);
              resolve(null);
            };
            img.src = path;
          });
        };

        Promise.all([downloadBg, downloadQr, downloadAvatar]).then(([tempBgPath, tempQrPath, tempAvatarPath]) => {
          // 保存临时下载文件缓存，以便在 Tab 切换重绘时直接读取
          this._tempBg = tempBgPath;
          this._tempQr = tempQrPath;
          this._tempAvatar = tempAvatarPath;

          const avatarFallbackPromise = tempAvatarPath 
            ? Promise.resolve(tempAvatarPath)
            : new Promise((res) => {
                wx.getImageInfo({
                  src: '/images/profile.png',
                  success: (info) => res(info.path),
                  fail: () => res('')
                });
              });

          avatarFallbackPromise.then((finalAvatarPath) => {
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
                
                const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null;
                const dpr = (systemInfo && systemInfo.pixelRatio) || 2;
                
                canvas.width = 750 * dpr;
                canvas.height = 1000 * dpr;
                ctx.scale(dpr, dpr);
                
                // 预加载全部绘制图片
                Promise.all([
                  loadImage(canvas, tempBgPath),
                  loadImage(canvas, tempQrPath),
                  loadImage(canvas, finalAvatarPath)
                ]).then(([bgImg, qrImg, avatarImg]) => {
                  const template = this.data.activeTemplate;
                  if (template === 'morandi') {
                    this.drawMorandiPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else if (template === 'vogue') {
                    this.drawVoguePoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else {
                    this.drawReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  }
                });
              });
          });
        }).catch((err) => {
          console.error(err);
          wx.hideLoading();
          wx.showToast({ title: '下载素材失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '拉取餐食记录失败', icon: 'none' });
      }
    });
  },

  drawGourmetVitalityPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    // 获取当前用户的打卡达标状态
    const myId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    const myMember = this.data.members.find(m => String(m.id) === String(myId)) || null;
    const todayChecked = myMember ? myMember.todayChecked : false;
    const successCount = myMember ? myMember.successCount : 0;

    console.log('Generating poster:', {
      myId,
      todayChecked,
      successCount,
      membersList: this.data.members,
      matchedMember: myMember
    });

    // 1. Clear Canvas
    ctx.clearRect(0, 0, 750, 1000);
    
    // 2. Draw Background image or fallback gradient
    if (bgImg) {
      ctx.drawImage(bgImg, 0, 0, 750, 1000);
    } else {
      const gradient = ctx.createLinearGradient(0, 0, 750, 1000);
      gradient.addColorStop(0, '#EEF2F6');
      gradient.addColorStop(1, '#E2E8F0');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, 750, 1000);
    }

    // 3. Draw white semi-transparent rounded card in the center (glassmorphism: opacity 0.65)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.15)';
    ctx.shadowBlur = 40;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 15;
    
    ctx.beginPath();
    this.drawRoundedRect(ctx, 45, 60, 660, 880, 36);
    ctx.fill();

    // Cancel shadow for other drawing
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetY = 0;
    
    // Draw subtle white border for card (adds to glass effect)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // 4. Draw Header text
    ctx.font = 'bold 38px sans-serif';
    ctx.fillStyle = '#0F172A';
    ctx.fillText('咔擦算卡 · 契约自律挑战', 90, 145);

    ctx.font = '24px sans-serif';
    ctx.fillStyle = '#475569';
    if (todayChecked) {
      ctx.fillText('今日已达标，契约持续生效中', 90, 190);
    } else {
      ctx.fillText('今日尚未达标，记得及时记录饮食', 90, 190);
    }

    // Separator Line
    ctx.strokeStyle = 'rgba(15, 23, 42, 0.08)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(90, 230);
    ctx.lineTo(660, 230);
    ctx.stroke();

    // 5. Draw Avatar on the top right of the card (center: x=600, y=145, radius=40)
    if (avatarImg) {
      ctx.save();
      ctx.beginPath();
      ctx.arc(600, 145, 40, 0, 2 * Math.PI);
      ctx.clip();
      ctx.drawImage(avatarImg, 560, 105, 80, 80);
      ctx.restore();
      
      // Draw white border for avatar
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(600, 145, 40, 0, 2 * Math.PI);
      ctx.stroke();
    }

    // 6. Draw Team metadata
    ctx.font = 'bold 28px sans-serif';
    ctx.fillStyle = '#1E293B';
    ctx.fillText('小队: ' + (this.data.teamName || '契约挑战组'), 90, 285);
    
    // Draw progress badge (Solid indigo badge with white text)
    const badgeText = '进度: 第 ' + this.data.currentDay + ' / ' + this.data.targetDays + ' 天';
    ctx.font = 'bold 24px sans-serif';
    const badgeWidth = ctx.measureText(badgeText).width + 30;
    ctx.fillStyle = '#4F46E5';
    ctx.beginPath();
    this.drawRoundedRect(ctx, 90, 315, badgeWidth, 44, 10);
    ctx.fill();
    
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText(badgeText, 105, 346);

    // 7. Draw Calorie Circular Progress Ring (replicates homepage style)
    const cx = 200;
    const cy = 515;
    const r = 80;
    const lineWidth = 14;
    const startAngle = -0.54 * Math.PI;
    const totalAngle = 1.62 * Math.PI;
    const endAngle = startAngle + totalAngle;

    // Background white highlight circle
    ctx.shadowColor = 'rgba(255, 255, 255, 0.9)';
    ctx.shadowBlur = 18;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.72)';
    ctx.lineWidth = lineWidth + 14;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.arc(cx, cy, r, startAngle, endAngle);
    ctx.stroke();

    // Soft dark drop shadow for track
    ctx.shadowColor = 'rgba(148, 163, 184, 0.16)';
    ctx.shadowBlur = 12;
    ctx.shadowOffsetY = 8;
    ctx.strokeStyle = 'rgba(231, 238, 243, 0.55)';
    ctx.lineWidth = lineWidth + 4;
    ctx.beginPath();
    ctx.arc(cx, cy, r, startAngle, endAngle);
    ctx.stroke();

    // Actual track
    ctx.shadowBlur = 0;
    ctx.shadowOffsetY = 0;
    const trackGradient = ctx.createLinearGradient(cx - r, cy - r, cx + r, cy + r);
    trackGradient.addColorStop(0, 'rgba(255, 255, 255, 0.86)');
    trackGradient.addColorStop(0.52, 'rgba(226, 238, 244, 0.82)');
    trackGradient.addColorStop(1, 'rgba(255, 255, 255, 0.7)');
    ctx.strokeStyle = trackGradient;
    ctx.lineWidth = lineWidth;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.arc(cx, cy, r, startAngle, endAngle);
    ctx.stroke();

    // Active progress arc (gradient from #2DD4BF to #10B981 and #A8E86A)
    const progressPercent = this.data.targetDays > 0 
      ? Math.min(100, Math.max(15, (this.data.currentDay / this.data.targetDays) * 100)) 
      : 80;
    const progressEndAngle = startAngle + (progressPercent / 100) * totalAngle;

    const activeGradient = ctx.createLinearGradient(cx - r, cy + r, cx + r, cy - r);
    if (todayChecked) {
      activeGradient.addColorStop(0, '#2DD4BF'); // Mint green/Cyan
      activeGradient.addColorStop(0.58, '#10B981'); // Emerald green
      activeGradient.addColorStop(1, '#A8E86A'); // Lime green
      ctx.shadowColor = 'rgba(45, 212, 191, 0.34)';
    } else {
      activeGradient.addColorStop(0, '#FB923C'); // Light orange
      activeGradient.addColorStop(0.58, '#F97316'); // Dark orange
      activeGradient.addColorStop(1, '#F59E0B'); // Amber/yellow
      ctx.shadowColor = 'rgba(249, 115, 22, 0.34)';
    }
    ctx.shadowBlur = 14;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 5;
    ctx.strokeStyle = activeGradient;
    ctx.lineWidth = lineWidth;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.arc(cx, cy, r, startAngle, progressEndAngle);
    ctx.stroke();

    // White inner shadow line for glossy glassmorphism
    ctx.shadowBlur = 0;
    ctx.shadowOffsetY = 0;
    ctx.globalAlpha = 0.42;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.85)';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(cx, cy, r - lineWidth / 2 + 2, startAngle + 0.02, progressEndAngle - 0.02);
    ctx.stroke();
    ctx.globalAlpha = 1;

    // Ring inner text
    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = todayChecked ? '#10B981' : '#F97316';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(todayChecked ? '达标' : '未达标', cx, cy);
    
    // Stats text next to the ring
    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';
    
    ctx.font = 'bold 30px sans-serif';
    ctx.fillStyle = '#0F172A';
    ctx.fillText(todayChecked ? '今日契约达标 🏆' : '今日尚未达标 ⚡', 320, 480);
    
    ctx.font = '24px sans-serif';
    ctx.fillStyle = '#475569';
    ctx.fillText('挑战池: ' + this.data.points + ' 积分', 320, 525);
    ctx.fillText('连续自律天数: ' + successCount + ' 天', 320, 565);

    // 8. Progress Grid (Ticks) - Adaptive Grid to support 7, 14, 21 days without overlapping QR Code
    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.25)';
    ctx.shadowBlur = 4;
    ctx.shadowOffsetX = 1;
    ctx.shadowOffsetY = 1;
    ctx.fillText('我的契约进度:', 90, 685);
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;

    let boxSize = 36;
    let gapX = 10;
    let gapY = 12;
    let cols = 8;
    let checkFont = 'bold 20px sans-serif';
    let numFont = '14px sans-serif';

    if (this.data.targetDays > 42) {
      boxSize = 22;
      gapX = 5;
      gapY = 6;
      cols = 15;
      checkFont = 'bold 13px sans-serif';
      numFont = '10px sans-serif';
    } else if (this.data.targetDays > 21) {
      boxSize = 28;
      gapX = 6;
      gapY = 8;
      cols = 12;
      checkFont = 'bold 16px sans-serif';
      numFont = '12px sans-serif';
    }

    const startX = 90;
    const startY = 715;

    for (let i = 0; i < this.data.targetDays; i++) {
      const row = Math.floor(i / cols);
      const col = i % cols;
      const x = startX + col * (boxSize + gapX);
      const y = startY + row * (boxSize + gapY);
      
      const isChecked = myMember && myMember.ticks && myMember.ticks[i] && myMember.ticks[i].checked;
      
      ctx.fillStyle = isChecked ? 'rgba(16, 185, 129, 0.08)' : '#F8FAFC';
      ctx.strokeStyle = isChecked ? '#10B981' : '#E2E8F0';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(x + boxSize/2, y + boxSize/2, boxSize/2, 0, 2 * Math.PI);
      ctx.fill();
      ctx.stroke();

      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      if (isChecked) {
        ctx.font = checkFont;
        ctx.fillStyle = '#10B981';
        ctx.fillText('✓', x + boxSize/2, y + boxSize/2);
      } else {
        ctx.font = numFont;
        ctx.fillStyle = '#94A3B8';
        ctx.fillText((i + 1).toString(), x + boxSize/2, y + boxSize/2);
      }
    }
    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';

    // 9. Brand Quote - Shifted to y = 890 to avoid overlap
    ctx.font = 'italic bold 26px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.25)';
    ctx.shadowBlur = 4;
    ctx.shadowOffsetX = 1;
    ctx.shadowOffsetY = 1;
    ctx.fillText('“咔擦一拍，每一口都算数”', 90, 890);
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;

    // 10. Draw QR Code (Bottom Right)
    ctx.fillStyle = '#FFFFFF';
    ctx.strokeStyle = 'rgba(15, 23, 42, 0.05)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 510, 695, 120, 120, 16);
    ctx.fill();
    ctx.stroke();

    if (qrImg) {
      ctx.drawImage(qrImg, 520, 705, 100, 100);
    } else {
      ctx.fillStyle = '#6366F1';
      ctx.fillRect(525, 710, 30, 30);
      ctx.fillRect(585, 710, 30, 30);
      ctx.fillStyle = '#6366F1';
      ctx.fillRect(525, 770, 30, 30);
      ctx.fillRect(585, 770, 10, 10);
      ctx.fillRect(565, 750, 15, 15);
    }
    
    ctx.font = '16px sans-serif';
    ctx.fillStyle = '#64748B';
    ctx.fillText('扫码加入我们', 520, 840);
    
    this.saveCanvasToAlbum(canvas);
  },

  switchTemplate(e) {
    const template = e.currentTarget.dataset.template;
    if (template === this.data.activeTemplate) return;

    this.setData({ activeTemplate: template });
    wx.showLoading({ title: '正在切换海报样式...' });

    // Retrieve cached assets
    const tempBgPath = this._tempBg || '';
    const tempQrPath = this._tempQr || '';
    const tempAvatarPath = this._tempAvatar || '';

    // If avatar failed, use local fallback converted to temp path
    const avatarFallbackPromise = tempAvatarPath 
      ? Promise.resolve(tempAvatarPath)
      : new Promise((res) => {
          wx.getImageInfo({
            src: '/images/profile.png',
            success: (info) => res(info.path),
            fail: () => res('')
          });
        });

    avatarFallbackPromise.then((finalAvatarPath) => {
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
          
          const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null;
          const dpr = (systemInfo && systemInfo.pixelRatio) || 2;
          
          canvas.width = 750 * dpr;
          canvas.height = 1000 * dpr;
          ctx.scale(dpr, dpr);
const loadImage = (canvas, path) => {
            return new Promise((resolve) => {
              if (!path) { resolve(null); return; }
              const img = canvas.createImage();
              const timer = setTimeout(() => resolve(null), 3000);
              img.onload = () => { clearTimeout(timer); resolve(img); };
              img.onerror = () => { clearTimeout(timer); resolve(null); };
              img.src = path;
            });
          };

          Promise.all([
            loadImage(canvas, tempBgPath),
            loadImage(canvas, tempQrPath),
            loadImage(canvas, finalAvatarPath)
          ]).then(([bgImg, qrImg, avatarImg]) => {
            if (template === 'morandi') {
              this.drawMorandiPoster(canvas, ctx, bgImg, qrImg, avatarImg);
            } else if (template === 'vogue') {
              this.drawVoguePoster(canvas, ctx, bgImg, qrImg, avatarImg);
            } else {
              this.drawReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg);
            }
          });
        });
    });
  },

  drawMorandiPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const myId = user ? user.id : null;
    const myMember = (this.data.members || []).find(m => String(m.id) === String(myId)) || null;
    const successCount = myMember ? myMember.successCount : 1;
    const teamName = this.data.teamName || '食刻契约挑战组';

    // 1. Sage Green & Soft Cream Gradient Background
    ctx.clearRect(0, 0, 750, 1000);
    const bgGrad = ctx.createLinearGradient(0, 0, 750, 1000);
    bgGrad.addColorStop(0, '#E8EFE5'); // Soft Morandi Sage Green
    bgGrad.addColorStop(1, '#D8E2D3');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, 750, 1000);

    // 2. High-End Frosted Glass Container Card
    ctx.fillStyle = 'rgba(255, 255, 255, 0.88)';
    ctx.shadowColor = 'rgba(47, 65, 52, 0.12)';
    ctx.shadowBlur = 32;
    ctx.shadowOffsetY = 16;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 40, 50, 670, 900, 36);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // 3. Header Text & Minimalist Lifestyle Tag
    ctx.font = 'bold 36px sans-serif';
    ctx.fillStyle = '#2D3A31';
    ctx.fillText('咔嚓算卡 · 契约打卡战报', 80, 125);

    ctx.font = '22px sans-serif';
    ctx.fillStyle = '#5A6E60';
    ctx.fillText('小队: ' + teamName + ' · 连胜 ' + successCount + ' 天', 80, 165);

    // 4. Polaroid Photo Frame with food image
    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.08)';
    ctx.shadowBlur = 16;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 80, 195, 590, 440, 24);
    ctx.fill();
    ctx.shadowColor = 'transparent';

    if (bgImg) {
      ctx.save();
      ctx.beginPath();
      this.drawRoundedRect(ctx, 95, 210, 560, 360, 16);
      ctx.clip();
      ctx.drawImage(bgImg, 95, 210, 560, 360);
      ctx.restore();
    } else {
      ctx.fillStyle = '#E2E8F0';
      ctx.beginPath();
      this.drawRoundedRect(ctx, 95, 210, 560, 360, 16);
      ctx.fill();
      ctx.font = '28px sans-serif';
      ctx.fillStyle = '#94A3B8';
      ctx.fillText('🥗 记录每一餐的精致美好', 220, 400);
    }

    // Photo Caption Inside Polaroid
    ctx.font = 'bold 24px sans-serif';
    ctx.fillStyle = '#334155';
    ctx.fillText('🔥 今日契约完成！累计打卡 ' + successCount + ' 天', 105, 605);

    // 5. User Avatar & Profile Bar (Bottom Left)
    if (avatarImg) {
      ctx.save();
      ctx.beginPath();
      ctx.arc(120, 785, 36, 0, 2 * Math.PI);
      ctx.clip();
      ctx.drawImage(avatarImg, 84, 749, 72, 72);
      ctx.restore();
      
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(120, 785, 36, 0, 2 * Math.PI);
      ctx.stroke();
    }

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#1E293B';
    ctx.fillText(nickname, 170, 775);

    ctx.font = '20px sans-serif';
    ctx.fillStyle = '#64748B';
    ctx.fillText('与小队伙伴一起对赌自律', 170, 805);

    // 6. QR Code (Bottom Right)
    if (qrImg) {
      ctx.drawImage(qrImg, 530, 735, 140, 140);
      ctx.font = '18px sans-serif';
      ctx.fillStyle = '#64748B';
      ctx.fillText('扫码加入小队', 550, 895);
    }

    this.saveCanvasToAlbum(canvas);
  },

  drawVoguePoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const myId = user ? user.id : null;
    const myMember = (this.data.members || []).find(m => String(m.id) === String(myId)) || null;
    const successCount = myMember ? myMember.successCount : 1;
    const teamName = this.data.teamName || '契约减脂队';

    ctx.clearRect(0, 0, 750, 1000);

    if (bgImg) {
      ctx.drawImage(bgImg, 0, 0, 750, 1000);
      const grad = ctx.createLinearGradient(0, 0, 0, 1000);
      grad.addColorStop(0, 'rgba(15, 23, 42, 0.7)');
      grad.addColorStop(0.4, 'rgba(15, 23, 42, 0.3)');
      grad.addColorStop(1, 'rgba(15, 23, 42, 0.85)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 750, 1000);
    } else {
      const grad = ctx.createLinearGradient(0, 0, 750, 1000);
      grad.addColorStop(0, '#0F172A');
      grad.addColorStop(1, '#1E293B');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 750, 1000);
    }

    ctx.font = 'bold 44px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText('SHIKE HEALTH EDITORIAL', 65, 110);

    ctx.font = 'bold 20px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.fillText('TEAM: ' + teamName + ' · STREAK ' + successCount + ' DAYS', 65, 150);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(65, 175);
    ctx.lineTo(685, 175);
    ctx.stroke();

    ctx.font = 'bold 100px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText(String(successCount), 65, 300);

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#38BDF8';
    ctx.fillText('DAYS OF CONTINUOUS DISCIPLINE', 65, 345);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 65, 400, 620, 280, 24);
    ctx.fill();
    ctx.stroke();

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText('TEAM CHALLENGE METRICS', 95, 455);

    ctx.font = '24px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.fillText(`• 小队名称: ${teamName}`, 95, 510);
    ctx.fillText(`• 目标周期: ${this.data.targetDays || 30} 天减脂契约`, 95, 560);
    ctx.fillText(`• 达标奖励: 赢取 ${this.data.points || 500} 积分大池`, 95, 610);

    ctx.fillStyle = 'rgba(15, 23, 42, 0.8)';
    ctx.beginPath();
    this.drawRoundedRect(ctx, 65, 730, 620, 200, 24);
    ctx.fill();

    if (avatarImg) {
      ctx.save();
      ctx.beginPath();
      ctx.arc(125, 830, 40, 0, 2 * Math.PI);
      ctx.clip();
      ctx.drawImage(avatarImg, 85, 790, 80, 80);
      ctx.restore();
      ctx.strokeStyle = '#38BDF8';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(125, 830, 40, 0, 2 * Math.PI);
      ctx.stroke();
    }

    ctx.font = 'bold 28px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText(nickname, 185, 815);

    ctx.font = '20px sans-serif';
    ctx.fillStyle = '#94A3B8';
    ctx.fillText('咔嚓算卡 · 契约减脂组成员', 185, 855);

    if (qrImg) {
      ctx.drawImage(qrImg, 525, 760, 140, 140);
    }

    this.saveCanvasToAlbum(canvas);
  },

  drawReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const myId = user ? user.id : null;
    const myMember = (this.data.members || []).find(m => String(m.id) === String(myId)) || null;
    const successCount = myMember ? myMember.successCount : 1;

    ctx.clearRect(0, 0, 750, 1000);
    ctx.fillStyle = '#E2E8F0';
    ctx.fillRect(0, 0, 750, 1000);

    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.12)';
    ctx.shadowBlur = 24;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 55, 45, 640, 910, 16);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.font = 'bold 36px monospace';
    ctx.fillStyle = '#0F172A';
    ctx.fillText('=== CALORIE BILL RECORD ===', 100, 120);

    ctx.font = '22px monospace';
    ctx.fillStyle = '#475569';
    ctx.fillText('STORE: 咔嚓算卡 HEALTH LAB', 100, 170);
    ctx.fillText('CUSTOMER: ' + nickname, 100, 205);
    ctx.fillText('INVITE CODE: ' + (this.data.inviteCode || 'SHIKE'), 100, 240);
    ctx.fillText('-----------------------------------', 100, 275);

    ctx.font = 'bold 26px monospace';
    ctx.fillStyle = '#1E293B';
    ctx.fillText('ITEM               STREAK  STATUS', 100, 320);
    ctx.font = '22px monospace';
    ctx.fillStyle = '#475569';
    ctx.fillText('打卡连胜           ' + successCount + ' 天    ✓ 达标', 100, 365);
    ctx.fillText('契约天数           ' + (this.data.targetDays || 30) + ' 天    进行中', 100, 410);
    ctx.fillText('奖池积分           ' + (this.data.points || 500) + ' pt   生效中', 100, 455);
    ctx.fillText('-----------------------------------', 100, 505);

    ctx.font = 'bold 30px monospace';
    ctx.fillStyle = '#0F172A';
    ctx.fillText('CHALLENGE STREAK: ' + successCount + ' DAYS', 100, 555);

    ctx.font = '22px monospace';
    ctx.fillStyle = '#059669';
    ctx.fillText('STATUS: ✓ 契约对赌持续生效中', 100, 615);
    ctx.fillText('-----------------------------------', 100, 665);

    if (qrImg) {
      ctx.drawImage(qrImg, 490, 710, 160, 160);
    }

    ctx.font = '20px monospace';
    ctx.fillStyle = '#64748B';
    ctx.fillText('THANK YOU FOR BEING DISCIPLINED!', 100, 760);
    ctx.fillText('SCAN QR CODE TO JOIN US', 100, 800);

    this.saveCanvasToAlbum(canvas);
  },

  drawGlowCircle(ctx, cx, cy, r, colorRgb) {
    const radGlow = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
    radGlow.addColorStop(0, 'rgba(' + colorRgb + ', 0.35)');
    radGlow.addColorStop(0.5, 'rgba(' + colorRgb + ', 0.15)');
    radGlow.addColorStop(1, 'rgba(' + colorRgb + ', 0)');
    ctx.fillStyle = radGlow;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.fill();
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
    const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null;
    const dpr = (systemInfo && systemInfo.pixelRatio) || 2;

    wx.canvasToTempFilePath({
      canvas: canvas,
      x: 0,
      y: 0,
      width: 750,
      height: 1000,
      destWidth: 750 * dpr,
      destHeight: 1000 * dpr,
      success: (tempRes) => {
        const filePath = tempRes.tempFilePath;
        wx.hideLoading();
        this.setData({
          tempPosterPath: filePath,
          showSharePosterModal: true
        });
      },
      fail: (err) => {
        wx.hideLoading();
        console.error(err);
        wx.showToast({ title: '导出海报失败', icon: 'none' });
      }
    });
  },

  hideSharePosterModal() {
    this.setData({ showSharePosterModal: false });
  },

  onSavePosterClick() {
    const filePath = this.data.tempPosterPath;
    if (!filePath) return;
    this.setData({ showSharePosterModal: false });
    wx.showLoading({ title: '正在保存到相册...' });
    wx.saveImageToPhotosAlbum({
      filePath: filePath,
      success: () => {
        wx.hideLoading();
        wx.showModal({
          title: '海报保存成功',
          content: '打卡海报已保存到相册！可以手动发朋友圈/小红书分享自律打卡哦！',
          showCancel: false
        });
      },
      fail: (err) => {
        wx.hideLoading();
        if (err.errMsg.indexOf('auth deny') > -1 || err.errMsg.indexOf('authorize:fail') > -1) {
          wx.showModal({
            title: '保存失败',
            content: '相册保存权限已被拒绝，请开启权限后重试。',
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

  onSharePosterClick() {
    const filePath = this.data.tempPosterPath;
    if (!filePath) return;
    this.setData({ showSharePosterModal: false });
    wx.showShareImageMenu({
      path: filePath,
      fail: () => {
        wx.showToast({ title: '调起微信分享失败，请直接保存到相册', icon: 'none' });
      }
    });
  },

  onQuitTeam() {
    const user = app.globalData.userInfo;
    if (!user) return;

    wx.showModal({
      title: '退出小队对赌',
      content: '中途退出对赌小队将直接扣除100个信用分。确定要放弃承诺退出挑战吗？',
      cancelText: '狠心退出',
      confirmText: '坚守承诺',
      success: (res) => {
        if (res.cancel) {
          wx.showLoading({ title: '正在退出...' });
          wx.request({
            url: `${app.globalData.baseUrl}/team/${this.data.teamId}/leave?userId=${user.id}`,
            method: 'POST',
            success: (leaveRes) => {
              wx.hideLoading();
              if (leaveRes.data && leaveRes.data.code === 200) {
                wx.showToast({ title: '已退出小队，扣分100', icon: 'none' });
                // Update local userInfo points
                user.points = Math.max(0, (user.points || 1000) - 100);
                app.globalData.userInfo = user;
                
                setTimeout(() => {
                  this.fetchTeamData(user.id);
                }, 1500);
              } else {
                wx.showToast({ title: leaveRes.data.message || '退出失败', icon: 'none' });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '网络连接失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  onNudgeTeammate(e) {
    const targetUserId = e.currentTarget.dataset.userId;
    const name = e.currentTarget.dataset.name || '队友';
    const { userId, teamId } = this.data;

    if (!targetUserId || !teamId || !userId) return;

    wx.showLoading({ title: '提醒发送中...' });
    wx.request({
      url: `${app.globalData.baseUrl}/team/nudge`,
      method: 'POST',
      header: { 'content-type': 'application/x-www-form-urlencoded' },
      data: { senderId: userId, targetUserId, teamId },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({
            title: res.data.data || `已提醒 ${name}！`,
            icon: 'success',
            duration: 2000
          });
        } else {
          wx.showToast({
            title: (res.data && res.data.message) || '提醒失败',
            icon: 'none',
            duration: 2000
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
      }
    });
  },

  onAvatarError(e) {
    const index = e.currentTarget.dataset.index;
    if (index !== undefined && this.data.members && this.data.members[index]) {
      const key = `members[${index}].avatar`;
      this.setData({
        [key]: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0'
      });
    }
  },

  checkPendingNudgeAlert(userId) {
    if (!userId) return;
    wx.request({
      url: `${app.globalData.baseUrl}/team/nudge/alert?userId=${userId}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          wx.showModal({
            title: '🔔 小队打卡提醒',
            content: res.data.data,
            confirmText: '去拍照打卡',
            confirmColor: '#10B981',
            cancelText: '知道啦',
            success: (mRes) => {
              if (mRes.confirm) {
                wx.switchTab({ url: '/pages/index/index' });
              }
            }
          });
        }
      }
    });
  }
});
