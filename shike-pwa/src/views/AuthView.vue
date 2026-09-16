<template>
  <div class="min-h-screen pb-12 px-5 pt-8 max-w-md mx-auto flex flex-col justify-between">
    <div>
      <!-- Back to App Button -->
      <div class="flex items-center justify-between mb-6">
        <button
          @click="$router.push('/')"
          class="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 active:scale-95 transition-all"
        >
          <ArrowLeft class="w-5 h-5" />
        </button>
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">ShiKe ID</span>
        <div class="w-9"></div>
      </div>

      <!-- Brand Hero -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 rounded-3xl bg-emerald-500 text-white flex items-center justify-center font-black text-2xl shadow-xl shadow-emerald-500/25 mx-auto mb-3">
          S
        </div>
        <h1 class="text-2xl font-black text-slate-900 leading-tight">
          {{ mode === 'login' ? t('auth.welcomeBack') : t('auth.createAccount') }}
        </h1>
        <p class="text-xs text-slate-500 mt-1.5">
          {{ mode === 'login' ? t('auth.welcomeBackSub') : t('auth.createAccountSub') }}
        </p>
      </div>

      <!-- Mode Switcher Tabs (Sign In / Sign Up) -->
      <div class="flex bg-slate-100 p-1 rounded-2xl mb-6 shadow-inner text-xs font-bold">
        <button
          @click="mode = 'login'"
          class="flex-1 py-2.5 rounded-xl transition-all"
          :class="mode === 'login' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500 hover:text-slate-700'"
        >
          {{ t('auth.signIn') }}
        </button>
        <button
          @click="mode = 'register'"
          class="flex-1 py-2.5 rounded-xl transition-all"
          :class="mode === 'register' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500 hover:text-slate-700'"
        >
          {{ t('auth.signUp') }}
        </button>
      </div>

      <!-- Personalized Quiz Promo Card (Register Mode) -->
      <div
        v-if="mode === 'register'"
        @click="$router.push('/onboarding')"
        class="mb-5 p-3.5 rounded-2xl bg-gradient-to-br from-emerald-500/10 via-emerald-50/50 to-emerald-500/5 border border-emerald-300/80 cursor-pointer hover:border-emerald-400 transition-all flex items-center justify-between group shadow-sm"
      >
        <div class="pr-2">
          <div class="flex items-center gap-1.5 text-xs font-black text-emerald-800">
            <Sparkles class="w-3.5 h-3.5 text-emerald-600" />
            <span>{{ t('auth.quizPromoTitle') }}</span>
          </div>
          <div class="text-[11px] text-slate-500 mt-0.5 leading-snug">
            {{ t('auth.quizPromoDesc') }}
          </div>
        </div>
        <div class="w-7 h-7 rounded-xl bg-emerald-500 text-white flex items-center justify-center shrink-0 group-hover:translate-x-0.5 transition-transform shadow-sm">
          <ArrowRight class="w-4 h-4" />
        </div>
      </div>

      <!-- Google One-Click Login (Opens Account Chooser Sheet) -->
      <button
        @click="showGoogleModal = true"
        :disabled="isLoading"
        class="w-full py-3.5 px-4 rounded-2xl bg-white border border-slate-200 hover:border-emerald-400 hover:shadow-md shadow-sm flex items-center justify-between text-xs font-bold text-slate-800 active:scale-[0.98] transition-all mb-5 group"
      >
        <div class="flex items-center gap-3">
          <svg class="w-4 h-4" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>{{ t('auth.continueWithGoogle') }}</span>
        </div>
        <span class="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-black uppercase tracking-wider">
          {{ t('auth.recommended') }}
        </span>
      </button>

      <!-- Google Account Chooser Modal -->
      <GoogleChooserModal
        v-if="showGoogleModal"
        @select="handleGoogleAccountSelect"
        @close="showGoogleModal = false"
      />

      <!-- Divider -->
      <div class="flex items-center gap-3 mb-5">
        <div class="h-px bg-slate-200 flex-1"></div>
        <span class="text-[11px] text-slate-400 uppercase tracking-wider font-medium">{{ t('auth.orWithEmail') }}</span>
        <div class="h-px bg-slate-200 flex-1"></div>
      </div>

      <!-- Form Inputs -->
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <!-- Nickname (Register Only) -->
        <div v-if="mode === 'register'">
          <label class="block text-xs font-bold text-slate-700 mb-1.5">{{ t('auth.nickname') }}</label>
          <div class="relative">
            <input
              v-model="form.nickname"
              type="text"
              required
              :placeholder="t('auth.nicknamePlaceholder')"
              class="w-full px-4 py-3 rounded-2xl bg-white border border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 text-xs font-medium text-slate-800 outline-none transition-all"
            />
          </div>
        </div>

        <!-- Email -->
        <div>
          <div class="flex items-center justify-between mb-1.5">
            <label class="block text-xs font-bold text-slate-700">{{ t('auth.email') }}</label>
            <span v-if="form.email && !isEmailValid" class="text-[10px] font-semibold text-rose-500 flex items-center gap-1">
              <AlertCircle class="w-3 h-3" />
              <span>{{ t('auth.invalidEmail') }}</span>
            </span>
          </div>
          <div class="relative">
            <input
              v-model="form.email"
              type="email"
              required
              :placeholder="t('auth.emailPlaceholder')"
              class="w-full px-4 py-3 rounded-2xl bg-white border text-xs font-medium text-slate-800 outline-none transition-all"
              :class="!isEmailValid && form.email ? 'border-rose-300 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20' : 'border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'"
            />
          </div>

          <!-- Did you mean suggestion banner -->
          <div
            v-if="suggestedEmail"
            class="mt-1.5 px-3 py-1.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center justify-between animate-fade-in"
          >
            <div class="flex items-center gap-1.5 truncate">
              <Sparkles class="w-3.5 h-3.5 text-amber-600 shrink-0" />
              <span class="truncate font-medium">{{ t('auth.emailSuggestion', { email: suggestedEmail }) }}</span>
            </div>
            <button
              type="button"
              @click="applyEmailSuggestion"
              class="ml-2 px-2.5 py-0.5 rounded-lg bg-amber-200/80 hover:bg-amber-300 text-amber-950 font-bold text-[11px] shrink-0 active:scale-95 transition-all"
            >
              {{ t('auth.fixEmail') }}
            </button>
          </div>
        </div>

        <!-- Password -->
        <div>
          <div class="flex items-center justify-between mb-1.5">
            <label class="block text-xs font-bold text-slate-700">{{ t('auth.password') }}</label>
            <span v-if="form.password && form.password.length < 6" class="text-[10px] font-semibold text-rose-500">
              {{ t('auth.passwordTooShort') }}
            </span>
          </div>
          <div class="relative">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              required
              minlength="6"
              :placeholder="t('auth.passwordPlaceholder')"
              class="w-full px-4 py-3 rounded-2xl bg-white border text-xs font-medium text-slate-800 outline-none transition-all pr-10"
              :class="form.password && form.password.length < 6 ? 'border-rose-300 focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20' : 'border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <Eye v-if="!showPassword" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Submit Button -->
        <button
          type="submit"
          :disabled="isLoading"
          class="w-full py-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-sm shadow-lg shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all mt-2"
        >
          <span v-if="isLoading">{{ authStore.lang === 'zh' ? '正在提交...' : 'Please wait...' }}</span>
          <span v-else>{{ mode === 'login' ? t('auth.signInBtn') : t('auth.signUpBtn') }}</span>
        </button>
      </form>
    </div>

    <!-- Bottom Guest Explorer Link -->
    <div class="text-center pt-8">
      <router-link
        to="/"
        class="text-xs font-semibold text-slate-400 hover:text-emerald-600 transition-colors"
      >
        {{ t('auth.continueAsGuest') }} &rarr;
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Eye, EyeOff, Sparkles, ArrowRight, AlertCircle } from 'lucide-vue-next'
import confetti from 'canvas-confetti'
import GoogleChooserModal from '../components/GoogleChooserModal.vue'
import { useAuthStore } from '../stores/authStore'
import { useI18n } from '../i18n'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()

const mode = ref('login') // 'login' | 'register'
const showPassword = ref(false)
const isLoading = ref(false)
const showGoogleModal = ref(false)

const form = reactive({
  email: '',
  password: '',
  nickname: ''
})

// Common email domain typos & quick fixes
const DOMAIN_TYPOS = {
  'gmai.com': 'gmail.com',
  'gamil.com': 'gmail.com',
  'gmial.com': 'gmail.com',
  'gmaill.com': 'gmail.com',
  'gmaik.com': 'gmail.com',
  'hotmial.com': 'hotmail.com',
  'hotmai.com': 'hotmail.com',
  'hotamil.com': 'hotmail.com',
  'outlok.com': 'outlook.com',
  'outloo.com': 'outlook.com',
  'yaho.com': 'yahoo.com',
  'yahooo.com': 'yahoo.com',
  'yaho.co': 'yahoo.com',
  'icld.com': 'icloud.com',
  'iclod.com': 'icloud.com',
  'iclou.com': 'icloud.com'
}

const suggestedEmail = computed(() => {
  if (!form.email || !form.email.includes('@')) return ''
  const parts = form.email.split('@')
  if (parts.length !== 2) return ''
  const [local, domain] = parts
  const lowerDomain = domain.toLowerCase().trim()
  if (DOMAIN_TYPOS[lowerDomain]) {
    return `${local}@${DOMAIN_TYPOS[lowerDomain]}`
  }
  return ''
})

const isEmailValid = computed(() => {
  if (!form.email) return true
  // Requires valid local part, @, domain part, and valid TLD of at least 2 chars
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(form.email.trim())
})

const applyEmailSuggestion = () => {
  if (suggestedEmail.value) {
    form.email = suggestedEmail.value
  }
}

const handleSubmit = async () => {
  if (!form.email || !isEmailValid.value) {
    alert(t('auth.invalidEmail'))
    return
  }
  if (!form.password || form.password.length < 6) {
    alert(t('auth.passwordTooShort'))
    return
  }

  isLoading.value = true
  try {
    if (mode.value === 'login') {
      await authStore.login(form.email.trim(), form.password)
    } else {
      await authStore.register(form.email.trim(), form.password, form.nickname.trim())
    }
    confetti({
      particleCount: 60,
      spread: 60,
      origin: { y: 0.6 }
    })
    if (route.query.upgrade === 'pro') {
      authStore.upgradeVipSuccess()
    }
    const target = route.query.redirect || '/profile'
    router.push(target)
  } catch (err) {
    alert('Authentication error: ' + err.message)
  } finally {
    isLoading.value = false
  }
}

const handleGoogleAccountSelect = async (account) => {
  showGoogleModal.value = false
  isLoading.value = true
  try {
    await authStore.loginWithGoogle(account)
    confetti({
      particleCount: 60,
      spread: 60,
      origin: { y: 0.6 }
    })
    if (route.query.upgrade === 'pro') {
      authStore.upgradeVipSuccess()
    }
    const target = route.query.redirect || '/profile'
    router.push(target)
  } catch (err) {
    alert('Google login failed: ' + err.message)
  } finally {
    isLoading.value = false
  }
}
</script>
