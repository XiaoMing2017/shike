// pet_entity_3d.js - 3D 独立卡通萌宠实体与自主生命状态机
export class PetEntity3D {
  constructor(THREE, species = 'DRAGON', stageRank = 1) {
    this.THREE = THREE;
    this.species = species;
    this.stageRank = stageRank;

    // 状态机与计时器
    this.state = 'IDLE'; // IDLE, WANDER, SNIFF_FLOWER, DRINK_WATER, REST_SIT, SLEEP, JOY_BOUNCE, EAT
    this.stateTimer = 0;
    this.stateDuration = 4.0;
    this.animTime = 0;
    this.walkCycle = 0;

    // 空间坐标与导航寻路
    this.position = new THREE.Vector3(0, 0.45, 0);
    this.targetPosition = new THREE.Vector3(0, 0.45, 0);
    this.targetRotationY = 0;
    this.currentRotationY = 0;
    this.moveSpeed = 1.6;

    // 眨眼与表情
    this.blinkTimer = 0;
    this.isBlinking = false;
    this.isSmiling = false;

    // 3D 对象根节点
    this.root = new THREE.Group();
    this.root.position.copy(this.position);

    // 构建三维几何体与骨骼
    this.build3DMesh();
  }

  build3DMesh() {
    const THREE = this.THREE;

    // 1. 配色材质方案
    const colors = {
      DRAGON: { body: 0x86EFAC, belly: 0xFEF3C7, horn: 0xFDE047, wing: 0xBBF7D0, blush: 0xFDA4AF },
      TOTORO: { body: 0x94A3B8, belly: 0xFEF3C7, horn: 0x64748B, wing: 0xCBD5E1, blush: 0xFECDD3 },
      CAT:    { body: 0xFDBA74, belly: 0xFFF7ED, horn: 0xFB923C, wing: 0xFED7AA, blush: 0xFDA4AF },
      DOG:    { body: 0xFDE047, belly: 0xFEF3C7, horn: 0xEAB308, wing: 0xFEF08A, blush: 0xFECDD3 }
    };
    const c = colors[this.species] || colors.DRAGON;

    this.bodyMat = new THREE.MeshLambertMaterial({ color: c.body });
    this.bellyMat = new THREE.MeshLambertMaterial({ color: c.belly });
    this.hornMat = new THREE.MeshLambertMaterial({ color: c.horn });
    this.wingMat = new THREE.MeshLambertMaterial({ color: c.wing, transparent: true, opacity: 0.9 });
    this.blushMat = new THREE.MeshLambertMaterial({ color: c.blush });
    this.eyeMat = new THREE.MeshBasicMaterial({ color: 0x0F172A });
    this.shineMat = new THREE.MeshBasicMaterial({ color: 0xFFFFFF });

    // 2. 地面软阴影 (贴地多边形)
    const shadowGeo = new THREE.CircleGeometry(0.55, 24);
    const shadowMat = new THREE.MeshBasicMaterial({
      color: 0x1E293B,
      transparent: true,
      opacity: 0.28,
      depthWrite: false
    });
    this.shadowMesh = new THREE.Mesh(shadowGeo, shadowMat);
    this.shadowMesh.rotation.x = -Math.PI / 2;
    this.shadowMesh.position.y = -0.43;
    this.root.add(this.shadowMesh);

    // 3. 模型主装配体
    this.model = new THREE.Group();
    this.root.add(this.model);

    // 4. 圆润身体 (Q版梨形)
    const bodyGeo = new THREE.SphereGeometry(0.48, 20, 18);
    bodyGeo.scale(1.0, 1.08, 0.92);
    this.bodyMesh = new THREE.Mesh(bodyGeo, this.bodyMat);
    this.model.add(this.bodyMesh);

    // 小白肚皮
    const bellyGeo = new THREE.SphereGeometry(0.38, 16, 14);
    bellyGeo.scale(0.85, 0.95, 0.35);
    this.bellyMesh = new THREE.Mesh(bellyGeo, this.bellyMat);
    this.bellyMesh.position.set(0, -0.05, 0.32);
    this.model.add(this.bellyMesh);

    // 5. 头部与面部装配
    this.headGroup = new THREE.Group();
    this.headGroup.position.set(0, 0.38, 0.05);
    this.model.add(this.headGroup);

    const headGeo = new THREE.SphereGeometry(0.44, 20, 18);
    headGeo.scale(1.05, 0.96, 0.98);
    this.headMesh = new THREE.Mesh(headGeo, this.bodyMat);
    this.headGroup.add(this.headMesh);

    // 萌系大眼睛与高光
    this.eyes = new THREE.Group();
    this.headGroup.add(this.eyes);

    const eyeGeo = new THREE.SphereGeometry(0.075, 12, 10);
    eyeGeo.scale(0.7, 1.0, 0.4);

    this.leftEye = new THREE.Mesh(eyeGeo, this.eyeMat);
    this.leftEye.position.set(-0.16, 0.04, 0.40);
    this.eyes.add(this.leftEye);

    this.rightEye = new THREE.Mesh(eyeGeo, this.eyeMat);
    this.rightEye.position.set(0.16, 0.04, 0.40);
    this.eyes.add(this.rightEye);

    // 眼睛白色星星高光
    const shineGeo = new THREE.SphereGeometry(0.026, 8, 8);
    const leftShine = new THREE.Mesh(shineGeo, this.shineMat);
    leftShine.position.set(-0.14, 0.07, 0.43);
    this.eyes.add(leftShine);

    const rightShine = new THREE.Mesh(shineGeo, this.shineMat);
    rightShine.position.set(0.18, 0.07, 0.43);
    this.eyes.add(rightShine);

    // 粉嫩腮红
    const blushGeo = new THREE.CircleGeometry(0.055, 12);
    const leftBlush = new THREE.Mesh(blushGeo, this.blushMat);
    leftBlush.position.set(-0.25, -0.06, 0.36);
    leftBlush.rotation.y = -0.3;
    this.headGroup.add(leftBlush);

    const rightBlush = new THREE.Mesh(blushGeo, this.blushMat);
    rightBlush.position.set(0.25, -0.06, 0.36);
    rightBlush.rotation.y = 0.3;
    this.headGroup.add(rightBlush);

    // 耳朵/角
    this.buildSpeciesFeatures(THREE);

    // 6. 独立四肢 (供走步摆臂与坐卧使用)
    const armGeo = new THREE.CylinderGeometry(0.08, 0.08, 0.22, 8);
    
    this.leftArm = new THREE.Group();
    this.leftArm.position.set(-0.42, 0.08, 0.12);
    const lArmMesh = new THREE.Mesh(armGeo, this.bodyMat);
    lArmMesh.position.set(0, -0.1, 0);
    lArmMesh.rotation.z = 0.35;
    this.leftArm.add(lArmMesh);
    this.model.add(this.leftArm);

    this.rightArm = new THREE.Group();
    this.rightArm.position.set(0.42, 0.08, 0.12);
    const rArmMesh = new THREE.Mesh(armGeo, this.bodyMat);
    rArmMesh.position.set(0, -0.1, 0);
    rArmMesh.rotation.z = -0.35;
    this.rightArm.add(rArmMesh);
    this.model.add(this.rightArm);

    // 短萌小腿
    const legGeo = new THREE.SphereGeometry(0.12, 10, 8);
    legGeo.scale(0.85, 0.7, 1.3);

    this.leftLeg = new THREE.Group();
    this.leftLeg.position.set(-0.24, -0.36, 0.1);
    const lLegMesh = new THREE.Mesh(legGeo, this.bodyMat);
    this.leftLeg.add(lLegMesh);
    this.model.add(this.leftLeg);

    this.rightLeg = new THREE.Group();
    this.rightLeg.position.set(0.24, -0.36, 0.1);
    const rLegMesh = new THREE.Mesh(legGeo, this.bodyMat);
    this.rightLeg.add(rLegMesh);
    this.model.add(this.rightLeg);

    // 7. 尾巴
    this.tail = new THREE.Group();
    this.tail.position.set(0, -0.25, -0.42);
    const tailGeo = new THREE.ConeGeometry(0.12, 0.32, 8);
    tailGeo.rotateX(-Math.PI / 3);
    const tailMesh = new THREE.Mesh(tailGeo, this.bodyMat);
    this.tail.add(tailMesh);
    this.model.add(this.tail);

    // 阶段进阶专属配饰 (花环/光环)
    if (this.stageRank >= 2) {
      const crownGeo = new THREE.TorusGeometry(0.24, 0.04, 8, 18);
      crownGeo.rotateX(Math.PI / 2);
      const crownMat = new THREE.MeshLambertMaterial({ color: 0xF472B6 });
      const crown = new THREE.Mesh(crownGeo, crownMat);
      crown.position.set(0, 0.40, 0);
      this.headGroup.add(crown);
    }
  }

  buildSpeciesFeatures(THREE) {
    if (this.species === 'DRAGON') {
      // 龙角
      const hornGeo = new THREE.ConeGeometry(0.065, 0.22, 8);
      const lHorn = new THREE.Mesh(hornGeo, this.hornMat);
      lHorn.position.set(-0.22, 0.38, -0.05);
      lHorn.rotation.z = -0.4;
      lHorn.rotation.x = -0.2;
      this.headGroup.add(lHorn);

      const rHorn = new THREE.Mesh(hornGeo, this.hornMat);
      rHorn.position.set(0.22, 0.38, -0.05);
      rHorn.rotation.z = 0.4;
      rHorn.rotation.x = -0.2;
      this.headGroup.add(rHorn);

      // 小龙翼 (采用圆润卡通薄翼 Shape)
      this.wings = new THREE.Group();
      this.wings.position.set(0, 0.08, -0.38);

      const wingShape = new THREE.Shape();
      wingShape.moveTo(0, 0);
      wingShape.quadraticCurveTo(-0.25, 0.28, -0.42, 0.22);
      wingShape.quadraticCurveTo(-0.22, 0.06, -0.15, -0.12);
      wingShape.closePath();

      const wingGeo = new THREE.ShapeGeometry(wingShape);

      const lWing = new THREE.Mesh(wingGeo, this.wingMat);
      lWing.position.set(-0.06, 0, 0);
      lWing.rotation.y = 0.25;
      this.wings.add(lWing);

      const rWing = new THREE.Mesh(wingGeo, this.wingMat);
      rWing.position.set(0.06, 0, 0);
      rWing.rotation.y = Math.PI - 0.25;
      this.wings.add(rWing);

      this.model.add(this.wings);
    } else if (this.species === 'TOTORO') {
      // 龙猫萌耳
      const earGeo = new THREE.ConeGeometry(0.09, 0.28, 8);
      const lEar = new THREE.Mesh(earGeo, this.bodyMat);
      lEar.position.set(-0.22, 0.42, 0);
      lEar.rotation.z = -0.25;
      this.headGroup.add(lEar);

      const rEar = new THREE.Mesh(earGeo, this.bodyMat);
      rEar.position.set(0.22, 0.42, 0);
      rEar.rotation.z = 0.25;
      this.headGroup.add(rEar);
      // 萌耳
      const earGeo = new THREE.ConeGeometry(0.12, 0.24, 6);
      const lEar = new THREE.Mesh(earGeo, this.bodyMat);
      lEar.position.set(-0.26, 0.36, 0);
      lEar.rotation.z = -0.35;
      this.headGroup.add(lEar);

      const rEar = new THREE.Mesh(earGeo, this.bodyMat);
      rEar.position.set(0.26, 0.36, 0);
      rEar.rotation.z = 0.35;
      this.headGroup.add(rEar);
    }
  }

  // 🌟 自主生命系统帧循环驱动
  update(deltaTime, worldWaypoints = []) {
    this.animTime += deltaTime;
    this.stateTimer += deltaTime;

    // 1. 自然眨眼循环
    this.blinkTimer += deltaTime;
    if (this.blinkTimer > 3.6) {
      this.isBlinking = true;
      this.eyes.scale.y = 0.12;
      if (this.blinkTimer > 3.75) {
        this.eyes.scale.y = 1.0;
        this.isBlinking = false;
        this.blinkTimer = Math.random() * 0.8;
      }
    }

    // 2. 状态机行为更新
    switch (this.state) {
      case 'IDLE':
        this.updateIdleState(deltaTime, worldWaypoints);
        break;
      case 'WANDER':
        this.updateWanderState(deltaTime);
        break;
      case 'SNIFF_FLOWER':
        this.updateSniffState(deltaTime);
        break;
      case 'DRINK_WATER':
        this.updateDrinkState(deltaTime);
        break;
      case 'REST_SIT':
        this.updateSitState(deltaTime);
        break;
      case 'SLEEP':
        this.updateSleepState(deltaTime);
        break;
      case 'JOY_BOUNCE':
        this.updateJoyState(deltaTime);
        break;
      case 'EAT':
        this.updateEatState(deltaTime);
        break;
    }

    // 3. 平滑旋转朝向目标
    let diffRot = this.targetRotationY - this.currentRotationY;
    while (diffRot < -Math.PI) diffRot += Math.PI * 2;
    while (diffRot > Math.PI) diffRot -= Math.PI * 2;
    this.currentRotationY += diffRot * Math.min(1.0, deltaTime * 8.0);
    this.root.rotation.y = this.currentRotationY;

    // 4. 同步根节点位置
    this.root.position.x = this.position.x;
    this.root.position.z = this.position.z;
    this.root.position.y = this.position.y;
  }

  // 待机呼吸与随机决策
  updateIdleState(deltaTime, waypoints) {
    const t = this.animTime * 3.2;
    // 呼吸起伏
    this.model.position.y = Math.sin(t) * 0.02;
    this.model.scale.set(1.0 + Math.cos(t) * 0.02, 1.0 - Math.sin(t) * 0.02, 1.0 + Math.cos(t) * 0.02);
    this.headGroup.rotation.y = Math.sin(this.animTime * 1.5) * 0.15;
    this.tail.rotation.z = Math.sin(this.animTime * 4.0) * 0.2;
    this.shadowMesh.scale.setScalar(1.0 + Math.sin(t) * 0.05);

    // 待机超时，自主决定下一步行为
    if (this.stateTimer > this.stateDuration) {
      this.stateTimer = 0;
      const roll = Math.random();
      if (roll < 0.5 && waypoints && waypoints.length > 0) {
        // 50% 概率自主漫步寻路
        const randomPoint = waypoints[Math.floor(Math.random() * waypoints.length)];
        this.navigateTo(randomPoint.x, randomPoint.z, randomPoint.action || 'IDLE');
      } else if (roll < 0.7) {
        // 20% 概率坐下休息
        this.setState('REST_SIT', 3.5);
      } else if (roll < 0.85) {
        // 15% 概率原地开心摇摆
        this.setState('JOY_BOUNCE', 1.8);
      } else {
        // 继续待机看风景
        this.stateDuration = 3.0 + Math.random() * 3.0;
      }
    }
  }

  // 漫步寻路
  updateWanderState(deltaTime) {
    this.walkCycle += deltaTime * 10.0;
    const dir = new this.THREE.Vector3().subVectors(this.targetPosition, this.position);
    dir.y = 0;
    const dist = dir.length();

    if (dist < 0.1) {
      // 到达目的地，转入目标动作
      this.leftArm.rotation.x = 0;
      this.rightArm.rotation.x = 0;
      this.leftLeg.rotation.x = 0;
      this.rightLeg.rotation.x = 0;
      this.setState(this.pendingAction || 'IDLE', 3.5);
      return;
    }

    dir.normalize();
    this.targetRotationY = Math.atan2(dir.x, dir.z);

    // 迈步移动
    const step = Math.min(dist, this.moveSpeed * deltaTime);
    this.position.addScaledVector(dir, step);

    // 走步欢快弹跳与肢体摆动
    this.model.position.y = Math.abs(Math.sin(this.walkCycle)) * 0.08;
    this.leftLeg.rotation.x = Math.sin(this.walkCycle) * 0.55;
    this.rightLeg.rotation.x = -Math.sin(this.walkCycle) * 0.55;
    this.leftArm.rotation.x = -Math.sin(this.walkCycle) * 0.45;
    this.rightArm.rotation.x = Math.sin(this.walkCycle) * 0.45;
    this.tail.rotation.y = Math.sin(this.walkCycle) * 0.35;
    if (this.wings) this.wings.rotation.x = Math.sin(this.walkCycle * 2) * 0.4;
  }

  // 闻花
  updateSniffState(deltaTime) {
    this.headGroup.rotation.x = 0.32;
    this.headGroup.rotation.z = Math.sin(this.animTime * 6.0) * 0.08;
    this.tail.rotation.z = Math.sin(this.animTime * 8.0) * 0.3;
    if (this.stateTimer > this.stateDuration) {
      this.headGroup.rotation.set(0, 0, 0);
      this.setState('IDLE', 3.0);
    }
  }

  // 饮水
  updateDrinkState(deltaTime) {
    this.headGroup.rotation.x = 0.42;
    this.model.position.y = -0.06 + Math.sin(this.animTime * 5.0) * 0.02;
    if (this.stateTimer > this.stateDuration) {
      this.headGroup.rotation.set(0, 0, 0);
      this.model.position.y = 0;
      this.setState('IDLE', 3.0);
    }
  }

  // 坐下休息
  updateSitState(deltaTime) {
    this.model.position.y = -0.12;
    this.leftLeg.rotation.x = -1.1;
    this.rightLeg.rotation.x = -1.1;
    this.headGroup.rotation.z = Math.sin(this.animTime * 1.5) * 0.1;
    if (this.stateTimer > this.stateDuration) {
      this.model.position.y = 0;
      this.leftLeg.rotation.x = 0;
      this.rightLeg.rotation.x = 0;
      this.setState('IDLE', 2.5);
    }
  }

  // 睡觉打盹
  updateSleepState(deltaTime) {
    this.model.position.y = -0.16;
    this.model.rotation.z = 0.25;
    this.eyes.scale.y = 0.08;
    if (this.stateTimer > this.stateDuration) {
      this.model.position.y = 0;
      this.model.rotation.z = 0;
      this.eyes.scale.y = 1.0;
      this.setState('IDLE', 3.0);
    }
  }

  // 欢快后空翻跳跃
  updateJoyState(deltaTime) {
    const p = Math.min(1.0, this.stateTimer / this.stateDuration);
    this.model.position.y = Math.sin(p * Math.PI) * 0.85;
    this.model.rotation.x = p * Math.PI * 2;
    this.shadowMesh.scale.setScalar(Math.max(0.4, 1.0 - this.model.position.y * 0.6));
    if (p >= 1.0) {
      this.model.position.y = 0;
      this.model.rotation.x = 0;
      this.shadowMesh.scale.setScalar(1.0);
      this.setState('IDLE', 3.0);
    }
  }

  // 就餐大快朵颐
  updateEatState(deltaTime) {
    this.headGroup.rotation.x = Math.abs(Math.sin(this.animTime * 12.0)) * 0.25;
    this.leftArm.rotation.x = 0.8 + Math.sin(this.animTime * 12.0) * 0.2;
    this.rightArm.rotation.x = 0.8 + Math.cos(this.animTime * 12.0) * 0.2;
    if (this.stateTimer > this.stateDuration) {
      this.headGroup.rotation.set(0, 0, 0);
      this.leftArm.rotation.set(0, 0, 0);
      this.rightArm.rotation.set(0, 0, 0);
      this.setState('JOY_BOUNCE', 1.6);
    }
  }

  // 触发导航寻路
  navigateTo(x, z, actionAfterArrive = 'IDLE') {
    this.targetPosition.set(x, 0.45, z);
    this.pendingAction = actionAfterArrive;
    this.state = 'WANDER';
    this.stateTimer = 0;
  }

  setState(newState, duration = 3.0) {
    this.state = newState;
    this.stateTimer = 0;
    this.stateDuration = duration;
  }

  triggerTapReaction() {
    this.setState('JOY_BOUNCE', 1.2);
  }

  triggerFeedAction(foodBowlPos) {
    if (foodBowlPos) {
      this.navigateTo(foodBowlPos.x, foodBowlPos.z, 'EAT');
    } else {
      this.setState('EAT', 2.8);
    }
  }
}
