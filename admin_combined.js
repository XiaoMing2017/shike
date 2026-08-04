
    let rawUserData = [];
    let currentDetailRecords = null;
    let currentModalAvatarUrl = '';

    // Image URL Resolver (handles local fallback, WeChat qlogo, and /uploads/ paths)
    function resolveAvatarUrl(url) {
      const defaultAvatar = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';
      if (!url || url === '/images/profile.png' || url === 'tmp' || url.includes('.tmp')) {
        return defaultAvatar;
      }
      if (url.startsWith('/uploads/')) {
        return '/api/v1' + url;
      }
      if (url.startsWith('uploads/')) {
        return '/api/v1/' + url;
      }
      return url;
    }

    function previewAvatar(url, name) {
      const realUrl = resolveAvatarUrl(url);
      document.getElementById('lightboxImg').src = realUrl;
      document.getElementById('lightboxTitle').innerText = `用户 [ ${name} ] 的高清头像`;
      document.getElementById('lightboxLink').href = realUrl;
      document.getElementById('imageLightbox').classList.add('active');
    }

    function previewModalUserAvatar() {
      if (currentModalAvatarUrl) {
        previewAvatar(currentModalAvatarUrl, document.getElementById('modalUserName').innerText);
      }
    }

    function closeLightbox() {
      document.getElementById('imageLightbox').classList.remove('active');
    }

    // Fetch Dashboard Stats & User List
    async function loadDashboard() {
      try {
        const [statsRes, usersRes] = await Promise.all([
          fetch('/api/v1/admin/stats').then(res => res.json()),
          fetch('/api/v1/admin/users').then(res => res.json())
        ]);

        if (statsRes.code === 200) {
          const stats = statsRes.data;
          document.getElementById('totalUsers').innerText = stats.totalUsers || 0;
          document.getElementById('todayNewUsers').innerText = stats.todayNewUsers || 0;
          document.getElementById('todayAiRecognitions').innerText = stats.todayAiRecognitions || 0;
          document.getElementById('todayDietRecords').innerText = stats.todayDietRecords || 0;
          document.getElementById('todayActiveUsers').innerText = stats.todayActiveUsers || 0;
          document.getElementById('activeTeams').innerText = stats.activeTeams || 0;
        }

        if (usersRes.code === 200) {
          rawUserData = usersRes.data || [];
          renderUserTable(rawUserData);
        }
      } catch (err) {
        console.error('Failed to load admin data:', err);
      }
    }

    function renderUserTable(users) {
      const tbody = document.getElementById('userTableBody');
      if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty-tip">暂无匹配的用户数据</td></tr>';
        return;
      }

      tbody.innerHTML = users.map(item => {
        const u = item.user;
        const nickname = u.nickname || '微信用户';
        const avatar = resolveAvatarUrl(u.avatarUrl);
        const genderStr = u.gender === 2 ? '女 ♀️' : (u.gender === 1 ? '男 ♂️' : '未设置');
        const statsStr = (u.height && u.weight) ? `${u.height}cm / ${u.weight}kg` : '未录入';
        const calStr = u.targetCalories ? `${u.targetCalories} kcal` : '未计算';
        const aiCount = item.todayAiCount || 0;
        const aiBadgeClass = aiCount >= 10 ? 'badge-ai limit-reached' : 'badge-ai';
        const createdAt = u.createdAt ? u.createdAt.replace('T', ' ').substring(0, 16) : '-';

        return `
          <tr>
            <td>
              <div class="user-cell">
                <img src="${avatar}" class="user-avatar" title="点击放大预览头像" onclick="previewAvatar('${u.avatarUrl || ''}', '${nickname}')" onerror="this.src='https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0'">
                <div>
                  <div class="user-name">${nickname}</div>
                  <div class="user-id-badge">ID: ${u.id}</div>
                </div>
              </div>
            </td>
            <td>${genderStr} · ${u.age || '-'}岁<br><span style="font-size:12px; color:var(--text-muted);">${statsStr}</span></td>
            <td><strong>${calStr}</strong><br><span style="font-size:11px; color:var(--text-muted);">TDEE: ${u.tdee || '-'}</span></td>
            <td><span class="badge ${aiBadgeClass}">${aiCount} / 10 次</span></td>
            <td>${item.totalDietCount || 0} 餐 / ${item.totalExerciseCount || 0} 运动</td>
            <td><span class="badge badge-points">${u.points || 0} pts</span></td>
            <td><span style="font-size:12px; color:var(--text-muted);">${createdAt}</span></td>
            <td>
              <button class="btn-action" onclick="viewUserDetail(${u.id})">
                <i class="fa-solid fa-chart-line"></i> 使用明细
              </button>
            </td>
          </tr>
        `;
      }).join('');
    }

    function filterUsers() {
      const q = document.getElementById('searchInput').value.trim().toLowerCase();
      if (!q) {
        renderUserTable(rawUserData);
        return;
      }
      const filtered = rawUserData.filter(item => {
        const u = item.user;
        return (u.nickname && u.nickname.toLowerCase().includes(q)) || 
               String(u.id).includes(q) || 
               (u.openid && u.openid.toLowerCase().includes(q));
      });
      renderUserTable(filtered);
    }

    async function viewUserDetail(userId) {
      document.getElementById('detailModal').classList.add('active');
      document.getElementById('dietList').innerHTML = '<div class="empty-tip">加载明细数据中...</div>';
      
      try {
        const res = await fetch(`/api/v1/admin/users/${userId}/records`).then(r => r.json());
        if (res.code === 200) {
          currentDetailRecords = res.data;
          const u = currentDetailRecords.user;
          currentModalAvatarUrl = u.avatarUrl || '';
          document.getElementById('modalUserAvatar').src = resolveAvatarUrl(u.avatarUrl);
          document.getElementById('modalUserName').innerText = u.nickname || '微信用户';
          document.getElementById('modalUserOpenid').innerText = `OpenID: ${u.openid}`;
          document.getElementById('modalBodyStats').innerText = `${u.height || '-'} cm / ${u.weight || '-'} kg (${u.age || '-'} 岁)`;
          document.getElementById('modalMetabolism').innerText = `BMR: ${u.bmr || '-'} | TDEE: ${u.tdee || '-'}`;
          document.getElementById('modalTargetCal').innerText = `${u.goal || 'MAINTAIN'} (${u.targetCalories || '-'} kcal)`;
          document.getElementById('modalPoints').innerText = `${u.points || 0} pts`;

          renderDietRecords(currentDetailRecords.dietRecords);
          renderExerciseRecords(currentDetailRecords.exerciseRecords, currentDetailRecords.waterRecords);
          renderTeamCheckins(currentDetailRecords.teamCheckins);
        }
      } catch (err) {
        console.error('Failed to load user records:', err);
      }
    }

    function renderDietRecords(records) {
      const list = document.getElementById('dietList');
      if (!records || records.length === 0) {
        list.innerHTML = '<div class="empty-tip">该用户暂无饮食打卡记录</div>';
        return;
      }

      list.innerHTML = records.map(r => {
        let foodItemsHtml = '';
        try {
          const items = JSON.parse(r.foodItems || '[]');
          foodItemsHtml = items.map(i => `<span style="font-size:12px; color:var(--primary); font-weight:600;">${i.name} (${i.calories}kcal)</span>`).join('、');
        } catch(e) {
          foodItemsHtml = r.foodItems || '-';
        }

        const dateStr = r.recordDate || '';
        const imgUrl = resolveAvatarUrl(r.imageUrl);
        const imgHtml = r.imageUrl ? `<img src="${imgUrl}" class="food-img" title="点击放大图片" onclick="previewAvatar('${r.imageUrl}', '餐品解析图片')">` : '';

        return `
          <div class="food-card">
            <div class="food-card-header">
              <span class="meal-tag">${r.mealType}</span>
              <span style="font-size:12px; color:var(--text-muted);">${dateStr}</span>
            </div>
            ${imgHtml}
            <div style="font-size:13px; font-weight:700; margin: 4px 0;">${r.totalCalories} kcal</div>
            <div style="font-size:12px; color:var(--text-muted); margin-bottom: 4px;">${foodItemsHtml}</div>
            <div style="font-size:11px; color:var(--text-muted);">
              碳水:${r.totalCarbs || 0}g | 蛋白:${r.totalProtein || 0}g | 脂肪:${r.totalFat || 0}g
            </div>
          </div>
        `;
      }).join('');
    }

    function renderExerciseRecords(exercises, waters) {
      const container = document.getElementById('exerciseList');
      let html = '<h4 style="margin-bottom:12px; font-size:14px; color:var(--primary);">🏃 运动记录</h4>';
      
      if (!exercises || exercises.length === 0) {
        html += '<div class="empty-tip" style="padding:20px;">暂无运动记录</div>';
      } else {
        html += '<table><thead><tr><th>日期</th><th>运动项目</th><th>消耗热量</th></tr></thead><tbody>';
        html += exercises.map(e => `
          <tr>
            <td>${e.recordDate}</td>
            <td>${e.activityName || '运动'}</td>
            <td style="color:#F97316; font-weight:700;">-${e.caloriesBurned} kcal</td>
          </tr>
        `).join('');
        html += '</tbody></table>';
      }

      html += '<h4 style="margin:20px 0 12px 0; font-size:14px; color:#3B82F6;">💧 饮水记录</h4>';
      if (!waters || waters.length === 0) {
        html += '<div class="empty-tip" style="padding:20px;">暂无饮水记录</div>';
      } else {
        html += '<table><thead><tr><th>日期</th><th>饮水总量</th></tr></thead><tbody>';
        html += waters.map(w => `
          <tr>
            <td>${w.recordDate}</td>
            <td style="color:#3B82F6; font-weight:700;">${w.amount} ml</td>
          </tr>
        `).join('');
        html += '</tbody></table>';
      }

      container.innerHTML = html;
    }

    function renderTeamCheckins(checkins) {
      const container = document.getElementById('teamCheckinList');
      if (!checkins || checkins.length === 0) {
        container.innerHTML = '<div class="empty-tip">该用户暂无小队打卡记录</div>';
        return;
      }

      container.innerHTML = `
        <table>
          <thead>
            <tr>
              <th>小队 ID</th>
              <th>打卡日期</th>
              <th>打卡状态</th>
            </tr>
          </thead>
          <tbody>
            ${checkins.map(c => `
              <tr>
                <td>Team #${c.teamId}</td>
                <td>${c.checkinDate}</td>
                <td>
                  <span class="badge ${c.isSuccess === 1 ? 'badge-points' : 'badge-ai limit-reached'}">
                    ${c.isSuccess === 1 ? '✅ 打卡成功' : '❌ 未打卡/失败'}
                  </span>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }

    function switchTab(tab) {
      document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
      event.target.classList.add('active');

      document.getElementById('tabDiet').style.display = tab === 'diet' ? 'block' : 'none';
      document.getElementById('tabExercise').style.display = tab === 'exercise' ? 'block' : 'none';
      document.getElementById('tabTeam').style.display = tab === 'team' ? 'block' : 'none';
    }

    function closeModal() {
      document.getElementById('detailModal').classList.remove('active');
    }

    // --- Team List & Members Modal Management ---
    let rawTeamData = [];
    let currentTeamFilter = 'ALL';

    async function openTeamModal() {
      document.getElementById('teamListModal').classList.add('active');
      document.getElementById('teamCardsContainer').innerHTML = '<div class="empty-tip">加载小队数据中...</div>';
      document.getElementById('teamModalSubtitle').innerText = '正在获取全部减脂小队与成员数据...';

      try {
        const res = await fetch('/api/v1/admin/teams').then(r => r.json());
        if (res.code === 200) {
          rawTeamData = res.data || [];
          const activeCount = rawTeamData.filter(t => t.status === 'ACTIVE').length;
          document.getElementById('teamModalSubtitle').innerText = `目前共有 ${rawTeamData.length} 支小队（包含 ${activeCount} 支运行中战队）`;
          renderTeamCards(rawTeamData);
        } else {
          document.getElementById('teamCardsContainer').innerHTML = '<div class="empty-tip">加载小队数据失败</div>';
        }
      } catch (err) {
        console.error('Failed to load teams:', err);
        document.getElementById('teamCardsContainer').innerHTML = '<div class="empty-tip">网络请求异常</div>';
      }
    }

    function closeTeamModal() {
      document.getElementById('teamListModal').classList.remove('active');
    }

    function setTeamFilter(filter) {
      currentTeamFilter = filter;
      document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
      if (filter === 'ALL') document.getElementById('chipAll').classList.add('active');
      if (filter === 'ACTIVE') document.getElementById('chipActive').classList.add('active');
      if (filter === 'FINISHED') document.getElementById('chipFinished').classList.add('active');
      filterTeams();
    }

    function filterTeams() {
      const q = document.getElementById('teamSearchInput').value.trim().toLowerCase();
      let list = rawTeamData;

      if (currentTeamFilter === 'ACTIVE') {
        list = list.filter(t => t.status === 'ACTIVE');
      } else if (currentTeamFilter === 'FINISHED') {
        list = list.filter(t => t.status !== 'ACTIVE');
      }

      if (q) {
        list = list.filter(t => {
          const matchName = t.teamName && t.teamName.toLowerCase().includes(q);
          const matchCode = t.inviteCode && t.inviteCode.toLowerCase().includes(q);
          const matchCreator = t.creatorName && t.creatorName.toLowerCase().includes(q);
          const matchMember = t.members && t.members.some(m => m.nickname && m.nickname.toLowerCase().includes(q));
          return matchName || matchCode || matchCreator || matchMember;
        });
      }

      renderTeamCards(list);
    }

    function renderTeamCards(teams) {
      const container = document.getElementById('teamCardsContainer');
      if (!teams || teams.length === 0) {
        container.innerHTML = '<div class="empty-tip">暂无匹配的打卡小队</div>';
        return;
      }

      container.innerHTML = teams.map(t => {
        const isRunning = t.status === 'ACTIVE';
        const statusHtml = isRunning 
          ? '<span class="status-badge status-active">🔥 运行中</span>' 
          : `<span class="status-badge status-finished">🏁 ${t.status}</span>`;
        const dateStr = t.createdAt ? t.createdAt.replace('T', ' ').substring(0, 10) : '-';

        const membersHtml = (t.members || []).map(m => {
          const mAvatar = resolveAvatarUrl(m.avatarUrl);
          const roleHtml = m.isCreator 
            ? '<span class="role-badge-creator"><i class="fa-solid fa-crown"></i> 队长</span>' 
            : '<span class="role-badge-member"><i class="fa-solid fa-user"></i> 队员</span>';
          const checkedBadge = m.todayChecked 
            ? '<span class="badge badge-points">✅ 今日已打卡</span>' 
            : '<span class="badge badge-ai limit-reached">⏳ 今日未打卡</span>';
          const joinTime = m.joinedAt ? m.joinedAt.replace('T', ' ').substring(0, 10) : '-';

          return `
            <tr>
              <td>
                <div class="user-cell">
                  <img src="${mAvatar}" class="user-avatar" title="点击放大预览头像" onclick="previewAvatar('${m.avatarUrl || ''}', '${m.nickname}')" onerror="this.src='https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0'">
                  <div>
                    <div class="user-name">${m.nickname} ${roleHtml}</div>
                    <div class="user-id-badge">ID: ${m.userId}</div>
                  </div>
                </div>
              </td>
              <td>${checkedBadge}</td>
              <td><strong style="color:var(--primary);">${m.successCount} 天</strong> / ${t.targetDays} 天</td>
              <td><span style="color:#F97316; font-weight:700;">${m.points} pts</span></td>
              <td><span style="font-size:12px; color:var(--text-muted);">${joinTime}</span></td>
            </tr>
          `;
        }).join('');

        return `
          <div class="team-card">
            <div class="team-header">
              <div class="team-title">
                <i class="fa-solid fa-flag" style="color: #3B82F6;"></i> ${t.teamName}
                ${statusHtml}
              </div>
              <div style="display:flex; align-items:center; gap:12px;">
                <span style="font-size: 13px; color: var(--text-muted);">
                  邀请码: <strong style="color:#FFF; background:rgba(255,255,255,0.1); padding:2px 8px; border-radius:6px; font-family:monospace; font-size:14px;">${t.inviteCode}</strong>
                </span>
                ${isRunning ? `<button class="filter-chip" onclick="toggleTeamStatus(${t.id}, 'FAILED')" style="border-color:#EF4444; color:#F87171; font-size:11px; padding:3px 8px;"><i class="fa-solid fa-power-off"></i> 结束战队</button>` : ''}
              </div>
            </div>

            <div class="team-meta">
              <div class="team-meta-item">
                <i class="fa-solid fa-crown" style="color:#F59E0B;"></i> 队长: <strong>${t.creatorName}</strong> (ID: ${t.creatorId})
              </div>
              <div class="team-meta-item">
                <i class="fa-solid fa-calendar-check" style="color:#10B981;"></i> 目标: <strong>${t.targetDays} 天</strong> (已进行第 ${t.currentDay} 天)
              </div>
              <div class="team-meta-item">
                <i class="fa-solid fa-coins" style="color:#F97316;"></i> 契约保证金: <strong>${t.depositPoints} pts</strong> / 人
              </div>
              <div class="team-meta-item">
                <i class="fa-solid fa-users" style="color:#3B82F6;"></i> 成员人数: <strong>${t.memberCount} 人</strong>
              </div>
              <div class="team-meta-item">
                <i class="fa-solid fa-clock"></i> 创建时间: ${dateStr}
              </div>
            </div>

            <div class="table-responsive" style="margin-top: 10px;">
              <table>
                <thead>
                  <tr>
                    <th>队伍成员</th>
                    <th>今日打卡状态</th>
                    <th>累计打卡成功数</th>
                    <th>契约积分</th>
                    <th>加入时间</th>
                  </tr>
                </thead>
                <tbody>
                  ${membersHtml || '<tr><td colspan="5" class="empty-tip">暂无成员</td></tr>'}
                </tbody>
              </table>
            </div>
          </div>
        `;
      }).join('');
    }

    async function toggleTeamStatus(teamId, newStatus) {
      if (!confirm(`确定要将小队 (ID: ${teamId}) 的状态修改为 [ ${newStatus} ] 吗？`)) return;
      try {
        const res = await fetch(`/api/v1/admin/teams/${teamId}/status?status=${newStatus}`, { method: 'POST' }).then(r => r.json());
        if (res.code === 200) {
          openTeamModal();
          loadDashboard();
        } else {
          alert('操作失败: ' + (res.message || '未知错误'));
        }
      } catch (err) {
        console.error('Update status error:', err);
        alert('请求失败');
      }
    }

    // Auto-update clock
    setInterval(() => {
      const now = new Date();
      document.getElementById('currentTime').innerText = now.toLocaleString('zh-CN');
    }, 1000);

    // Initial Load
    loadDashboard();
  