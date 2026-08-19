// pages/pet/engine/UtilityAIDecision.js
/**
 * Utility AI 效用决策引擎
 * 综合评估内在需求 (Needs) + 宠物性格偏置 (Personality) + 环境物件，
 * 计算各候选行为效用得分，自主决定宠物的下一个生活目标。
 */

// 5 大神兽性格偏置矩阵
const PERSONALITY_TRAITS = {
  DRAGON: { // 青玉小幼龙：活泼探索型
    active: 1.3,
    lazy: 0.7,
    curious: 1.4,
    gluttonous: 1.1,
    affectionate: 1.0
  },
  TOTORO: { // 治愈大龙猫：治愈懒惰型
    active: 0.6,
    lazy: 1.6,
    curious: 0.8,
    gluttonous: 1.3,
    affectionate: 1.2
  },
  CAT: { // 软萌元气猫：灵动好奇型
    active: 1.2,
    lazy: 1.1,
    curious: 1.5,
    gluttonous: 0.9,
    affectionate: 0.9
  },
  DOG: { // 忠诚自律狗：亲人运动型
    active: 1.5,
    lazy: 0.5,
    curious: 1.1,
    gluttonous: 1.2,
    affectionate: 1.6
  },
  QILIN: { // 祥瑞小麒麟：稳重祥瑞型
    active: 1.0,
    lazy: 0.9,
    curious: 1.2,
    gluttonous: 1.0,
    affectionate: 1.1
  }
};

class UtilityAIDecision {
  constructor(petType = 'DRAGON') {
    this.petType = petType;
    this.traits = PERSONALITY_TRAITS[petType] || PERSONALITY_TRAITS['DRAGON'];
  }

  setPetType(petType) {
    this.petType = petType;
    this.traits = PERSONALITY_TRAITS[petType] || PERSONALITY_TRAITS['DRAGON'];
  }

  // 评估所有可选行为得分，选取得分最高的目标
  evaluateNextAction(needs, smartObjects, lastActionId = null) {
    const candidates = [];

    // 1. 评估：沙发打盹 / 睡觉 (由低精力、高困意、懒惰性格驱动)
    if (smartObjects.sofa) {
      let score = (100 - needs.energy) * 1.2 * this.traits.lazy;
      if (lastActionId === 'sofa') score *= 0.2; // 避免刚刚醒来立即重复睡
      candidates.push({
        obj: smartObjects.sofa,
        score: score + (Math.random() * 10 - 5) // 随机微扰
      });
    }

    // 2. 评估：闻绿植 / 探索花草 (由高好奇心驱动)
    if (smartObjects.plant || smartObjects.flower_hill) {
      const obj = smartObjects.plant || smartObjects.flower_hill;
      let score = needs.curiosity * 1.1 * this.traits.curious;
      if (lastActionId === obj.id) score *= 0.3;
      candidates.push({
        obj: obj,
        score: score + (Math.random() * 10 - 5)
      });
    }

    // 3. 评估：地毯伸懒腰 / 草坪打滚 (由中等精力与好心情驱动)
    if (smartObjects.rug || smartObjects.lawn_center) {
      const obj = smartObjects.rug || smartObjects.lawn_center;
      let score = (needs.mood * 0.6 + needs.boredom * 0.4) * 0.9;
      if (lastActionId === obj.id) score *= 0.4;
      candidates.push({
        obj: obj,
        score: score + (Math.random() * 10 - 5)
      });
    }

    // 4. 评估：瑜伽垫 / 跑步机玩耍 (由活泼度与高精力驱动)
    if (smartObjects.yoga_mat || smartObjects.treadmill) {
      const obj = smartObjects.yoga_mat || smartObjects.treadmill;
      let score = (needs.energy * 0.5 + needs.boredom * 0.5) * this.traits.active;
      if (lastActionId === obj.id) score *= 0.3;
      candidates.push({
        obj: obj,
        score: score + (Math.random() * 10 - 5)
      });
    }

    // 5. 评估：飘窗远眺 / 果树乘凉
    if (smartObjects.window || smartObjects.fruit_tree) {
      const obj = smartObjects.window || smartObjects.fruit_tree;
      let score = (needs.curiosity * 0.6 + (100 - needs.energy) * 0.4);
      if (lastActionId === obj.id) score *= 0.3;
      candidates.push({
        obj: obj,
        score: score + (Math.random() * 10 - 5)
      });
    }

    // 6. 评估：喝水 (由口渴度驱动)
    if (smartObjects.waterfall_pond || smartObjects.fountain) {
      const obj = smartObjects.waterfall_pond || smartObjects.fountain;
      let score = needs.thirst * 1.3;
      candidates.push({
        obj: obj,
        score: score + (Math.random() * 8)
      });
    }

    // 7. 评估：食盆就餐 (如果饥饿度高且有食物)
    if (smartObjects.food_bowl && needs.hunger > 60) {
      let score = needs.hunger * 1.4 * this.traits.gluttonous;
      candidates.push({
        obj: smartObjects.food_bowl,
        score: score + 15
      });
    }

    // 排序选取得分最高项
    candidates.sort((a, b) => b.score - a.score);
    return candidates[0] ? candidates[0].obj : (smartObjects.rug || smartObjects.lawn_center);
  }
}

module.exports = UtilityAIDecision;
