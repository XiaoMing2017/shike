// pet_world_3d.js - 2.5D 卡通 3D 浮空岛微缩世界引擎 (分层物理遮挡 + 视差阻尼 + 自主萌宠生命)
const { PetEntity3D } = require('./pet_entity_3d');

class PetWorld3D {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    console.log('[PetWorld3D] canvas.width:', canvas.width, 'canvas.height:', canvas.height, 'ctx:', !!this.ctx);
    this.options = options;
    this.isDestroyed = false;

    this.width = options.width || 375;
    this.height = options.height || 300;
    this.dpr = options.pixelRatio || 2;

    // 🔴 最基础的直绘测试（确认 ctx 能把颜色写到屏幕）
    if (this.ctx) {
      try {
        const cw = this.canvas.width;
        const ch = this.canvas.height;
        this.ctx.fillStyle = '#BAE6FD';
        this.ctx.fillRect(0, 0, cw, ch);
        this.ctx.fillStyle = '#10B981';
        this.ctx.fillRect(cw * 0.1, ch * 0.1, cw * 0.8, ch * 0.8);
        console.log('[PetWorld3D] ✅ Basic draw test executed, cw=', cw, 'ch=', ch);
      } catch (e) {
        console.error('[PetWorld3D] ❌ Basic draw test FAILED:', e && e.message);
      }
    }

    // 空间视差摄像机与阻尼系统
    this.isDragging = false;
    this.touchStartX = 0;
    this.touchStartY = 0;
    this.panX = 0;
    this.panY = 0;
    this.targetPanX = 0;
    this.targetPanY = 0;
    this.time = 0;
    this.lastTimestamp = Date.now();

    // 远景漂浮云层 (3D 空间独立对象)
    this.clouds = [
      { x: 30, y: 35, scale: 0.85, speed: 4.5, opacity: 0.85 },
      { x: 220, y: 25, scale: 1.15, speed: 3.2, opacity: 0.90 },
      { x: 440, y: 40, scale: 0.95, speed: 5.0, opacity: 0.80 },
      { x: 120, y: 70, scale: 0.70, speed: 2.8, opacity: 0.75 }
    ];

    // 樱花飘落与金粉粒子系统
    this.particles = [];
    this.initParticles();

    // 浮岛兴趣点导航网络 (Waypoints)
    this.waypoints = [
      { name: 'RUG', x: 270, y: 310, action: 'IDLE' },
      { name: 'POND', x: 230, y: 245, action: 'DRINK_WATER' },
      { name: 'FLOWER', x: 165, y: 215, action: 'SNIFF_FLOWER' },
      { name: 'BEHIND_TREE', x: 420, y: 195, action: 'IDLE' }, // 走到大树后方，触发真实遮挡
      { name: 'SOFA', x: 385, y: 330, action: 'REST_SIT' },
      { name: 'BRIDGE', x: 350, y: 300, action: 'WANDER' },
      { name: 'GARDEN', x: 240, y: 365, action: 'JOY_BOUNCE' }
    ];
    this.foodBowlPos = { x: 200, y: 335 };

    // 载入各独立空间图层
    this.layers = {};
    this.loadLayers();

    // 创建 3D 萌宠自主实体
    this.pet = new PetEntity3D(canvas, options.species || 'DRAGON', options.stageRank || 1, () => {
      this.renderScene(0);
    });

    // 🌟 同步首帧即时绘制，拒绝任何加载黑白闪烁
    this.renderScene(0);

    // 启动 60FPS 跨端稳定渲染循环
    this.animate = this.animate.bind(this);
    this.timerId = setInterval(() => {
      this.animate();
    }, 16);
  }

  loadLayers() {
    const layerConfigs = [
      { key: 'island', path: '/images/pets/world_layers/layer_island_base.png', fallback: '../../images/pets/world_layers/layer_island_base.png' },
      { key: 'tree', path: '/images/pets/world_layers/layer_tree.png', fallback: '../../images/pets/world_layers/layer_tree.png' },
      { key: 'bridge', path: '/images/pets/world_layers/layer_bridge.png', fallback: '../../images/pets/world_layers/layer_bridge.png' },
      { key: 'foreground', path: '/images/pets/world_layers/layer_foreground.png', fallback: '../../images/pets/world_layers/layer_foreground.png' }
    ];

    layerConfigs.forEach(cfg => {
      if (this.canvas && typeof this.canvas.createImage === 'function') {
        const img = this.canvas.createImage();
        img.onload = () => {
          this.layers[cfg.key] = img;
          this.renderScene(0);
        };
        img.onerror = () => {
          const fb = this.canvas.createImage();
          fb.onload = () => { 
            this.layers[cfg.key] = fb; 
            this.renderScene(0);
          };
          fb.src = cfg.fallback;
        };
        img.src = cfg.path;
      }
    });
  }

  initParticles() {
    this.particles = [];
    for (let i = 0; i < 18; i++) {
      this.particles.push({
        x: Math.random() * 600,
        y: Math.random() * 450,
        size: 3 + Math.random() * 4,
        speedX: -15 - Math.random() * 20,
        speedY: 10 + Math.random() * 15,
        rot: Math.random() * Math.PI * 2,
        rotSpeed: (Math.random() - 0.5) * 2,
        alpha: 0.4 + Math.random() * 0.5,
        type: i % 3 === 0 ? 'SPARKLE' : 'PETAL'
      });
    }
  }

  onTouchStart(e) {
    if (!e.touches || e.touches.length === 0) return;
    this.isDragging = true;
    this.touchStartX = e.touches[0].clientX;
    this.touchStartY = e.touches[0].clientY;
  }

  onTouchMove(e) {
    if (!this.isDragging || !e.touches || e.touches.length === 0) return;
    const dx = e.touches[0].clientX - this.touchStartX;
    const dy = e.touches[0].clientY - this.touchStartY;
    this.touchStartX = e.touches[0].clientX;
    this.touchStartY = e.touches[0].clientY;

    this.targetPanX = Math.max(-28, Math.min(28, this.targetPanX + dx * 0.45));
    this.targetPanY = Math.max(-18, Math.min(18, this.targetPanY + dy * 0.35));
  }

  onTouchEnd() {
    this.isDragging = false;
  }

  triggerPetTap() {
    if (this.pet) this.pet.triggerTapReaction();
  }

  triggerPetFeed() {
    if (this.pet) this.pet.triggerFeedAction(this.foodBowlPos);
  }

  navigateToSpot(spotName) {
    const found = this.waypoints.find(w => w.name === spotName);
    if (found && this.pet) {
      this.pet.navigateTo(found.x, found.y, found.action);
    }
  }

  setSpecies(species, stageRank) {
    if (this.pet) {
      this.pet.setSpecies(species, stageRank);
      this.renderScene(0);
    }
  }

  animate() {
    if (this.isDestroyed) return;

    const now = Date.now();
    const dt = Math.min(0.1, (now - this.lastTimestamp) / 1000 || 0.016);
    this.lastTimestamp = now;
    this.time += dt;

    this.panX += (this.targetPanX - this.panX) * 0.12;
    this.panY += (this.targetPanY - this.panY) * 0.12;

    const idleSway = Math.sin(this.time * 0.8) * 2.5;

    if (this.pet) {
      this.pet.update(dt, this.waypoints);
    }

    this.clouds.forEach(c => {
      c.x += c.speed * dt;
      if (c.x > 620) c.x = -80;
    });

    this.particles.forEach(p => {
      p.x += p.speedX * dt;
      p.y += p.speedY * dt;
      p.rot += p.rotSpeed * dt;
      if (p.x < -20 || p.y > 480) {
        p.x = 550 + Math.random() * 80;
        p.y = -20 + Math.random() * 40;
      }
    });

    this.renderScene(idleSway);
  }

  renderScene(idleSway = 0) {
    const ctx = this.ctx;
    if (!ctx) return;

    try {
      const cw = this.canvas.width;
      const ch = this.canvas.height;

      if (!cw || !ch) {
        console.warn('[PetWorld3D] cw/ch is 0:', cw, ch);
        return;
      }

      ctx.clearRect(0, 0, cw, ch);

      const baseScale = (cw / 600);
      const originX = (cw - 600 * baseScale) / 2;
      const originY = (ch - 496 * baseScale) / 2 + 10 * baseScale;

      // ☁️ 1. 天空渐变背景
      const skyPanX = (this.panX + idleSway) * 0.15;
      const skyPanY = this.panY * 0.15;

      const skyGrad = ctx.createLinearGradient(0, 0, 0, ch);
      skyGrad.addColorStop(0, '#93C5FD');
      skyGrad.addColorStop(0.55, '#BAE6FD');
      skyGrad.addColorStop(1.0, '#E0F2FE');
      ctx.fillStyle = skyGrad;
      ctx.fillRect(0, 0, cw, ch);

      // ☁️ 云层
      this.clouds.forEach(cloud => {
        ctx.save();
        const cx = (cloud.x + skyPanX) * baseScale;
        const cy = (cloud.y + skyPanY) * baseScale;
        const s = cloud.scale * baseScale;
        ctx.fillStyle = `rgba(255, 255, 255, ${cloud.opacity})`;
        ctx.beginPath();
        ctx.arc(cx, cy, 22 * s, 0, Math.PI * 2);
        ctx.arc(cx + 18 * s, cy - 6 * s, 26 * s, 0, Math.PI * 2);
        ctx.arc(cx + 42 * s, cy - 2 * s, 20 * s, 0, Math.PI * 2);
        ctx.arc(cx + 26 * s, cy + 8 * s, 18 * s, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      });

      // 🏝️ 2. 浮空岛基底
      const islandPanX = this.panX + idleSway;
      const islandPanY = this.panY;

      if (this.layers.island) {
        ctx.drawImage(
          this.layers.island,
          originX + islandPanX * baseScale,
          originY + islandPanY * baseScale,
          600 * baseScale,
          496 * baseScale
        );
      } else {
        ctx.save();
        ctx.fillStyle = '#86EFAC';
        ctx.beginPath();
        ctx.ellipse(originX + (300 + islandPanX) * baseScale, originY + (280 + islandPanY) * baseScale, 240 * baseScale, 140 * baseScale, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

      // 🌊 3. 水波微光
      ctx.save();
      const shimmerAlpha = 0.25 + Math.sin(this.time * 4.5) * 0.15;
      ctx.fillStyle = `rgba(255, 255, 255, ${shimmerAlpha})`;
      const pondX = originX + (250 + islandPanX) * baseScale;
      const pondY = originY + (240 + islandPanY) * baseScale;
      ctx.beginPath();
      ctx.ellipse(pondX, pondY, 40 * baseScale, 18 * baseScale, -0.2, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      // 🌲 4. 深度排序遮挡
      const renderQueue = [];

      renderQueue.push({
        type: 'TREE', depth: 295,
        render: () => {
          if (this.layers.tree) {
            ctx.drawImage(this.layers.tree,
              originX + (348 + islandPanX) * baseScale, originY + (85 + islandPanY) * baseScale,
              229 * baseScale, 279 * baseScale);
          }
        }
      });

      renderQueue.push({
        type: 'BRIDGE', depth: 350,
        render: () => {
          if (this.layers.bridge) {
            ctx.drawImage(this.layers.bridge,
              originX + (317 + islandPanX) * baseScale, originY + (270 + islandPanY) * baseScale,
              152 * baseScale, 132 * baseScale);
          }
        }
      });

      if (this.pet) {
        renderQueue.push({
          type: 'PET', depth: this.pet.y + 20,
          render: () => {
            this.pet.render(ctx, originX / baseScale + islandPanX, originY / baseScale + islandPanY, baseScale);
          }
        });
      }

      renderQueue.sort((a, b) => a.depth - b.depth);
      renderQueue.forEach(item => item.render());

      // 🌿 5. 前景花丛
      const fgPanX = (this.panX + idleSway) * 1.45;
      const fgPanY = this.panY * 1.45;
      if (this.layers.foreground) {
        ctx.drawImage(this.layers.foreground,
          originX + (50 + fgPanX) * baseScale, originY + (335 + fgPanY) * baseScale,
          539 * baseScale, 139 * baseScale);
      }

      // ✨ 6. 飘落樱花瓣与金粉粒子
      this.particles.forEach(p => {
        ctx.save();
        const px = originX + (p.x + islandPanX * 0.7) * baseScale;
        const py = originY + (p.y + islandPanY * 0.7) * baseScale;
        ctx.translate(px, py);
        ctx.rotate(p.rot);
        if (p.type === 'PETAL') {
          ctx.fillStyle = `rgba(244, 114, 182, ${p.alpha})`;
          ctx.beginPath();
          ctx.ellipse(0, 0, p.size * baseScale, p.size * 0.55 * baseScale, 0, 0, Math.PI * 2);
          ctx.fill();
        } else {
          ctx.fillStyle = `rgba(253, 224, 71, ${p.alpha})`;
          ctx.beginPath();
          ctx.arc(0, 0, p.size * 0.45 * baseScale, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
      });

    } catch (err) {
      console.error('[PetWorld3D] ❌ renderScene ERROR:', err && err.message, err && err.stack);
    }
  }

  destroy() {
    this.isDestroyed = true;
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}

module.exports = { PetWorld3D };
