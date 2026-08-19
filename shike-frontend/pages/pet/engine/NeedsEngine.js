// pages/pet/engine/NeedsEngine.js
/**
 * 宠物内在 8 大生理与心理需求系统 (Needs Simulator)
 * 实时演进：饥饿、口渴、精力、好奇心、无聊度、心情、社交需求
 */

class NeedsEngine {
  constructor(initialData = {}) {
    this.hunger = initialData.hunger ?? 25;       // 饥饿度 (0=饱, 100=极饿)
    this.thirst = initialData.thirst ?? 20;       // 口渴度 (0=饱, 100=极渴)
    this.energy = initialData.energy ?? 80;       // 精力值 (100=满, 0=精疲力竭)
    this.curiosity = initialData.curiosity ?? 60; // 好奇心 (0~100)
    this.boredom = initialData.boredom ?? 30;     // 无聊度 (0~100)
    this.mood = initialData.mood ?? 85;           // 心情值 (0=抑郁, 100=极度快乐)
    this.social = initialData.social ?? 40;       // 社交/亲人欲望 (0~100)
    this.lastTickTime = Date.now();
  }

  // 每秒需求自然流逝更新
  tick(deltaSeconds = 1) {
    // 饥饿与口渴缓慢上升
    this.hunger = Math.min(100, this.hunger + 0.15 * deltaSeconds);
    this.thirst = Math.min(100, this.thirst + 0.18 * deltaSeconds);

    // 精力缓慢消耗 (走动/玩耍时消耗加速)
    this.energy = Math.max(0, this.energy - 0.10 * deltaSeconds);

    // 好奇心与无聊度缓慢上升
    this.curiosity = Math.min(100, this.curiosity + 0.20 * deltaSeconds);
    this.boredom = Math.min(100, this.boredom + 0.25 * deltaSeconds);
    this.social = Math.min(100, this.social + 0.12 * deltaSeconds);

    // 饥饿过高会降低心情
    if (this.hunger > 75 || this.boredom > 80) {
      this.mood = Math.max(20, this.mood - 0.1 * deltaSeconds);
    }
  }

  // 行为对需求的反哺与消耗
  onEat() {
    this.hunger = Math.max(0, this.hunger - 45);
    this.mood = Math.min(100, this.mood + 15);
    this.boredom = Math.max(0, this.boredom - 10);
  }

  onDrink() {
    this.thirst = Math.max(0, this.thirst - 50);
    this.mood = Math.min(100, this.mood + 8);
  }

  onSleep(durationSec = 10) {
    this.energy = Math.min(100, this.energy + 4.0 * durationSec);
    this.boredom = Math.max(0, this.boredom - 15);
  }

  onPlay() {
    this.boredom = Math.max(0, this.boredom - 40);
    this.curiosity = Math.max(0, this.curiosity - 25);
    this.energy = Math.max(0, this.energy - 15);
    this.mood = Math.min(100, this.mood + 20);
  }

  onExplore() {
    this.curiosity = Math.max(0, this.curiosity - 35);
    this.boredom = Math.max(0, this.boredom - 20);
    this.energy = Math.max(0, this.energy - 8);
  }

  onPat() {
    this.social = Math.max(0, this.social - 45);
    this.mood = Math.min(100, this.mood + 25);
    this.boredom = Math.max(0, this.boredom - 15);
  }

  // 综合评估当前心情标签
  getMoodStatus() {
    if (this.energy < 25) return { mood: 'TIRED', text: '困意浓浓 💤' };
    if (this.hunger > 70) return { mood: 'HUNGRY', text: '肚子咕咕叫 🥺' };
    if (this.mood > 80) return { mood: 'HAPPY', text: '超级开心 😄' };
    if (this.boredom > 60) return { mood: 'BORED', text: '想要玩耍 🎾' };
    return { mood: 'NORMAL', text: '悠然自得 🌱' };
  }
}

module.exports = NeedsEngine;
