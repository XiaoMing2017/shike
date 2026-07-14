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
    targetDaysOptions: [7, 14, 21],
    showTargetDaysSheet: false,
    showSharePosterModal: false,
    tempPosterPath: '',
    activeTemplate: 'polaroid' // Default to polaroid (User preference), can switch to receipt or vitality
  },

  onLoad(options) {
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
    return {
      title: `我发起了${this.data.targetDays}天减脂对赌小队，赢取 ${this.data.points} 积分！加入我的队伍吧！`,
      path: `/pages/team/team?inviteCode=${this.data.inviteCode}`
    };
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
                  if (template === 'vitality') {
                    this.drawGourmetVitalityPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else if (template === 'receipt') {
                    this.drawCalorieReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else if (template === 'polaroid') {
                    this.drawPolaroidPoster(canvas, ctx, bgImg, qrImg, avatarImg);
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
    ctx.fillText('食刻 · 契约自律挑战', 90, 145);

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

    const startX = 90;
    const startY = 715;
    const boxSize = 36;
    const gapX = 10;
    const gapY = 12;
    const cols = 8;

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
        ctx.font = 'bold 20px sans-serif';
        ctx.fillStyle = '#10B981';
        ctx.fillText('✓', x + boxSize/2, y + boxSize/2);
      } else {
        ctx.font = '14px sans-serif';
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
    ctx.fillText('“自律食刻，每一口都算数”', 90, 890);
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
            if (template === 'vitality') {
              this.drawGourmetVitalityPoster(canvas, ctx, bgImg, qrImg, avatarImg);
            } else if (template === 'receipt') {
              this.drawCalorieReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg);
            } else if (template === 'polaroid') {
              this.drawPolaroidPoster(canvas, ctx, bgImg, qrImg, avatarImg);
            }
          });
        });
    });
  },

  drawCalorieReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const myId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    const myMember = this.data.members.find(m => String(m.id) === String(myId)) || null;
    const todayChecked = myMember ? myMember.todayChecked : false;
    const successCount = myMember ? myMember.successCount : 0;
    const targetCalories = app.globalData.userInfo && app.globalData.userInfo.targetCalories ? app.globalData.userInfo.targetCalories : 2000;

    ctx.clearRect(0, 0, 750, 1000);

    ctx.fillStyle = '#F4F1EA';
    ctx.fillRect(0, 0, 750, 1000);

    ctx.fillStyle = '#FCFAF5';
    ctx.shadowColor = 'rgba(27, 26, 23, 0.15)';
    ctx.shadowBlur = 30;
    ctx.shadowOffsetY = 10;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 80, 60, 590, 880, 12);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetY = 0;

    ctx.strokeStyle = '#E2DDD3';
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    
    ctx.font = 'bold 36px Courier New, Courier, monospace, sans-serif';
    ctx.fillStyle = '#1B1A17';
    ctx.fillText('食刻 · 契约打卡账单', 375, 130);

    ctx.font = '22px Courier New, Courier, monospace, sans-serif';
    ctx.fillText('SHIKE DIET BILL', 375, 175);

    ctx.textAlign = 'left';
    ctx.fillStyle = '#4F4E4A';
    ctx.font = '22px Courier New, Courier, monospace, sans-serif';
    
    const todayStr = new Date().toISOString().split('T')[0];
    ctx.fillText('日期: ' + todayStr, 120, 230);
    ctx.fillText('邀请码: ' + (this.data.inviteCode || '无'), 120, 265);
    ctx.fillText('连续达标: ' + successCount + ' 天', 120, 300);

    ctx.strokeStyle = '#D6D0C2';
    ctx.setLineDash([4, 4]);
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(120, 335);
    ctx.lineTo(630, 335);
    ctx.stroke();
    ctx.setLineDash([]); 

    const parsedItems = [];
    let grandTotal = 0;
    const dietRecords = this._dietRecords || [];
    dietRecords.forEach(record => {
      try {
        const items = JSON.parse(record.foodItems);
        if (Array.isArray(items)) {
          items.forEach(item => {
            const cal = Math.round(item.calories || ((item.unitCalories || 0) * item.weight) || 0);
            parsedItems.push({
              name: item.name,
              weight: item.weight,
              calories: cal
            });
            grandTotal += cal;
          });
        }
      } catch (e) {
        console.error('Error parsing foodItems for receipt:', e);
      }
    });

    ctx.fillStyle = '#1B1A17';
    ctx.font = '24px Courier New, Courier, monospace, sans-serif';
    let currentY = 380;
    
    const formatReceiptLine = (left, right, maxLen = 28) => {
      const getVisualLength = (str) => {
        let len = 0;
        for (let i = 0; i < str.length; i++) {
          if (str.charCodeAt(i) > 127) {
            len += 2;
          } else {
            len += 1;
          }
        }
        return len;
      };
      const dotsCount = maxLen - getVisualLength(left) - getVisualLength(right);
      if (dotsCount <= 0) return left + ' ' + right;
      return left + ' ' + '.'.repeat(dotsCount) + ' ' + right;
    };

    if (parsedItems.length === 0) {
      ctx.fillStyle = '#8E8B82';
      ctx.textAlign = 'center';
      ctx.fillText('[ 今日暂无饮食记录 ]', 375, currentY + 40);
      ctx.fillText('继续保持自律生活！', 375, currentY + 80);
      ctx.textAlign = 'left';
      currentY += 120;
    } else {
      parsedItems.forEach((item, index) => {
        if (currentY > 620) return; 
        const itemNumStr = String(index + 1).padStart(2, '0') + '. ';
        const namePart = item.name.length > 8 ? item.name.substring(0, 8) + '..' : item.name;
        const leftText = itemNumStr + namePart + ' (' + item.weight + 'g)';
        const rightText = item.calories + ' KCAL';
        
        ctx.fillText(formatReceiptLine(leftText, rightText), 120, currentY);
        currentY += 45;
      });
    }

    if (parsedItems.length > 0 && currentY < 560) {
      currentY = 560;
    }

    ctx.strokeStyle = '#D6D0C2';
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(120, currentY + 10);
    ctx.lineTo(630, currentY + 10);
    ctx.stroke();
    ctx.setLineDash([]); 
    
    currentY += 50;

    ctx.font = 'bold 24px Courier New, Courier, monospace, sans-serif';
    ctx.fillText(formatReceiptLine('今日累计摄入', grandTotal + ' KCAL'), 120, currentY);
    ctx.font = '24px Courier New, Courier, monospace, sans-serif';
    ctx.fillText(formatReceiptLine('每日目标预算', targetCalories + ' KCAL'), 120, currentY + 45);
    
    const diff = targetCalories - grandTotal;
    const diffText = diff >= 0 ? '+' + diff : String(diff);
    ctx.fillText(formatReceiptLine('热量结余', diffText + ' KCAL'), 120, currentY + 90);

    ctx.save();
    ctx.translate(330, currentY + 15);
    ctx.rotate(-12 * Math.PI / 180); 
    
    if (todayChecked) {
      ctx.strokeStyle = 'rgba(16, 185, 129, 0.75)'; 
      ctx.fillStyle = 'rgba(16, 185, 129, 0.75)';
      ctx.lineWidth = 4;
      ctx.strokeRect(-90, -30, 180, 60);
      ctx.font = 'bold 30px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('已达标', 0, 2);
    } else {
      ctx.strokeStyle = 'rgba(239, 68, 68, 0.75)'; 
      ctx.fillStyle = 'rgba(239, 68, 68, 0.75)';
      ctx.lineWidth = 4;
      ctx.strokeRect(-90, -30, 180, 60);
      ctx.font = 'bold 30px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('进行中', 0, 2);
    }
    ctx.restore();

    ctx.strokeStyle = '#D6D0C2';
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(120, 785);
    ctx.lineTo(630, 785);
    ctx.stroke();
    ctx.setLineDash([]); 

    ctx.fillStyle = '#2D2B27';
    let barX = 140;
    const barY = 815;
    const barHeight = 45;
    const barPatterns = [3, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 2, 1, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2, 4, 2, 1, 3, 2];
    barPatterns.forEach(w => {
      ctx.fillRect(barX, barY, w, barHeight);
      barX += w + Math.floor(Math.random() * 3) + 1;
    });

    ctx.font = '16px Courier New, Courier, monospace, sans-serif';
    ctx.fillStyle = '#4F4E4A';
    ctx.textAlign = 'center';
    ctx.fillText('* ' + (this.data.inviteCode || 'SHIKE') + ' *', 230, 885);

    ctx.fillStyle = '#FFFFFF';
    ctx.strokeStyle = '#E2DDD3';
    ctx.lineWidth = 2;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 510, 695, 120, 120, 12);
    ctx.fill();
    ctx.stroke();

    if (qrImg) {
      ctx.drawImage(qrImg, 520, 705, 100, 100);
    } else {
      ctx.fillStyle = '#6366F1';
      ctx.fillRect(530, 715, 25, 25);
      ctx.fillRect(575, 715, 25, 25);
      ctx.fillRect(530, 760, 25, 25);
      ctx.fillRect(575, 760, 10, 10);
    }
    
    ctx.font = '14px sans-serif';
    ctx.fillStyle = '#8E8B82';
    ctx.fillText('扫码加入小队', 570, 840);

    this.saveCanvasToAlbum(canvas);
  },

  drawPolaroidPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const myId = app.globalData.userInfo ? app.globalData.userInfo.id : null;
    const myMember = this.data.members.find(m => String(m.id) === String(myId)) || null;
    const todayChecked = myMember ? myMember.todayChecked : false;
    const successCount = myMember ? myMember.successCount : 0;

    ctx.clearRect(0, 0, 750, 1000);

    const bgGradient = ctx.createLinearGradient(0, 0, 750, 1000);
    bgGradient.addColorStop(0, '#FAF6F0');
    bgGradient.addColorStop(1, '#EFE4D6');
    ctx.fillStyle = bgGradient;
    ctx.fillRect(0, 0, 750, 1000);

    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(79, 70, 58, 0.16)';
    ctx.shadowBlur = 35;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 15;
    
    ctx.beginPath();
    this.drawRoundedRect(ctx, 100, 120, 550, 620, 8);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetY = 0;

    ctx.fillStyle = '#F1ECE4';
    ctx.fillRect(130, 150, 490, 440);

    if (bgImg) {
      ctx.save();
      ctx.beginPath();
      ctx.rect(130, 150, 490, 440);
      ctx.clip();
      ctx.drawImage(bgImg, 130, 150, 490, 653); 
      ctx.restore();
    }

    ctx.fillStyle = 'rgba(245, 158, 11, 0.35)'; 
    ctx.save();
    ctx.translate(375, 120);
    ctx.rotate(-4 * Math.PI / 180);
    ctx.fillRect(-65, -20, 130, 36);
    ctx.strokeStyle = 'rgba(255,255,255,0.4)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(-65, -20); ctx.lineTo(-65, 16);
    ctx.moveTo(65, -20); ctx.lineTo(65, 16);
    ctx.stroke();
    ctx.restore();

    ctx.textAlign = 'left';
    ctx.fillStyle = '#2C251C';
    
    ctx.font = 'italic bold 26px sans-serif';
    const todayStr = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' });
    ctx.fillText('📅 ' + todayStr, 140, 640);
    
    ctx.font = 'italic bold 24px sans-serif';
    ctx.fillText('🔥 连续自律: ' + successCount + ' 天', 140, 685);

    ctx.save();
    ctx.translate(540, 650);
    ctx.rotate(15 * Math.PI / 180);
    ctx.strokeStyle = todayChecked ? '#10B981' : '#F59E0B';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(0, 0, 40, 0, 2 * Math.PI);
    ctx.stroke();
    ctx.fillStyle = todayChecked ? '#10B981' : '#F59E0B';
    ctx.font = 'bold 20px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(todayChecked ? '已达标' : '进行中', 0, 0);
    ctx.restore();

    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';
    ctx.font = 'italic bold 32px sans-serif';
    ctx.fillStyle = '#4A3E31';
    
    ctx.shadowColor = 'rgba(255, 255, 255, 0.8)';
    ctx.shadowBlur = 4;
    ctx.fillText('“自律食刻，每一口都算数”', 100, 830);
    ctx.font = 'bold 24px sans-serif';
    ctx.fillStyle = '#6E5D4F';
    ctx.fillText('小队: ' + (this.data.teamName || '契约挑战组'), 100, 875);
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;

    ctx.fillStyle = '#FFFFFF';
    ctx.strokeStyle = 'rgba(74, 62, 49, 0.1)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 510, 775, 120, 120, 16);
    ctx.fill();
    ctx.stroke();

    if (qrImg) {
      ctx.drawImage(qrImg, 520, 785, 100, 100);
    } else {
      ctx.fillStyle = '#F59E0B';
      ctx.fillRect(530, 795, 25, 25);
      ctx.fillRect(575, 795, 25, 25);
      ctx.fillRect(530, 840, 25, 25);
      ctx.fillRect(575, 840, 10, 10);
    }

    ctx.textAlign = 'center';
    ctx.font = '14px sans-serif';
    ctx.fillStyle = '#6E5D4F';
    ctx.fillText('扫码加入对赌', 570, 915);

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
  }
});
