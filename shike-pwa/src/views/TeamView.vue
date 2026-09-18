<template>
  <div class="min-h-screen pb-28 px-4 pt-6 max-w-md mx-auto">
    <!-- Toast Notification -->
    <transition
      enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="-translate-y-4 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="toastMessage"
        class="fixed top-5 left-1/2 -translate-x-1/2 z-50 bg-slate-900/95 text-white text-xs font-semibold px-4 py-2.5 rounded-full shadow-xl flex items-center gap-2 border border-slate-700/60 backdrop-blur-md"
      >
        <Sparkles class="w-3.5 h-3.5 text-emerald-400" />
        <span>{{ toastMessage }}</span>
      </div>
    </transition>

    <!-- Header -->
    <div class="flex items-start justify-between gap-3 mb-4">
      <div class="min-w-0 flex-1">
        <h1 class="text-lg font-bold text-slate-900 leading-tight">
          {{ activeTab === 'coach' ? t('team.coachTitle') : t('team.title') }}
        </h1>
        <p class="text-xs text-slate-500 mt-0.5 leading-normal">
          {{ activeTab === 'coach' ? t('team.coachSubtitle') : t('team.subtitle') }}
        </p>
      </div>

      <div class="flex items-center gap-1.5 shrink-0 pt-0.5">
        <!-- User Personal Gems Pill -->
        <button
          @click="showGemInfoToast"
          class="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-50 hover:bg-amber-100 border border-amber-200/80 text-amber-900 text-xs font-bold shadow-2xs active:scale-95 transition-all shrink-0 whitespace-nowrap"
          :title="t('team.myBalance')"
        >
          <Gem class="w-3.5 h-3.5 text-amber-500 shrink-0" />
          <span>{{ authStore.user?.points || 200 }}</span>
        </button>

        <div
          v-if="activeTab === 'squad'"
          class="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-bold border border-emerald-200/60 shrink-0 whitespace-nowrap"
        >
          {{ t('team.dayOf', { current: 3, total: 7 }) }}
        </div>
        <div
          v-else
          class="px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-600 text-xs font-bold border border-indigo-200/60 flex items-center gap-1 shrink-0 whitespace-nowrap"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse shrink-0"></span>
          <span class="whitespace-nowrap">{{ coachStore.cohort.week }} / {{ coachStore.cohort.totalWeeks }} wk</span>
        </div>
      </div>
    </div>

    <!-- Segmented Tab Switcher: Buddy Squad vs Coach Hub -->
    <div class="bg-slate-200/70 p-1 rounded-2xl grid grid-cols-2 gap-1 mb-5">
      <button
        @click="activeTab = 'squad'"
        class="h-10 px-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 min-w-0"
        :class="activeTab === 'squad' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'"
      >
        <Users class="w-3.5 h-3.5 shrink-0" />
        <span class="truncate">{{ t('team.tabPeer') }}</span>
      </button>

      <button
        @click="activeTab = 'coach'"
        class="h-10 px-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 min-w-0"
        :class="activeTab === 'coach' ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30' : 'text-slate-500 hover:text-slate-800'"
      >
        <ShieldCheck class="w-3.5 h-3.5 shrink-0" />
        <span class="truncate">{{ t('team.tabCoach') }}</span>
        <span
          class="text-[9px] px-1.5 py-0.5 rounded-full font-black uppercase shrink-0 leading-none"
          :class="activeTab === 'coach' ? 'bg-white/20 text-white' : 'bg-indigo-100 text-indigo-700'"
        >PRO</span>
      </button>
    </div>

    <!-- TAB 1: PEER BUDDY SQUAD (5P) -->
    <div v-if="activeTab === 'squad'">
      <!-- Squad Pledge Pool Card -->
      <div class="glass-card rounded-3xl p-5 mb-5 relative overflow-hidden bg-gradient-to-br from-emerald-500/10 via-white to-emerald-500/5">
        <div class="flex items-center justify-between mb-2">
          <div>
            <span class="text-[11px] font-bold uppercase tracking-wider text-slate-400">{{ t('team.poolTitle') }}</span>
            <div class="text-3xl font-black text-slate-900 mt-0.5 flex items-center gap-1.5">
              <span>750</span>
              <span class="text-sm font-bold text-emerald-600">{{ t('team.gems') }}</span>
            </div>
          </div>
          <div class="w-12 h-12 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/30">
            <Trophy class="w-6 h-6" />
          </div>
        </div>

        <!-- Metric Mini-Pills: My Pledged vs My Balance -->
        <div class="grid grid-cols-2 gap-2 my-3.5">
          <div class="p-3 rounded-2xl bg-white/90 border border-emerald-100 shadow-2xs flex flex-col justify-between">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('team.myPledged') }}</span>
            <div class="flex items-center justify-between mt-1">
              <div class="text-base font-black text-emerald-700 flex items-center gap-1">
                <span>150</span>
                <span class="text-[10px] font-semibold text-emerald-600">{{ t('team.gems') }}</span>
              </div>
              <span class="text-[9px] font-extrabold px-1.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700 border border-emerald-200/60">
                ✓ {{ t('team.pledgedStatus') }}
              </span>
            </div>
          </div>
          <div class="p-3 rounded-2xl bg-white/90 border border-amber-100 shadow-2xs flex flex-col justify-between">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('team.myBalance') }}</span>
            <div class="flex items-center justify-between mt-1">
              <div class="text-base font-black text-amber-600 flex items-center gap-1">
                <span>{{ authStore.user?.points || 200 }}</span>
                <span class="text-[10px] font-semibold text-amber-600">{{ t('team.gems') }}</span>
              </div>
              <span class="text-xs">💎</span>
            </div>
          </div>
        </div>

        <p class="text-xs text-slate-500 leading-relaxed">
          {{ t('team.poolDesc') }}
        </p>

        <!-- Squad Code -->
        <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
          <span class="text-slate-400">{{ t('team.inviteCode') }}: <strong class="text-slate-700 font-mono text-sm">A8X2B</strong></span>
          <button @click="copyCode('A8X2B')" class="text-emerald-600 font-bold hover:underline">
            {{ copiedCode === 'A8X2B' ? t('team.copied') : t('team.copyCode') }}
          </button>
        </div>
      </div>

      <!-- Coach Guidance & Daily Interaction (Nudge & Review for current user) -->
      <CoachGuidanceSection />

      <!-- Teammates Leaderboard -->
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-base font-bold text-slate-800">{{ t('team.membersTitle') }} (4/5)</h2>
        <span class="text-xs text-slate-400">{{ t('team.openSlot') }}</span>
      </div>

      <div class="space-y-3">
        <div
          v-for="member in squadMembers"
          :key="member.id"
          class="glass-card rounded-2xl p-3.5 flex items-center justify-between"
        >
          <div class="flex items-center gap-3">
            <div class="relative">
              <img :src="member.avatar" class="w-11 h-11 rounded-full object-cover bg-slate-100" />
              <div
                v-if="member.checkedInToday"
                class="absolute -bottom-0.5 -right-0.5 w-4 h-4 rounded-full bg-emerald-500 border-2 border-white flex items-center justify-center text-white"
              >
                <Check class="w-2.5 h-2.5 stroke-[3]" />
              </div>
            </div>
            <div>
              <div class="flex items-center gap-1.5">
                <h3 class="text-sm font-bold text-slate-800">{{ member.name }}</h3>
                <span v-if="member.isYou" class="text-[10px] font-bold bg-slate-100 text-slate-500 px-1.5 py-0.2 rounded">{{ t('team.you') }}</span>
              </div>
              <div class="text-xs text-slate-400 mt-0.5">
                {{ member.checkedInToday ? (authStore.lang === 'zh' ? '今日已打卡 (1,420 千卡)' : 'Logged today (1,420 kcal)') : (authStore.lang === 'zh' ? '待记录晚餐' : 'Pending dinner log') }}
              </div>
            </div>
          </div>

          <div class="text-right">
            <div class="text-xs font-bold" :class="member.checkedInToday ? 'text-emerald-600' : 'text-amber-500'">
              {{ member.checkedInToday ? t('team.checkedIn') : t('team.inProgress') }}
            </div>
            <div class="text-[11px] text-slate-400">{{ t('team.streak', { n: member.streak }) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- TAB 2: COACH SUPERVISION HUB (PORTAL) -->
    <div v-else>
      <!-- Coach Overview Card -->
      <div class="rounded-3xl p-5 mb-4 bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 text-white shadow-xl shadow-indigo-950/25 border border-indigo-500/20">
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <img :src="coachStore.coachProfile.avatar" class="w-12 h-12 rounded-2xl object-cover border-2 border-indigo-400/40" />
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-base font-black text-white">{{ coachStore.coachProfile.name }}</h2>
                <span class="text-[10px] font-black uppercase tracking-wider bg-indigo-500/30 text-indigo-300 border border-indigo-400/30 px-2 py-0.5 rounded-full">
                  {{ coachStore.coachProfile.tier }}
                </span>
              </div>
              <p class="text-xs text-indigo-200/70 mt-0.5">{{ coachStore.coachProfile.title }}</p>
            </div>
          </div>
        </div>

        <!-- Cohort & Seat Progress -->
        <div class="bg-white/10 rounded-2xl p-3.5 backdrop-blur-sm border border-white/10">
          <div class="flex items-center justify-between text-xs mb-2">
            <span class="font-bold text-slate-100">{{ coachStore.cohort.name }}</span>
            <span class="text-emerald-400 font-bold">
              {{ t('team.cohortCapacity', { used: coachStore.coachProfile.totalClients, max: coachStore.coachProfile.maxSeats }) }}
            </span>
          </div>
          <!-- Progress bar -->
          <div class="w-full h-2.5 bg-black/30 rounded-full overflow-hidden p-0.5">
            <div
              class="h-full bg-gradient-to-r from-indigo-400 via-emerald-400 to-teal-300 rounded-full transition-all duration-500"
              :style="{ width: coachStore.capacityUsedPercent + '%' }"
            ></div>
          </div>
          <div class="mt-2.5 flex items-center justify-between text-[11px] text-slate-300">
            <span>{{ authStore.lang === 'zh' ? '学员专属邀请码' : 'Client Invite Code' }}: <strong class="text-white font-mono tracking-wider ml-1">{{ coachStore.coachProfile.inviteCode }}</strong></span>
            <button @click="copyCode(coachStore.coachProfile.inviteCode)" class="text-indigo-300 hover:text-white font-bold transition flex items-center gap-1">
              <span>{{ copiedCode === coachStore.coachProfile.inviteCode ? t('team.copied') : t('team.copyCode') }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Quick KPI Status Ribbon -->
      <div class="grid grid-cols-3 gap-2.5 mb-4">
        <!-- On Track (Green) -->
        <div class="glass-card rounded-2xl p-3 border-emerald-100 bg-emerald-50/50 text-center">
          <div class="text-xl font-black text-emerald-600 leading-tight">{{ coachStore.goodCount }}</div>
          <div class="text-[11px] font-bold text-emerald-700 mt-0.5 flex items-center justify-center gap-1">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>{{ t('team.statusGood') }}</span>
          </div>
        </div>

        <!-- Missing Log (Yellow) -->
        <div class="glass-card rounded-2xl p-3 border-amber-100 bg-amber-50/50 text-center">
          <div class="text-xl font-black text-amber-600 leading-tight">{{ coachStore.warningCount }}</div>
          <div class="text-[11px] font-bold text-amber-700 mt-0.5 flex items-center justify-center gap-1">
            <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
            <span>{{ t('team.statusWarning') }}</span>
          </div>
        </div>

        <!-- Over Budget (Red) -->
        <div class="glass-card rounded-2xl p-3 border-rose-100 bg-rose-50/50 text-center">
          <div class="text-xl font-black text-rose-600 leading-tight">{{ coachStore.dangerCount }}</div>
          <div class="text-[11px] font-bold text-rose-700 mt-0.5 flex items-center justify-center gap-1">
            <span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
            <span>{{ t('team.statusDanger') }}</span>
          </div>
        </div>
      </div>

      <!-- Coach Perspective: Simulate Student Replies Dock (模拟学员回执互动) -->
      <div class="mb-4 p-3.5 bg-slate-900/95 text-white rounded-2xl border border-indigo-500/30 shadow-md">
        <div class="flex items-center justify-between text-xs mb-2">
          <span class="font-bold flex items-center gap-1.5 text-indigo-300">
            <Sparkles class="w-3.5 h-3.5 text-amber-400" />
            <span>{{ authStore.lang === 'zh' ? '⚡ 模拟学员回复教练互动' : '⚡ Simulate Student Replies to Coach' }}</span>
          </span>
          <span class="text-[10px] text-slate-400 font-mono">Coach Perspective</span>
        </div>

        <div class="grid grid-cols-3 gap-2">
          <button
            @click="coachStore.triggerSimulatedStudentReply('sarah_nudge')"
            class="py-1.5 px-2 rounded-xl bg-amber-500 hover:bg-amber-600 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1 text-center"
          >
            <CheckCheck class="w-3 h-3 shrink-0" />
            <span class="truncate">{{ authStore.lang === 'zh' ? 'Sarah 回执打卡' : 'Sarah Nudge Ack' }}</span>
          </button>

          <button
            @click="coachStore.triggerSimulatedStudentReply('david_review')"
            class="py-1.5 px-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1 text-center"
          >
            <MessageSquare class="w-3 h-3 shrink-0" />
            <span class="truncate">{{ authStore.lang === 'zh' ? 'David 回复点评' : 'David Reply Note' }}</span>
          </button>

          <button
            @click="coachStore.triggerSimulatedStudentReply('emma_praise')"
            class="py-1.5 px-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1 text-center"
          >
            <ThumbsUp class="w-3 h-3 shrink-0" />
            <span class="truncate">{{ authStore.lang === 'zh' ? 'Emma 感谢点赞' : 'Emma Thanks' }}</span>
          </button>
        </div>
      </div>

      <!-- Bulk Action Toolbar -->
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-sm font-bold text-slate-800">
          {{ authStore.lang === 'zh' ? '学员实时摄入大盘' : 'Live Client Nutrition Roster' }} ({{ coachStore.clients.length }})
        </h3>
        <button
          @click="handleNudgeAll"
          class="px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm shadow-amber-500/30 active:scale-95 transition"
        >
          <Bell class="w-3.5 h-3.5" />
          <span>{{ t('team.nudgeAll', { n: coachStore.warningCount + coachStore.dangerCount }) }}</span>
        </button>
      </div>

      <!-- Client Supervision Cards List -->
      <div class="space-y-3.5 mb-6">
        <div
          v-for="client in coachStore.clients"
          :key="client.id"
          class="glass-card rounded-3xl p-4 transition-all"
          :class="{
            'border-rose-200 bg-rose-50/20': client.status === 'danger',
            'border-amber-200 bg-amber-50/20': client.status === 'warning',
            'border-emerald-200 bg-emerald-50/20': client.status === 'good'
          }"
        >
          <!-- Client Header -->
          <div class="flex items-start justify-between gap-2.5 mb-3">
            <div class="flex items-center gap-3 min-w-0 flex-1">
              <div class="relative shrink-0">
                <img :src="client.avatar" class="w-11 h-11 rounded-full object-cover bg-slate-100 shrink-0" />
                <span
                  class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full border-2 border-white"
                  :class="{
                    'bg-rose-500': client.status === 'danger',
                    'bg-amber-500': client.status === 'warning',
                    'bg-emerald-500': client.status === 'good'
                  }"
                ></span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-1.5 flex-wrap">
                  <h4 class="text-sm font-bold text-slate-900 truncate max-w-[130px] sm:max-w-none">{{ client.name }}</h4>
                  <span class="text-[10px] bg-slate-100 text-slate-600 font-semibold px-1.5 py-0.5 rounded-md shrink-0 whitespace-nowrap">
                    {{ getClientGoal(client) }}
                  </span>
                </div>
                <div class="text-[11px] text-slate-400 mt-0.5 truncate">
                  {{ authStore.lang === 'zh' ? '最近记餐' : 'Last logged' }}: {{ client.lastLogTime }}
                </div>
              </div>
            </div>

            <!-- Red/Yellow/Green Badge -->
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0 whitespace-nowrap shadow-xs self-start"
              :class="{
                'bg-rose-100 text-rose-700 border-rose-200': client.status === 'danger',
                'bg-amber-100 text-amber-700 border-amber-200': client.status === 'warning',
                'bg-emerald-100 text-emerald-700 border-emerald-200': client.status === 'good'
              }"
            >
              {{ client.status === 'danger' ? t('team.statusDanger') : (client.status === 'warning' ? t('team.statusWarning') : t('team.statusGood')) }}
            </span>
          </div>

          <!-- Status Reason Tagline -->
          <div
            class="text-xs font-semibold px-2.5 py-1.5 rounded-xl mb-3 flex items-center gap-1.5"
            :class="{
              'bg-rose-100/70 text-rose-800': client.status === 'danger',
              'bg-amber-100/70 text-amber-800': client.status === 'warning',
              'bg-emerald-100/70 text-emerald-800': client.status === 'good'
            }"
          >
            <AlertTriangle v-if="client.status === 'danger'" class="w-3.5 h-3.5 text-rose-600 flex-shrink-0" />
            <Bell v-else-if="client.status === 'warning'" class="w-3.5 h-3.5 text-amber-600 flex-shrink-0" />
            <Check v-else class="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
            <span class="truncate">{{ authStore.lang === 'zh' ? client.statusReasonZh : client.statusReasonEn }}</span>
          </div>

          <!-- Student Reply Bubble (学员最新回执与互动) -->
          <div
            v-if="client.studentReply"
            class="mb-3 p-3 bg-gradient-to-r from-emerald-50 via-teal-50/40 to-white rounded-2xl border-2 border-emerald-400/50 shadow-xs animate-fade-in"
          >
            <div class="flex items-center justify-between text-xs mb-1.5">
              <div class="flex items-center gap-1.5 text-emerald-800 font-black">
                <MessageSquare class="w-3.5 h-3.5 text-emerald-600 fill-emerald-100" />
                <span>{{ authStore.lang === 'zh' ? '学员最新回执' : 'Student Reply' }}</span>
                <span class="text-[10px] text-slate-400 font-normal">({{ client.studentReply.timeZh }})</span>
              </div>
              <button
                @click.stop="coachStore.dismissStudentReply(client.id)"
                class="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition active:scale-90"
                :title="authStore.lang === 'zh' ? '消除回执' : 'Dismiss'"
              >
                <X class="w-3 h-3" />
              </button>
            </div>

            <p class="text-xs text-slate-800 font-medium leading-relaxed pl-5">
              “{{ authStore.lang === 'zh' ? client.studentReply.textZh : client.studentReply.textEn }}”
            </p>

            <!-- Coach Quick Reaction Bar -->
            <div class="mt-2.5 pt-2 border-t border-emerald-100/80 flex items-center justify-between pl-5">
              <span class="text-[10px] font-bold text-slate-400">
                {{ authStore.lang === 'zh' ? '教练快捷互动:' : 'Coach Quick Reaction:' }}
              </span>
              <div class="flex items-center gap-1.5">
                <button
                  @click="coachStore.reactToStudentReply(client.id, '👍')"
                  class="px-2 py-0.5 rounded-lg text-[11px] font-bold transition active:scale-90 border flex items-center gap-0.5"
                  :class="client.studentReply.reaction === '👍' ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs' : 'bg-white hover:bg-emerald-50 text-slate-700 border-slate-200'"
                >
                  <span>👍 赞</span>
                </button>
                <button
                  @click="coachStore.reactToStudentReply(client.id, '🔥')"
                  class="px-2 py-0.5 rounded-lg text-[11px] font-bold transition active:scale-90 border flex items-center gap-0.5"
                  :class="client.studentReply.reaction === '🔥' ? 'bg-amber-600 text-white border-amber-600 shadow-xs' : 'bg-white hover:bg-amber-50 text-slate-700 border-slate-200'"
                >
                  <span>🔥 稳住</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Calorie & Protein Compliance Bars -->
          <div class="space-y-2 mb-3 bg-white/70 p-2.5 rounded-2xl border border-slate-100">
            <!-- Calorie Bar -->
            <div>
              <div class="flex items-center justify-between text-xs mb-1">
                <span class="text-slate-500 font-medium">{{ authStore.lang === 'zh' ? '热量进度' : 'Calorie Intake' }}</span>
                <span class="font-mono font-bold" :class="client.currentCalories > client.targetCalories ? 'text-rose-600' : 'text-slate-700'">
                  {{ client.currentCalories }} / {{ client.targetCalories }} kcal
                </span>
              </div>
              <div class="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all"
                  :class="client.currentCalories > client.targetCalories ? 'bg-rose-500' : 'bg-emerald-500'"
                  :style="{ width: Math.min(100, Math.round((client.currentCalories / client.targetCalories) * 100)) + '%' }"
                ></div>
              </div>
            </div>

            <!-- Protein Bar -->
            <div>
              <div class="flex items-center justify-between text-xs mb-1">
                <span class="text-slate-500 font-medium">{{ authStore.lang === 'zh' ? '蛋白质达标' : 'Protein Goal' }}</span>
                <span class="font-mono font-bold text-indigo-600">
                  {{ client.proteinCurrent }}g / {{ client.proteinTarget }}g
                </span>
              </div>
              <div class="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-indigo-500 rounded-full transition-all"
                  :style="{ width: Math.min(100, Math.round((client.proteinCurrent / client.proteinTarget) * 100)) + '%' }"
                ></div>
              </div>
            </div>
          </div>

          <!-- Coach Action Buttons: Praise, Nudge, Note -->
          <div class="flex items-center gap-2 pt-1 border-t border-slate-100/80">
            <!-- Praise button -->
            <button
              @click="handlePraise(client)"
              class="flex-1 py-1.5 px-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition"
              :class="client.praised ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 hover:bg-emerald-50 hover:text-emerald-700 text-slate-600'"
            >
              <ThumbsUp class="w-3.5 h-3.5" :class="client.praised ? 'fill-current' : ''" />
              <span>{{ client.praised ? (authStore.lang === 'zh' ? '已点赞' : 'Praised') : t('team.actionPraise') }}</span>
            </button>

            <!-- Nudge button -->
            <button
              @click="handleNudge(client)"
              class="flex-1 py-1.5 px-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition"
              :class="client.nudged ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 hover:bg-amber-50 hover:text-amber-700 text-slate-600'"
            >
              <Bell class="w-3.5 h-3.5" :class="client.nudged ? 'fill-current' : ''" />
              <span>{{ client.nudged ? (authStore.lang === 'zh' ? '已催促' : 'Nudged') : t('team.actionNudge') }}</span>
            </button>

            <!-- Note button -->
            <button
              @click="toggleNoteEditor(client.id)"
              class="flex-1 py-1.5 px-2 rounded-xl text-xs font-bold flex items-center justify-center gap-1 bg-slate-100 hover:bg-indigo-50 hover:text-indigo-700 text-slate-600 transition"
              :class="activeNoteId === client.id ? 'bg-indigo-100 text-indigo-700' : ''"
            >
              <MessageSquare class="w-3.5 h-3.5" />
              <span>{{ t('team.actionNote') }}</span>
            </button>
          </div>

          <!-- Expandable Coach Note Editor -->
          <div v-if="activeNoteId === client.id" class="mt-3 pt-2.5 border-t border-slate-100">
            <textarea
              v-model="client.coachNote"
              rows="2"
              class="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 bg-white"
              :placeholder="t('team.coachNotePlaceholder')"
            ></textarea>
            <div class="mt-1.5 flex justify-end">
              <button
                @click="saveNote(client)"
                class="px-3 py-1 rounded-lg bg-indigo-600 text-white text-xs font-bold hover:bg-indigo-700 transition"
              >
                {{ t('team.saveNote') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Coach Tier Capacity Upgrade Card -->
      <div class="rounded-3xl p-4 bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 border border-indigo-500/30 text-white flex items-center justify-between shadow-lg">
        <div>
          <div class="text-xs font-bold text-indigo-300 uppercase tracking-wide">
            {{ t('team.coachPlanBadge') }}
          </div>
          <div class="text-xs text-slate-300 mt-0.5">
            {{ t('team.expandToStudio') }}
          </div>
        </div>
        <button
          @click="showUpgradeToast"
          class="px-3 py-1.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 text-slate-950 text-xs font-black hover:opacity-90 transition active:scale-95 shadow-md shadow-emerald-500/20"
        >
          $129.99/mo
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  Trophy,
  Check,
  CheckCheck,
  X,
  Bell,
  ThumbsUp,
  MessageSquare,
  AlertTriangle,
  ShieldCheck,
  ChevronRight,
  Users,
  Sparkles,
  Gem
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/authStore'
import { useCoachStore } from '../stores/coachStore'
import { useI18n } from '../i18n'
import CoachGuidanceSection from '../components/CoachGuidanceSection.vue'

const authStore = useAuthStore()
const coachStore = useCoachStore()
const { t } = useI18n()

const showGemInfoToast = () => {
  showToast(t('team.gemInfoToast'))
}

const activeTab = ref(coachStore.activeTab)
const copiedCode = ref(null)
const toastMessage = ref('')
const activeNoteId = ref(null)
const pushPermissionStatus = ref(typeof window !== 'undefined' && 'Notification' in window ? Notification.permission : 'unsupported')

const requestPushPermission = async () => {
  if (typeof window !== 'undefined' && 'Notification' in window) {
    try {
      const res = await Notification.requestPermission()
      pushPermissionStatus.value = res
    } catch (e) {
      console.warn('Push permission error:', e)
    }
  }
}

const getClientGoal = (client) => {
  if (!client) return ''
  const isZh = authStore.lang === 'zh'
  if (isZh && client.goalZh) return client.goalZh
  if (!isZh && client.goalEn) return client.goalEn
  const g = client.goal || ''
  if (isZh) {
    if (g.includes('Fat Loss') || g.includes('Cut')) return '减脂控卡'
    if (g.includes('Lean Muscle') || g.includes('Muscle')) return '增肌塑形'
    if (g.includes('Recomp')) return '体态重塑'
    return '减脂塑形'
  }
  return g.includes('Fat Loss') ? 'Fat Loss' : (g.includes('Lean Muscle') ? 'Lean Muscle' : (g.includes('Recomp') ? 'Body Recomp' : g))
}

const squadMembers = ref([
  { id: 1, name: 'Alex Rivera', isYou: true, avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120', checkedInToday: true, streak: 3 },
  { id: 2, name: 'Emma Watson', isYou: false, avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=120', checkedInToday: true, streak: 3 },
  { id: 3, name: 'David Kim', isYou: false, avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120', checkedInToday: false, streak: 2 },
  { id: 4, name: 'Sarah Miller', isYou: false, avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120', checkedInToday: true, streak: 3 }
])

const showToast = (msg) => {
  toastMessage.value = msg
  setTimeout(() => {
    toastMessage.value = ''
  }, 2500)
}

const copyCode = (code) => {
  navigator.clipboard?.writeText(code)
  copiedCode.value = code
  showToast(authStore.lang === 'zh' ? `邀请码 ${code} 已复制` : `Invite code ${code} copied`)
  setTimeout(() => {
    copiedCode.value = null
  }, 2000)
}

const handleNudge = (client) => {
  coachStore.nudgeClient(client.id)
  showToast(t('team.nudgedToast', { name: client.name }))
}

const handlePraise = (client) => {
  coachStore.praiseClient(client.id)
  showToast(t('team.praisedToast', { name: client.name }))
}

const handleNudgeAll = () => {
  const count = coachStore.nudgeAllPending()
  showToast(t('team.allNudged'))
}

const toggleNoteEditor = (clientId) => {
  activeNoteId.value = activeNoteId.value === clientId ? null : clientId
}

const saveNote = (client) => {
  coachStore.saveCoachNote(client.id, client.coachNote)
  activeNoteId.value = null
  showToast(t('team.noteSaved'))
}

const showUpgradeToast = () => {
  showToast(authStore.lang === 'zh' ? '正在连接 Lemon Squeezy 机构席位收银台...' : 'Redirecting to Lemon Squeezy Studio Checkout...')
}
</script>