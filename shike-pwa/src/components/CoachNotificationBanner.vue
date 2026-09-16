<template>
  <transition
    enter-active-class="transform ease-out duration-400 transition"
    enter-from-class="-translate-y-16 opacity-0 scale-95"
    enter-to-class="translate-y-0 opacity-100 scale-100"
    leave-active-class="transition ease-in duration-200"
    leave-from-class="translate-y-0 opacity-100 scale-100"
    leave-to-class="-translate-y-16 opacity-0 scale-95"
  >
    <div
      v-if="notification && notification.visible"
      class="fixed top-3 inset-x-3 max-w-sm mx-auto z-50 p-3.5 rounded-2xl bg-slate-900/95 text-white backdrop-blur-xl border border-indigo-500/40 shadow-2xl shadow-indigo-950/40 flex items-start gap-3 select-none animate-pulse-border"
      @click="handleBannerClick"
    >
      <!-- Avatar with status badge -->
      <div class="relative shrink-0 mt-0.5">
        <img
          :src="notification.senderAvatar || coachStore.coachProfile.avatar"
          class="w-10 h-10 rounded-full object-cover border-2 border-indigo-400 shadow-md"
        />
        <div
          class="absolute -bottom-1 -right-1 w-4 h-4 rounded-full flex items-center justify-center text-white text-[9px] shadow-sm"
          :class="{
            'bg-emerald-500': notification.type === 'student_reply' || notification.type === 'praise',
            'bg-amber-500': notification.type === 'nudge',
            'bg-indigo-600': notification.type === 'review'
          }"
        >
          <CheckCheck v-if="notification.type === 'student_reply'" class="w-2.5 h-2.5 fill-current" />
          <Bell v-else-if="notification.type === 'nudge'" class="w-2.5 h-2.5 fill-current" />
          <ThumbsUp v-else-if="notification.type === 'praise'" class="w-2.5 h-2.5 fill-current" />
          <MessageSquare v-else class="w-2.5 h-2.5 fill-current" />
        </div>
      </div>

      <!-- Notification Content -->
      <div class="flex-1 min-w-0">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5">
            <span class="text-xs font-black text-indigo-300">{{ notification.senderName || coachStore.coachProfile.name }}</span>
            <span
              class="text-[9px] border px-1 py-0.2 rounded font-bold uppercase tracking-wider"
              :class="notification.senderRole ? 'bg-emerald-500/30 text-emerald-200 border-emerald-400/30' : 'bg-indigo-500/30 text-indigo-200 border-indigo-400/30'"
            >
              {{ notification.senderRole || 'Coach' }}
            </span>
          </div>
          <span class="text-[10px] text-slate-400 font-mono">{{ notification.time }}</span>
        </div>

        <div class="text-xs font-bold text-white mt-0.5 flex items-center gap-1">
          <span v-if="notification.type === 'student_reply'" class="text-emerald-400">📬</span>
          <span v-else-if="notification.type === 'nudge'" class="text-amber-400">🔔</span>
          <span v-else-if="notification.type === 'praise'" class="text-emerald-400">🌟</span>
          <span v-else class="text-indigo-400">💬</span>
          <span>{{ authStore.lang === 'zh' ? notification.titleZh : notification.titleEn }}</span>
        </div>

        <p class="text-[11px] text-slate-300 mt-1 leading-snug line-clamp-2">
          {{ authStore.lang === 'zh' ? notification.messageZh : notification.messageEn }}
        </p>

        <!-- Quick Action Buttons -->
        <div class="mt-2.5 flex items-center gap-2">
          <button
            v-if="notification.actionUrl"
            @click.stop="handleAction"
            class="px-3 py-1 rounded-lg bg-emerald-500 hover:bg-emerald-600 text-white text-[11px] font-bold shadow-xs active:scale-95 transition flex items-center gap-1"
          >
            <span>{{ authStore.lang === 'zh' ? notification.actionTextZh : notification.actionTextEn }}</span>
            <ChevronRight class="w-3 h-3" />
          </button>
          <button
            @click.stop="coachStore.dismissPush()"
            class="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-slate-300 text-[11px] font-medium transition"
          >
            {{ authStore.lang === 'zh' ? '关闭' : 'Dismiss' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, MessageSquare, ThumbsUp, ChevronRight, CheckCheck } from 'lucide-vue-next'
import { useCoachStore } from '../stores/coachStore'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const coachStore = useCoachStore()
const authStore = useAuthStore()

const notification = computed(() => coachStore.incomingPushNotification)

// Play subtle soft notification chime using Web Audio API
const playChime = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(587.33, ctx.currentTime) // D5
    osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15) // A5
    gain.gain.setValueAtTime(0.08, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.4)
  } catch (e) {}
}

// Trigger OS-level system notification (Lock screen / Notification Center)
const triggerSystemNotification = (notif) => {
  if (!('Notification' in window) || Notification.permission !== 'granted' || !notif) return
  const title = (authStore.lang === 'zh' ? notif.titleZh : notif.titleEn) || '食刻 ShiKe 督导提醒'
  const body = (authStore.lang === 'zh' ? notif.messageZh : notif.messageEn) || ''
  try {
    if (navigator.serviceWorker && navigator.serviceWorker.ready) {
      navigator.serviceWorker.ready.then(reg => {
        reg.showNotification(title, {
          body,
          icon: notif.senderAvatar || '/pwa-192x192.png',
          badge: '/favicon.ico',
          vibrate: [200, 100, 200],
          tag: 'shike-push-' + Date.now()
        })
      }).catch(() => {
        new Notification(title, { body, icon: notif.senderAvatar || '/pwa-192x192.png' })
      })
    } else {
      new Notification(title, { body, icon: notif.senderAvatar || '/pwa-192x192.png' })
    }
  } catch (e) {
    console.warn('System Notification error:', e)
  }
}

let autoDismissTimer = null

watch(() => notification.value?.visible, (val) => {
  if (val) {
    playChime()
    triggerSystemNotification(notification.value)
    if (autoDismissTimer) clearTimeout(autoDismissTimer)
    autoDismissTimer = setTimeout(() => {
      coachStore.dismissPush()
    }, 7000)
  }
})

const handleBannerClick = () => {
  handleAction()
}

const handleAction = () => {
  if (notification.value?.actionUrl) {
    router.push(notification.value.actionUrl)
  }
  coachStore.dismissPush()
}
</script>
