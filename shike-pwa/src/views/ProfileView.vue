<template>
  <div class="min-h-screen pb-28 px-4 pt-6 max-w-md mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-lg font-bold text-slate-900 leading-tight">{{ t('profile.title') }}</h1>
      <button
        v-if="!authStore.isVip"
        @click="authStore.openPaywall"
        class="px-3 py-1 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold shadow-sm"
      >
        {{ t('profile.upgradePro') }}
      </button>
      <div v-else class="text-right">
        <div class="flex items-center gap-1.5 justify-end">
          <span class="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold inline-block">
            {{ t('profile.proActive') }}
          </span>
          <a
            href="https://app.lemonsqueezy.com/my-orders"
            target="_blank"
            rel="noopener noreferrer"
            class="px-2 py-0.5 rounded-full bg-purple-50 text-purple-700 hover:bg-purple-100 text-[10px] font-bold inline-flex items-center gap-0.5 border border-purple-200/60 transition-colors"
          >
            <span>{{ isZh ? '管理订阅' : 'Manage Sub' }}</span>
            <ExternalLink class="w-2.5 h-2.5" />
          </a>
        </div>
        <div v-if="subscriptionDetails" class="text-[10px] text-emerald-600 mt-1 font-mono flex items-center justify-end gap-1">
          <span>{{ subscriptionDetails.startDate }} 至 {{ subscriptionDetails.expireDate }}</span>
          <span class="font-bold text-emerald-700">({{ isZh ? `剩${subscriptionDetails.daysRemaining}天` : `${subscriptionDetails.daysRemaining}d left` }})</span>
        </div>
        <div v-else-if="authStore.user?.vipExpireTime" class="text-[10px] text-emerald-600 mt-0.5 font-mono">
          至 {{ authStore.user.vipExpireTime.substring(0, 10) }}
        </div>
      </div>
    </div>

    <!-- Guest Mode Banner (Shown only when in Guest Mode) -->
    <div
      v-if="authStore.isGuest"
      class="glass-card rounded-3xl p-4 mb-5 border-emerald-300/80 bg-gradient-to-r from-emerald-500/10 via-emerald-50 to-white flex items-center justify-between shadow-sm"
    >
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shrink-0 shadow-md shadow-emerald-500/20">
          <UserPlus class="w-5 h-5" />
        </div>
        <div>
          <div class="text-xs font-bold text-slate-800">{{ t('auth.guestBannerTitle') }}</div>
          <div class="text-[11px] text-slate-500 mt-0.5 max-w-[180px] leading-tight">
            {{ t('auth.guestBannerDesc') }}
          </div>
        </div>
      </div>
      <router-link
        to="/auth"
        class="px-3 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold shrink-0 shadow-md shadow-emerald-500/20 active:scale-95 transition-all"
      >
        {{ t('auth.guestBannerBtn') }}
      </router-link>
    </div>

    <!-- User Info Card -->
    <div class="glass-card rounded-3xl p-5 mb-5 flex items-center gap-4">
      <div class="w-16 h-16 rounded-2xl bg-emerald-500 text-white flex items-center justify-center font-black text-2xl shadow-lg shadow-emerald-500/20">
        {{ (authStore.user?.nickname || 'A').charAt(0).toUpperCase() }}
      </div>
      <div>
        <div class="flex items-center gap-2">
          <h2 class="text-base font-bold text-slate-800">
            {{ authStore.user?.nickname || 'Guest User' }}
          </h2>
          <span
            class="text-[10px] font-bold px-1.5 py-0.5 rounded-full"
            :class="authStore.isGuest ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'"
          >
            {{ authStore.isGuest ? t('profile.guest') : t('profile.member') }}
          </span>
        </div>
        <p class="text-xs text-slate-400 mt-0.5">
          {{ authStore.user?.email || t('profile.goal') }}
        </p>
        <div class="flex items-center gap-2 mt-2 flex-wrap">
          <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[11px] font-semibold">
            <span>{{ t('profile.target', { n: dietStore.targetCalories }) }}</span>
          </div>
          <div class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200/80 text-[11px] font-bold">
            <Gem class="w-3 h-3 text-amber-500" />
            <span>{{ authStore.user?.points || 200 }} {{ t('team.gems') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- PRO Membership Details Card (Only shown when user is PRO) -->
    <div
      v-if="authStore.isVip && subscriptionDetails"
      class="glass-card rounded-3xl p-5 mb-5 border border-emerald-200/80 bg-gradient-to-br from-emerald-50/90 via-white to-teal-50/50 shadow-sm"
    >
      <div class="flex items-center justify-between pb-3 border-b border-emerald-100">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-white flex items-center justify-center shadow-md shadow-emerald-500/20">
            <Sparkles class="w-4 h-4" />
          </div>
          <div>
            <div class="text-xs font-bold text-slate-800 flex items-center gap-1.5">
              <span>{{ subscriptionDetails.planName }}</span>
              <span class="text-[10px] font-extrabold px-1.5 py-0.2 rounded-full bg-emerald-100 text-emerald-800">
                {{ isZh ? '生效中' : 'Active' }}
              </span>
            </div>
            <div class="text-[11px] text-slate-400 mt-0.5">
              {{ subscriptionDetails.planPrice }} · {{ isZh ? '自动续费开启' : 'Auto-renew active' }}
            </div>
          </div>
        </div>

        <a
          href="https://app.lemonsqueezy.com/my-orders"
          target="_blank"
          rel="noopener noreferrer"
          class="px-2.5 py-1.5 rounded-xl bg-white hover:bg-slate-50 active:scale-95 text-xs font-bold text-slate-700 flex items-center gap-1 border border-slate-200 shadow-2xs transition-all"
        >
          <span>{{ isZh ? '管理订阅' : 'Manage' }}</span>
          <ExternalLink class="w-3 h-3 text-slate-400" />
        </a>
      </div>

      <!-- 3-Column Dates & Status Grid -->
      <div class="grid grid-cols-3 gap-2 text-center text-xs mt-3">
        <div class="bg-white/80 p-2.5 rounded-2xl border border-emerald-100/60 shadow-2xs">
          <div class="text-[10px] text-slate-400 font-medium">{{ isZh ? '开通时间' : 'Start Date' }}</div>
          <div class="font-bold font-mono text-slate-800 mt-0.5">{{ subscriptionDetails.startDate }}</div>
        </div>
        <div class="bg-white/80 p-2.5 rounded-2xl border border-emerald-100/60 shadow-2xs">
          <div class="text-[10px] text-slate-400 font-medium">{{ isZh ? '下次扣费/到期' : 'Expires' }}</div>
          <div class="font-bold font-mono text-emerald-600 mt-0.5">{{ subscriptionDetails.expireDate }}</div>
        </div>
        <div class="bg-white/80 p-2.5 rounded-2xl border border-emerald-100/60 shadow-2xs">
          <div class="text-[10px] text-slate-400 font-medium">{{ isZh ? '剩余天数' : 'Remaining' }}</div>
          <div class="font-black text-amber-600 mt-0.5">{{ subscriptionDetails.daysRemaining }} {{ isZh ? '天' : 'days' }}</div>
        </div>
      </div>

      <!-- Helper Notice -->
      <div class="flex items-center justify-between mt-3 pt-2 text-[11px] text-slate-500">
        <div class="flex items-center gap-1.5">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>{{ isZh ? '已解锁全功能与无限 AI 测卡' : 'All PRO features & unlimited scans' }}</span>
        </div>
        <button @click="openLegalModal('refund')" class="text-emerald-600 hover:underline font-semibold">
          {{ isZh ? '7天退款保障' : '7d Refund' }}
        </button>
      </div>
    </div>

    <!-- Body Metrics & Target Card -->
    <div class="glass-card rounded-3xl p-5 mb-5 border border-emerald-100 bg-white shadow-sm">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <Activity class="w-4 h-4 text-emerald-500" />
          <h3 class="text-sm font-bold text-slate-800">
            {{ t('onboarding.updatePlanTitle') }}
          </h3>
        </div>
        <router-link
          to="/onboarding"
          class="text-xs font-bold text-emerald-600 hover:underline flex items-center gap-0.5"
        >
          <span>{{ t('profile.recalculate') }}</span>
          <ArrowRight class="w-3.5 h-3.5" />
        </router-link>
      </div>

      <div class="grid grid-cols-3 gap-2 text-center text-xs">
        <div class="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          <div class="text-[10px] text-slate-400 font-medium">{{ t('profile.dailyBudget') }}</div>
          <div class="font-black text-slate-900 mt-0.5">{{ dietStore.targetCalories }} kcal</div>
        </div>
        <div class="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          <div class="text-[10px] text-slate-400 font-medium">{{ t('profile.currentWeight') }}</div>
          <div class="font-bold text-slate-800 mt-0.5">
            {{ authStore.unitSystem === 'metric' ? (authStore.user?.weight || 76) + ' kg' : Math.round((authStore.user?.weight || 76) * 2.20462) + ' lbs' }}
          </div>
        </div>
        <div class="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
          <div class="text-[10px] text-slate-400 font-medium">{{ t('profile.goalWeight') }}</div>
          <div class="font-bold text-emerald-600 mt-0.5">
            {{ authStore.unitSystem === 'metric' ? (authStore.user?.customGoalWeight || 68) + ' kg' : Math.round((authStore.user?.customGoalWeight || 68) * 2.20462) + ' lbs' }}
          </div>
        </div>
      </div>
    </div>

    <!-- Preferences Settings Group -->
    <div class="space-y-4 mb-6">
      <h2 class="text-xs font-bold uppercase tracking-wider text-slate-400 px-1">{{ t('profile.preferences') }}</h2>

      <!-- Unit System Toggle -->
      <div class="glass-card rounded-2xl p-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-slate-600">
            <Scale class="w-4 h-4" />
          </div>
          <div>
            <div class="text-sm font-bold text-slate-800">{{ t('profile.units') }}</div>
            <div class="text-xs text-slate-400">{{ t('profile.unitsDesc') }}</div>
          </div>
        </div>
        <div class="flex bg-slate-100 p-1 rounded-xl text-xs font-bold">
          <button
            @click="authStore.setUnitSystem('metric')"
            class="px-2.5 py-1 rounded-lg transition-all"
            :class="authStore.unitSystem === 'metric' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'"
          >
            {{ t('profile.metric') }}
          </button>
          <button
            @click="authStore.setUnitSystem('imperial')"
            class="px-2.5 py-1 rounded-lg transition-all"
            :class="authStore.unitSystem === 'imperial' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'"
          >
            {{ t('profile.imperial') }}
          </button>
        </div>
      </div>

      <!-- Language Selector -->
      <div class="glass-card rounded-2xl p-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-slate-100 flex items-center justify-center text-slate-600">
            <Languages class="w-4 h-4" />
          </div>
          <div>
            <div class="text-sm font-bold text-slate-800">{{ t('profile.language') }}</div>
            <div class="text-xs text-slate-400">{{ t('profile.languageDesc') }}</div>
          </div>
        </div>
        <button
          @click="isLangModalOpen = true"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200/70 active:scale-95 transition-all text-xs font-bold text-slate-800"
        >
          <span class="text-sm leading-none">{{ currentLangMeta?.flag }}</span>
          <span>{{ currentLangMeta?.nativeName }}</span>
          <ChevronDown class="w-3.5 h-3.5 text-slate-400 ml-0.5" />
        </button>
      </div>
    </div>

    <!-- Compact Horizontal Legal & Support Bar -->
    <div class="glass-card rounded-2xl p-2.5 mb-5 flex items-center justify-around text-slate-700 shadow-xs">
      <button
        @click="openLegalModal('refund')"
        class="flex-1 py-1.5 px-0.5 flex flex-col items-center gap-1 rounded-xl hover:bg-slate-100/70 active:scale-95 transition-all group"
      >
        <div class="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-105 transition-transform">
          <RotateCcw class="w-4 h-4" />
        </div>
        <span class="text-[11px] font-bold text-slate-700 tracking-tight">{{ t('legal.refundShort') }}</span>
      </button>

      <div class="w-px h-6 bg-slate-200/60 shrink-0"></div>

      <button
        @click="openLegalModal('terms')"
        class="flex-1 py-1.5 px-0.5 flex flex-col items-center gap-1 rounded-xl hover:bg-slate-100/70 active:scale-95 transition-all group"
      >
        <div class="w-8 h-8 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center group-hover:scale-105 transition-transform">
          <FileText class="w-4 h-4" />
        </div>
        <span class="text-[11px] font-bold text-slate-700 tracking-tight">{{ t('legal.termsShort') }}</span>
      </button>

      <div class="w-px h-6 bg-slate-200/60 shrink-0"></div>

      <button
        @click="openLegalModal('privacy')"
        class="flex-1 py-1.5 px-0.5 flex flex-col items-center gap-1 rounded-xl hover:bg-slate-100/70 active:scale-95 transition-all group"
      >
        <div class="w-8 h-8 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center group-hover:scale-105 transition-transform">
          <ShieldCheck class="w-4 h-4" />
        </div>
        <span class="text-[11px] font-bold text-slate-700 tracking-tight">{{ t('legal.privacyShort') }}</span>
      </button>

      <div class="w-px h-6 bg-slate-200/60 shrink-0"></div>

      <button
        @click="openLegalModal('contact')"
        class="flex-1 py-1.5 px-0.5 flex flex-col items-center gap-1 rounded-xl hover:bg-slate-100/70 active:scale-95 transition-all group"
      >
        <div class="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:scale-105 transition-transform">
          <Mail class="w-4 h-4" />
        </div>
        <span class="text-[11px] font-bold text-slate-700 tracking-tight">{{ t('legal.contactShort') }}</span>
      </button>
    </div>

    <!-- Health & Legal Disclaimer -->
    <div class="p-4 rounded-2xl bg-amber-50/70 border border-amber-200/50 mb-6 text-xs text-amber-900 space-y-1">
      <div class="font-bold flex items-center gap-1">
        <ShieldAlert class="w-3.5 h-3.5 text-amber-600" />
        <span>{{ t('profile.disclaimerTitle') }}</span>
      </div>
      <p class="text-[11px] text-amber-800/90 leading-relaxed">
        {{ t('profile.disclaimerDesc') }}
      </p>
    </div>

    <!-- Account Management Group -->
    <div class="space-y-3">
      <!-- Sign In Button (if guest) -->
      <router-link
        v-if="authStore.isGuest"
        to="/auth"
        class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs transition-colors flex items-center justify-center gap-1.5 shadow-sm"
      >
        <LogIn class="w-3.5 h-3.5" />
        <span>{{ t('auth.signIn') }} / {{ t('auth.signUp') }}</span>
      </router-link>

      <!-- Sign Out Button (if logged in) -->
      <button
        v-else
        @click="handleSignOut"
        class="w-full py-3 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
      >
        <LogOut class="w-3.5 h-3.5" />
        <span>{{ t('auth.signOut') }}</span>
      </button>

      <!-- Delete Account (GDPR Compliance) -->
      <button
        @click="confirmDeleteAccount"
        class="w-full py-3 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
      >
        <Trash2 class="w-3.5 h-3.5" />
        <span>{{ t('profile.deleteAccount') }}</span>
      </button>

      <div class="text-center text-[10px] text-slate-400">
        {{ t('profile.version') }}
      </div>
    </div>

    <!-- Language Selection Modal / Drawer -->
    <div
      v-if="isLangModalOpen"
      class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
    >
      <div
        class="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
        @click="isLangModalOpen = false"
      ></div>
      <div
        class="relative w-full max-w-sm bg-white rounded-t-3xl sm:rounded-3xl p-5 shadow-2xl z-10 space-y-3 max-h-[85vh] overflow-y-auto"
      >
        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
          <div class="flex items-center gap-2">
            <Languages class="w-4 h-4 text-emerald-600" />
            <h3 class="text-sm font-bold text-slate-900">{{ t('profile.languageModalTitle') }}</h3>
          </div>
          <button
            @click="isLangModalOpen = false"
            class="p-1 rounded-full text-slate-400 hover:text-slate-600 transition-colors"
          >
            <X class="w-4 h-4" />
          </button>
        </div>
        <div class="space-y-1.5 pt-1">
          <button
            v-for="item in SUPPORTED_LANGUAGES"
            :key="item.code"
            @click="selectLanguage(item.code)"
            class="w-full flex items-center justify-between p-3 rounded-2xl transition-all text-left"
            :class="authStore.lang === item.code ? 'bg-emerald-50 border border-emerald-300 text-emerald-950' : 'hover:bg-slate-50 border border-transparent text-slate-700'"
          >
            <div class="flex items-center gap-3">
              <span class="text-xl leading-none">{{ item.flag }}</span>
              <div>
                <div class="text-xs font-bold">{{ item.nativeName }}</div>
                <div class="text-[10px] text-slate-400">{{ item.name }}</div>
              </div>
            </div>
            <Check v-if="authStore.lang === item.code" class="w-4 h-4 text-emerald-600 shrink-0" />
          </button>
        </div>
      </div>
    </div>

    <!-- Legal & Refund Policy Modal -->
    <LegalModal
      :isOpen="isLegalModalOpen"
      :initialTab="legalModalTab"
      @close="isLegalModalOpen = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import confetti from 'canvas-confetti'
import {
  Scale,
  Languages,
  ShieldAlert,
  Trash2,
  UserPlus,
  LogIn,
  LogOut,
  Activity,
  ArrowRight,
  ChevronDown,
  Check,
  X,
  Gem,
  RotateCcw,
  CreditCard,
  FileText,
  ShieldCheck,
  ChevronRight,
  ExternalLink,
  Mail,
  Sparkles
} from 'lucide-vue-next'
import LegalModal from '../components/LegalModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useDietStore } from '../stores/dietStore'
import { useI18n, SUPPORTED_LANGUAGES } from '../i18n'

const route = useRoute()
const authStore = useAuthStore()
const dietStore = useDietStore()
const { t } = useI18n()

onMounted(async () => {
  await authStore.refreshProfile()
  if (route.query.payment === 'success') {
    confetti({
      particleCount: 100,
      spread: 70,
      origin: { y: 0.6 }
    })
  }
})

const isLangModalOpen = ref(false)
const currentLangMeta = computed(
  () => SUPPORTED_LANGUAGES.find((l) => l.code === authStore.lang) || SUPPORTED_LANGUAGES[0]
)
const isZh = computed(() => authStore.lang === 'zh')

const subscriptionDetails = computed(() => {
  if (!authStore.isVip || !authStore.user?.vipExpireTime) return null

  const expireRaw = authStore.user.vipExpireTime
  const expireDateStr = expireRaw.substring(0, 10)
  const expireDate = new Date(expireRaw)
  const now = new Date()

  // Calculate remaining days
  const diffMs = expireDate.getTime() - now.getTime()
  const daysRemaining = Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)))

  // Identify plan type
  const rawPlan = authStore.user.vipPlanType || ''
  const isWeekly = rawPlan.toUpperCase() === 'WEEKLY' || daysRemaining <= 14

  const planName = isWeekly
    ? (isZh.value ? 'ShiKe Pro 周度会员计划' : 'ShiKe Pro Weekly Pass')
    : (isZh.value ? 'ShiKe Pro 年度会员计划' : 'ShiKe Pro Annual Pass')

  const planPrice = isWeekly ? '$4.99 / 周' : '$39.99 / 年'

  // Compute start date (from vipStartTime or subtract period)
  let startDateStr = ''
  if (authStore.user.vipStartTime) {
    startDateStr = authStore.user.vipStartTime.substring(0, 10)
  } else {
    const startDate = new Date(expireDate)
    if (isWeekly) {
      startDate.setDate(startDate.getDate() - 7)
    } else {
      startDate.setFullYear(startDate.getFullYear() - 1)
    }
    startDateStr = startDate.toISOString().substring(0, 10)
  }

  return {
    planName,
    planPrice,
    startDate: startDateStr,
    expireDate: expireDateStr,
    daysRemaining,
    isWeekly
  }
})

const isLegalModalOpen = ref(false)
const legalModalTab = ref('refund')

const openLegalModal = (tab = 'refund') => {
  legalModalTab.value = tab
  isLegalModalOpen.value = true
}

const selectLanguage = (code) => {
  authStore.setLang(code)
  isLangModalOpen.value = false
}

const handleSignOut = () => {
  if (confirm(t('auth.signOutConfirm'))) {
    authStore.logout()
  }
}

const confirmDeleteAccount = () => {
  if (confirm(t('profile.deleteConfirm'))) {
    localStorage.clear()
    alert(t('profile.deleteSuccess'))
    window.location.reload()
  }
}
</script>
