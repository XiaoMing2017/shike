// pet_world_3d.js - 2.5D 卡通 3D 浮空岛微缩世界引擎
const { PetEntity3D } = require('./pet_entity_3d');

class PetWorld3D {
  constructor(canvas, THREE, options = {}) {
    this.canvas = canvas;
    this.THREE = THREE;
    this.options = options;
    this.isDestroyed = false;

    // 时间与帧率
    this.clock = new THREE.Clock();
    this.time = 0;

    // 交互与视差摄像机控制
    this.isDragging = false;
    this.touchStartX = 0;
    this.touchStartY = 0;
    this.yaw = 0.785; // 45度
    this.pitch = 0.68; // 约40度俯视
    this.targetYaw = 0.785;
    this.targetPitch = 0.68;
    this.cameraDistance = 12.5;

    // 初始化渲染管线与场景图
    this.initRenderer();
    this.initScene();
    this.initCamera();
    this.initLights();

    // 构建 3D 微缩世界场景对象
    this.buildSkyAndClouds();
    this.buildFloatingIslandTerrain();
    this.buildWaterAndRiver();
    this.buildBridgeAndArchitecture();
    this.buildTreesAndFlowers();
    this.buildForegroundFlora();

    // 创建 3D 独立萌宠实体
    this.initPetEntity(options.species || 'DRAGON', options.stageRank || 1);

    // 启动 60FPS 渲染循环
    this.animate = this.animate.bind(this);
    this.rafId = this.requestFrame(this.animate);
  }

  initRenderer() {
    const THREE = this.THREE;
    this.renderer = new THREE.WebGLRenderer({
      canvas: this.canvas,
      antialias: true,
      alpha: true,
      powerPreference: 'high-performance'
    });
    this.renderer.setSize(this.canvas.width, this.canvas.height, false);
    // 启用原生深度测试与软阴影
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  }

  initScene() {
    const THREE = this.THREE;
    this.scene = new THREE.Scene();
    this.scene.fog = new THREE.FogExp2(0xFDFBF7, 0.025);
  }

  initCamera() {
    const THREE = this.THREE;
    const aspect = this.canvas.width / this.canvas.height;
    this.camera = new THREE.PerspectiveCamera(36, aspect, 0.2, 100);
    this.updateCameraPosition();
  }

  initLights() {
    const THREE = this.THREE;
    // 1. 全局明亮环境光 (确保所有 3D 几何体与材质通透鲜活)
    const ambientLight = new THREE.AmbientLight(0xFFFFFF, 0.85);
    this.scene.add(ambientLight);

    // 2. 半球环境光 (温暖天空光 + 柔和地面反光)
    this.hemiLight = new THREE.HemisphereLight(0xFFF7ED, 0x93C5FD, 0.75);
    this.scene.add(this.hemiLight);

    // 3. 主太阳方向光 (产生柔和深度阴影)
    this.dirLight = new THREE.DirectionalLight(0xFFFAF0, 0.95);
    this.dirLight.position.set(9, 15, 7);
    this.dirLight.castShadow = true;
    this.scene.add(this.dirLight);

    // 4. 侧面柔和补光
    const fillLight = new THREE.DirectionalLight(0xFEF08A, 0.45);
    fillLight.position.set(-8, 6, -6);
    this.scene.add(fillLight);
  }

  // ☁️ 1. 天空与远景 3D 云层
  buildSkyAndClouds() {
    const THREE = this.THREE;
    this.cloudsGroup = new THREE.Group();
    this.scene.add(this.cloudsGroup);

    const cloudMat = new THREE.MeshLambertMaterial({ color: 0xFFFFFF, transparent: true, opacity: 0.88 });

    // 生成 5 朵由三维多球体组合的立体白云
    this.clouds = [];
    const cloudConfigs = [
      { x: -6, y: 5.2, z: -8, scale: 1.2, speed: 0.15 },
      { x: 2, y: 6.0, z: -9, scale: 0.9, speed: 0.12 },
      { x: 7, y: 4.8, z: -7, scale: 1.1, speed: 0.18 },
      { x: -2, y: 6.8, z: -10, scale: 1.4, speed: 0.10 },
      { x: 5, y: 5.5, z: -6, scale: 0.8, speed: 0.14 }
    ];

    cloudConfigs.forEach(cfg => {
      const cloud = new THREE.Group();
      const numPuffs = 4 + Math.floor(Math.random() * 3);
      for (let i = 0; i < numPuffs; i++) {
        const puffGeo = new THREE.SphereGeometry(0.5 + Math.random() * 0.4, 12, 10);
        const puff = new THREE.Mesh(puffGeo, cloudMat);
        puff.position.set((i - numPuffs / 2) * 0.55, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.3);
        cloud.add(puff);
      }
      cloud.position.set(cfg.x, cfg.y, cfg.z);
      cloud.scale.setScalar(cfg.scale);
      cloud.userData = { speed: cfg.speed, baseZ: cfg.z };
      this.cloudsGroup.add(cloud);
      this.clouds.push(cloud);
    });
  }

  // 🏝️ 2. 浮空岛主体地形与地毯小径
  buildFloatingIslandTerrain() {
    const THREE = this.THREE;
    this.terrainGroup = new THREE.Group();
    this.scene.add(this.terrainGroup);

    // 顶层草坪地块 (等轴测圆角浮岛多边形)
    const islandShape = new THREE.Shape();
    const r = 4.2;
    for (let i = 0; i < 8; i++) {
      const angle = (i / 8) * Math.PI * 2;
      const radius = r + (Math.sin(angle * 3) * 0.35);
      const px = Math.cos(angle) * radius;
      const py = Math.sin(angle) * radius;
      if (i === 0) islandShape.moveTo(px, py);
      else islandShape.lineTo(px, py);
    }
    islandShape.closePath();

    const extrudeSettings = {
      depth: 1.4,
      bevelEnabled: true,
      bevelSegments: 4,
      steps: 1,
      bevelSize: 0.3,
      bevelThickness: 0.3
    };

    const islandGeo = new THREE.ExtrudeGeometry(islandShape, extrudeSettings);
    islandGeo.rotateX(Math.PI / 2);

    const grassMat = new THREE.MeshLambertMaterial({ color: 0x86EFAC });
    this.islandTop = new THREE.Mesh(islandGeo, grassMat);
    this.islandTop.position.y = 0;
    this.islandTop.receiveShadow = true;
    this.terrainGroup.add(this.islandTop);

    // 悬浮泥土岩石断层
    const rockBaseGeo = new THREE.ConeGeometry(3.6, 3.2, 8);
    rockBaseGeo.rotateX(Math.PI);
    const rockMat = new THREE.MeshLambertMaterial({ color: 0x78716C });
    const rockBase = new THREE.Mesh(rockBaseGeo, rockMat);
    rockBase.position.set(0, -2.4, 0);
    this.terrainGroup.add(rockBase);

    // 鹅卵石小径 (Cobblestone Paths)
    const pathMat = new THREE.MeshLambertMaterial({ color: 0xF5F5F4 });
    const pathCoords = [
      [0, 0.46, 0.2], [-0.5, 0.46, 0.6], [-1.0, 0.46, 1.1],
      [0.6, 0.46, -0.4], [1.2, 0.46, -1.0], [1.8, 0.46, -1.6],
      [0.7, 0.46, 0.8], [1.4, 0.46, 1.4], [2.1, 0.46, 1.8]
    ];

    pathCoords.forEach(([x, y, z]) => {
      const stoneGeo = new THREE.CylinderGeometry(0.35 + Math.random() * 0.1, 0.4, 0.05, 7);
      const stone = new THREE.Mesh(stoneGeo, pathMat);
      stone.position.set(x, y - 0.44, z);
      stone.rotation.y = Math.random() * Math.PI;
      stone.receiveShadow = true;
      this.terrainGroup.add(stone);
    });

    // 软糯米白大圆地毯 (居中)
    const rugGeo = new THREE.CylinderGeometry(1.2, 1.25, 0.04, 24);
    const rugMat = new THREE.MeshLambertMaterial({ color: 0xFEF3C7 });
    const rug = new THREE.Mesh(rugGeo, rugMat);
    rug.position.set(0, 0.03, 0.4);
    rug.receiveShadow = true;
    this.terrainGroup.add(rug);
  }

  // 🌊 3. 水池、河流与瀑布
  buildWaterAndRiver() {
    const THREE = this.THREE;
    this.waterGroup = new THREE.Group();
    this.scene.add(this.waterGroup);

    // 曲线水池平面
    const pondGeo = new THREE.CylinderGeometry(1.4, 1.5, 0.15, 18);
    this.waterMat = new THREE.MeshLambertMaterial({
      color: 0x38BDF8,
      transparent: true,
      opacity: 0.82
    });
    this.pondMesh = new THREE.Mesh(pondGeo, this.waterMat);
    this.pondMesh.position.set(-2.0, -0.05, 1.3);
    this.waterGroup.add(this.pondMesh);

    // 瀑布 (悬崖边倾泻而下的水流)
    const waterFallGeo = new THREE.BoxGeometry(0.8, 2.6, 0.12);
    const waterfallMat = new THREE.MeshLambertMaterial({
      color: 0x7DD3FC,
      transparent: true,
      opacity: 0.75
    });
    this.waterfallMesh = new THREE.Mesh(waterFallGeo, waterfallMat);
    this.waterfallMesh.position.set(-3.6, -1.2, 1.3);
    this.waterfallMesh.rotation.z = 0.18;
    this.waterGroup.add(this.waterfallMesh);

    // 睡莲叶与花朵
    const lilyPadGeo = new THREE.CircleGeometry(0.22, 10);
    lilyPadGeo.rotateX(-Math.PI / 2);
    const lilyPadMat = new THREE.MeshLambertMaterial({ color: 0x22C55E });
    const lilyPad = new THREE.Mesh(lilyPadGeo, lilyPadMat);
    lilyPad.position.set(-1.8, 0.04, 1.1);
    this.waterGroup.add(lilyPad);

    const lilyFlowerGeo = new THREE.SphereGeometry(0.08, 8, 6);
    const lilyFlowerMat = new THREE.MeshLambertMaterial({ color: 0xF472B6 });
    const lilyFlower = new THREE.Mesh(lilyFlowerGeo, lilyFlowerMat);
    lilyFlower.position.set(-1.8, 0.10, 1.1);
    this.waterGroup.add(lilyFlower);
  }

  // 🌉 4. 木质拱桥与小屋家具
  buildBridgeAndArchitecture() {
    const THREE = this.THREE;
    this.archGroup = new THREE.Group();
    this.scene.add(this.archGroup);

    // 木拱桥 (跨越小溪)
    const bridgeGroup = new THREE.Group();
    bridgeGroup.position.set(-1.4, 0.05, -0.4);
    bridgeGroup.rotation.y = 0.65;

    const woodMat = new THREE.MeshLambertMaterial({ color: 0xD97706 });
    const plankMat = new THREE.MeshLambertMaterial({ color: 0xB45309 });

    // 拱桥踏板 (7 块木板拼成弧线)
    for (let i = 0; i < 7; i++) {
      const t = (i - 3) / 3;
      const py = (1 - t * t) * 0.18;
      const pz = t * 0.9;
      const plankGeo = new THREE.BoxGeometry(0.9, 0.05, 0.16);
      const plank = new THREE.Mesh(plankGeo, plankMat);
      plank.position.set(0, py + 0.05, pz);
      plank.rotation.x = -t * 0.35;
      plank.castShadow = true;
      plank.receiveShadow = true;
      bridgeGroup.add(plank);
    }

    // 4 根立柱护栏
    const postGeo = new THREE.CylinderGeometry(0.04, 0.04, 0.45, 6);
    const postCoords = [[-0.42, 0.4], [0.42, 0.4], [-0.42, -0.4], [0.42, -0.4]];
    postCoords.forEach(([px, pz]) => {
      const post = new THREE.Mesh(postGeo, woodMat);
      post.position.set(px, 0.22, pz);
      post.castShadow = true;
      bridgeGroup.add(post);
    });

    this.archGroup.add(bridgeGroup);

    // 阳光小屋与原木沙发
    const sofaGroup = new THREE.Group();
    sofaGroup.position.set(2.4, 0.02, -1.4);
    sofaGroup.rotation.y = -0.55;

    const sofaMat = new THREE.MeshLambertMaterial({ color: 0xFEF08A });
    const seatGeo = new THREE.BoxGeometry(1.4, 0.35, 0.65);
    const seat = new THREE.Mesh(seatGeo, sofaMat);
    seat.position.y = 0.2;
    seat.castShadow = true;
    seat.receiveShadow = true;
    sofaGroup.add(seat);

    const backGeo = new THREE.BoxGeometry(1.4, 0.55, 0.2);
    const back = new THREE.Mesh(backGeo, sofaMat);
    back.position.set(0, 0.55, -0.22);
    back.castShadow = true;
    sofaGroup.add(back);

    this.archGroup.add(sofaGroup);

    // 运动瑜伽垫与哑铃
    const matGeo = new THREE.BoxGeometry(0.7, 0.02, 1.3);
    const yogaMat = new THREE.Mesh(matGeo, new THREE.MeshLambertMaterial({ color: 0x38BDF8 }));
    yogaMat.position.set(2.3, 0.02, 0.9);
    yogaMat.rotation.y = 0.35;
    yogaMat.receiveShadow = true;
    this.archGroup.add(yogaMat);

    // 食盆 (Food Bowl)
    const bowlGeo = new THREE.CylinderGeometry(0.24, 0.18, 0.12, 12);
    const bowl = new THREE.Mesh(bowlGeo, new THREE.MeshLambertMaterial({ color: 0xFB923C }));
    bowl.position.set(-0.8, 0.06, 0.9);
    bowl.castShadow = true;
    this.archGroup.add(bowl);
    this.foodBowlPos = new THREE.Vector3(-0.8, 0.45, 0.9);
  }

  // 🌲 5. 3D 蓬蓬树木与花草群 (具有真实空间体积与遮挡能力)
  buildTreesAndFlowers() {
    const THREE = this.THREE;
    this.floraGroup = new THREE.Group();
    this.scene.add(this.floraGroup);

    const trunkMat = new THREE.MeshLambertMaterial({ color: 0x78350F });
    const leafMats = [
      new THREE.MeshLambertMaterial({ color: 0x22C55E }),
      new THREE.MeshLambertMaterial({ color: 0x16A34A }),
      new THREE.MeshLambertMaterial({ color: 0x4ADE80 }),
      new THREE.MeshLambertMaterial({ color: 0xF472B6 }) // 樱花粉树
    ];

    // 3 棵不同位置的大树 (产生自然遮挡)
    const treePositions = [
      { x: -2.8, z: -1.8, h: 2.2, colorIdx: 0 },
      { x: 0.8, z: -3.2, h: 2.5, colorIdx: 1 },
      { x: 3.4, z: 1.6, h: 2.1, colorIdx: 3 } // 右侧樱花粉树
    ];

    treePositions.forEach(cfg => {
      const tree = new THREE.Group();
      tree.position.set(cfg.x, 0, cfg.z);

      // 树干
      const trunkGeo = new THREE.CylinderGeometry(0.16, 0.24, cfg.h, 8);
      const trunk = new THREE.Mesh(trunkGeo, trunkMat);
      trunk.position.y = cfg.h / 2;
      trunk.castShadow = true;
      trunk.receiveShadow = true;
      tree.add(trunk);

      // 蓬蓬球形树冠 (3~4 个多边形球体交错)
      const leafMat = leafMats[cfg.colorIdx];
      const foliageGroup = new THREE.Group();
      foliageGroup.position.y = cfg.h - 0.2;

      const offsets = [
        [0, 0.6, 0, 0.95],
        [-0.4, 0.2, 0.2, 0.75],
        [0.4, 0.1, -0.2, 0.80],
        [0.1, 0.2, 0.4, 0.70]
      ];

      offsets.forEach(([ox, oy, oz, r]) => {
        const puffGeo = new THREE.SphereGeometry(r, 12, 10);
        const puff = new THREE.Mesh(puffGeo, leafMat);
        puff.position.set(ox, oy, oz);
        puff.castShadow = true;
        puff.receiveShadow = true;
        foliageGroup.add(puff);
      });

      tree.add(foliageGroup);
      this.floraGroup.add(tree);
    });

    // 散落在草坪上的彩色花朵
    const flowerColors = [0xF43F5E, 0xFBBF24, 0xA855F7, 0x38BDF8];
    const flowerCoords = [
      [-1.2, 0.02, -1.6], [-2.2, 0.02, -0.6], [1.4, 0.02, 2.3],
      [2.6, 0.02, 2.1], [-0.4, 0.02, 2.4], [1.8, 0.02, -2.6]
    ];

    flowerCoords.forEach(([fx, fy, fz], idx) => {
      const fGroup = new THREE.Group();
      fGroup.position.set(fx, fy, fz);

      const fStemGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.24, 4);
      const fStem = new THREE.Mesh(fStemGeo, trunkMat);
      fStem.position.y = 0.12;
      fGroup.add(fStem);

      const fHeadGeo = new THREE.SphereGeometry(0.12, 8, 6);
      const fHead = new THREE.Mesh(fHeadGeo, new THREE.MeshLambertMaterial({ color: flowerColors[idx % flowerColors.length] }));
      fHead.position.y = 0.24;
      fHead.castShadow = true;
      fGroup.add(fHead);

      this.floraGroup.add(fGroup);
    });
  }

  // 🌿 6. 前景遮挡植物 (产生剧烈视差与空间深度)
  buildForegroundFlora() {
    const THREE = this.THREE;
    this.foregroundGroup = new THREE.Group();
    this.scene.add(this.foregroundGroup);

    const fgMat = new THREE.MeshLambertMaterial({ color: 0x15803D });

    // 前景左侧大叶片与右侧花枝
    const fgBush1 = new THREE.Group();
    fgBush1.position.set(-3.2, -0.2, 3.8); // 靠近摄像机视锥近端
    for (let i = 0; i < 3; i++) {
      const leafGeo = new THREE.SphereGeometry(0.45 + i * 0.1, 10, 8);
      leafGeo.scale(0.5, 1.2, 0.3);
      const leaf = new THREE.Mesh(leafGeo, fgMat);
      leaf.position.set(i * 0.3, i * 0.2, 0);
      leaf.rotation.z = -0.4 + i * 0.2;
      fgBush1.add(leaf);
    }
    this.foregroundGroup.add(fgBush1);

    const fgBush2 = new THREE.Group();
    fgBush2.position.set(3.4, -0.2, 3.6);
    for (let i = 0; i < 3; i++) {
      const leafGeo = new THREE.SphereGeometry(0.45 + i * 0.1, 10, 8);
      leafGeo.scale(0.5, 1.2, 0.3);
      const leaf = new THREE.Mesh(leafGeo, fgMat);
      leaf.position.set(-i * 0.3, i * 0.2, 0);
      leaf.rotation.z = 0.4 - i * 0.2;
      fgBush2.add(leaf);
    }
    this.foregroundGroup.add(fgBush2);
  }

  // 🐾 7. 初始化 3D 萌宠实体
  initPetEntity(species, stageRank) {
    this.pet = new PetEntity3D(this.THREE, species, stageRank);
    this.scene.add(this.pet.root);

    // 空间生活巡逻兴趣点 (Waypoints)
    this.waypoints = [
      { name: 'RUG', x: 0, z: 0.4, action: 'IDLE' },
      { name: 'SOFA', x: 1.8, z: -1.2, action: 'REST_SIT' },
      { name: 'FITNESS', x: 2.1, z: 0.8, action: 'JOY_BOUNCE' },
      { name: 'POND', x: -1.6, z: 1.2, action: 'DRINK_WATER' },
      { name: 'FLOWER', x: -1.2, z: -1.6, action: 'SNIFF_FLOWER' },
      { name: 'GARDEN', x: 1.2, z: 2.2, action: 'IDLE' }
    ];
  }

  // 📸 摄像机坐标更新 (支持视差轨道阻尼与柔和呼吸)
  updateCameraPosition() {
    // 平滑阻尼插值
    this.yaw += (this.targetYaw - this.yaw) * 0.08;
    this.pitch += (this.targetPitch - this.pitch) * 0.08;

    // 自主轻微呼吸摇摆
    const sway = Math.sin(this.time * 0.4) * 0.03;
    const currentYaw = this.yaw + sway;

    const x = Math.sin(currentYaw) * Math.cos(this.pitch) * this.cameraDistance;
    const y = Math.sin(this.pitch) * this.cameraDistance;
    const z = Math.cos(currentYaw) * Math.cos(this.pitch) * this.cameraDistance;

    this.camera.position.set(x, y, z);
    this.camera.lookAt(0, 0.4, 0);
  }

  // 🎮 触摸手势控制摄像机视差
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

    // 限制视差旋转范围，保持 2.5D 稳定视角
    this.targetYaw -= dx * 0.005;
    this.targetPitch = Math.max(0.45, Math.min(0.95, this.targetPitch + dy * 0.004));
  }

  onTouchEnd() {
    this.isDragging = false;
  }

  // 🌟 60FPS 主渲染循环
  animate() {
    if (this.isDestroyed) return;
    this.rafId = this.requestFrame(this.animate);

    const deltaTime = Math.min(0.1, this.clock.getDelta());
    this.time += deltaTime;

    // 1. 驱动云层极慢视差飘动
    this.clouds.forEach(cloud => {
      cloud.position.x += cloud.userData.speed * deltaTime;
      if (cloud.position.x > 12) cloud.position.x = -12;
    });

    // 2. 驱动水面与瀑布流动微波
    if (this.waterfallMesh) {
      this.waterfallMesh.scale.y = 1.0 + Math.sin(this.time * 8.0) * 0.05;
    }

    // 3. 驱动 3D 萌宠自主生命状态机与寻路
    if (this.pet) {
      this.pet.update(deltaTime, this.waypoints);
    }

    // 4. 更新 2.5D 视差摄像机
    this.updateCameraPosition();

    // 5. WebGL 硬件加速绘制
    this.renderer.render(this.scene, this.camera);
  }

  // 互动指令分发
  triggerPetTap() {
    if (this.pet) this.pet.triggerTapReaction();
  }

  triggerPetFeed() {
    if (this.pet) this.pet.triggerFeedAction(this.foodBowlPos);
  }

  navigateToSpot(spotName) {
    const found = this.waypoints.find(w => w.name === spotName);
    if (found && this.pet) {
      this.pet.navigateTo(found.x, found.z, found.action);
    }
  }

  setSpecies(species, stageRank) {
    if (this.pet) {
      this.scene.remove(this.pet.root);
    }
    this.initPetEntity(species, stageRank);
  }

  requestFrame(cb) {
    if (this.canvas && typeof this.canvas.requestAnimationFrame === 'function') {
      return this.canvas.requestAnimationFrame(cb);
    }
    return setTimeout(cb, 16);
  }

  cancelFrame(id) {
    if (this.canvas && typeof this.canvas.cancelAnimationFrame === 'function') {
      this.canvas.cancelAnimationFrame(id);
    } else {
      clearTimeout(id);
    }
  }

  destroy() {
    this.isDestroyed = true;
    if (this.rafId) {
      this.cancelFrame(this.rafId);
    }
    if (this.renderer) {
      this.renderer.dispose();
    }
  }
}

module.exports = { PetWorld3D };
