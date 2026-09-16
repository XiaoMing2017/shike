<template>
  <div class="min-h-screen pb-28 px-4 pt-6 max-w-md mx-auto">
    <!-- Header with Weekly / Monthly Switcher -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-lg font-bold text-slate-900 leading-tight">{{ t('history.title') }}</h1>
        <p class="text-xs text-slate-500">{{ t('history.subtitle') }}</p>
      </div>
      <!-- View Mode Buttons -->
      <div class="flex gap-1 bg-slate-100 p-1 rounded-xl text-xs font-bold text-slate-600 shadow-inner">
        <button
          @click="viewMode = 'weekly'"
          class="px-2.5 py-1 rounded-lg transition-all"
          :class="viewMode === 'weekly' ? 'bg-white shadow-sm text-slate-900 font-bold' : 'text-slate-500 hover:text-slate-700'"
        >
          {{ t('history.weekly') }}
        </button>
        <button
          @click="viewMode = 'monthly'"
          class="px-2.5 py-1 rounded-lg transition-all"
          :class="viewMode === 'monthly' ? 'bg-white shadow-sm text-slate-900 font-bold' : 'text-slate-500 hover:text-slate-700'"
        >
          {{ t('history.monthly') }}
        </button>
      </div>
    </div>

    <!-- 1. Weekly View Mode -->
    <div v-if="viewMode === 'weekly'" class="space-y-5 animate-fade-in">
      <!-- 7-Day Calorie Bar Chart -->
      <div class="glass-card rounded-3xl p-5">
        <div class="flex justify-between items-center mb-4">
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">{{ t('history.average7d') }}</span>
          <span class="text-sm font-black text-slate-800">{{ t('history.kcalPerDay', { n: weekAvgIntake }) }}</span>
        </div>
        
        <!-- Bar Chart -->
        <div class="flex justify-between items-end h-32 pt-4 px-1">
          <div
            v-for="day in weekStats"
            :key="day.label"
            class="flex flex-col items-center gap-1.5 flex-1 cursor-pointer group"
            @click="selectWeekDay(day)"
          >
            <span class="text-[9px] text-slate-400 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
              {{ day.calories }}
            </span>
            <div class="w-4 bg-slate-100 rounded-full h-20 flex items-end overflow-hidden">
              <div
                class="w-full rounded-full transition-all duration-700"
                :class="day.isToday ? 'bg-emerald-500' : 'bg-slate-300 group-hover:bg-emerald-400'"
                :style="{ height: Math.max(day.percent, day.caloriesNum > 0 ? 8 : 0) + '%' }"
              ></div>
            </div>
            <span
              class="text-[10px] font-semibold transition-colors"
              :class="day.isToday ? 'text-emerald-600 font-bold' : 'text-slate-400 group-hover:text-slate-600'"
            >
              {{ authStore.lang === 'zh' ? day.labelZh : day.label }}
            </span>
          </div>
        </div>

        <!-- Weekly On-Track Summary -->
        <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
          <div class="flex items-center gap-1.5 text-slate-600">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>{{ t('history.targetMet') }}: <strong>{{ weekTargetMetDays }}/7 {{ authStore.lang === 'zh' ? '天' : 'days' }}</strong></span>
          </div>
          <div class="text-emerald-600 font-bold">
            {{ t('history.estDeficit') }}: {{ weekDeficitFormatted }}
          </div>
        </div>
      </div>
    </div>

    <!-- 2. Monthly View Mode (Calendar & Heatmap) -->
    <div v-else class="space-y-5 animate-fade-in">
      <div class="glass-card rounded-3xl p-5">
        <!-- Month Header -->
        <div class="flex justify-between items-center mb-4">
          <div class="flex items-center gap-2">
            <Calendar class="w-4 h-4 text-emerald-500" />
            <h2 class="text-sm font-bold text-slate-800">{{ currentMonthTitle }}</h2>
          </div>
          <span class="text-xs font-semibold text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full">
            {{ monthConsistency }}% {{ t('history.consistency') }}
          </span>
        </div>

        <!-- Monthly 3-Stat Metric Cards -->
        <div class="grid grid-cols-3 gap-2 mb-4">
          <div class="bg-slate-50 p-2 rounded-xl text-center border border-slate-100">
            <div class="text-[10px] text-slate-400 font-medium">{{ t('history.daysLogged') }}</div>
            <div class="text-xs font-bold text-slate-800 mt-0.5">{{ monthDaysLogged }} / {{ monthTotalDays }}</div>
          </div>
          <div class="bg-slate-50 p-2 rounded-xl text-center border border-slate-100">
            <div class="text-[10px] text-slate-400 font-medium">{{ t('history.average30d') }}</div>
            <div class="text-xs font-bold text-slate-800 mt-0.5">{{ monthAvgIntake }} kcal</div>
          </div>
          <div class="bg-slate-50 p-2 rounded-xl text-center border border-slate-100">
            <div class="text-[10px] text-slate-400 font-medium">{{ t('history.estDeficit') }}</div>
            <div class="text-xs font-bold text-emerald-600 mt-0.5">{{ monthDeficitFormatted }}</div>
          </div>
        </div>

        <!-- Weekday Labels -->
        <div class="grid grid-cols-7 gap-1 text-center text-[10px] font-bold text-slate-400 mb-2">
          <span v-for="w in (authStore.lang === 'zh' ? ['一','二','三','四','五','六','日'] : ['M','T','W','T','F','S','S'])" :key="w">
            {{ w }}
          </span>
        </div>

        <!-- Month Calendar Grid -->
        <div class="grid grid-cols-7 gap-1.5 text-center">
          <div
            v-for="day in monthDays"
            :key="day.day"
            @click="selectMonthDay(day)"
            class="aspect-square rounded-xl flex flex-col items-center justify-center text-xs transition-all cursor-pointer relative border"
            :class="[
              selectedDayNum === day.day
                ? 'border-emerald-500 ring-2 ring-emerald-500/20 shadow-sm font-bold'
                : 'border-transparent hover:border-slate-200',
              day.status === 'success' ? 'bg-emerald-50 text-emerald-800' :
              day.status === 'warning' ? 'bg-amber-50 text-amber-800' :
              day.status === 'empty' ? 'bg-slate-50 text-slate-300' : 'bg-white text-slate-700'
            ]"
          >
            <span class="text-[11px] leading-none">{{ day.day }}</span>
            <span
              v-if="day.status !== 'empty'"
              class="w-1.5 h-1.5 rounded-full mt-1"
              :class="day.status === 'success' ? 'bg-emerald-500' : 'bg-amber-500'"
            ></span>
          </div>
        </div>

        <!-- Legend -->
        <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-center gap-4 text-[11px] text-slate-400">
          <div class="flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>{{ t('history.targetMet') }} (&lt;{{ targetCalDisplay }})</span>
          </div>
          <div class="flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-amber-500"></span>
            <span>{{ t('history.targetOver') }} (&gt;{{ targetCalDisplay }})</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Selected Date & Meal Records List -->
    <div class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-sm font-bold text-slate-800">
          {{ selectedDateLabel }}
        </h2>
        <span class="text-xs text-slate-400">
          {{ currentDisplayRecords.length }} {{ authStore.lang === 'zh' ? '条记录' : 'meals' }}
        </span>
      </div>

      <!-- Empty State -->
      <div
        v-if="currentDisplayRecords.length === 0"
        class="glass-card rounded-2xl p-6 text-center border-dashed border-2 border-slate-200/80 bg-white/60"
      >
        <div class="w-10 h-10 mx-auto rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mb-2.5">
          <UtensilsCrossed class="w-5 h-5" />
        </div>
        <div class="text-sm font-bold text-slate-700 mb-1">
          {{ authStore.lang === 'zh' ? '暂无餐饮记录' : 'No meal records' }}
        </div>
        <p class="text-xs text-slate-400">
          {{ authStore.lang === 'zh' ? '该日期无打卡数据，去记录一餐开启健康生活' : 'No diet records logged for this day' }}
        </p>
      </div>

      <!-- Meals List -->
      <div
        v-else
        v-for="item in currentDisplayRecords"
        :key="item.id"
        class="glass-card rounded-2xl p-4 flex items-center justify-between hover:shadow-md transition-shadow"
      >
        <div class="flex items-center gap-3">
          <img :src="item.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300'" class="w-12 h-12 rounded-xl object-cover bg-slate-100 shrink-0" />
          <div>
            <div class="text-sm font-bold text-slate-800">{{ item.name || item.dishName || (authStore.lang === 'zh' ? '营养健康餐' : 'Nutritious Meal') }}</div>
            <div class="text-xs text-slate-400">{{ item.mealTime || (authStore.lang === 'zh' ? '今日' : 'Today') }} • {{ item.protein ?? item.totalProtein ?? 0 }}g {{ authStore.lang === 'zh' ? '蛋白质' : 'Protein' }}</div>
          </div>
        </div>
        <div class="text-right">
          <div class="text-sm font-black text-slate-900">{{ item.calories ?? item.totalCalories ?? 0 }}</div>
          <div class="text-[10px] text-slate-400">{{ authStore.lang === 'zh' ? '千卡' : 'kcal' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Calendar, UtensilsCrossed } from 'lucide-vue-next'
import { useAuthStore } from '../stores/authStore'
import { useDietStore, normalizeDietRecord } from '../stores/dietStore'
import { useI18n } from '../i18n'
import client from '../api/client'

const authStore = useAuthStore()
const dietStore = useDietStore()
const { t } = useI18n()

// View Mode State: 'weekly' | 'monthly'
const viewMode = ref('weekly')
const today = new Date()
const selectedDayNum = ref(today.getDate())
const isViewingToday = ref(true)
const selectedCustomTitle = ref('')
const selectedDateRecords = ref([])

const targetCalDisplay = computed(() => dietStore.targetCalories || 2150)

// Weekly Dashboard Data
const weekAvgIntake = ref(0)
const weekTargetMetDays = ref(0)
const weekDeficitFormatted = ref('0 kcal')
const weekStats = ref([])

// Monthly Dashboard Data
const monthConsistency = ref(0)
const monthDaysLogged = ref(0)
const monthTotalDays = ref(30)
const monthAvgIntake = ref(0)
const monthDeficitFormatted = ref('0 kcal')
const monthDays = ref([])

const currentMonthTitle = computed(() => {
  const options = { year: 'numeric', month: 'long' }
  const locale = authStore.lang === 'zh' ? 'zh-CN' : 'en-US'
  return today.toLocaleDateString(locale, options)
})

const selectedDateLabel = computed(() => {
  if (isViewingToday.value) {
    return authStore.lang === 'zh' ? '今日餐次明细' : "Today's Meal Details"
  }
  if (selectedCustomTitle.value) {
    return selectedCustomTitle.value
  }
  return authStore.lang === 'zh'
    ? `${selectedDayNum.value}日 餐饮记录`
    : `Day ${selectedDayNum.value} Records`
})

const currentDisplayRecords = computed(() => {
  if (isViewingToday.value) {
    return dietStore.records
  }
  return selectedDateRecords.value
})

const initWeekStats = () => {
  const days = [
    { label: 'Mon', labelZh: '周一' },
    { label: 'Tue', labelZh: '周二' },
    { label: 'Wed', labelZh: '周三' },
    { label: 'Thu', labelZh: '周四' },
    { label: 'Fri', labelZh: '周五' },
    { label: 'Sat', labelZh: '周六' },
    { label: 'Sun', labelZh: '周日' }
  ]
  const currentDayIndex = (today.getDay() + 6) % 7 // Monday = 0
  weekStats.value = days.map((d, i) => {
    const isT = i === currentDayIndex
    return {
      ...d,
      labelLong: d.label,
      calories: isT && dietStore.consumedCalories > 0 ? `${dietStore.consumedCalories} kcal` : '0 kcal',
      caloriesNum: isT ? dietStore.consumedCalories : 0,
      percent: isT && targetCalDisplay.value > 0 ? Math.min(100, Math.round((dietStore.consumedCalories / targetCalDisplay.value) * 100)) : 0,
      isToday: isT
    }
  })
}

const initMonthDays = () => {
  const year = today.getFullYear()
  const month = today.getMonth()
  const totalDays = new Date(year, month + 1, 0).getDate()
  monthTotalDays.value = totalDays

  monthDays.value = Array.from({ length: totalDays }, (_, i) => {
    const day = i + 1
    const isT = day === today.getDate()
    let status = 'empty'
    if (isT && dietStore.consumedCalories > 0) {
      status = dietStore.consumedCalories <= targetCalDisplay.value ? 'success' : 'warning'
    }
    return { day, status, isToday: isT }
  })
}

const loadHistoryData = async () => {
  initWeekStats()
  initMonthDays()

  const userId = authStore.userId || 1
  try {
    const weekRes = await client.get('/diet/week-dashboard', { params: { userId } })
    if (weekRes) {
      weekAvgIntake.value = weekRes.avgDailyIntake || 0
      weekDeficitFormatted.value = `${(weekRes.accumulatedDeficit || 0) >= 0 ? '-' : '+'}${Math.abs(weekRes.accumulatedDeficit || 0)} kcal`
      if (weekRes.dailyDetails && Array.isArray(weekRes.dailyDetails)) {
        const labelsEn = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        const labelsZh = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        let metCount = 0
        weekStats.value = weekRes.dailyDetails.map((item, idx) => {
          const intake = item.intake || 0
          const target = item.target || targetCalDisplay.value
          if (intake > 0 && intake <= target) metCount++
          return {
            label: labelsEn[idx] || item.dayName,
            labelZh: labelsZh[idx] || item.dayName,
            labelLong: item.date || labelsEn[idx],
            calories: `${intake} kcal`,
            caloriesNum: intake,
            percent: target > 0 ? Math.min(100, Math.round((intake / target) * 100)) : 0,
            isToday: item.isToday || false,
            date: item.date
          }
        })
        weekTargetMetDays.value = metCount
      }
    }
  } catch (e) {
    console.warn('Week dashboard API unavailable:', e.message)
  }

  try {
    const monthRes = await client.get('/diet/month-dashboard', { params: { userId } })
    if (monthRes) {
      monthConsistency.value = monthRes.checkinRate || 0
      monthDaysLogged.value = monthRes.checkinCount || 0
      monthAvgIntake.value = monthRes.avgDailyIntake || 0
      monthDeficitFormatted.value = `${(monthRes.accumulatedDeficit || 0) >= 0 ? '-' : '+'}${Math.abs(monthRes.accumulatedDeficit || 0)} kcal`
      if (monthRes.daysInMonth) monthTotalDays.value = monthRes.daysInMonth
    }
  } catch (e) {
    console.warn('Month dashboard API unavailable:', e.message)
  }
}

const selectWeekDay = async (day) => {
  if (day.isToday) {
    isViewingToday.value = true
    selectedCustomTitle.value = ''
    selectedDateRecords.value = dietStore.records
    return
  }
  isViewingToday.value = false
  selectedCustomTitle.value = authStore.lang === 'zh' ? `${day.labelZh} 餐饮记录` : `${day.label} Records`
  if (day.date) {
    try {
      const res = await client.get('/diet/daily', { params: { userId: authStore.userId || 1, date: day.date } })
      selectedDateRecords.value = (res || []).map(normalizeDietRecord)
    } catch (e) {
      selectedDateRecords.value = []
    }
  } else {
    selectedDateRecords.value = []
  }
}

const selectMonthDay = async (day) => {
  selectedDayNum.value = day.day
  if (day.day === today.getDate()) {
    isViewingToday.value = true
    selectedCustomTitle.value = ''
    selectedDateRecords.value = dietStore.records
    return
  }
  isViewingToday.value = false
  selectedCustomTitle.value = authStore.lang === 'zh' ? `${day.day}日 餐饮记录` : `Day ${day.day} Records`
  const y = today.getFullYear()
  const m = String(today.getMonth() + 1).padStart(2, '0')
  const d = String(day.day).padStart(2, '0')
  const dateStr = `${y}-${m}-${d}`
  try {
    const res = await client.get('/diet/daily', { params: { userId: authStore.userId || 1, date: dateStr } })
    selectedDateRecords.value = (res || []).map(normalizeDietRecord)
  } catch (e) {
    selectedDateRecords.value = []
  }
}

onMounted(async () => {
  await dietStore.fetchTodaySummary(authStore.userId)
  await loadHistoryData()
})
</script>

