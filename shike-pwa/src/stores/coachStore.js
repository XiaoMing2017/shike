import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCoachStore = defineStore('coach', () => {
  const activeTab = ref('coach') // 'squad' (peer) or 'coach' (supervision hub)
  
  const coachProfile = ref({
    name: 'Coach Marcus',
    title: 'NASM-CPT • Elite Nutrition Coach',
    avatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120',
    tier: 'Coach Pro',
    maxSeats: 30,
    totalClients: 24,
    inviteCode: 'SHRED-30X'
  })

  const cohort = ref({
    name: '2026 Summer Shred & Lean Muscle Camp',
    week: 2,
    totalWeeks: 8,
    daysLeft: 42
  })

  const clients = ref([
    {
      id: 'c1',
      name: 'Sarah Miller',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120',
      goal: 'Fat Loss (-0.5kg/wk)',
      goalZh: '减脂控卡',
      goalEn: 'Fat Loss',
      targetCalories: 1550,
      currentCalories: 1890,
      proteinTarget: 110,
      proteinCurrent: 78,
      status: 'danger', // red: over calorie limit
      statusReasonZh: '热量超标 +340 kcal，脂肪摄入偏高',
      statusReasonEn: 'Over budget +340 kcal, elevated fat intake',
      lastLogTime: '6:45 PM',
      nudged: false,
      praised: false,
      coachNote: 'Watch out for excess salad dressing at dinner!'
    },
    {
      id: 'c2',
      name: 'David Kim',
      avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120',
      goal: 'Fat Loss (-0.5kg/wk)',
      goalZh: '减脂控卡',
      goalEn: 'Fat Loss',
      targetCalories: 1750,
      currentCalories: 1120,
      proteinTarget: 135,
      proteinCurrent: 82,
      status: 'warning', // yellow: pending dinner
      statusReasonZh: '晚餐待打卡（距离目标尚差 630 kcal）',
      statusReasonEn: 'Pending dinner log (630 kcal remaining)',
      lastLogTime: '1:15 PM',
      nudged: false,
      praised: false,
      coachNote: 'Good lunch choices, remember to hit protein on dinner.'
    },
    {
      id: 'c3',
      name: 'Emma Watson',
      avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=120',
      goal: 'Lean Muscle (+200 kcal)',
      goalZh: '增肌塑形',
      goalEn: 'Lean Muscle',
      targetCalories: 2050,
      currentCalories: 1980,
      proteinTarget: 130,
      proteinCurrent: 138,
      status: 'good', // green: on track
      statusReasonZh: '全天热量与高蛋白完美达标',
      statusReasonEn: 'Calorie deficit & high protein crushed',
      lastLogTime: '7:30 PM',
      nudged: false,
      praised: true,
      coachNote: 'Awesome consistency on post-workout shake!'
    },
    {
      id: 'c4',
      name: 'Jason Chen',
      avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120',
      goal: 'Fat Loss (-0.5kg/wk)',
      goalZh: '减脂控卡',
      goalEn: 'Fat Loss',
      targetCalories: 1800,
      currentCalories: 950,
      proteinTarget: 140,
      proteinCurrent: 65,
      status: 'warning', // yellow: under-eating or missing dinner
      statusReasonZh: '晚餐未打卡，蛋白质缺口过大',
      statusReasonEn: 'No dinner recorded, protein goal at risk',
      lastLogTime: '12:40 PM',
      nudged: false,
      praised: false,
      coachNote: 'Refuel tonight, do not starve your deficit.'
    },
    {
      id: 'c5',
      name: 'Elena Rostova',
      avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120',
      goal: 'Body Recomp',
      goalZh: '体态重塑',
      goalEn: 'Body Recomp',
      targetCalories: 1650,
      currentCalories: 1610,
      proteinTarget: 120,
      proteinCurrent: 124,
      status: 'good', // green: perfect
      statusReasonZh: '热量精准控制在理想区间，膳食纤维充足',
      statusReasonEn: 'Spot on calorie target with great fiber balance',
      lastLogTime: '8:05 PM',
      nudged: false,
      praised: false,
      coachNote: ''
    },
    {
      id: 'c6',
      name: 'Michael Torres',
      avatar: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=120',
      goal: 'Cut',
      goalZh: '减脂控卡',
      goalEn: 'Fat Loss',
      targetCalories: 1700,
      currentCalories: 2150,
      proteinTarget: 130,
      proteinCurrent: 92,
      status: 'danger', // red
      statusReasonZh: '夜宵热量偏高，超出今日限额 450 kcal',
      statusReasonEn: 'Late night snack spiked calories by +450 kcal',
      lastLogTime: '9:15 PM',
      nudged: false,
      praised: false,
      coachNote: 'Keep healthy Greek yogurt on hand for cravings.'
    }
  ])

  // Computed metrics
  const activeCount = computed(() => coachProfile.value.totalClients)
  const capacityUsedPercent = computed(() => Math.round((coachProfile.value.totalClients / coachProfile.value.maxSeats) * 100))
  const dangerCount = computed(() => clients.value.filter(c => c.status === 'danger').length)
  const warningCount = computed(() => clients.value.filter(c => c.status === 'warning').length)
  const goodCount = computed(() => clients.value.filter(c => c.status === 'good').length)

  // Actions
  const nudgeClient = (clientId) => {
    const client = clients.value.find(c => c.id === clientId)
    if (client) {
      client.nudged = true
      return client.name
    }
    return null
  }

  const nudgeAllPending = () => {
    const pendingClients = clients.value.filter(c => c.status === 'warning' || c.status === 'danger')
    pendingClients.forEach(c => {
      c.nudged = true
    })
    return pendingClients.length
  }

  const praiseClient = (clientId) => {
    const client = clients.value.find(c => c.id === clientId)
    if (client) {
      client.praised = true
      return client.name
    }
    return null
  }

  const saveCoachNote = (clientId, note) => {
    const client = clients.value.find(c => c.id === clientId)
    if (client) {
      client.coachNote = note
    }
  }

  // Personal Coach Nudge & Review for current user (Client view)
  const myCoachNudge = ref({
    active: true,
    coachName: 'Coach Marcus',
    coachAvatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120',
    coachTitle: 'NASM-CPT 私教',
    timeZh: '刚刚',
    timeEn: 'Just now',
    titleZh: '晚餐催打卡提醒',
    titleEn: 'Evening Check-in Reminder',
    messageZh: 'Hi！你的减脂计划晚餐还剩约 550 kcal 预算，别忘了拍照记录今晚摄入，注意多补充优质蛋白质与高纤维绿叶蔬菜！',
    messageEn: 'Hi! You have ~550 kcal budget left for dinner. Remember to log your meal and prioritize lean protein & green fiber!',
    urgency: 'high',
    replied: false,
    actionUrl: '/scan'
  })

  const myCoachReview = ref({
    active: true,
    coachName: 'Coach Marcus',
    coachAvatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=120',
    coachTitle: 'NASM-CPT 私教',
    timeZh: '今天 12:45',
    timeEn: 'Today 12:45 PM',
    titleZh: '今日餐食教练点评',
    titleEn: 'Coach Meal Review',
    dishNameZh: '午餐 • 挪威三文鱼牛油果波奇饭',
    dishNameEn: 'Lunch • Salmon Avocado Poke Bowl',
    calories: 470,
    protein: 38,
    rating: 5,
    tagZh: '优秀高蛋白减脂示范',
    tagEn: 'Optimal High-Protein Choice',
    commentZh: '中午这顿搭配非常干净标准！三文鱼提供了极佳的优质蛋白质与 Omega-3 脂肪酸，饱腹感强且血糖波动平稳。晚餐继续保持少油清淡！',
    commentEn: 'Spot-on lunch choice! Salmon delivers clean protein & essential Omega-3 fats. Keep dinner clean and lean tonight!',
    voiceSeconds: 12,
    repliedText: ''
  })

  const incomingPushNotification = ref(null)

  const triggerSimulatedNudge = () => {
    myCoachNudge.value.active = true
    myCoachNudge.value.timeZh = '刚刚'
    myCoachNudge.value.timeEn = 'Just now'
    myCoachNudge.value.replied = false
    incomingPushNotification.value = {
      visible: true,
      type: 'nudge',
      titleZh: '私教 Marcus 催你记晚餐',
      titleEn: 'Coach Marcus Nudged Dinner',
      messageZh: 'Hi！你的减脂计划晚餐还剩约 550 kcal，别忘了记录今晚摄入，优先补充优质蛋白质！',
      messageEn: 'Hi! You have ~550 kcal left for dinner. Remember to snap your meal and hit your protein target!',
      time: '刚刚',
      actionTextZh: '立即拍照记餐',
      actionTextEn: 'Snap Meal Now',
      actionUrl: '/scan'
    }
  }

  const triggerSimulatedReview = (customNote) => {
    myCoachReview.value.active = true
    myCoachReview.value.timeZh = '刚刚'
    myCoachReview.value.timeEn = 'Just now'
    myCoachReview.value.repliedText = ''
    if (customNote) {
      myCoachReview.value.commentZh = customNote
      myCoachReview.value.commentEn = customNote
    }
    incomingPushNotification.value = {
      visible: true,
      type: 'review',
      titleZh: '私教 Marcus 点评了你的餐食',
      titleEn: 'Coach Marcus Reviewed Meal',
      messageZh: myCoachReview.value.commentZh,
      messageEn: myCoachReview.value.commentEn,
      time: '刚刚',
      actionTextZh: '查看点评详情',
      actionTextEn: 'View Feedback',
      actionUrl: '/team'
    }
  }

  const triggerSimulatedPraise = () => {
    incomingPushNotification.value = {
      visible: true,
      type: 'praise',
      titleZh: '私教 Marcus 为你今天的自律点赞！',
      titleEn: 'Coach Marcus Praised Your Consistency!',
      messageZh: '太棒了！本周热量预算执行非常精准，体脂率正稳步下降，继续保持！🔥',
      messageEn: 'Awesome work! Your deficit execution is spot on this week. Keep up the momentum! 🔥',
      time: '刚刚',
      actionTextZh: '去小队查看',
      actionTextEn: 'View Squad',
      actionUrl: '/team'
    }
  }

  const dismissPush = () => {
    if (incomingPushNotification.value) {
      incomingPushNotification.value.visible = false
    }
  }

  const dismissNudge = () => {
    if (myCoachNudge.value) {
      myCoachNudge.value.active = false
    }
  }

  const dismissReview = () => {
    if (myCoachReview.value) {
      myCoachReview.value.active = false
    }
  }

  const replyNudge = () => {
    if (myCoachNudge.value) {
      myCoachNudge.value.replied = true
      // Card automatically fades out / eliminates after acknowledgement
      setTimeout(() => {
        myCoachNudge.value.active = false
      }, 900)
    }
  }

  const replyReview = (text) => {
    if (myCoachReview.value) {
      myCoachReview.value.repliedText = text
      // Card automatically fades out / eliminates after reply
      setTimeout(() => {
        myCoachReview.value.active = false
      }, 1000)
    }
  }

  // Coach Perspective: Simulate receiving replies from students
  const triggerSimulatedStudentReply = (type = 'sarah_nudge') => {
    if (type === 'sarah_nudge') {
      const client = clients.value.find(c => c.id === 'c1')
      if (client) {
        client.nudged = false
        client.studentReply = {
          textZh: '教练好！晚餐已拍照打卡并上传，彩椒炒鸡胸配生菜，今日总热量已精准控制在 1530 kcal！',
          textEn: 'Hey Coach! Dinner snapped & logged. Clean chicken salad, total calories locked at 1530 kcal!',
          timeZh: '刚刚',
          timeEn: 'Just now',
          replied: true,
          reaction: ''
        }
        client.currentCalories = 1530
        client.proteinCurrent = 112
        client.status = 'good'
        client.statusReasonZh = '晚餐已补打卡回执，热量与高蛋白双达标'
        client.statusReasonEn = 'Dinner logged. Calorie budget & protein target met!'
        client.lastLogTime = '刚刚'

        incomingPushNotification.value = {
          visible: true,
          type: 'student_reply',
          senderName: client.name,
          senderAvatar: client.avatar,
          senderRole: '学员回执',
          titleZh: '学员 Sarah Miller 刚回执了催打卡',
          titleEn: 'Sarah Miller Acknowledged Nudge',
          messageZh: client.studentReply.textZh,
          messageEn: client.studentReply.textEn,
          time: '刚刚',
          actionTextZh: '去学员大盘查看',
          actionTextEn: 'View Client Roster',
          actionUrl: '/team'
        }
      }
    } else if (type === 'david_review') {
      const client = clients.value.find(c => c.id === 'c2')
      if (client) {
        client.studentReply = {
          textZh: '收到 Marcus 教练指导！晚餐已按建议加了鸡胸肉，今晚少油少盐，明天 6:30 晨跑闹钟已设好！',
          textEn: 'Got your guidance Coach! Hit protein target, lean dinner, and alarm set for 6:30 AM jog!',
          timeZh: '刚刚',
          timeEn: 'Just now',
          replied: true,
          reaction: ''
        }
        client.currentCalories = 1680
        client.proteinCurrent = 136
        client.status = 'good'
        client.statusReasonZh = '已按教练要求补充晚餐高蛋白，全天达标'
        client.statusReasonEn = 'Met dinner protein per coach note, on track'
        client.lastLogTime = '刚刚'

        incomingPushNotification.value = {
          visible: true,
          type: 'student_reply',
          senderName: client.name,
          senderAvatar: client.avatar,
          senderRole: '学员回复',
          titleZh: '学员 David Kim 回复了你的指导便签',
          titleEn: 'David Kim Replied to Your Note',
          messageZh: client.studentReply.textZh,
          messageEn: client.studentReply.textEn,
          time: '刚刚',
          actionTextZh: '去学员大盘查看',
          actionTextEn: 'View Client Roster',
          actionUrl: '/team'
        }
      }
    } else if (type === 'emma_praise') {
      const client = clients.value.find(c => c.id === 'c3')
      if (client) {
        client.studentReply = {
          textZh: '谢谢 Marcus 教练点赞表扬！本周体脂率稳步下降，我会继续严格执行营养配比！🔥',
          textEn: 'Thanks for the praise Coach! Calorie deficit is locked in this week! 🔥',
          timeZh: '刚刚',
          timeEn: 'Just now',
          replied: true,
          reaction: ''
        }
        incomingPushNotification.value = {
          visible: true,
          type: 'student_reply',
          senderName: client.name,
          senderAvatar: client.avatar,
          senderRole: '学员回音',
          titleZh: '学员 Emma Watson 回复了你的点赞',
          titleEn: 'Emma Watson Replied to Your Praise',
          messageZh: client.studentReply.textZh,
          messageEn: client.studentReply.textEn,
          time: '刚刚',
          actionTextZh: '去学员大盘查看',
          actionTextEn: 'View Client Roster',
          actionUrl: '/team'
        }
      }
    }
  }

  const reactToStudentReply = (clientId, reaction = '👍') => {
    const client = clients.value.find(c => c.id === clientId)
    if (client && client.studentReply) {
      client.studentReply.reaction = reaction
    }
  }

  const dismissStudentReply = (clientId) => {
    const client = clients.value.find(c => c.id === clientId)
    if (client) {
      client.studentReply = null
    }
  }

  return {
    activeTab,
    coachProfile,
    cohort,
    clients,
    activeCount,
    capacityUsedPercent,
    dangerCount,
    warningCount,
    goodCount,
    myCoachNudge,
    myCoachReview,
    incomingPushNotification,
    nudgeClient,
    nudgeAllPending,
    praiseClient,
    saveCoachNote,
    triggerSimulatedNudge,
    triggerSimulatedReview,
    triggerSimulatedPraise,
    triggerSimulatedStudentReply,
    reactToStudentReply,
    dismissStudentReply,
    dismissPush,
    dismissNudge,
    dismissReview,
    replyNudge,
    replyReview
  }
})