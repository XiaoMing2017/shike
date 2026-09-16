<template>
  <div class="min-h-screen pb-28 px-4 pt-6 max-w-md mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-full bg-emerald-500 text-white flex items-center justify-center font-bold text-lg shadow-md shadow-emerald-500/20">
          S
        </div>
        <div>
          <h1 class="text-lg font-bold text-slate-900 leading-tight">{{ t('home.title') }}</h1>
          <p class="text-xs text-slate-500">{{ todayFormatted }}</p>
        </div>
      </div>
      
      <!-- Streak & Pro Badge -->
      <div class="flex items-center gap-2">
        <div class="flex items-center gap-1 px-2.5 py-1 rounded-full bg-orange-50 border border-orange-200/60 text-orange-600 text-xs font-semibold">
          <Flame class="w-3.5 h-3.5 fill-orange-500 text-orange-500" />
          <span>{{ t('home.streak', { n: authStore.user?.currentStreak || 0 }) }}</span>
        </div>
        <button
          v-if="!authStore.isVip"
          @click="authStore.openPaywall"
          class="px-2.5 py-1 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold shadow-sm transition-transform active:scale-95"
        >
          {{ t('home.goPro') }}
        </button>
      </div>
    </div>

    <!-- Main Calorie Glassmorphism Card -->
    <div class="glass-card rounded-3xl p-5 mb-5 relative overflow-hidden">
      <div class="flex items-center justify-between">
        <!-- Left: Calorie Count & Target -->
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">{{ t('home.remaining') }}</span>
          <div class="text-4xl font-black text-slate-900 mt-0.5 tracking-tight">
            {{ dietStore.remainingCalories }}
            <span class="text-sm font-semibold text-slate-400">kcal</span>
          </div>
          <div class="text-xs text-slate-500 mt-1 flex items-center gap-1">
            <span>{{ t('home.baseTarget') }}:</span>
            <span class="font-semibold text-slate-700">{{ dietStore.targetCalories }} kcal</span>
          </div>
        </div>

        <!-- Right: Circular Visual Ring -->
        <div class="relative w-24 h-24 flex items-center justify-center">
          <svg class="w-full h-full -rotate-90" viewBox="0 0 36 36">
            <path
              class="text-slate-100"
              stroke-width="3.5"
              stroke="currentColor"
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            />
            <path
              class="text-emerald-500 transition-all duration-1000 ease-out"
              stroke-dasharray="100, 100"
              :stroke-dashoffset="100 - dietStore.calorieProgress"
              stroke-linecap="round"
              stroke-width="3.8"
              stroke="currentColor"
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            />
          </svg>
          <div class="absolute flex flex-col items-center">
            <span class="text-base font-black text-slate-800">{{ dietStore.consumedCalories }}</span>
            <span class="text-[10px] text-slate-400 -mt-0.5">{{ t('home.eaten') }}</span>
          </div>
        </div>
      </div>

      <!-- Macro Progress Bars (Carbs, Protein, Fat) -->
      <div class="grid grid-cols-3 gap-3 mt-5 pt-4 border-t border-slate-100">
        <!-- Protein -->
        <div class="bg-slate-50/70 p-2.5 rounded-2xl border border-slate-100/80">
          <div class="flex justify-between items-center text-xs mb-1">
            <span class="text-slate-500 font-medium">{{ t('home.protein') }}</span>
            <span class="font-bold text-slate-800">{{ dietStore.protein }}g</span>
          </div>
          <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
            <div class="bg-blue-500 h-full rounded-full" :style="{ width: Math.min(100, (dietStore.protein / 140) * 100) + '%' }"></div>
          </div>
        </div>

        <!-- Carbs (with Net Carbs hint) -->
        <div class="bg-slate-50/70 p-2.5 rounded-2xl border border-slate-100/80">
          <div class="flex justify-between items-center text-xs mb-1">
            <span class="text-slate-500 font-medium">{{ t('home.carbs') }}</span>
            <span class="font-bold text-slate-800">{{ dietStore.carbs }}g</span>
          </div>
          <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
            <div class="bg-amber-500 h-full rounded-full" :style="{ width: Math.min(100, (dietStore.carbs / 220) * 100) + '%' }"></div>
          </div>
        </div>

        <!-- Fat -->
        <div class="bg-slate-50/70 p-2.5 rounded-2xl border border-slate-100/80">
          <div class="flex justify-between items-center text-xs mb-1">
            <span class="text-slate-500 font-medium">{{ t('home.fat') }}</span>
            <span class="font-bold text-slate-800">{{ dietStore.fat }}g</span>
          </div>
          <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
            <div class="bg-rose-500 h-full rounded-full" :style="{ width: Math.min(100, (dietStore.fat / 65) * 100) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Hydration Tracker (Quick Tap) -->
    <div class="glass-card rounded-2xl p-4 mb-5 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-blue-50 text-blue-500 flex items-center justify-center">
          <Droplets class="w-5 h-5 fill-blue-500/20" />
        </div>
        <div>
          <div class="text-sm font-bold text-slate-800">{{ t('home.waterTitle') }}</div>
          <div class="text-xs text-slate-500">{{ dietStore.waterMl }} / {{ dietStore.waterTargetMl }} ml</div>
        </div>
      </div>
      <button
        @click="dietStore.addWater(250)"
        class="px-3 py-1.5 rounded-xl bg-blue-500 hover:bg-blue-600 text-white text-xs font-bold flex items-center gap-1 active:scale-95 transition-all shadow-sm shadow-blue-500/20"
      >
        <Plus class="w-3.5 h-3.5" />
        <span>{{ t('home.addWater') }}</span>
      </button>
    </div>

    <!-- Meals Logged Section -->
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-base font-bold text-slate-800">{{ t('home.loggedMeals') }}</h2>
      <router-link to="/scan" class="text-xs font-semibold text-emerald-600 flex items-center gap-0.5">
        <span>{{ t('home.snapMeal') }}</span>
      </router-link>
    </div>

    <!-- Meals List / Empty State -->
    <div class="space-y-3">
      <div
        v-if="dietStore.records.length === 0"
        class="glass-card rounded-2xl p-6 text-center border-dashed border-2 border-slate-200/80 bg-white/60"
      >
        <div class="w-12 h-12 mx-auto rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center mb-3 shadow-inner">
          <UtensilsCrossed class="w-6 h-6" />
        </div>
        <div class="text-sm font-bold text-slate-800 mb-1">
          {{ t('home.emptyTitle') }}
        </div>
        <p class="text-xs text-slate-400 mb-4 max-w-[240px] mx-auto leading-relaxed">
          {{ t('home.emptyDesc') }}
        </p>
        <router-link
          to="/scan"
          class="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold shadow-sm shadow-emerald-500/20 active:scale-95 transition-all"
        >
          <Camera class="w-3.5 h-3.5" />
          <span>{{ t('home.logFirstMeal') }}</span>
        </router-link>
      </div>

      <div
        v-else
        v-for="meal in dietStore.records"
        :key="meal.id"
        class="glass-card rounded-2xl p-3.5 flex items-center gap-3.5 hover:shadow-md transition-shadow"
      >
        <img
          :src="meal.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300'"
          alt="meal thumbnail"
          class="w-14 h-14 rounded-xl object-cover shrink-0 bg-slate-100"
        />
        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-800 truncate">
              {{ meal.name || meal.dishName || (authStore.lang === 'zh' ? '营养健康餐' : 'Nutritious Meal') }}
            </h3>
            <span class="text-sm font-black text-slate-900 ml-2">
              {{ meal.calories ?? meal.totalCalories ?? 0 }} kcal
            </span>
          </div>
          <div class="text-[11px] text-slate-400 mt-0.5 flex items-center gap-2">
            <span>{{ meal.mealTime || (authStore.lang === 'zh' ? '今日' : 'Today') }}</span>
            <span class="inline-block w-1 h-1 rounded-full bg-slate-300"></span>
            <span class="text-emerald-600 font-medium">{{ meal.oilModifier || '适中' }}</span>
          </div>
          <div class="flex gap-3 text-[11px] text-slate-500 mt-1">
            <span>P: {{ meal.protein ?? meal.totalProtein ?? 0 }}g</span>
            <span>C: {{ meal.carbs ?? meal.totalCarbs ?? 0 }}g</span>
            <span>F: {{ meal.fat ?? meal.totalFat ?? 0 }}g</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { Flame, Droplets, Plus, UtensilsCrossed, Camera } from 'lucide-vue-next'
import { useAuthStore } from '../stores/authStore'
import { useDietStore } from '../stores/dietStore'
import { useI18n } from '../i18n'

const authStore = useAuthStore()
const dietStore = useDietStore()
const { t } = useI18n()

const localeMap = {
  zh: 'zh-CN',
  es: 'es-ES',
  de: 'de-DE',
  fr: 'fr-FR',
  ja: 'ja-JP',
  pt: 'pt-BR',
  en: 'en-US'
}

const todayFormatted = computed(() => {
  const options = { weekday: 'short', month: 'short', day: 'numeric' }
  const locale = localeMap[authStore.lang] || 'en-US'
  return new Date().toLocaleDateString(locale, options)
})

onMounted(async () => {
  await authStore.initSession()
  await dietStore.fetchTodaySummary(authStore.userId)
})
</script>
