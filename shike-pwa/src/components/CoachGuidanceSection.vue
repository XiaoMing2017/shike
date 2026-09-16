<template>
  <div class="space-y-3.5 mb-5">
    <!-- 1. Coach Check-In Nudge Card (教练催打卡提醒) -->
    <Transition name="card-collapse">
      <div
        v-if="coachStore.myCoachNudge && coachStore.myCoachNudge.active"
        class="glass-card rounded-3xl p-4 border-2 border-amber-400/40 bg-gradient-to-br from-amber-50/70 via-white to-amber-50/30 shadow-md relative overflow-hidden"
      >
        <div class="flex items-start justify-between gap-3 mb-2.5">
          <div class="flex items-center gap-3 min-w-0">
            <div class="relative shrink-0">
              <img
                :src="coachStore.myCoachNudge.coachAvatar"
                class="w-11 h-11 rounded-full object-cover border-2 border-amber-300 shadow-sm"
              />
              <div class="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-amber-500 text-white flex items-center justify-center text-[9px] shadow-xs">
                <Bell class="w-2.5 h-2.5 fill-current" />
              </div>
            </div>
            <div class="min-w-0">
              <div class="flex items-center gap-1.5 flex-wrap">
                <h3 class="text-sm font-black text-slate-900">{{ coachStore.myCoachNudge.coachName }}</h3>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">
                  {{ authStore.lang === 'zh' ? '私教催打卡' : 'Coach Nudge' }}
                </span>
              </div>
              <div class="text-[11px] text-slate-400 mt-0.5">
                {{ authStore.lang === 'zh' ? coachStore.myCoachNudge.timeZh : coachStore.myCoachNudge.timeEn }} • {{ coachStore.myCoachNudge.coachTitle }}
              </div>
            </div>
          </div>

          <div class="flex items-center gap-1.5 shrink-0">
            <!-- Action tag -->
            <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500 text-white shadow-xs animate-pulse">
              {{ authStore.lang === 'zh' ? '待完成' : 'Pending' }}
            </span>
            <!-- Close / Dismiss button -->
            <button
              @click.stop="coachStore.dismissNudge()"
              class="p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-amber-100/60 transition active:scale-90"
              :title="authStore.lang === 'zh' ? '消除此卡片' : 'Dismiss card'"
            >
              <X class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        <!-- Message Bubble -->
        <div class="p-3 bg-white/90 rounded-2xl border border-amber-100/80 text-xs text-slate-700 leading-relaxed shadow-xs mb-3">
          <p class="font-medium">
            {{ authStore.lang === 'zh' ? coachStore.myCoachNudge.messageZh : coachStore.myCoachNudge.messageEn }}
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-2">
          <router-link
            to="/scan"
            class="flex-1 py-2 px-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 active:scale-95 text-white text-xs font-bold shadow-sm shadow-emerald-500/30 flex items-center justify-center gap-1.5 transition"
          >
            <Camera class="w-3.5 h-3.5" />
            <span>{{ authStore.lang === 'zh' ? '立即拍照记餐并回传教练' : 'Snap Meal & Send to Coach' }}</span>
          </router-link>

          <button
            v-if="!coachStore.myCoachNudge.replied"
            @click="handleAcknowledgeNudge"
            class="px-3 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition active:scale-95 shrink-0"
          >
            {{ authStore.lang === 'zh' ? '收到' : 'Got it' }}
          </button>
          <div v-else class="text-[11px] font-bold text-emerald-600 px-2 flex items-center gap-1 animate-pulse">
            <Check class="w-3.5 h-3.5" />
            <span>{{ authStore.lang === 'zh' ? '已回执 (消除中...)' : 'Replied (closing...)' }}</span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 2. Coach Meal Review Card (最新餐食教练专业点评) -->
    <Transition name="card-collapse">
      <div
        v-if="coachStore.myCoachReview && coachStore.myCoachReview.active"
        class="glass-card rounded-3xl p-4 border-2 border-indigo-400/30 bg-gradient-to-br from-indigo-50/60 via-white to-purple-50/20 shadow-md relative overflow-hidden"
      >
        <!-- Header -->
        <div class="flex items-start justify-between gap-3 mb-2.5">
          <div class="flex items-center gap-3 min-w-0">
            <div class="relative shrink-0">
              <img
                :src="coachStore.myCoachReview.coachAvatar"
                class="w-11 h-11 rounded-full object-cover border-2 border-indigo-300 shadow-sm"
              />
              <div class="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-indigo-600 text-white flex items-center justify-center text-[9px] shadow-xs">
                <MessageSquare class="w-2.5 h-2.5 fill-current" />
              </div>
            </div>
            <div class="min-w-0">
              <div class="flex items-center gap-1.5 flex-wrap">
                <h3 class="text-sm font-black text-slate-900">{{ coachStore.myCoachReview.coachName }}</h3>
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800 border border-indigo-200">
                  {{ authStore.lang === 'zh' ? '餐食指导' : 'Coach Review' }}
                </span>
              </div>
              <div class="text-[11px] text-slate-400 mt-0.5">
                {{ authStore.lang === 'zh' ? coachStore.myCoachReview.timeZh : coachStore.myCoachReview.timeEn }} • {{ coachStore.myCoachReview.coachTitle }}
              </div>
            </div>
          </div>

          <div class="flex items-center gap-1.5 shrink-0">
            <!-- Rating Stars -->
            <div class="flex items-center gap-0.5 bg-amber-50 px-2 py-1 rounded-full border border-amber-200/60">
              <span class="text-[10px] font-black text-amber-700 mr-0.5">5.0</span>
              <Star v-for="n in 5" :key="n" class="w-3 h-3 fill-amber-400 text-amber-400" />
            </div>
            <!-- Close / Dismiss button -->
            <button
              @click.stop="coachStore.dismissReview()"
              class="p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-indigo-100/60 transition active:scale-90 ml-0.5"
              :title="authStore.lang === 'zh' ? '消除此卡片' : 'Dismiss card'"
            >
              <X class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        <!-- Target Meal Badge -->
        <div class="flex items-center justify-between bg-slate-100/80 px-3 py-1.5 rounded-xl text-xs text-slate-700 mb-2.5">
          <span class="font-bold flex items-center gap-1 truncate">
            <Utensils class="w-3.5 h-3.5 text-indigo-500 shrink-0" />
            <span class="truncate">{{ authStore.lang === 'zh' ? coachStore.myCoachReview.dishNameZh : coachStore.myCoachReview.dishNameEn }}</span>
          </span>
          <span class="text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 shrink-0 ml-2">
            {{ authStore.lang === 'zh' ? coachStore.myCoachReview.tagZh : coachStore.myCoachReview.tagEn }}
          </span>
        </div>

        <!-- Coach Review Comment Content -->
        <div class="p-3 bg-white/90 rounded-2xl border border-indigo-100/80 text-xs text-slate-700 leading-relaxed shadow-xs mb-3">
          <p class="font-medium text-slate-800">
            {{ authStore.lang === 'zh' ? coachStore.myCoachReview.commentZh : coachStore.myCoachReview.commentEn }}
          </p>

          <!-- Simulated Voice Note Bar -->
          <div
            @click="toggleVoiceNote"
            class="mt-2.5 pt-2 border-t border-slate-100 flex items-center justify-between cursor-pointer group select-none"
          >
            <div class="flex items-center gap-2 text-indigo-600 font-bold text-[11px]">
              <div
                class="w-6 h-6 rounded-full bg-indigo-500 text-white flex items-center justify-center transition group-hover:scale-105"
              >
                <Volume2 v-if="isPlayingVoice" class="w-3 h-3 animate-pulse" />
                <Play v-else class="w-3 h-3 ml-0.5 fill-current" />
              </div>
              <span>{{ isPlayingVoice ? (authStore.lang === 'zh' ? '正在播放私教语音指导...' : 'Playing voice note...') : (authStore.lang === 'zh' ? '点击收听私教 12" 语音' : 'Listen to 12" Coach Voice') }}</span>
            </div>
            <!-- Sound Wave Animation -->
            <div class="flex items-center gap-0.5">
              <span
                v-for="i in 8"
                :key="i"
                class="w-1 bg-indigo-400 rounded-full transition-all duration-300"
                :class="isPlayingVoice ? 'animate-bounce' : 'h-2 bg-slate-300'"
                :style="isPlayingVoice ? { height: (i % 3 === 0 ? 16 : i % 2 === 0 ? 12 : 8) + 'px', animationDelay: (i * 80) + 'ms' } : {}"
              ></span>
            </div>
          </div>
        </div>

        <!-- Quick Reply Section -->
        <div class="pt-1">
          <div class="text-[10px] text-slate-400 font-bold mb-1.5 uppercase tracking-wider">
            {{ authStore.lang === 'zh' ? '快捷回执教练' : 'Quick Reply to Coach' }}:
          </div>
          <div class="flex items-center gap-1.5 flex-wrap">
            <button
              v-for="reply in quickReplies"
              :key="reply"
              @click="sendReply(reply)"
              class="px-2.5 py-1 rounded-lg text-xs font-semibold transition active:scale-95 border"
              :class="selectedReply === reply ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs' : 'bg-slate-50 hover:bg-indigo-50 text-slate-600 hover:text-indigo-700 border-slate-200/80'"
            >
              {{ reply }}
            </button>
          </div>

          <div v-if="selectedReply" class="mt-2 text-[11px] text-emerald-600 font-bold flex items-center gap-1 animate-fade-in">
            <Check class="w-3.5 h-3.5" />
            <span>{{ authStore.lang === 'zh' ? '教练已收到您的回复 (卡片消除中...)' : 'Coach received reply (closing...)' }}</span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Resolved Indicator when both cards are dismissed/replied -->
    <Transition name="card-collapse">
      <div
        v-if="!coachStore.myCoachNudge?.active && !coachStore.myCoachReview?.active"
        class="p-3.5 rounded-2xl bg-gradient-to-r from-emerald-50/90 to-teal-50/70 border border-emerald-200/80 flex items-center justify-between text-xs text-emerald-900 shadow-xs animate-fade-in"
      >
        <div class="flex items-center gap-2.5 min-w-0">
          <div class="w-7 h-7 rounded-full bg-emerald-500 text-white flex items-center justify-center shrink-0 shadow-xs">
            <CheckCheck class="w-4 h-4" />
          </div>
          <div class="min-w-0">
            <div class="font-bold text-xs truncate">
              {{ authStore.lang === 'zh' ? '今日教练督导待办已全部回复消除 🎉' : 'All coach guidance items resolved 🎉' }}
            </div>
            <p class="text-[11px] text-emerald-700/90 mt-0.5 truncate">
              {{ authStore.lang === 'zh' ? '已向 Coach Marcus 同步打卡与餐食回执，继续保持！' : 'Replies synced with Coach Marcus. Keep up the rhythm!' }}
            </p>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 3. Interactive Simulation Dock (快捷测试触发栏) -->
    <div class="p-3 bg-slate-900/90 text-white rounded-2xl border border-slate-700 shadow-md">
      <div class="flex items-center justify-between text-xs mb-2">
        <span class="font-bold flex items-center gap-1 text-indigo-300">
          <Sparkles class="w-3.5 h-3.5 text-amber-400" />
          <span>{{ authStore.lang === 'zh' ? '⚡ 实时模拟教练推流互动' : '⚡ Simulate Live Coach Actions' }}</span>
        </span>
        <span class="text-[10px] text-slate-400 font-mono">Demo Mode</span>
      </div>

      <div class="grid grid-cols-3 gap-2">
        <button
          @click="coachStore.triggerSimulatedNudge()"
          class="py-1.5 px-2 rounded-xl bg-amber-500 hover:bg-amber-600 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1"
        >
          <Bell class="w-3 h-3" />
          <span>{{ authStore.lang === 'zh' ? '催我打卡' : 'Nudge Me' }}</span>
        </button>

        <button
          @click="coachStore.triggerSimulatedReview()"
          class="py-1.5 px-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1"
        >
          <MessageSquare class="w-3 h-3" />
          <span>{{ authStore.lang === 'zh' ? '给我点评' : 'Review Me' }}</span>
        </button>

        <button
          @click="coachStore.triggerSimulatedPraise()"
          class="py-1.5 px-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white text-[11px] font-bold shadow-xs transition flex items-center justify-center gap-1"
        >
          <ThumbsUp class="w-3 h-3" />
          <span>{{ authStore.lang === 'zh' ? '点赞表扬' : 'Praise Me' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Bell,
  MessageSquare,
  Camera,
  Star,
  Utensils,
  Check,
  CheckCheck,
  X,
  Play,
  Volume2,
  Sparkles,
  ThumbsUp
} from 'lucide-vue-next'
import { useCoachStore } from '../stores/coachStore'
import { useAuthStore } from '../stores/authStore'

const coachStore = useCoachStore()
const authStore = useAuthStore()

const isPlayingVoice = ref(false)
const selectedReply = ref(coachStore.myCoachReview?.repliedText || '')
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

watch(
  () => coachStore.myCoachReview?.repliedText,
  (val) => {
    selectedReply.value = val || ''
  }
)

const quickReplies = computed(() => {
  if (authStore.lang === 'zh') {
    return [
      '收到！今晚一定少酱少油',
      '谢谢教练点评，已准备鸡胸肉',
      '饱腹感超强，继续加油！'
    ]
  }
  return [
    'Got it! Keeping dinner lean & clean',
    'Thanks Coach! Hitting protein goal',
    'Feeling great, staying on track!'
  ]
})

const handleAcknowledgeNudge = () => {
  coachStore.replyNudge()
}

const toggleVoiceNote = () => {
  isPlayingVoice.value = !isPlayingVoice.value
  if (isPlayingVoice.value) {
    // Simulated voice playback sound
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)()
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(320, ctx.currentTime)
      osc.frequency.linearRampToValueAtTime(440, ctx.currentTime + 0.3)
      gain.gain.setValueAtTime(0.05, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.2)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start()
      osc.stop(ctx.currentTime + 1.2)
    } catch (e) {}
    setTimeout(() => {
      isPlayingVoice.value = false
    }, 4000)
  }
}

const sendReply = (text) => {
  selectedReply.value = text
  coachStore.replyReview(text)
}
</script>

<style scoped>
.card-collapse-enter-active,
.card-collapse-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  max-height: 500px;
}
.card-collapse-enter-from,
.card-collapse-leave-to {
  opacity: 0;
  transform: scale(0.97) translateY(-6px);
  max-height: 0;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}
</style>
