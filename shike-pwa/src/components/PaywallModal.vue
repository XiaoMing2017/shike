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
          <div class="flex justify-center gap-4 text-[11px] text-slate-500">
            <button @click="handleRestore" class="hover:underline">{{ t('paywall.restore') }}</button>
            <span>•</span>
            <a href="#" class="hover:underline">{{ t('paywall.terms') }}</a>
            <span>•</span>
            <a href="#" class="hover:underline">{{ t('paywall.privacy') }}</a>
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
    </div>

    <!-- Google Chooser Modal -->
    <GoogleChooserModal
      v-if="showGoogleModal"
      @select="handleGoogleAccountSelect"
      @close="showGoogleModal = false"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { X, Check, ShieldCheck, Mail, ArrowLeft } from 'lucide-vue-next'
import confetti from 'canvas-confetti'
import GoogleChooserModal from './GoogleChooserModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useI18n } from '../i18n'

const props = defineProps({
  isOpen: Boolean
})

const emit = defineEmits(['close'])
const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const step = ref('plans') // 'plans' | 'bind_account'
const selectedPlan = ref('yearly')
const isProcessing = ref(false)
const showGoogleModal = ref(false)

// Reset step when modal opens
watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    step.value = 'plans'
  }
})

const close = () => {
  step.value = 'plans'
  emit('close')
}

const handleSubscribe = async () => {
  // GUEST INTERCEPT: If user has no registered email, prompt account link first!
  if (authStore.isGuest) {
    step.value = 'bind_account'
    return
  }

  // If user is already authenticated, directly process checkout & activation
  await executeUpgrade()
}

const executeUpgrade = async () => {
  isProcessing.value = true
  try {
    await new Promise((r) => setTimeout(r, 1000))
    authStore.upgradeVipSuccess()
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    })
    close()
  } catch (err) {
    alert('Payment process cancelled or failed: ' + err.message)
  } finally {
    isProcessing.value = false
  }
}

const handleGoogleAccountSelect = async (account) => {
  showGoogleModal.value = false
  isProcessing.value = true
  try {
    // 1. Authenticate user with Google account
    await authStore.loginWithGoogle(account)
    // 2. Seamlessly complete PRO upgrade for this newly linked account
    authStore.upgradeVipSuccess()
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    })
    close()
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

const handleRestore = () => {
  if (authStore.isGuest) {
    step.value = 'bind_account'
    return
  }
  authStore.upgradeVipSuccess()
  alert(t('paywall.restoredSuccess'))
  close()
}
</script>
