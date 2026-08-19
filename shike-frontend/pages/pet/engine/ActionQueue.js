// pages/pet/engine/ActionQueue.js
/**
 * 复合行为序列执行器与打断系统 (Action Sequence Queue & Interrupt Handler)
 * 负责解析多步动作链（走动 -> 观察 -> 跳跃 -> 翻滚 -> 睡觉等），
 * 管理逐帧平滑寻路移动与高优先级玩家交互打断。
 */

class ActionQueue {
  constructor(navMesh, onStateUpdate) {
    this.navMesh = navMesh;
    this.onStateUpdate = onStateUpdate; // 回调通知 Page.setData 更新 UI
    this.currentAction = null;
    this.stepIndex = 0;
    this.isPlaying = false;
    this.currentWaypoints = [];
    this.waypointIndex = 0;
    this.moveTimer = null;
    this.actionTimer = null;
  }

  // 启动一个新的复合行为序列
  startActionSequence(smartObject, currentPos) {
    this.stopCurrent();
    this.currentAction = smartObject;
    this.stepIndex = 0;
    this.isPlaying = true;
    this.executeCurrentStep(currentPos);
  }

  // 执行当前步骤
  executeCurrentStep(currentPos) {
    if (!this.isPlaying || !this.currentAction) return;

    const steps = this.currentAction.actionSteps || [];
    if (this.stepIndex >= steps.length) {
      // 动作链全部完成，通知结束并由 AI 选下一个
      this.isPlaying = false;
      if (this.onStateUpdate) {
        this.onStateUpdate({
          type: 'ACTION_FINISHED',
          actionId: this.currentAction.id
        });
      }
      return;
    }

    const step = steps[this.stepIndex];

    switch (step.type) {
      case 'WALK':
      case 'FAST_WALK':
        this.handleWalkStep(step, currentPos);
        break;

      case 'JUMP_UP':
      case 'JUMP_DOWN':
      case 'JUMP':
        this.handleJumpStep(step);
        break;

      case 'SLEEP':
      case 'LIE_DOWN':
      case 'STRETCH':
      case 'ROLL':
      case 'SNIFF':
      case 'EAT':
      case 'DRINK':
      case 'BOUNCE_PLAY':
      case 'GAZE':
      case 'LOOK_AROUND':
      case 'IDLE':
      case 'HAPPY':
      default:
        this.handleStationaryStep(step);
        break;
    }
  }

  // 1. 处理走动路段 (调用 A* 寻路)
  handleWalkStep(step, currentPos) {
    const target = step.target;
    const path = this.navMesh.findPath(currentPos.x, currentPos.y, target.x, target.y);
    this.currentWaypoints = path;
    this.waypointIndex = 0;

    const isFast = step.type === 'FAST_WALK';
    this.stepThroughWaypoints(isFast);
  }

  // 沿 A* 折线路点逐段移动
  stepThroughWaypoints(isFast = false) {
    if (this.waypointIndex >= this.currentWaypoints.length) {
      // 到达终点，进入下一步
      this.stepIndex++;
      const lastPoint = this.currentWaypoints[this.currentWaypoints.length - 1];
      this.executeCurrentStep(lastPoint);
      return;
    }

    const nextPoint = this.currentWaypoints[this.waypointIndex];
    this.waypointIndex++;

    if (this.onStateUpdate) {
      this.onStateUpdate({
        type: 'MOVE_TO_POINT',
        point: nextPoint,
        isFast: isFast,
        depthScale: this.navMesh.calcDepthScale(nextPoint.y),
        thoughtText: this.currentAction.thoughtText
      });
    }

    // 每段路点移动耗时 (普通漫步 1.6s，快跑 0.9s)
    const legDuration = isFast ? 950 : 1600;
    this.moveTimer = setTimeout(() => {
      this.stepThroughWaypoints(isFast);
    }, legDuration);
  }

  // 2. 处理原地互动步骤 (睡觉/嗅探/伸懒腰/就餐)
  handleStationaryStep(step) {
    const duration = step.duration || 2000;

    if (this.onStateUpdate) {
      this.onStateUpdate({
        type: 'PLAY_ANIMATION',
        animType: step.type.toLowerCase(),
        dialogue: step.dialogue || null,
        bubble: step.bubble || null,
        thoughtText: this.currentAction.thoughtText,
        duration: duration
      });
    }

    this.actionTimer = setTimeout(() => {
      this.stepIndex++;
      this.executeCurrentStep();
    }, duration);
  }

  // 3. 处理跳跃步骤 (跳上沙发/下沙发)
  handleJumpStep(step) {
    const duration = step.duration || 1000;

    if (this.onStateUpdate) {
      this.onStateUpdate({
        type: 'JUMP_ANIMATION',
        target: step.target,
        depthScale: step.target ? this.navMesh.calcDepthScale(step.target.y) : null,
        duration: duration
      });
    }

    this.actionTimer = setTimeout(() => {
      this.stepIndex++;
      this.executeCurrentStep(step.target);
    }, duration);
  }

  // 4. 玩家行为高优先级打断 (抚摸/投喂/呼唤)
  interruptWithPlayerAction(interruptAction, currentPos) {
    this.stopCurrent();
    this.startActionSequence(interruptAction, currentPos);
  }

  stopCurrent() {
    this.isPlaying = false;
    if (this.moveTimer) {
      clearTimeout(this.moveTimer);
      this.moveTimer = null;
    }
    if (this.actionTimer) {
      clearTimeout(this.actionTimer);
      this.actionTimer = null;
    }
  }
}

module.exports = ActionQueue;
