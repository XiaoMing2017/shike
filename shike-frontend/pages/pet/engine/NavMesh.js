// pages/pet/engine/NavMesh.js
/**
 * 2.5D/3D 等距房间 NavMesh 空间网格与 A* 避障寻路系统
 * 负责场景可通行区域划分、障碍物碰撞剔除、折线平滑寻路与 3D 近大远小透视深度计算
 */

// 20x20 空间栅格 (0~100% 坐标映射)
const GRID_SIZE = 20;

// 各场景的障碍物区域定义 (百分比坐标 [minX, maxX, minY, maxY])
const SCENE_OBSTACLES = {
  ROOM: [
    { name: 'sofa_back', minX: 54, maxX: 82, minY: 34, maxY: 46 },    // 沙发主体与后靠背
    { name: 'bookshelf_wall', minX: 66, maxX: 88, minY: 20, maxY: 33 },// 书架墙角
    { name: 'window_shelf', minX: 14, maxX: 40, minY: 28, maxY: 44 },  // 窗台大盆栽
    { name: 'corner_basket', minX: 36, maxX: 46, minY: 72, maxY: 84 }, // 地面收纳篮
    { name: 'nightstand', minX: 72, maxX: 86, minY: 48, maxY: 60 }     // 床头小几
  ],
  ISLAND: [
    { name: 'waterfall_rock', minX: 34, maxX: 54, minY: 22, maxY: 38 },// 瀑布岩石
    { name: 'pond_water', minX: 52, maxX: 68, minY: 44, maxY: 56 }     // 水潭中心
  ],
  YARD: [
    { name: 'treadmill_block', minX: 16, maxX: 34, minY: 56, maxY: 72 },// 跑步机底座
    { name: 'fountain_basin', minX: 40, maxX: 54, minY: 44, maxY: 56 }, // 喷泉池
    { name: 'patio_chair', minX: 24, maxX: 38, minY: 38, maxY: 50 }     // 露台躺椅
  ]
};

// 场景边界 (有效地面范围)
const SCENE_BOUNDS = {
  ROOM: { minX: 20, maxX: 80, minY: 42, maxY: 78 },
  ISLAND: { minX: 24, maxX: 76, minY: 36, maxY: 74 },
  YARD: { minX: 22, maxX: 78, minY: 40, maxY: 76 }
};

class NavMesh {
  constructor(sceneKey = 'ROOM') {
    this.sceneKey = sceneKey;
    this.obstacles = SCENE_OBSTACLES[sceneKey] || SCENE_OBSTACLES['ROOM'];
    this.bounds = SCENE_BOUNDS[sceneKey] || SCENE_BOUNDS['ROOM'];
  }

  setScene(sceneKey) {
    this.sceneKey = sceneKey;
    this.obstacles = SCENE_OBSTACLES[sceneKey] || SCENE_OBSTACLES['ROOM'];
    this.bounds = SCENE_BOUNDS[sceneKey] || SCENE_BOUNDS['ROOM'];
  }

  // 坐标是否在障碍物内
  isBlocked(x, y) {
    // 边界检测
    if (x < this.bounds.minX || x > this.bounds.maxX || y < this.bounds.minY || y > this.bounds.maxY) {
      return true;
    }
    for (const obs of this.obstacles) {
      if (x >= obs.minX && x <= obs.maxX && y >= obs.minY && y <= obs.maxY) {
        return true;
      }
    }
    return false;
  }

  // 寻找最近的可通行点
  findNearestWalkable(x, y) {
    if (!this.isBlocked(x, y)) return { x, y };
    
    for (let r = 2; r <= 20; r += 2) {
      for (let dx = -r; dx <= r; dx += 2) {
        for (let dy = -r; dy <= r; dy += 2) {
          const nx = x + dx;
          const ny = y + dy;
          if (!this.isBlocked(nx, ny)) {
            return { x: nx, y: ny };
          }
        }
      }
    }
    return { x: 50, y: 60 }; // 默认地毯安全点
  }

  // 3D 等距透视缩放系数 (Y 越大代表越靠近镜头前景，模型越大)
  calcDepthScale(y) {
    const clampedY = Math.max(35, Math.min(80, y));
    // 远处 (y=35) 缩放 0.80，近处 (y=80) 缩放 1.15
    const scale = 0.80 + ((clampedY - 35) / 45) * 0.35;
    return parseFloat(scale.toFixed(2));
  }

  // A* 寻路算法：返回从起点到终点的可行路点数组
  findPath(startX, startY, endX, endY) {
    const start = this.findNearestWalkable(startX, startY);
    const end = this.findNearestWalkable(endX, endY);

    // 如果直线无阻挡，直接返回目标点
    if (!this.isLineBlocked(start.x, start.y, end.x, end.y)) {
      return [{ x: end.x, y: end.y }];
    }

    // A* 网格搜索
    const toGrid = (val) => Math.floor((val / 100) * GRID_SIZE);
    const toPercent = (gridVal) => (gridVal / GRID_SIZE) * 100 + (100 / (GRID_SIZE * 2));

    const sGx = toGrid(start.x);
    const sGy = toGrid(start.y);
    const eGx = toGrid(end.x);
    const eGy = toGrid(end.y);

    const openList = [];
    const closedSet = new Set();

    const startNode = {
      gx: sGx, gy: sGy,
      g: 0,
      h: Math.abs(sGx - eGx) + Math.abs(sGy - eGy),
      f: 0,
      parent: null
    };
    startNode.f = startNode.g + startNode.h;
    openList.push(startNode);

    const keyOf = (gx, gy) => `${gx}_${gy}`;
    const dirs = [
      { dx: 1, dy: 0 }, { dx: -1, dy: 0 }, { dx: 0, dy: 1 }, { dx: 0, dy: -1 },
      { dx: 1, dy: 1 }, { dx: -1, dy: 1 }, { dx: 1, dy: -1 }, { dx: -1, dy: -1 }
    ];

    let targetNode = null;
    let iterations = 0;

    while (openList.length > 0 && iterations < 300) {
      iterations++;
      openList.sort((a, b) => a.f - b.f);
      const current = openList.shift();
      const cKey = keyOf(current.gx, current.gy);

      if (current.gx === eGx && current.gy === eGy) {
        targetNode = current;
        break;
      }
      closedSet.add(cKey);

      for (const d of dirs) {
        const nGx = current.gx + d.dx;
        const nGy = current.gy + d.dy;
        const nKey = keyOf(nGx, nGy);

        if (closedSet.has(nKey)) continue;

        const pX = toPercent(nGx);
        const pY = toPercent(nGy);
        if (this.isBlocked(pX, pY)) continue;

        const stepCost = (d.dx !== 0 && d.dy !== 0) ? 1.414 : 1.0;
        const tentativeG = current.g + stepCost;

        let existing = openList.find(n => n.gx === nGx && n.gy === nGy);
        if (!existing) {
          const h = Math.abs(nGx - eGx) + Math.abs(nGy - eGy);
          const newNode = {
            gx: nGx, gy: nGy,
            g: tentativeG,
            h: h,
            f: tentativeG + h,
            parent: current
          };
          openList.push(newNode);
        } else if (tentativeG < existing.g) {
          existing.g = tentativeG;
          existing.f = existing.g + existing.h;
          existing.parent = current;
        }
      }
    }

    if (!targetNode) {
      return [{ x: end.x, y: end.y }];
    }

    // 回溯路径
    const path = [];
    let curr = targetNode;
    while (curr && curr.parent) {
      path.unshift({ x: toPercent(curr.gx), y: toPercent(curr.gy) });
      curr = curr.parent;
    }
    // 确保包含终点精确坐标
    path.push({ x: end.x, y: end.y });

    // 路径平滑化
    return this.smoothPath(start, path);
  }

  // 检测两点间直线是否有障碍
  isLineBlocked(x1, y1, x2, y2) {
    const steps = 8;
    for (let i = 1; i < steps; i++) {
      const t = i / steps;
      const checkX = x1 + (x2 - x1) * t;
      const checkY = y1 + (y2 - y1) * t;
      if (this.isBlocked(checkX, checkY)) return true;
    }
    return false;
  }

  // 路径平滑采样
  smoothPath(start, rawPath) {
    if (rawPath.length <= 2) return rawPath;
    const smoothed = [];
    let current = start;

    for (let i = 0; i < rawPath.length; i++) {
      // 如果从当前点到后续点可以直接无阻挡到达，跳过中间冗余点
      let furthest = i;
      for (let j = rawPath.length - 1; j > i; j--) {
        if (!this.isLineBlocked(current.x, current.y, rawPath[j].x, rawPath[j].y)) {
          furthest = j;
          break;
        }
      }
      smoothed.push(rawPath[furthest]);
      current = rawPath[furthest];
      i = furthest;
    }
    return smoothed;
  }
}

module.exports = NavMesh;
