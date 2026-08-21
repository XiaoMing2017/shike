// pet_entity_3d.js - 2.5D 自主生命萌宠实体与状态机
class PetEntity3D {
  constructor(canvas, species = 'DRAGON', stageRank = 1) {
    this.canvas = canvas;
    this.species = species;
    this.stageRank = stageRank;

    // 空间坐标与导航 (以浮岛中心 600x496 为相对坐标系)
    this.x = 270;
    this.y = 310;
    this.targetX = 270;
    this.targetY = 310;
    this.width = 110;
    this.height = 110;
    this.facing = 1; // 1: 右侧, -1: 左侧
    this.moveSpeed = 48; // px / sec

    // 状态机系统
    this.state = 'IDLE'; // IDLE, WANDER, SNIFF_FLOWER, DRINK_WATER, REST_SIT, SLEEP, JOY_BOUNCE, EAT
    this.stateTimer = 0;
    this.stateDuration = 4.5;
    this.animTime = 0;
    this.walkCycle = 0;

    // 弹簧物理与表情
    this.scaleX = 1.0;
    this.scaleY = 1.0;
    this.jumpY = 0;
    this.emojiBubble = '';
    this.emojiTimer = 0;

    // 加载萌宠高清立绘图像
    this.img = null;
    this.initImage();
  }

  initImage() {
    if (!this.canvas || typeof this.canvas.createImage !== 'function') return;
    const img = this.canvas.createImage();
    const speciesMap = {
      DRAGON: 'dragon',
      TOTORO: 'totoro',
      CAT: 'cat',
      DOG: 'dog'
    };
    const key = speciesMap[this.species] || 'dragon';
    const rank = Math.min(3, Math.max(1, this.stageRank || 1));
    img.src = `/images/pets/pet_${key}_stage${rank}.png`;
    img.onload = () => {
      this.img = img;
    };
  }

  setSpecies(species, stageRank) {
    this.species = species;
    this.stageRank = stageRank;
    this.initImage();
  }

  // 导航寻路
  navigateTo(tx, ty, nextAction = 'IDLE') {
    this.targetX = tx;
    this.targetY = ty;
    this.nextAction = nextAction;
    this.state = 'WANDER';
    this.stateTimer = 0;
  }

  triggerTapReaction() {
    this.state = 'JOY_BOUNCE';
    this.stateTimer = 0;
    this.stateDuration = 2.2;
    this.showEmoji('💖');
  }

  triggerFeedAction(bowlPos) {
    this.showEmoji('🍎');
    if (bowlPos) {
      this.navigateTo(bowlPos.x, bowlPos.y, 'EAT');
    } else {
      this.state = 'EAT';
      this.stateTimer = 0;
      this.stateDuration = 3.5;
    }
  }

  showEmoji(emoji) {
    this.emojiBubble = emoji;
    this.emojiTimer = 2.5;
  }

  // 自主生命状态机帧更新
  update(dt, waypoints = []) {
    this.animTime += dt;
    this.stateTimer += dt;

    if (this.emojiTimer > 0) {
      this.emojiTimer -= dt;
      if (this.emojiTimer <= 0) this.emojiBubble = '';
    }

    // 1. 状态转移逻辑
    if (this.state === 'WANDER') {
      const dx = this.targetX - this.x;
      const dy = this.targetY - this.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dx > 2) this.facing = 1;
      else if (dx < -2) this.facing = -1;

      if (dist < 4) {
        this.x = this.targetX;
        this.y = this.targetY;
        this.state = this.nextAction || 'IDLE';
        this.stateTimer = 0;
        this.stateDuration = 3.5 + Math.random() * 4.0;
        this.jumpY = 0;
      } else {
        const step = this.moveSpeed * dt;
        this.x += (dx / dist) * Math.min(step, dist);
        this.y += (dy / dist) * Math.min(step, dist);
        this.walkCycle += dt * 9;
        this.jumpY = Math.abs(Math.sin(this.walkCycle)) * 9; // 欢快蹦跳
      }
    } else {
      this.jumpY = 0;
      // 状态倒计时结束，自主选择下一个生活行为
      if (this.stateTimer >= this.stateDuration && waypoints.length > 0) {
        const randomWp = waypoints[Math.floor(Math.random() * waypoints.length)];
        this.navigateTo(randomWp.x, randomWp.y, randomWp.action);
      }
    }

    // 2. 状态动作微表情与形变动画
    switch (this.state) {
      case 'IDLE':
        // 软糯待机呼吸
        this.scaleY = 1.0 + Math.sin(this.animTime * 3.2) * 0.04;
        this.scaleX = 1.0 - Math.sin(this.animTime * 3.2) * 0.03;
        break;
      case 'SNIFF_FLOWER':
        // 低头闻花
        this.scaleY = 0.94 + Math.sin(this.animTime * 4.0) * 0.03;
        this.scaleX = 1.06;
        if (Math.random() < 0.03 && !this.emojiBubble) this.showEmoji('🌸');
        break;
      case 'DRINK_WATER':
        // 俯身喝水
        this.scaleY = 0.90 + Math.sin(this.animTime * 5.0) * 0.04;
        this.scaleX = 1.08;
        if (Math.random() < 0.03 && !this.emojiBubble) this.showEmoji('💧');
        break;
      case 'REST_SIT':
        // 坐下休息
        this.scaleY = 0.92;
        this.scaleX = 1.06;
        break;
      case 'SLEEP':
        // 蜷缩打瞌睡
        this.scaleY = 0.88 + Math.sin(this.animTime * 1.8) * 0.02;
        this.scaleX = 1.10;
        if (Math.random() < 0.02 && !this.emojiBubble) this.showEmoji('💤');
        break;
      case 'JOY_BOUNCE':
        // 360° 欢快后空翻跳跃
        this.jumpY = Math.abs(Math.sin(this.animTime * 7.0)) * 22;
        this.scaleY = 1.0 + Math.sin(this.animTime * 14.0) * 0.12;
        this.scaleX = 1.0 - Math.sin(this.animTime * 14.0) * 0.08;
        break;
      case 'EAT':
        // 欢快进食嚼嚼
        this.scaleY = 1.0 + Math.abs(Math.sin(this.animTime * 9.0)) * 0.08;
        this.scaleX = 1.0 - Math.abs(Math.sin(this.animTime * 9.0)) * 0.05;
        break;
    }
  }

  // 渲染萌宠
  render(ctx, offsetX, offsetY, scale) {
    const drawX = (this.x + offsetX) * scale;
    const drawY = (this.y + offsetY - this.jumpY) * scale;
    const w = this.width * scale;
    const h = this.height * scale;

    ctx.save();

    // 1. 绘制地面柔和接触阴影 (Soft Shadow)
    const shadowW = w * 0.65 * (1.0 - (this.jumpY / 45));
    const shadowH = h * 0.22 * (1.0 - (this.jumpY / 45));
    const groundY = (this.y + offsetY + this.height * 0.40) * scale;
    
    ctx.beginPath();
    ctx.ellipse((this.x + offsetX) * scale, groundY, Math.max(5, shadowW / 2), Math.max(2, shadowH / 2), 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(28, 25, 23, 0.22)';
    ctx.fill();

    // 2. 绘制萌宠身体
    ctx.translate(drawX, drawY);
    if (this.facing < 0) ctx.scale(-1, 1);
    ctx.scale(this.scaleX, this.scaleY);

    if (this.img) {
      ctx.drawImage(this.img, -w / 2, -h / 2, w, h);
    } else {
      // 备用纯色圆球占位
      ctx.beginPath();
      ctx.arc(0, 0, w / 2, 0, Math.PI * 2);
      ctx.fillStyle = '#86EFAC';
      ctx.fill();
    }

    ctx.restore();

    // 3. 绘制自主情感微气泡 (Emoji Bubble)
    if (this.emojiBubble) {
      ctx.save();
      const bubbleX = drawX;
      const bubbleY = drawY - h * 0.58 - Math.sin(this.animTime * 4.0) * 4;
      
      // 气泡圆角背景
      ctx.beginPath();
      ctx.arc(bubbleX, bubbleY, 16 * scale, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
      ctx.shadowColor = 'rgba(0, 0, 0, 0.12)';
      ctx.shadowBlur = 6;
      ctx.fill();

      // 表情文字
      ctx.font = `${Math.round(18 * scale)}px sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(this.emojiBubble, bubbleX, bubbleY + 1);
      ctx.restore();
    }
  }
}

module.exports = { PetEntity3D };
