// pages/pet/engine/SmartObjects.js
/**
 * 3D 环境智能物体感知注册表 (Smart Objects Registry)
 * 定义房间各家具物件的交互点坐标、行为序列（动作链）、持续时长与优先级
 */

const SMART_OBJECTS = {
  ROOM: {
    // 1. 软糯白地毯 (休息/伸懒腰/翻滚)
    rug: {
      id: 'rug',
      name: '软糯白地毯',
      pos: { x: 48, y: 58 },
      interactionType: 'RUG_PLAY',
      priority: 3,
      thoughtText: '跑到地毯上伸个懒腰 🐾',
      actionSteps: [
        { type: 'WALK', target: { x: 48, y: 58 } },
        { type: 'STRETCH', duration: 2500, dialogue: '伸个大大的懒腰～好舒服！✨' },
        { type: 'ROLL', duration: 3000, dialogue: '在软软的地毯上打滚儿～' },
        { type: 'IDLE', duration: 2000 }
      ]
    },
    // 2. 温馨沙发 (跳上沙发/打盹/睡觉)
    sofa: {
      id: 'sofa',
      name: '温馨布艺沙发',
      pos: { x: 62, y: 48 },
      interactionType: 'SOFA_NAP',
      priority: 5,
      thoughtText: '跳上沙发打个惬意的小盹 🛋️',
      actionSteps: [
        { type: 'WALK', target: { x: 56, y: 54 } },
        { type: 'JUMP_UP', target: { x: 62, y: 48 }, duration: 1200 },
        { type: 'TURN', duration: 800 },
        { type: 'LIE_DOWN', duration: 1500, dialogue: '呼噜噜～沙发上太暖和啦 💤' },
        { type: 'SLEEP', duration: 9000, bubble: 'zZZ' },
        { type: 'WAKE_UP', duration: 1500 },
        { type: 'JUMP_DOWN', target: { x: 54, y: 58 }, duration: 1000 }
      ]
    },
    // 3. 室内阳光绿植 (探头观察/闻植物)
    plant: {
      id: 'plant',
      name: '室内落地绿植',
      pos: { x: 28, y: 50 },
      interactionType: 'SNIFF_PLANT',
      priority: 2,
      thoughtText: '去闻闻落地窗边的大植物 🌿',
      actionSteps: [
        { type: 'WALK', target: { x: 30, y: 52 } },
        { type: 'SNIFF', duration: 3000, dialogue: '嗅嗅～这盆绿植有阳光青草的味道 🌱' },
        { type: 'LOOK_AROUND', duration: 2000 },
        { type: 'IDLE', duration: 1000 }
      ]
    },
    // 4. 健康食盆 (大口干饭)
    food_bowl: {
      id: 'food_bowl',
      name: '健康营养食盆',
      pos: { x: 74, y: 72 },
      interactionType: 'EAT_FOOD',
      priority: 6,
      thoughtText: '奔向食盆大口干饭 🥣',
      actionSteps: [
        { type: 'FAST_WALK', target: { x: 74, y: 72 } },
        { type: 'EAT', duration: 3500, dialogue: '嚼嚼嚼！减脂餐真香～😋' },
        { type: 'LICK_MOUTH', duration: 1500 },
        { type: 'HAPPY', duration: 1200, dialogue: '吃饱饱，今天陪你一起燃脂！🔥' }
      ]
    },
    // 5. 瑜伽垫与小哑铃 (运动玩耍)
    yoga_mat: {
      id: 'yoga_mat',
      name: '瑜伽垫与运动区',
      pos: { x: 68, y: 68 },
      interactionType: 'WORKOUT_PLAY',
      priority: 3,
      thoughtText: '去瑜伽垫上踩一踩小哑铃 🏃',
      actionSteps: [
        { type: 'WALK', target: { x: 68, y: 68 } },
        { type: 'BOUNCE_PLAY', duration: 3000, dialogue: '主人今天运动打卡了吗？跟我一起动起来！💪' },
        { type: 'IDLE', duration: 2000 }
      ]
    },
    // 6. 阳光飘窗 (远眺发呆)
    window: {
      id: 'window',
      name: '落地阳光大窗',
      pos: { x: 32, y: 46 },
      interactionType: 'WINDOW_GAZE',
      priority: 2,
      thoughtText: '站在窗边看看外面的小鸟 ☀️',
      actionSteps: [
        { type: 'WALK', target: { x: 32, y: 46 } },
        { type: 'GAZE', duration: 4000, dialogue: '窗外阳光真好，今天也是元气满满的一天 🐦' },
        { type: 'IDLE', duration: 1500 }
      ]
    }
  },

  ISLAND: {
    flower_hill: {
      id: 'flower_hill',
      name: '向日葵花坡',
      pos: { x: 44, y: 46 },
      interactionType: 'CHASE_BUTTERFLY',
      priority: 3,
      thoughtText: '在花坡上扑彩蝶 🦋',
      actionSteps: [
        { type: 'WALK', target: { x: 44, y: 46 } },
        { type: 'BOUNCE_PLAY', duration: 3500, dialogue: '抓到一只小彩蝶啦！嘻嘻～✨' },
        { type: 'IDLE', duration: 2000 }
      ]
    },
    waterfall_pond: {
      id: 'waterfall_pond',
      name: '清冽泉池',
      pos: { x: 64, y: 56 },
      interactionType: 'DRINK_WATER',
      priority: 4,
      thoughtText: '走到清泉边喝口水 💧',
      actionSteps: [
        { type: 'WALK', target: { x: 64, y: 56 } },
        { type: 'DRINK', duration: 3000, dialogue: '咕咚咕咚～泉水好甜好清凉！💦' },
        { type: 'IDLE', duration: 1500 }
      ]
    },
    fruit_tree: {
      id: 'fruit_tree',
      name: '结晶果树',
      pos: { x: 54, y: 38 },
      interactionType: 'REST_TREE',
      priority: 3,
      thoughtText: '在果树下乘凉小憩 🍃',
      actionSteps: [
        { type: 'WALK', target: { x: 54, y: 38 } },
        { type: 'LIE_DOWN', duration: 2000, dialogue: '树荫下微风吹着好惬意～' },
        { type: 'SLEEP', duration: 6000, bubble: 'zZZ' },
        { type: 'WAKE_UP', duration: 1000 }
      ]
    },
    food_bowl: {
      id: 'food_bowl',
      name: '空岛食盆',
      pos: { x: 32, y: 66 },
      interactionType: 'EAT_FOOD',
      priority: 6,
      thoughtText: '去木桥边享用美食 🍎',
      actionSteps: [
        { type: 'FAST_WALK', target: { x: 32, y: 66 } },
        { type: 'EAT', duration: 3500, dialogue: '大口嚼嚼嚼～超满足！😋' },
        { type: 'HAPPY', duration: 1200 }
      ]
    }
  },

  YARD: {
    lawn_center: {
      id: 'lawn_center',
      name: '阳光绿茵草坪',
      pos: { x: 56, y: 60 },
      interactionType: 'LAWN_ROLL',
      priority: 3,
      thoughtText: '在露台草坪上撒欢打滚 🌿',
      actionSteps: [
        { type: 'WALK', target: { x: 56, y: 60 } },
        { type: 'ROLL', duration: 3000, dialogue: '草地好软呀！翻个跟头～🐾' },
        { type: 'IDLE', duration: 2000 }
      ]
    },
    treadmill: {
      id: 'treadmill',
      name: '户外迷你跑步机',
      pos: { x: 34, y: 64 },
      interactionType: 'TREADMILL_RUN',
      priority: 4,
      thoughtText: '在跑步机旁陪主人锻炼 🏃',
      actionSteps: [
        { type: 'WALK', target: { x: 34, y: 64 } },
        { type: 'BOUNCE_PLAY', duration: 4000, dialogue: '1，2，3，4！坚持运动燃脂！🔥' },
        { type: 'IDLE', duration: 2000 }
      ]
    },
    fountain: {
      id: 'fountain',
      name: '雕花喷泉',
      pos: { x: 70, y: 52 },
      interactionType: 'FOUNTAIN_WATER',
      priority: 3,
      thoughtText: '去喷泉水池边洗洗脸 💦',
      actionSteps: [
        { type: 'WALK', target: { x: 70, y: 52 } },
        { type: 'DRINK', duration: 3000, dialogue: '喷泉水好清澈，体态越来越棒啦！💧' },
        { type: 'IDLE', duration: 1500 }
      ]
    },
    food_bowl: {
      id: 'food_bowl',
      name: '庭院食盆',
      pos: { x: 26, y: 56 },
      interactionType: 'EAT_FOOD',
      priority: 6,
      thoughtText: '享用自律大餐 🥣',
      actionSteps: [
        { type: 'FAST_WALK', target: { x: 26, y: 56 } },
        { type: 'EAT', duration: 3500, dialogue: '香喷喷！美味又健康～😋' },
        { type: 'HAPPY', duration: 1200 }
      ]
    }
  }
};

module.exports = SMART_OBJECTS;
