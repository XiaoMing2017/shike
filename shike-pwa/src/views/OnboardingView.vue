<template>
  <div class="min-h-screen pb-12 px-5 pt-6 max-w-md mx-auto flex flex-col justify-between">
    <!-- Top Progress Bar & Header -->
    <div>
      <div class="flex items-center justify-between mb-4">
        <button
          v-if="currentStep > 1 && currentStep !== 5"
          @click="currentStep--"
          class="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 active:scale-95 transition-all"
        >
          <ArrowLeft class="w-4 h-4" />
        </button>
        <div v-else class="w-8"></div>

        <span class="text-xs font-bold text-slate-400">
          {{ t('onboarding.step', { current: currentStep, total: 5 }) }}
        </span>

        <button
          @click="$router.push('/')"
          class="text-xs font-semibold text-slate-400 hover:text-slate-600"
        >
          {{ authStore.lang === 'zh' ? '跳过' : 'Skip' }}
        </button>
      </div>

      <!-- Linear Stepper Bar -->
      <div class="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden mb-6">
        <div
          class="bg-emerald-500 h-full rounded-full transition-all duration-300"
          :style="{ width: (currentStep / 5) * 100 + '%' }"
        ></div>
      </div>

      <!-- STEP 1: Primary Goal -->
      <div v-if="currentStep === 1" class="space-y-4 animate-fade-in">
        <div>
          <h1 class="text-2xl font-black text-slate-900 leading-tight">
            {{ t('onboarding.goalTitle') }}
          </h1>
          <p class="text-xs text-slate-500 mt-1.5">
            {{ t('onboarding.goalDesc') }}
          </p>
        </div>

        <div class="space-y-3 pt-2">
          <!-- Lose Weight -->
          <div
            @click="form.goal = 'LOSE_WEIGHT'"
            class="p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center gap-3.5"
            :class="form.goal === 'LOSE_WEIGHT' ? 'border-emerald-500 bg-emerald-50/50 shadow-sm' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="w-11 h-11 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
              <Flame class="w-6 h-6" />
            </div>
            <div>
              <div class="font-bold text-slate-900 text-sm">{{ t('onboarding.loseWeight') }}</div>
              <div class="text-[11px] text-slate-500 mt-0.5">{{ t('onboarding.loseWeightSub') }}</div>
            </div>
          </div>

          <!-- Maintain -->
          <div
            @click="form.goal = 'MAINTAIN'"
            class="p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center gap-3.5"
            :class="form.goal === 'MAINTAIN' ? 'border-emerald-500 bg-emerald-50/50 shadow-sm' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="w-11 h-11 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center shrink-0">
              <Activity class="w-6 h-6" />
            </div>
            <div>
              <div class="font-bold text-slate-900 text-sm">{{ t('onboarding.maintain') }}</div>
              <div class="text-[11px] text-slate-500 mt-0.5">{{ t('onboarding.maintainSub') }}</div>
            </div>
          </div>

          <!-- Build Muscle -->
          <div
            @click="form.goal = 'GAIN_MUSCLE'"
            class="p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-center gap-3.5"
            :class="form.goal === 'GAIN_MUSCLE' ? 'border-emerald-500 bg-emerald-50/50 shadow-sm' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="w-11 h-11 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center shrink-0">
              <Dumbbell class="w-6 h-6" />
            </div>
            <div>
              <div class="font-bold text-slate-900 text-sm">{{ t('onboarding.gainMuscle') }}</div>
              <div class="text-[11px] text-slate-500 mt-0.5">{{ t('onboarding.gainMuscleSub') }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- STEP 2: Gender & Age -->
      <div v-else-if="currentStep === 2" class="space-y-6 animate-fade-in">
        <div>
          <h1 class="text-2xl font-black text-slate-900 leading-tight">
            {{ t('onboarding.genderAgeTitle') }}
          </h1>
          <p class="text-xs text-slate-500 mt-1.5">
            {{ t('onboarding.genderAgeDesc') }}
          </p>
        </div>

        <!-- Gender Selection -->
        <div>
          <label class="block text-xs font-bold text-slate-700 mb-2">Biological Sex</label>
          <div class="grid grid-cols-2 gap-3">
            <button
              type="button"
              @click="form.gender = 1"
              class="py-4 rounded-2xl border-2 font-bold text-sm flex flex-col items-center gap-2 transition-all"
              :class="form.gender === 1 ? 'border-emerald-500 bg-emerald-50 text-emerald-800 shadow-sm' : 'border-slate-200 bg-white text-slate-600'"
            >
              <span class="text-2xl">👨</span>
              <span>{{ t('onboarding.male') }}</span>
            </button>
            <button
              type="button"
              @click="form.gender = 2"
              class="py-4 rounded-2xl border-2 font-bold text-sm flex flex-col items-center gap-2 transition-all"
              :class="form.gender === 2 ? 'border-emerald-500 bg-emerald-50 text-emerald-800 shadow-sm' : 'border-slate-200 bg-white text-slate-600'"
            >
              <span class="text-2xl">👩</span>
              <span>{{ t('onboarding.female') }}</span>
            </button>
          </div>
        </div>

        <!-- Age Input -->
        <div>
          <div class="flex justify-between items-center mb-2">
            <label class="text-xs font-bold text-slate-700">{{ t('onboarding.age') }}</label>
            <span class="text-lg font-black text-emerald-600">{{ form.age }} yrs</span>
          </div>
          <input
            v-model.number="form.age"
            type="range"
            min="14"
            max="80"
            class="w-full accent-emerald-500"
          />
          <div class="flex justify-between text-[10px] text-slate-400 mt-1">
            <span>14</span>
            <span>45</span>
            <span>80</span>
          </div>
        </div>
      </div>

      <!-- STEP 3: Height & Weight (Metric vs Imperial) -->
      <div v-else-if="currentStep === 3" class="space-y-6 animate-fade-in">
        <div class="flex justify-between items-start">
          <div>
            <h1 class="text-2xl font-black text-slate-900 leading-tight">
              {{ t('onboarding.bodyTitle') }}
            </h1>
            <p class="text-xs text-slate-500 mt-1.5">
              {{ t('onboarding.bodyDesc') }}
            </p>
          </div>
          <!-- Unit Switcher -->
          <div class="flex bg-slate-100 p-1 rounded-xl text-xs font-bold shrink-0">
            <button
              @click="setUnit('metric')"
              class="px-2 py-0.5 rounded-lg transition-all"
              :class="authStore.unitSystem === 'metric' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'"
            >
              kg
            </button>
            <button
              @click="setUnit('imperial')"
              class="px-2 py-0.5 rounded-lg transition-all"
              :class="authStore.unitSystem === 'imperial' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'"
            >
              lbs
            </button>
          </div>
        </div>

        <!-- Height -->
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-100">
          <div class="flex justify-between items-center mb-1">
            <span class="text-xs font-bold text-slate-700">{{ t('onboarding.height') }}</span>
            <span class="text-base font-black text-slate-900">
              {{ authStore.unitSystem === 'metric' ? form.height + ' cm' : formatInches(form.height) }}
            </span>
          </div>
          <input
            v-model.number="form.height"
            type="range"
            min="130"
            max="220"
            class="w-full accent-emerald-500 mt-2"
          />
        </div>

        <!-- Weight -->
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-100">
          <div class="flex justify-between items-center mb-1">
            <span class="text-xs font-bold text-slate-700">{{ t('onboarding.weight') }}</span>
            <span class="text-base font-black text-slate-900">
              {{ authStore.unitSystem === 'metric' ? form.weight + ' kg' : Math.round(form.weight * 2.20462) + ' lbs' }}
            </span>
          </div>
          <input
            v-model.number="form.weight"
            type="range"
            min="40"
            max="160"
            class="w-full accent-emerald-500 mt-2"
          />
        </div>
      </div>

      <!-- STEP 4: Target Weight & Activity Level -->
      <div v-else-if="currentStep === 4" class="space-y-5 animate-fade-in">
        <div>
          <h1 class="text-2xl font-black text-slate-900 leading-tight">
            {{ t('onboarding.activityTitle') }}
          </h1>
          <p class="text-xs text-slate-500 mt-1.5">
            {{ t('onboarding.activityDesc') }}
          </p>
        </div>

        <!-- Target Weight if Lose Weight -->
        <div v-if="form.goal === 'LOSE_WEIGHT'" class="bg-emerald-50/50 p-3.5 rounded-2xl border border-emerald-100">
          <div class="flex justify-between items-center">
            <span class="text-xs font-bold text-emerald-800">{{ t('onboarding.targetWeight') }}</span>
            <span class="text-sm font-black text-emerald-900">
              {{ authStore.unitSystem === 'metric' ? form.targetWeight + ' kg' : Math.round(form.targetWeight * 2.20462) + ' lbs' }}
            </span>
          </div>
          <input
            v-model.number="form.targetWeight"
            type="range"
            :min="Math.max(40, form.weight - 30)"
            :max="form.weight"
            class="w-full accent-emerald-500 mt-2"
          />
        </div>

        <!-- 4 Activity Levels -->
        <div class="space-y-2.5">
          <div
            v-for="act in activityOptions"
            :key="act.key"
            @click="form.activityLevel = act.key"
            class="p-3.5 rounded-2xl border-2 transition-all cursor-pointer flex items-center justify-between"
            :class="form.activityLevel === act.key ? 'border-emerald-500 bg-emerald-50/60 shadow-sm' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div>
              <div class="font-bold text-slate-800 text-xs">{{ act.title }}</div>
              <div class="text-[11px] text-slate-400 mt-0.5">{{ act.desc }}</div>
            </div>
            <Check v-if="form.activityLevel === act.key" class="w-4 h-4 text-emerald-600 stroke-[3]" />
          </div>
        </div>
      </div>

      <!-- STEP 5: The "Aha!" Calculation & Register to Save Plan -->
      <div v-else-if="currentStep === 5" class="space-y-5 animate-fade-in">
        <div>
          <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-xs font-bold">
            ✨ {{ authStore.lang === 'zh' ? '个性化定制报告' : 'Personalized Profile' }}
          </span>
          <h1 class="text-2xl font-black text-slate-900 leading-tight mt-1">
            {{ t('onboarding.resultTitle') }}
          </h1>
          <p class="text-xs text-slate-500 mt-1">
            {{ t('onboarding.resultDesc') }}
          </p>
        </div>

        <!-- Metabolic Target Card -->
        <div class="glass-card rounded-3xl p-5 border-emerald-300/80 bg-gradient-to-br from-emerald-500/10 via-white to-emerald-500/5 shadow-md">
          <div class="flex justify-between items-baseline mb-3">
            <span class="text-xs font-bold uppercase tracking-wider text-slate-400">{{ t('onboarding.dailyBudget') }}</span>
            <span class="text-3xl font-black text-emerald-600">{{ computedResults.targetCalories }} <span class="text-xs font-bold text-slate-400">kcal/day</span></span>
          </div>

          <!-- BMR & TDEE submetrics -->
          <div class="grid grid-cols-2 gap-2 pt-3 border-t border-slate-100 text-xs">
            <div class="bg-white/80 p-2.5 rounded-xl border border-slate-100">
              <div class="text-[10px] text-slate-400 font-medium">{{ t('onboarding.bmr') }}</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ computedResults.bmr }} kcal</div>
            </div>
            <div class="bg-white/80 p-2.5 rounded-xl border border-slate-100">
              <div class="text-[10px] text-slate-400 font-medium">{{ t('onboarding.tdee') }}</div>
              <div class="font-bold text-slate-800 mt-0.5">{{ computedResults.tdee }} kcal</div>
            </div>
          </div>

          <!-- Forecast -->
          <div v-if="form.goal === 'LOSE_WEIGHT'" class="mt-3 text-center text-xs font-bold text-emerald-700 bg-emerald-100/60 py-2 rounded-xl">
            {{ t('onboarding.forecast', { weeks: computedResults.weeks }) }}
          </div>
        </div>

        <!-- If already logged in: One-click update plan confirmation -->
        <div v-if="authStore.isLoggedIn" class="glass-card rounded-3xl p-5 border border-emerald-200/90 bg-emerald-50/40">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 rounded-2xl bg-emerald-500 text-white flex items-center justify-center font-bold text-sm shadow-md shadow-emerald-500/20">
              {{ (authStore.user?.nickname || 'U').charAt(0).toUpperCase() }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-1.5">
                <h4 class="text-xs font-bold text-slate-800 truncate">{{ authStore.user?.nickname || 'Member' }}</h4>
                <span class="text-[10px] bg-emerald-100 text-emerald-700 font-bold px-1.5 py-0.2 rounded shrink-0">
                  {{ authStore.lang === 'zh' ? '当前已登录' : 'Signed In' }}
                </span>
              </div>
              <p class="text-[11px] text-slate-500 font-mono truncate mt-0.5">{{ authStore.user?.email }}</p>
            </div>
          </div>

          <div class="p-3 bg-white/80 rounded-2xl border border-emerald-100/80 mb-4 text-xs text-slate-600 leading-relaxed">
            {{ t('onboarding.updatePlanDesc') }}
          </div>

          <button
            @click="handleUpdateExistingUser"
            :disabled="isSubmitting"
            class="w-full py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-xs shadow-md shadow-emerald-500/25 flex items-center justify-center gap-1.5 transition-all"
          >
            <Check class="w-4 h-4 stroke-[3]" />
            <span>{{ isSubmitting ? (authStore.lang === 'zh' ? '正在更新...' : 'Updating...') : t('onboarding.updatePlanBtn') }}</span>
          </button>
        </div>

        <!-- Registration Form to Save Plan (Guest / Not Logged In) -->
        <div v-else class="glass-card rounded-3xl p-5 border border-slate-200">
          <h3 class="text-sm font-bold text-slate-800 mb-1">{{ t('onboarding.saveToAccountTitle') }}</h3>
          <p class="text-[11px] text-slate-500 mb-4">{{ t('onboarding.saveToAccountDesc') }}</p>

          <form @submit.prevent="handleFinalRegister" class="space-y-3">
            <div>
              <input
                v-model="account.email"
                type="email"
                required
                :placeholder="t('auth.emailPlaceholder')"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-medium text-slate-800 outline-none focus:border-emerald-500 focus:bg-white transition-all"
              />
            </div>
            <div>
              <input
                v-model="account.password"
                type="password"
                required
                minlength="6"
                :placeholder="t('auth.passwordPlaceholder')"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-medium text-slate-800 outline-none focus:border-emerald-500 focus:bg-white transition-all"
              />
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="w-full py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-xs shadow-md shadow-emerald-500/25 flex items-center justify-center gap-1.5 transition-all mt-2"
            >
              <Check class="w-4 h-4" />
              <span>{{ isSubmitting ? 'Saving...' : t('onboarding.finish') }}</span>
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- Bottom Next Button (Steps 1 - 4) -->
    <div v-if="currentStep < 5" class="pt-6">
      <button
        @click="currentStep++"
        class="w-full py-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-sm shadow-lg shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all"
      >
        <span>{{ t('onboarding.next') }}</span>
        <ArrowRight class="w-4 h-4" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Flame, Activity, Dumbbell, Check } from 'lucide-vue-next'
import confetti from 'canvas-confetti'
import client from '../api/client'
import { useAuthStore } from '../stores/authStore'
import { useDietStore } from '../stores/dietStore'
import { useI18n } from '../i18n'

const router = useRouter()
const authStore = useAuthStore()
const dietStore = useDietStore()
const { t } = useI18n()

const currentStep = ref(1)
const isSubmitting = ref(false)

const form = reactive({
  goal: 'LOSE_WEIGHT',
  gender: 1, // 1-male, 2-female
  age: 26,
  height: 175, // cm
  weight: 76,  // kg
  targetWeight: 68, // kg
  activityLevel: 'SEDENTARY'
})

onMounted(() => {
  if (authStore.user) {
    if (authStore.user.goal) form.goal = authStore.user.goal
    if (authStore.user.gender) form.gender = Number(authStore.user.gender)
    if (authStore.user.age) form.age = Number(authStore.user.age)
    if (authStore.user.height) form.height = Number(authStore.user.height)
    if (authStore.user.weight) form.weight = Number(authStore.user.weight)
    if (authStore.user.targetWeight || authStore.user.customGoalWeight) {
      form.targetWeight = Number(authStore.user.targetWeight || authStore.user.customGoalWeight)
    }
    if (authStore.user.activityLevel) form.activityLevel = authStore.user.activityLevel
  }
})

const account = reactive({
  email: '',
  password: ''
})

const activityOptions = computed(() => [
  { key: 'SEDENTARY', title: t('onboarding.sedentary'), desc: t('onboarding.sedentarySub') },
  { key: 'LIGHT', title: t('onboarding.light'), desc: t('onboarding.lightSub') },
  { key: 'MODERATE', title: t('onboarding.moderate'), desc: t('onboarding.moderateSub') },
  { key: 'ACTIVE', title: t('onboarding.active'), desc: t('onboarding.activeSub') }
])

const setUnit = (unit) => {
  authStore.setUnitSystem(unit)
}

const formatInches = (cm) => {
  const totalInches = Math.round(cm / 2.54)
  const ft = Math.floor(totalInches / 12)
  const inc = totalInches % 12
  return `${ft}'${inc}"`
}

// Mifflin-St Jeor clinical formula calculations
const computedResults = computed(() => {
  const w = form.weight
  const h = form.height
  const age = form.age
  let bmrVal
  if (form.gender === 2) {
    bmrVal = 10 * w + 6.25 * h - 5 * age - 161
  } else {
    bmrVal = 10 * w + 6.25 * h - 5 * age + 5
  }

  let multiplier = 1.2
  if (form.activityLevel === 'LIGHT') multiplier = 1.375
  if (form.activityLevel === 'MODERATE') multiplier = 1.55
  if (form.activityLevel === 'ACTIVE') multiplier = 1.725

  const tdeeVal = bmrVal * multiplier
  let targetCal = tdeeVal
  if (form.goal === 'LOSE_WEIGHT') targetCal = tdeeVal - 500
  if (form.goal === 'GAIN_MUSCLE') targetCal = tdeeVal + 300

  // Floor
  const floor = form.gender === 2 ? 1200 : 1500
  if (targetCal < floor) targetCal = floor

  // Weeks to goal
  const diffKg = Math.max(0, form.weight - form.targetWeight)
  const weeks = Math.max(1, Math.round(diffKg / 0.5))

  return {
    bmr: Math.round(bmrVal),
    tdee: Math.round(tdeeVal),
    targetCalories: Math.round(targetCal),
    weeks
  }
})

const handleUpdateExistingUser = async () => {
  isSubmitting.value = true
  try {
    if (authStore.user) {
      authStore.user.gender = form.gender
      authStore.user.age = form.age
      authStore.user.height = form.height
      authStore.user.weight = form.weight
      authStore.user.targetWeight = form.targetWeight
      authStore.user.customGoalWeight = form.targetWeight
      authStore.user.activityLevel = form.activityLevel
      authStore.user.goal = form.goal
      authStore.user.bmr = computedResults.value.bmr
      authStore.user.tdee = computedResults.value.tdee
      authStore.user.targetCalories = computedResults.value.targetCalories

      localStorage.setItem('shike_user', JSON.stringify(authStore.user))
    }

    dietStore.setTargetCalories(computedResults.value.targetCalories)

    // Sync to backend MySQL
    const userId = Number(authStore.user?.id || localStorage.getItem('shike_user_id'))
    if (userId && !isNaN(userId)) {
      try {
        await client.post('/user/profile', {
          userId,
          gender: form.gender,
          age: form.age,
          height: form.height,
          weight: form.weight,
          activityLevel: form.activityLevel,
          goal: form.goal,
          customGoalWeight: form.targetWeight
        })
      } catch (err) {
        console.warn('Backend profile update failed, updated locally:', err.message)
      }
    }

    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    })

    router.push('/profile')
  } catch (err) {
    alert('Failed to update plan: ' + err.message)
  } finally {
    isSubmitting.value = false
  }
}

const handleFinalRegister = async () => {
  isSubmitting.value = true
  try {
    const payload = {
      email: account.email,
      password: account.password,
      gender: form.gender,
      age: form.age,
      height: form.height,
      weight: form.weight,
      targetWeight: form.targetWeight,
      activityLevel: form.activityLevel,
      goal: form.goal,
      bmr: computedResults.value.bmr,
      tdee: computedResults.value.tdee,
      targetCalories: computedResults.value.targetCalories,
      nickname: account.email.split('@')[0]
    }

    // Call registration (real backend + local store sync)
    await authStore.register(payload)
    dietStore.setTargetCalories(computedResults.value.targetCalories)

    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    })

    router.push('/')
  } catch (err) {
    alert('Registration error: ' + err.message)
  } finally {
    isSubmitting.value = false
  }
}
</script>
