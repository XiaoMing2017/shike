/**
 * iso-world.js — 2.5D 等轴测微缩自律小屋场景搭建 & 宠物 AI
 * 依赖 iso-engine.js
 */
var engine = require('./iso-engine');
var Vec3 = engine.Vec3, Mat4 = engine.Mat4, Geometry = engine.Geometry;
var SceneNode = engine.SceneNode, IsoCamera = engine.IsoCamera, WebGLRenderer = engine.WebGLRenderer;

// ========== 颜色常量 (线性 RGB 0-1) ==========
var C = {
  WOOD:       [0.83, 0.65, 0.45],
  WOOD_DARK:  [0.55, 0.44, 0.28],
  WALL:       [0.99, 0.95, 0.90],
  WALL_ACCENT:[0.90, 0.88, 0.82],
  FLOOR:      [0.88, 0.78, 0.62],
  GRASS:      [0.53, 0.93, 0.67],
  GRASS_DARK: [0.35, 0.78, 0.45],
  SOFA_CREAM: [0.96, 0.94, 0.91],
  SOFA_CUSH:  [0.92, 0.90, 0.86],
  BED_FRAME:  [0.55, 0.44, 0.28],
  BED_SHEET:  [0.73, 0.97, 0.83],
  DESK_TOP:   [0.79, 0.66, 0.43],
  LAMP_GREEN: [0.30, 0.75, 0.45],
  LAMP_METAL: [0.65, 0.65, 0.60],
  WARDROBE:   [0.72, 0.58, 0.40],
  FENCE:      [0.63, 0.38, 0.03],
  BUSH:       [0.29, 0.87, 0.50],
  BUSH_FLOWER:[0.95, 0.70, 0.80],
  CHAIR_WOOD: [0.83, 0.70, 0.50],
  CHAIR_CLOTH:[0.98, 0.65, 0.65],
  VINYL_BASE: [0.16, 0.15, 0.14],
  VINYL_DISC: [0.10, 0.10, 0.10],
  SHADOW:     [0.10, 0.10, 0.10],
  // 宠物颜色
  DRAGON:     [0.53, 0.93, 0.67],
  CAT:        [0.99, 0.73, 0.45],
  TOTORO:     [0.80, 0.84, 0.88],
  DOG:        [0.99, 0.90, 0.55],
  EYE_BLACK:  [0.12, 0.14, 0.17],
  EYE_WHITE:  [0.98, 0.98, 0.98],
  BLUSH:      [0.98, 0.70, 0.75],
};

// ========== 家具点位 3D 坐标 ==========
var SPOTS_3D = {
  GARDEN:  { x: 0.0,  y: 0.0, z: 2.5  },
  SOFA:    { x: -1.6, y: 0.0, z: -0.8 },
  BED:     { x: 1.5,  y: 0.0, z: -0.9 },
  DESK:    { x: 1.6,  y: 0.0, z: 1.0  },
  DRESSER: { x: -1.2, y: 0.0, z: 0.8  }
};

// ========== Pet AI States ==========
var PET_STATE = { IDLE: 0, WALK: 1, SIT: 2, SLEEP: 3, JOY: 4, EAT: 5 };

// =====================================================================
//  IsometricWorld — 主类
// =====================================================================
class IsometricWorld {
  constructor() {
    this.renderer = null;
    this.camera = null;
    this.scene = null;
    this.canvas = null;
    this.animId = null;
    this.time = 0;
    this.lastTime = 0;
    // Pet
    this.petNode = null;
    this.petParts = {};
    this.petType = 'DRAGON';
    this.petPos = new Vec3(0, 0, 0);
    this.petTargetPos = new Vec3(0, 0, 0);
    this.petRotY = 0;
    this.petTargetRotY = 0;
    this.petState = PET_STATE.IDLE;
    this.petStateTimer = 0;
    // Touch
    this.touchStartX = 0;
    this.touchStartY = 0;
  }

  /**
   * 初始化整个 3D 世界
   */
  init(canvas) {
    this.canvas = canvas;
    var dpr = wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : 2;
    canvas.width = canvas.width * dpr;
    canvas.height = canvas.height * dpr;

    this.renderer = new WebGLRenderer();
    if (!this.renderer.init(canvas)) return false;

    this.camera = new IsoCamera();
    this.camera.aspect = canvas.width / canvas.height;

    this.scene = new SceneNode('root');

    this.buildRoom();
    this.buildFurniture();
    this.buildGarden();
    this.buildProps();

    this.lastTime = Date.now();
    this._loop();
    return true;
  }

  // =================== 房间基础结构 ===================
  buildRoom() {
    var r = this.renderer;
    // 室内地板
    var floor = this._makeNode('floor', Geometry.createBox(5.0, 0.15, 4.0), C.FLOOR);
    floor.position.set(0, -0.075, -0.5);
    this.scene.add(floor);

    // 露台草坪
    var grass = this._makeNode('grass', Geometry.createBox(4.0, 0.12, 2.2), C.GRASS);
    grass.position.set(0, -0.06, 2.6);
    this.scene.add(grass);

    // 草坪深色底边
    var grassBase = this._makeNode('grassBase', Geometry.createBox(4.2, 0.08, 2.4), C.GRASS_DARK);
    grassBase.position.set(0, -0.16, 2.6);
    this.scene.add(grassBase);

    // 左墙
    var wallL = this._makeNode('wallL', Geometry.createBox(0.12, 2.8, 5.2), C.WALL);
    wallL.position.set(-2.5, 1.4, 0.1);
    this.scene.add(wallL);

    // 后墙
    var wallB = this._makeNode('wallB', Geometry.createBox(5.12, 2.8, 0.12), C.WALL);
    wallB.position.set(0, 1.4, -2.5);
    this.scene.add(wallB);

    // 墙面装饰条 (踢脚线)
    var skirL = this._makeNode('skirL', Geometry.createBox(0.04, 0.12, 5.2), C.WALL_ACCENT);
    skirL.position.set(-2.42, 0.06, 0.1);
    this.scene.add(skirL);

    var skirB = this._makeNode('skirB', Geometry.createBox(5.12, 0.12, 0.04), C.WALL_ACCENT);
    skirB.position.set(0, 0.06, -2.42);
    this.scene.add(skirB);

    // 室内/露台矮隔断墙
    var partition = this._makeNode('partition', Geometry.createBox(5.0, 0.7, 0.10), C.WALL_ACCENT);
    partition.position.set(0, 0.35, 1.5);
    this.scene.add(partition);
  }

  // =================== 家具 ===================
  buildFurniture() {
    // --- 沙发 (SOFA) ---
    var sofa = new SceneNode('sofa');
    sofa.position.set(-1.6, 0, -0.8);

    // 底座/坐垫
    sofa.add(this._makeNode('sofaSeat', Geometry.createBox(1.2, 0.35, 0.6), C.SOFA_CREAM, 0, 0.175, 0));
    // 靠背
    sofa.add(this._makeNode('sofaBack', Geometry.createBox(1.2, 0.5, 0.12), C.SOFA_CUSH, 0, 0.6, -0.24));
    // 左扶手
    sofa.add(this._makeNode('sofaArmL', Geometry.createBox(0.1, 0.4, 0.6), C.SOFA_CREAM, -0.55, 0.35, 0));
    // 右扶手
    sofa.add(this._makeNode('sofaArmR', Geometry.createBox(0.1, 0.4, 0.6), C.SOFA_CREAM, 0.55, 0.35, 0));
    // 靠垫
    sofa.add(this._makeNode('cushion1', Geometry.createBox(0.28, 0.28, 0.14), C.BED_SHEET, -0.3, 0.49, -0.12));
    sofa.add(this._makeNode('cushion2', Geometry.createBox(0.28, 0.28, 0.14), C.BUSH_FLOWER, 0.3, 0.49, -0.12));

    this.scene.add(sofa);

    // --- 大床 (BED) ---
    var bed = new SceneNode('bed');
    bed.position.set(1.5, 0, -0.9);

    // 床架
    bed.add(this._makeNode('bedFrame', Geometry.createBox(1.3, 0.3, 0.9), C.BED_FRAME, 0, 0.15, 0));
    // 床头板
    bed.add(this._makeNode('bedHead', Geometry.createBox(1.3, 0.6, 0.08), C.BED_FRAME, 0, 0.6, -0.42));
    // 床垫
    bed.add(this._makeNode('bedMat', Geometry.createBox(1.2, 0.12, 0.8), C.SOFA_CREAM, 0, 0.36, 0));
    // 被褥
    bed.add(this._makeNode('bedBlanket', Geometry.createBox(1.1, 0.1, 0.5), C.BED_SHEET, 0, 0.44, 0.1));
    // 枕头
    bed.add(this._makeNode('pillow', Geometry.createBox(0.35, 0.1, 0.2), C.SOFA_CREAM, 0, 0.46, -0.25));

    this.scene.add(bed);

    // --- 书桌 (DESK) ---
    var desk = new SceneNode('desk');
    desk.position.set(1.6, 0, 1.0);

    // 桌面
    desk.add(this._makeNode('deskTop', Geometry.createBox(1.0, 0.06, 0.5), C.DESK_TOP, 0, 0.7, 0));
    // 4 桌腿
    var legG = Geometry.createCylinder(0.03, 0.03, 0.7, 6);
    desk.add(this._makeNode('legFL', legG, C.WOOD_DARK, -0.42, 0.35, 0.18));
    desk.add(this._makeNode('legFR', legG, C.WOOD_DARK, 0.42, 0.35, 0.18));
    desk.add(this._makeNode('legBL', legG, C.WOOD_DARK, -0.42, 0.35, -0.18));
    desk.add(this._makeNode('legBR', legG, C.WOOD_DARK, 0.42, 0.35, -0.18));
    // 台灯支柱
    desk.add(this._makeNode('lampPole', Geometry.createCylinder(0.02, 0.02, 0.3, 6), C.LAMP_METAL, 0.3, 0.88, -0.1));
    // 台灯头 (小球)
    desk.add(this._makeNode('lampHead', Geometry.createSphere(0.08, 8, 6), C.LAMP_GREEN, 0.3, 1.08, -0.1));

    this.scene.add(desk);

    // --- 衣柜 (DRESSER) ---
    var dresser = new SceneNode('dresser');
    dresser.position.set(-1.2, 0, 0.8);

    // 柜体
    dresser.add(this._makeNode('wardBody', Geometry.createBox(0.7, 1.4, 0.4), C.WARDROBE, 0, 0.7, 0));
    // 左门
    dresser.add(this._makeNode('wardDoorL', Geometry.createBox(0.33, 1.2, 0.02), C.WOOD, -0.15, 0.7, 0.22));
    // 右门
    dresser.add(this._makeNode('wardDoorR', Geometry.createBox(0.33, 1.2, 0.02), C.WOOD, 0.15, 0.7, 0.22));
    // 把手
    dresser.add(this._makeNode('handleL', Geometry.createCylinder(0.015, 0.015, 0.04, 6), C.LAMP_METAL, -0.04, 0.7, 0.24));
    dresser.add(this._makeNode('handleR', Geometry.createCylinder(0.015, 0.015, 0.04, 6), C.LAMP_METAL, 0.04, 0.7, 0.24));

    this.scene.add(dresser);
  }

  // =================== 花园 ===================
  buildGarden() {
    // 花篱栅栏 (前方)
    for (var i = -3; i <= 3; i++) {
      var post = this._makeNode('fpost' + i, Geometry.createBox(0.04, 0.4, 0.04), C.FENCE, i * 0.45, 0.2, 3.6);
      this.scene.add(post);
    }
    // 横杆
    var rail = this._makeNode('rail', Geometry.createBox(2.7, 0.03, 0.03), C.FENCE, 0, 0.32, 3.6);
    this.scene.add(rail);

    // 灌木丛
    var bushPositions = [
      [-1.2, 0.2, 3.5], [-0.5, 0.25, 3.55], [0.3, 0.22, 3.5], [1.0, 0.2, 3.55]
    ];
    for (var i = 0; i < bushPositions.length; i++) {
      var bp = bushPositions[i];
      var bush = this._makeNode('bush' + i, Geometry.createSphere(0.2, 8, 6), C.BUSH, bp[0], bp[1], bp[2]);
      this.scene.add(bush);
      // 小花点缀
      if (i % 2 === 0) {
        var flower = this._makeNode('flower' + i, Geometry.createSphere(0.06, 6, 4), C.BUSH_FLOWER, bp[0] + 0.1, bp[1] + 0.15, bp[2]);
        this.scene.add(flower);
      }
    }

    // 躺椅
    var chair = new SceneNode('deckchair');
    chair.position.set(0.8, 0, 2.8);
    chair.rotation.y = -0.3;
    // 支架
    chair.add(this._makeNode('chairFrame', Geometry.createBox(0.5, 0.04, 0.9), C.CHAIR_WOOD, 0, 0.28, 0));
    // 腿
    var chairLeg = Geometry.createBox(0.04, 0.28, 0.04);
    chair.add(this._makeNode('cLeg1', chairLeg, C.CHAIR_WOOD, -0.2, 0.14, -0.35));
    chair.add(this._makeNode('cLeg2', chairLeg, C.CHAIR_WOOD, 0.2, 0.14, -0.35));
    chair.add(this._makeNode('cLeg3', chairLeg, C.CHAIR_WOOD, -0.2, 0.14, 0.35));
    chair.add(this._makeNode('cLeg4', chairLeg, C.CHAIR_WOOD, 0.2, 0.14, 0.35));
    // 帆布面
    chair.add(this._makeNode('chairCloth', Geometry.createBox(0.44, 0.02, 0.8), C.CHAIR_CLOTH, 0, 0.31, 0));
    this.scene.add(chair);

    // 汀步石
    var stoneG = Geometry.createCylinder(0.12, 0.14, 0.04, 8);
    this.scene.add(this._makeNode('stone1', stoneG, C.WALL_ACCENT, -0.3, 0.02, 1.8));
    this.scene.add(this._makeNode('stone2', stoneG, C.WALL_ACCENT, 0.1, 0.02, 2.1));
    this.scene.add(this._makeNode('stone3', stoneG, C.WALL_ACCENT, -0.1, 0.02, 2.5));
  }

  // =================== 小道具 ===================
  buildProps() {
    // 黑胶唱机 (放在左墙边小桌上)
    var vinyl = new SceneNode('vinyl');
    vinyl.position.set(-2.0, 0, -0.2);

    // 小桌
    vinyl.add(this._makeNode('vTable', Geometry.createBox(0.4, 0.55, 0.3), C.WOOD, 0, 0.275, 0));
    // 唱机底座
    vinyl.add(this._makeNode('vBase', Geometry.createBox(0.3, 0.05, 0.25), C.VINYL_BASE, 0, 0.58, 0));
    // 唱片
    this.vinylDisc = this._makeNode('vDisc', Geometry.createCylinder(0.1, 0.1, 0.01, 12), C.VINYL_DISC, 0, 0.605, 0);
    vinyl.add(this.vinylDisc);

    this.scene.add(vinyl);

    // 水碗 (放在花园)
    var bowl = this._makeNode('bowl', Geometry.createCylinder(0.08, 0.1, 0.05, 10), [0.6, 0.8, 0.95], -0.8, 0.025, 2.3);
    this.scene.add(bowl);

    // 果盘 (沙发旁小几)
    var plate = this._makeNode('plate', Geometry.createCylinder(0.1, 0.12, 0.03, 10), C.SOFA_CREAM, -0.8, 0.025, -0.6);
    this.scene.add(plate);
    // 果子
    this.scene.add(this._makeNode('fruit1', Geometry.createSphere(0.04, 6, 4), [0.95, 0.3, 0.3], -0.82, 0.06, -0.62));
    this.scene.add(this._makeNode('fruit2', Geometry.createSphere(0.04, 6, 4), [0.4, 0.85, 0.3], -0.76, 0.06, -0.58));
  }

  // =================== 创建 3D 宠物 ===================
  createPet(type) {
    type = type || 'DRAGON';
    this.petType = type;
    var bodyColor = C[type] || C.DRAGON;

    // Remove old pet if any
    if (this.petNode) {
      var idx = this.scene.children.indexOf(this.petNode);
      if (idx >= 0) this.scene.children.splice(idx, 1);
    }

    var pet = new SceneNode('pet');
    var startSpot = SPOTS_3D.GARDEN;
    pet.position.set(startSpot.x, 0, startSpot.z);
    this.petPos.set(startSpot.x, 0, startSpot.z);
    this.petTargetPos.set(startSpot.x, 0, startSpot.z);

    // 身体 (纵向挤压椭球)
    var body = this._makeNode('body', Geometry.createSphere(0.22, 10, 8), bodyColor, 0, 0.32, 0);
    body.scaleVec.set(1, 0.85, 0.9);
    pet.add(body);
    this.petParts.body = body;

    // 头部
    var head = this._makeNode('head', Geometry.createSphere(0.2, 10, 8), bodyColor, 0, 0.58, 0.06);
    pet.add(head);
    this.petParts.head = head;

    // 眼白
    pet.add(this._makeNode('eyeWL', Geometry.createSphere(0.055, 6, 4), C.EYE_WHITE, -0.09, 0.61, 0.2));
    pet.add(this._makeNode('eyeWR', Geometry.createSphere(0.055, 6, 4), C.EYE_WHITE, 0.09, 0.61, 0.2));
    // 眼球
    pet.add(this._makeNode('eyeL', Geometry.createSphere(0.035, 6, 4), C.EYE_BLACK, -0.09, 0.61, 0.24));
    pet.add(this._makeNode('eyeR', Geometry.createSphere(0.035, 6, 4), C.EYE_BLACK, 0.09, 0.61, 0.24));

    // 脸颊 (腮红)
    pet.add(this._makeNode('blushL', Geometry.createSphere(0.04, 6, 4), C.BLUSH, -0.15, 0.55, 0.18));
    pet.add(this._makeNode('blushR', Geometry.createSphere(0.04, 6, 4), C.BLUSH, 0.15, 0.55, 0.18));

    // 四肢
    var legG = Geometry.createCylinder(0.05, 0.04, 0.14, 6);
    var legFL = this._makeNode('legFL', legG, bodyColor, -0.1, 0.07, 0.08);
    var legFR = this._makeNode('legFR', legG, bodyColor, 0.1, 0.07, 0.08);
    var legBL = this._makeNode('legBL', legG, bodyColor, -0.1, 0.07, -0.08);
    var legBR = this._makeNode('legBR', legG, bodyColor, 0.1, 0.07, -0.08);
    pet.add(legFL); pet.add(legFR); pet.add(legBL); pet.add(legBR);
    this.petParts.legs = [legFL, legFR, legBL, legBR];

    // 物种特征部件
    if (type === 'DRAGON') {
      // 小龙角
      pet.add(this._makeNode('hornL', Geometry.createCylinder(0.03, 0.01, 0.1, 5), [0.02, 0.60, 0.40], -0.08, 0.76, 0));
      pet.add(this._makeNode('hornR', Geometry.createCylinder(0.03, 0.01, 0.1, 5), [0.02, 0.60, 0.40], 0.08, 0.76, 0));
      // 小翅膀
      pet.add(this._makeNode('wingL', Geometry.createBox(0.14, 0.1, 0.02), [0.40, 0.90, 0.55], -0.22, 0.45, -0.06));
      pet.add(this._makeNode('wingR', Geometry.createBox(0.14, 0.1, 0.02), [0.40, 0.90, 0.55], 0.22, 0.45, -0.06));
    } else if (type === 'CAT') {
      // 三角猫耳
      pet.add(this._makeNode('earL', Geometry.createCylinder(0.05, 0.01, 0.08, 3), C.CAT, -0.1, 0.76, 0.04));
      pet.add(this._makeNode('earR', Geometry.createCylinder(0.05, 0.01, 0.08, 3), C.CAT, 0.1, 0.76, 0.04));
      // 尾巴
      pet.add(this._makeNode('tail', Geometry.createCylinder(0.025, 0.02, 0.25, 6), C.CAT, 0, 0.3, -0.22));
    } else if (type === 'TOTORO') {
      // 圆耳
      pet.add(this._makeNode('earL', Geometry.createSphere(0.06, 6, 4), C.TOTORO, -0.14, 0.74, 0.02));
      pet.add(this._makeNode('earR', Geometry.createSphere(0.06, 6, 4), C.TOTORO, 0.14, 0.74, 0.02));
      // 肚皮白斑
      pet.add(this._makeNode('belly', Geometry.createSphere(0.15, 8, 6), C.EYE_WHITE, 0, 0.3, 0.12));
    } else if (type === 'DOG') {
      // 垂耳
      var earG = Geometry.createSphere(0.06, 6, 4);
      var earL = this._makeNode('earL', earG, C.DOG, -0.16, 0.65, 0.04);
      earL.scaleVec.set(0.6, 1.2, 0.6);
      pet.add(earL);
      var earR = this._makeNode('earR', earG, C.DOG, 0.16, 0.65, 0.04);
      earR.scaleVec.set(0.6, 1.2, 0.6);
      pet.add(earR);
      // 短尾
      pet.add(this._makeNode('tail', Geometry.createSphere(0.04, 6, 4), C.DOG, 0, 0.38, -0.2));
    }

    // 地面接触阴影
    var shadow = this._makeNode('shadow', Geometry.createCylinder(0.18, 0.18, 0.01, 12), C.SHADOW, 0, 0.005, 0);
    shadow.alpha = 0.25;
    pet.add(shadow);
    this.petParts.shadow = shadow;

    this.petNode = pet;
    this.scene.add(pet);
  }

  // =================== 宠物控制 API ===================
  setPetTarget(spotId) {
    var sp = SPOTS_3D[spotId];
    if (!sp) return;
    this.petTargetPos.set(sp.x, sp.y, sp.z);
    this.petState = PET_STATE.WALK;
    this.petStateTimer = 0;
  }

  triggerPetJoy() {
    this.petState = PET_STATE.JOY;
    this.petStateTimer = 0;
  }

  triggerPetFeed() {
    this.petState = PET_STATE.EAT;
    this.petStateTimer = 0;
  }

  setLighting(mode) {
    if (this.renderer) this.renderer.setLighting(mode);
  }

  // =================== 触摸视差 ===================
  onTouchStart(x, y) {
    this.touchStartX = x;
    this.touchStartY = y;
  }

  onTouchMove(x, y) {
    var dx = (x - this.touchStartX) * 0.005;
    var dy = (y - this.touchStartY) * 0.005;
    this.camera.pan(-dx, dy);
    this.touchStartX = x;
    this.touchStartY = y;
  }

  onTouchRelease() {
    this.camera.release();
  }

  // =================== 碰撞检测 (点击宠物) ===================
  hitTestPet(touchX, touchY, canvasW, canvasH) {
    if (!this.petNode) return false;
    // 将宠物世界坐标投影到屏幕
    var proj = this.camera.getProjectionMatrix();
    var view = this.camera.getViewMatrix();
    var model = this.petNode.getLocalMatrix();
    var mvp = Mat4.mul(proj, Mat4.mul(view, model));
    // 宠物中心 (0, 0.4, 0) 局部坐标
    var m = mvp.m;
    var cx = m[12] + m[4] * 0.4;
    var cy = m[13] + m[5] * 0.4;
    var cw = m[15] + m[7] * 0.4;
    // NDC to screen
    var sx = (cx / cw * 0.5 + 0.5) * canvasW;
    var sy = (1.0 - (cy / cw * 0.5 + 0.5)) * canvasH;
    var dist = Math.sqrt((touchX - sx) * (touchX - sx) + (touchY - sy) * (touchY - sy));
    return dist < canvasW * 0.12;
  }

  // =================== 动画主循环 ===================
  _loop() {
    var self = this;
    var tick = function () {
      var now = Date.now();
      var dt = Math.min((now - self.lastTime) / 1000, 0.05);
      self.lastTime = now;
      self.time += dt;

      self._updatePet(dt);
      self._updateProps(dt);
      self.camera.update();
      self.renderer.render(self.scene, self.camera);

      self.animId = self.canvas.requestAnimationFrame(tick);
    };
    this.animId = this.canvas.requestAnimationFrame(tick);
  }

  _updatePet(dt) {
    if (!this.petNode) return;
    var t = this.time;

    this.petStateTimer += dt;

    var pos = this.petPos;
    var target = this.petTargetPos;

    // Movement
    if (this.petState === PET_STATE.WALK) {
      var dx = target.x - pos.x;
      var dz = target.z - pos.z;
      var dist = Math.sqrt(dx * dx + dz * dz);
      if (dist > 0.05) {
        var speed = 0.8 * dt;
        var nx = dx / dist, nz = dz / dist;
        pos.x += nx * speed;
        pos.z += nz * speed;
        // Face direction
        this.petTargetRotY = Math.atan2(nx, nz);
      } else {
        pos.x = target.x;
        pos.z = target.z;
        this.petState = PET_STATE.IDLE;
      }
    }

    // Smooth rotation
    var dRot = this.petTargetRotY - this.petRotY;
    while (dRot > Math.PI) dRot -= Math.PI * 2;
    while (dRot < -Math.PI) dRot += Math.PI * 2;
    this.petRotY += dRot * 4 * dt;

    this.petNode.position.set(pos.x, pos.y, pos.z);
    this.petNode.rotation.y = this.petRotY;

    // Animations per state
    var body = this.petParts.body;
    var head = this.petParts.head;
    var legs = this.petParts.legs;

    if (this.petState === PET_STATE.IDLE) {
      // Breathing
      var breath = Math.sin(t * 2.5) * 0.03;
      if (body) body.scaleVec.set(1 + breath * 0.5, 0.85 + breath, 0.9 + breath * 0.3);
      if (head) head.position.y = 0.58 + breath * 0.3;
      // Legs still
      if (legs) for (var i = 0; i < legs.length; i++) legs[i].rotation.x = 0;
    } else if (this.petState === PET_STATE.WALK) {
      // Walk bobble
      var walkBob = Math.sin(t * 10) * 0.02;
      this.petNode.position.y = walkBob > 0 ? walkBob : 0;
      // Leg swing
      if (legs) {
        var swing = Math.sin(t * 10) * 0.4;
        legs[0].rotation.x = swing;
        legs[1].rotation.x = -swing;
        legs[2].rotation.x = -swing;
        legs[3].rotation.x = swing;
      }
    } else if (this.petState === PET_STATE.JOY) {
      // Bouncing
      var jumpH = Math.abs(Math.sin(this.petStateTimer * 8)) * 0.2;
      this.petNode.position.y = jumpH;
      // Squash and stretch
      var squash = 1 + jumpH * 0.5;
      if (body) body.scaleVec.set(1 - jumpH * 0.3, 0.85 * squash, 0.9 - jumpH * 0.2);
      // Shadow shrinks when airborne
      var shadow = this.petParts.shadow;
      if (shadow) shadow.scaleVec.set(1 - jumpH * 2, 1, 1 - jumpH * 2);
      // End after 1.2s
      if (this.petStateTimer > 1.2) {
        this.petState = PET_STATE.IDLE;
        this.petNode.position.y = 0;
        if (shadow) shadow.scaleVec.set(1, 1, 1);
      }
    } else if (this.petState === PET_STATE.EAT) {
      // Chewing nod
      var nod = Math.sin(this.petStateTimer * 12) * 0.06;
      if (head) head.rotation.x = nod;
      if (this.petStateTimer > 1.5) {
        this.petState = PET_STATE.IDLE;
        if (head) head.rotation.x = 0;
      }
    }
  }

  _updateProps(dt) {
    // Vinyl disc rotation
    if (this.vinylDisc) {
      this.vinylDisc.rotation.y += dt * 2.0;
    }
  }

  // =================== 工具方法 ===================
  _makeNode(name, geom, color, x, y, z) {
    var node = new SceneNode(name);
    node.mesh = this.renderer.createMesh(geom, color);
    if (x != null) node.position.set(x, y, z);
    return node;
  }

  destroy() {
    if (this.animId && this.canvas) {
      this.canvas.cancelAnimationFrame(this.animId);
    }
    if (this.renderer) this.renderer.destroy();
    this.renderer = null;
    this.camera = null;
    this.scene = null;
    this.petNode = null;
    this.petParts = {};
  }
}

module.exports = { IsometricWorld: IsometricWorld, SPOTS_3D: SPOTS_3D };
