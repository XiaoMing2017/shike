<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-slate-900/60 backdrop-blur-sm transition-opacity"
  >
    <div
      class="w-full max-w-md bg-white rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border border-slate-100 max-h-[92vh] overflow-y-auto no-scrollbar animate-slide-up"
    >
      <!-- Close button -->
      <div class="flex justify-between items-center mb-4">
        <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-emerald-50 text-emerald-600 text-xs font-bold tracking-wide">
          {{ t('paywall.badge') }}
        </span>
        <button
          @click="close"
          class="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 flex items-center justify-center text-slate-500 transition-colors"
        >
          <X class="w-4 h-4" />
        </button>
      </div>

      <!-- STEP 1: Subscription Plans View -->
      <div v-if="step === 'plans'">
        <!-- Hero Header -->
        <div class="text-center mb-6">
          <h2 class="text-2xl font-black text-slate-900 leading-tight">
            {{ t('paywall.title') }}
          </h2>
          <p class="text-sm text-slate-500 mt-2">
            {{ t('paywall.desc') }}
          </p>
        </div>

        <!-- Feature Bullets -->
        <div class="space-y-3 mb-6 bg-slate-50 p-4 rounded-2xl border border-slate-100 text-sm">
          <div class="flex items-center gap-3">
            <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
              <Check class="w-3.5 h-3.5 stroke-[3]" />
            </div>
            <span class="text-slate-700 font-medium">{{ t('paywall.feature1') }}</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
              <Check class="w-3.5 h-3.5 stroke-[3]" />
            </div>
            <span class="text-slate-700 font-medium">{{ t('paywall.feature2') }}</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
              <Check class="w-3.5 h-3.5 stroke-[3]" />
            </div>
            <span class="text-slate-700 font-medium">{{ t('paywall.feature3') }}</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
              <Check class="w-3.5 h-3.5 stroke-[3]" />
            </div>
            <span class="text-slate-700 font-medium">{{ t('paywall.feature4') }}</span>
          </div>
        </div>

        <!-- Plan Selection -->
        <div class="space-y-3 mb-6">
          <!-- Annual Plan (Best Value) -->
          <div
            @click="selectedPlan = 'yearly'"
            class="relative p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between"
            :class="selectedPlan === 'yearly' ? 'border-emerald-500 bg-emerald-50/40' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="absolute -top-3 right-4 px-2.5 py-0.5 rounded-full bg-emerald-500 text-white text-[10px] font-bold uppercase tracking-wider shadow-sm">
              {{ t('paywall.saveBadge') }}
            </div>
            <div class="flex items-center gap-3">
              <div
                class="w-5 h-5 rounded-full border-2 flex items-center justify-center"
                :class="selectedPlan === 'yearly' ? 'border-emerald-500' : 'border-slate-300'"
              >
                <div v-if="selectedPlan === 'yearly'" class="w-2.5 h-2.5 rounded-full bg-emerald-500"></div>
              </div>
              <div>
                <div class="font-bold text-slate-800">{{ t('paywall.annualTitle') }}</div>
                <div class="text-xs text-slate-500">{{ t('paywall.annualSub') }}</div>
              </div>
            </div>
            <div class="text-right font-black text-slate-900">$39.99</div>
          </div>

          <!-- Weekly Plan -->
          <div
            @click="selectedPlan = 'weekly'"
            class="p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between"
            :class="selectedPlan === 'weekly' ? 'border-emerald-500 bg-emerald-50/40' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-5 h-5 rounded-full border-2 flex items-center justify-center"
                :class="selectedPlan === 'weekly' ? 'border-emerald-500' : 'border-slate-300'"
              >
                <div v-if="selectedPlan === 'weekly'" class="w-2.5 h-2.5 rounded-full bg-emerald-500"></div>
              </div>
              <div>
                <div class="font-bold text-slate-800">{{ t('paywall.weeklyTitle') }}</div>
                <div class="text-xs text-slate-500">{{ t('paywall.weeklySub') }}</div>
              </div>
            </div>
            <div class="text-right font-black text-slate-900">$4.99 <span class="text-xs font-normal text-slate-500">/wk</span></div>
          </div>
        </div>

        <!-- Checkout Button -->
        <button
          @click="handleSubscribe"
          :disabled="isProcessing"
          class="w-full py-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-base shadow-lg shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all"
        >
          <span v-if="isProcessing">{{ t('paywall.processing') }}</span>
          <span v-else>
            {{ selectedPlan === 'weekly' ? t('paywall.trialBtn') : t('paywall.unlockBtn') }}
          </span>
        </button>

        <!-- Terms & Restore -->
        <div class="mt-4 text-center space-y-1">
          <p class="text-[11px] text-slate-400">
            {{ t('paywall.autoRenews') }}
          </p>
          <div class="flex justify-center items-center gap-2 text-[11px] text-slate-500 flex-wrap">
            <button @click="handleRestore" class="hover:underline">{{ t('paywall.restore') }}</button>
            <span>•</span>
            <button @click="openLegal('refund')" class="hover:underline text-emerald-600 font-semibold">{{ authStore.lang === 'zh' ? '14天退款' : 'Refund Policy' }}</button>
            <span>•</span>
            <button @click="openLegal('terms')" class="hover:underline">{{ t('paywall.terms') }}</button>
            <span>•</span>
            <button @click="openLegal('privacy')" class="hover:underline">{{ t('paywall.privacy') }}</button>
          </div>
        </div>
      </div>

      <!-- STEP 2: Guest Intercept & Account Linking View -->
      <div v-else-if="step === 'bind_account'" class="space-y-6">
        <!-- Shield Icon & Header -->
        <div class="text-center pt-2">
          <div class="w-16 h-16 rounded-3xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-white flex items-center justify-center mx-auto shadow-xl shadow-emerald-500/25 mb-4">
            <ShieldCheck class="w-8 h-8" />
          </div>
          <h2 class="text-xl font-black text-slate-900 leading-tight">
            {{ t('paywall.guestBindTitle') }}
          </h2>
          <p class="text-xs text-slate-500 mt-2 max-w-xs mx-auto leading-relaxed">
            {{ t('paywall.guestBindDesc') }}
          </p>
        </div>

        <!-- Selected Plan Pill Summary -->
        <div class="p-3 bg-emerald-50/80 rounded-2xl border border-emerald-200/80 flex items-center justify-between text-xs">
          <div class="flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span class="font-bold text-slate-800">
              {{ selectedPlan === 'weekly' ? t('paywall.weeklyTitle') : t('paywall.annualTitle') }}
            </span>
          </div>
          <span class="font-extrabold text-emerald-700">
            {{ selectedPlan === 'weekly' ? '$4.99/wk' : '$39.99/yr' }}
          </span>
        </div>

        <!-- Action 1: Google One-Tap Login Button -->
        <div class="space-y-3">
          <button
            @click="showGoogleModal = true"
            class="w-full py-3.5 px-4 rounded-2xl border-2 border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50 text-slate-800 font-bold text-sm shadow-xs flex items-center justify-center gap-3 transition-all active:scale-[0.99]"
          >
            <!-- Google G Logo SVG -->
            <svg class="w-5 h-5 shrink-0" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            <span>{{ t('paywall.continueWithGoogle') }}</span>
          </button>

          <!-- Action 2: Email Sign In / Register Button -->
          <button
            @click="goToEmailAuth"
            class="w-full py-3.5 px-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-sm shadow-md shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all active:scale-[0.99]"
          >
            <Mail class="w-4 h-4" />
            <span>{{ t('paywall.continueWithEmail') }}</span>
          </button>
        </div>

        <p class="text-[11px] text-center text-slate-400 leading-snug">
          {{ t('paywall.guestNotice') }}
        </p>

        <!-- Back to Plans Link -->
        <div class="text-center pt-1">
          <button
            @click="step = 'plans'"
            class="text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors inline-flex items-center gap-1"
          >
            <ArrowLeft class="w-3.5 h-3.5" />
            <span>{{ t('paywall.backToPlans') }}</span>
          </button>
        </div>
      </div>

      <!-- STEP 3: Sandbox / Test Mode Checkout Modal -->
      <div v-else-if="step === 'test_checkout'" class="space-y-5 text-center pt-2">
        <div class="w-14 h-14 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center mx-auto shadow-sm">
          <Sparkles class="w-7 h-7" />
        </div>
        <div>
          <span class="px-2.5 py-0.5 rounded-full text-[11px] font-extrabold bg-amber-100 text-amber-800 uppercase tracking-wider">
            Lemon Squeezy 沙箱收银台
          </span>
          <h3 class="text-xl font-black text-slate-900 mt-2">
            订单已创建：{{ selectedPlan === 'yearly' ? '$39.99 (年卡)' : '$4.99 (周卡)' }}
          </h3>
          <p class="text-xs text-slate-500 mt-1 font-mono break-all">
            {{ currentOrderNo }}
          </p>
        </div>

        <div class="p-3.5 bg-slate-50 rounded-2xl border border-slate-200 text-left space-y-2 text-xs text-slate-600">
          <div class="flex justify-between">
            <span>套餐类型:</span>
            <span class="font-bold text-slate-800">{{ selectedPlan === 'yearly' ? 'Annual Pro (年卡)' : 'Weekly Pro (周卡)' }}</span>
          </div>
          <div class="flex justify-between">
            <span>支付托管通道:</span>
            <span class="font-bold text-slate-800">Lemon Squeezy MoR</span>
          </div>
          <div class="flex justify-between">
            <span>会员权益:</span>
            <span class="font-bold text-emerald-600">无限次 AI 拍照 + 深度健康分析</span>
          </div>
        </div>

        <div class="space-y-2.5">
          <!-- One-click simulate payment success button -->
          <button
            @click="handleSimulateTestSuccess"
            :disabled="isProcessing"
            class="w-full py-3.5 rounded-2xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-sm shadow-md shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all"
          >
            <Sparkles class="w-4 h-4" />
            <span>{{ isProcessing ? '正在模拟扣款履约...' : '🧪 一键模拟测试扣款成功' }}</span>
          </button>

          <button
            v-if="currentCheckoutUrl && currentCheckoutUrl.startsWith('http')"
            @click="openExternalCheckout"
            class="w-full py-3 rounded-2xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-xs flex items-center justify-center gap-1.5 transition-all"
          >
            <span>打开官方收银台链接</span>
          </button>
        </div>

        <div class="text-center pt-1">
          <button
            @click="step = 'plans'"
            class="text-xs font-semibold text-slate-400 hover:text-slate-600 inline-flex items-center gap-1"
          >
            <ArrowLeft class="w-3.5 h-3.5" />
            <span>返回方案选择</span>
          </button>
        </div>
      </div>

      <!-- STEP 4: Success View -->
      <div v-else-if="step === 'success'" class="space-y-5 text-center py-4">
        <div class="w-16 h-16 rounded-3xl bg-emerald-500 text-white flex items-center justify-center mx-auto shadow-xl shadow-emerald-500/25 animate-bounce">
          <Check class="w-8 h-8 stroke-[3]" />
        </div>
        <div>
          <h2 class="text-2xl font-black text-slate-900">
            PRO 会员激活成功！
          </h2>
          <p class="text-xs text-slate-500 mt-2">
            恭喜您成为 ShiKe PRO 会员，已解锁无限次 AI 拍照与深度健康建议。
          </p>
        </div>

        <button
          @click="close"
          class="w-full py-3.5 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm shadow-md transition-all active:scale-[0.98]"
        >
          立即开启体验
        </button>
      </div>
    </div>

    <!-- Google Chooser Modal -->
    <GoogleChooserModal
      v-if="showGoogleModal"
      @select="handleGoogleAccountSelect"
      @close="showGoogleModal = false"
    />

    <!-- Legal & Refund Policy Modal -->
    <LegalModal
      :isOpen="showLegalModal"
      :initialTab="legalTab"
      @close="showLegalModal = false"
    />
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { X, Check, ShieldCheck, Mail, ArrowLeft, Sparkles } from 'lucide-vue-next'
import confetti from 'canvas-confetti'
import GoogleChooserModal from './GoogleChooserModal.vue'
import LegalModal from './LegalModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useI18n } from '../i18n'
import client from '../api/client'

const props = defineProps({
  isOpen: Boolean
})

const emit = defineEmits(['close'])
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const step = ref('plans') // 'plans' | 'bind_account' | 'test_checkout' | 'success'
const selectedPlan = ref('yearly')
const isProcessing = ref(false)
const showGoogleModal = ref(false)
const showLegalModal = ref(false)
const legalTab = ref('refund')
const currentOrderNo = ref('')
const currentCheckoutUrl = ref('')
let pollTimer = null

const openLegal = (tab = 'refund') => {
  legalTab.value = tab
  showLegalModal.value = true
}

// Reset step when modal opens
watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    step.value = 'plans'
  } else {
    stopPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})

const close = () => {
  stopPolling()
  step.value = 'plans'
  emit('close')
}

const handleSubscribe = async () => {
  // GUEST INTERCEPT: If user has no registered email, prompt account link first!
  if (authStore.isGuest) {
    step.value = 'bind_account'
    return
  }

  // If user is already authenticated, create real checkout session
  await executeUpgrade()
}

const executeUpgrade = async () => {
  isProcessing.value = true
  try {
    const res = await client.post('/payment/checkout', {
      planType: selectedPlan.value,
      userId: authStore.user?.id
    })

    if (!res || !res.orderNo) {
      throw new Error('Could not create checkout session')
    }

    currentOrderNo.value = res.orderNo
    currentCheckoutUrl.value = res.checkoutUrl || ''

    // If Lemon.js is loaded and URL is an official lemon squeezy checkout
    if (window.LemonSqueezy?.Url && res.checkoutUrl && res.checkoutUrl.includes('lemonsqueezy.com')) {
      // Listen for Lemon Squeezy event
      if (window.LemonSqueezy?.Setup) {
        window.LemonSqueezy.Setup({
          eventHandler: async (event) => {
            if (event.event === 'Checkout.Success') {
              await onPaymentCompleted(res)
            }
          }
        })
      }
      window.LemonSqueezy.Url.Open(res.checkoutUrl)
      startPolling(res.orderNo)
    } else {
      // In development or sandbox mode: show sandbox confirmation view
      step.value = 'test_checkout'
    }
  } catch (err) {
    console.error('Payment checkout error:', err)
    alert('Payment checkout error: ' + err.message)
  } finally {
    isProcessing.value = false
  }
}

const handleSimulateTestSuccess = async () => {
  if (!currentOrderNo.value) return
  isProcessing.value = true
  try {
    const res = await client.post(`/payment/test-complete/${currentOrderNo.value}`)
    await onPaymentCompleted(res)
  } catch (err) {
    alert('Simulation error: ' + err.message)
  } finally {
    isProcessing.value = false
  }
}

const openExternalCheckout = () => {
  if (currentCheckoutUrl.value) {
    window.open(currentCheckoutUrl.value, '_blank')
    startPolling(currentOrderNo.value)
  }
}

const startPolling = (orderNo) => {
  stopPolling()
  let attempts = 0
  pollTimer = setInterval(async () => {
    attempts++
    if (attempts > 30) {
      stopPolling()
      return
    }
    try {
      const res = await client.get(`/payment/order-status/${orderNo}`)
      if (res && (res.status === 'PAID' || res.isVipActive)) {
        await onPaymentCompleted(res)
      }
    } catch (e) {
      // silent poll error
    }
  }, 2500)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const onPaymentCompleted = async (statusData = {}) => {
  stopPolling()
  authStore.upgradeVipSuccess(statusData)
  await authStore.refreshProfile()
  confetti({
    particleCount: 100,
    spread: 70,
    origin: { y: 0.6 }
  })
  step.value = 'success'
}

const handleGoogleAccountSelect = async (account) => {
  showGoogleModal.value = false
  isProcessing.value = true
  try {
    // 1. Authenticate user with Google account
    await authStore.loginWithGoogle(account)
    // 2. Proceed to checkout
    await executeUpgrade()
  } catch (err) {
    alert('Google account link failed: ' + err.message)
  } finally {
    isProcessing.value = false
  }
}

const goToEmailAuth = () => {
  close()
  router.push(`/auth?redirect=/scan&upgrade=pro&plan=${selectedPlan.value}`)
}

const handleRestore = async () => {
  if (authStore.isGuest) {
    step.value = 'bind_account'
    return
  }
  isProcessing.value = true
  try {
    const updated = await authStore.refreshProfile()
    if (updated && (updated.vipType === 'PRO' || updated.vipType === 'VIP' || updated.aiUnlimited)) {
      authStore.upgradeVipSuccess(updated)
      alert(t('paywall.restoredSuccess'))
      close()
    } else {
      alert('未检测到有效 PRO 会员订阅或已过期')
    }
  } catch (e) {
    alert('恢复失败: ' + e.message)
  } finally {
    isProcessing.value = false
  }
}
</script>
