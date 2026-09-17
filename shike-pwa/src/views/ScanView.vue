<template>
  <div class="min-h-screen pb-28 px-4 pt-6 max-w-md mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <button
        @click="$router.back()"
        class="w-9 h-9 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 active:scale-95 transition-all"
        title="返回"
      >
        <ArrowLeft class="w-5 h-5" />
      </button>
      <div class="text-center">
        <h1 class="text-base font-bold text-slate-900">{{ t('scan.title') }}</h1>
      </div>
      <div class="flex items-center">
        <span
          v-if="authStore.isVip"
          class="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500 text-white shadow-xs"
        >
          PRO ✨
        </span>
        <span
          v-else
          class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600"
        >
          {{ authStore.dailyScansRemaining }} 次可用
        </span>
      </div>
    </div>

    <!-- Mode Selection: Camera vs Photo Album (when no preview) -->
    <div v-if="!previewUrl" class="space-y-4">
      <!-- AI Engine Badge -->
      <div class="flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-full bg-emerald-50 border border-emerald-100/80 text-emerald-700 text-xs font-semibold mx-auto w-fit">
        <Sparkles class="w-3.5 h-3.5 text-emerald-500" />
        <span>{{ t('scan.multimodal') }}</span>
      </div>

      <!-- Main Action Options Container -->
      <div
        class="space-y-3"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <!-- 1. Live Camera Capture Button -->
        <div
          @click="triggerCamera"
          class="glass-card rounded-3xl p-5 border-2 border-emerald-500/30 hover:border-emerald-500 bg-gradient-to-br from-emerald-50/50 via-white to-emerald-50/20 shadow-sm hover:shadow-md transition-all cursor-pointer group active:scale-[0.99] flex items-center gap-4"
        >
          <div class="w-14 h-14 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/30 group-hover:scale-105 transition-transform shrink-0">
            <Camera class="w-7 h-7" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
              <h2 class="text-base font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
                {{ t('scan.takePhoto') }}
              </h2>
              <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                推荐
              </span>
            </div>
            <p class="text-xs text-slate-500 mt-0.5 leading-snug">
              {{ t('scan.takePhotoDesc') }}
            </p>
          </div>
          <ChevronRight class="w-5 h-5 text-slate-300 group-hover:text-emerald-500 group-hover:translate-x-0.5 transition-all shrink-0" />
        </div>

        <!-- 2. Choose from Photo Album Button -->
        <div
          @click="triggerAlbum"
          class="glass-card rounded-3xl p-5 border-2 border-slate-200 hover:border-emerald-400 bg-white shadow-sm hover:shadow-md transition-all cursor-pointer group active:scale-[0.99] flex items-center gap-4"
        >
          <div class="w-14 h-14 rounded-2xl bg-slate-100 text-slate-700 group-hover:bg-emerald-50 group-hover:text-emerald-600 flex items-center justify-center transition-colors shrink-0">
            <ImageIcon class="w-7 h-7" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
              <h2 class="text-base font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
                {{ t('scan.chooseAlbum') }}
              </h2>
              <span class="text-[10px] font-medium px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                相册选图
              </span>
            </div>
            <p class="text-xs text-slate-500 mt-0.5 leading-snug">
              {{ t('scan.chooseAlbumDesc') }}
            </p>
          </div>
          <ChevronRight class="w-5 h-5 text-slate-300 group-hover:text-emerald-500 group-hover:translate-x-0.5 transition-all shrink-0" />
        </div>

        <!-- Drag & Drop Zone Hint (Desktop & Web Friendly) -->
        <div
          class="rounded-2xl p-3 border border-dashed text-center transition-colors"
          :class="isDragging ? 'border-emerald-500 bg-emerald-50/50' : 'border-slate-200 bg-slate-50/50'"
        >
          <p class="text-[11px] text-slate-400 flex items-center justify-center gap-1.5">
            <UploadCloud class="w-3.5 h-3.5 text-slate-400" />
            <span>{{ isDragging ? '松开鼠标即可载入图片' : t('scan.dragDropHint') }}</span>
          </p>
        </div>
      </div>

      <!-- Quick Test Demo Dishes Tray -->
      <div class="pt-2">
        <div class="flex items-center justify-between mb-2.5">
          <span class="text-xs font-bold text-slate-600 flex items-center gap-1">
            <Utensils class="w-3.5 h-3.5 text-emerald-500" />
            <span>{{ t('scan.sampleTry') }}</span>
          </span>
          <span class="text-[10px] text-slate-400 font-medium">免拍直测</span>
        </div>
        <div class="grid grid-cols-3 gap-2">
          <button
            v-for="(dish, index) in sampleDishes"
            :key="index"
            @click="loadSampleDish(dish, index)"
            class="p-2.5 rounded-2xl bg-white border border-slate-100 hover:border-emerald-300 shadow-xs hover:shadow-sm text-left transition-all active:scale-95 group flex flex-col justify-between"
          >
            <div class="aspect-video w-full rounded-xl overflow-hidden bg-slate-100 mb-1.5">
              <img :src="dish.imageUrl" :alt="getSampleDishName(index)" class="w-full h-full object-cover group-hover:scale-105 transition-transform" />
            </div>
            <div class="text-[11px] font-bold text-slate-800 line-clamp-1 group-hover:text-emerald-600">
              {{ getSampleDishName(index) }}
            </div>
            <div class="text-[10px] font-semibold text-emerald-600 mt-0.5">
              ~{{ dish.calories }} kcal
            </div>
          </button>
        </div>
      </div>

      <!-- Hidden Native File Inputs -->
      <!-- 1. Dedicated Camera Input (Opens rear camera directly on mobile) -->
      <input
        ref="cameraInput"
        type="file"
        accept="image/*"
        capture="environment"
        class="hidden"
        @change="handleFileSelected"
      />

      <!-- 2. Dedicated Album / Gallery Input (Opens system photo gallery / album without forcing camera) -->
      <input
        ref="albumInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="handleFileSelected"
      />
    </div>

    <!-- Live Preview & Scanning State -->
    <div v-else class="space-y-5">
      <div class="relative rounded-3xl overflow-hidden shadow-xl bg-slate-900 aspect-square flex items-center justify-center">
        <img
          :src="previewUrl"
          alt="Dish preview"
          class="w-full h-full object-cover"
        />

        <!-- Radar Scan Overlay when Loading -->
        <div v-if="dietStore.isLoading" class="absolute inset-0 bg-slate-950/40 backdrop-blur-[2px] flex flex-col items-center justify-center text-white">
          <div class="w-20 h-20 rounded-full border-4 border-emerald-400 border-t-transparent animate-spin mb-4"></div>
          <p class="text-sm font-bold tracking-wide">{{ t('scan.analyzing') }}</p>
          <p class="text-xs text-slate-300 mt-1">{{ t('scan.analyzingSub') }}</p>
        </div>

        <!-- Reselect Buttons Group (Camera & Album) -->
        <div
          v-if="!dietStore.isLoading"
          class="absolute top-4 right-4 flex items-center gap-1.5"
        >
          <button
            @click="triggerCamera"
            class="px-3 py-1.5 rounded-full bg-black/60 backdrop-blur-md text-white text-xs font-semibold flex items-center gap-1 hover:bg-black/80 transition-all active:scale-95"
            :title="t('scan.retakeCamera')"
          >
            <Camera class="w-3.5 h-3.5" />
            <span>{{ t('scan.retakeCamera') }}</span>
          </button>
          <button
            @click="triggerAlbum"
            class="px-3 py-1.5 rounded-full bg-black/60 backdrop-blur-md text-white text-xs font-semibold flex items-center gap-1 hover:bg-black/80 transition-all active:scale-95"
            :title="t('scan.retakeAlbum')"
          >
            <ImageIcon class="w-3.5 h-3.5" />
            <span>{{ t('scan.retakeAlbum') }}</span>
          </button>
        </div>
      </div>

      <!-- Analysis Result Card -->
      <div v-if="scanResult && !dietStore.isLoading" class="glass-card rounded-3xl p-5 space-y-4 animate-slide-up">
        <div class="flex items-start justify-between">
          <div>
            <span class="text-[11px] font-bold uppercase tracking-wider text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
              {{ t('scan.confidence', { n: Math.round((scanResult.confidence || 0.95) * 100) }) }}
            </span>
            <h2 class="text-xl font-black text-slate-900 mt-1">{{ scanResult.dishName }}</h2>
          </div>
          <div class="text-right">
            <div class="text-3xl font-black text-slate-900 leading-none">
              {{ adjustedCalories }}
            </div>
            <span class="text-xs text-slate-400 font-semibold">{{ t('scan.calories') }}</span>
          </div>
        </div>

        <!-- Macros Breakdown Pill -->
        <div class="grid grid-cols-4 gap-2 text-center py-3 px-2 bg-slate-50 rounded-2xl border border-slate-100 text-xs">
          <div>
            <div class="text-slate-400 text-[10px] font-medium">{{ t('scan.protein') }}</div>
            <div class="font-bold text-slate-800 text-sm mt-0.5">{{ scanResult.protein }}g</div>
          </div>
          <div>
            <div class="text-slate-400 text-[10px] font-medium">{{ t('scan.netCarbs') }}</div>
            <div class="font-bold text-slate-800 text-sm mt-0.5">{{ Math.round(scanResult.netCarbs || scanResult.carbs) }}g</div>
          </div>
          <div>
            <div class="text-slate-400 text-[10px] font-medium">{{ t('scan.fat') }}</div>
            <div class="font-bold text-slate-800 text-sm mt-0.5">{{ Math.round(scanResult.fat) }}g</div>
          </div>
          <div>
            <div class="text-slate-400 text-[10px] font-medium">{{ t('scan.fiber') }}</div>
            <div class="font-bold text-slate-800 text-sm mt-0.5">{{ Math.round(scanResult.fiber || 4) }}g</div>
          </div>
        </div>

        <!-- PRO Exclusive Micronutrient & Anti-Inflammatory Deep Dive Card -->
        <div
          class="rounded-3xl border transition-all overflow-hidden relative"
          :class="authStore.isVip ? 'bg-gradient-to-br from-slate-900 via-slate-800 to-emerald-950 text-white border-emerald-500/40 shadow-md' : 'bg-slate-50/90 border-slate-200/80'"
        >
          <!-- VIP Unlocked View -->
          <div v-if="authStore.isVip" class="p-4 space-y-3.5">
            <!-- Header -->
            <div class="flex items-center justify-between gap-2 pb-2 border-b border-white/10">
              <div class="flex items-center gap-2 min-w-0">
                <span class="px-2 py-0.5 rounded-lg text-[10px] font-black bg-gradient-to-r from-amber-400 to-emerald-400 text-slate-950 uppercase tracking-wider shadow-xs shrink-0">
                  PRO
                </span>
                <h3 class="text-xs font-bold text-slate-100 truncate">
                  {{ t('scan.proMicronutrientsTitle') }}
                </h3>
              </div>
              <span class="px-2.5 py-0.5 rounded-full bg-emerald-500/15 border border-emerald-400/30 text-emerald-300 text-[10px] font-semibold flex items-center gap-1 shrink-0 whitespace-nowrap">
                <ShieldCheck class="w-3 h-3 text-emerald-400" />
                <span>{{ t('scan.proBadge') }}</span>
              </span>
            </div>

            <!-- Na:K Electrolyte & Anti-Bloat Ratio Bar -->
            <div class="bg-white/10 backdrop-blur-md rounded-2xl p-3 border border-white/10">
              <div class="flex items-center justify-between text-[11px] mb-1.5">
                <span class="text-slate-300 flex items-center gap-1">
                  <Droplets class="w-3.5 h-3.5 text-cyan-400" />
                  {{ t('scan.nakRatio') }}
                </span>
                <span class="font-extrabold text-emerald-400">
                  K:Na {{ scanResult.micronutrients?.nakRatio || '2.1' }} : 1.0
                </span>
              </div>
              <div class="w-full bg-white/20 h-1.5 rounded-full overflow-hidden mb-1.5">
                <div
                  class="bg-gradient-to-r from-emerald-400 to-cyan-400 h-full rounded-full transition-all duration-500"
                  :style="{ width: `${Math.min(100, Math.max(20, (scanResult.micronutrients?.nakRatio || 2) * 35))}%` }"
                ></div>
              </div>
              <p class="text-[10px] text-slate-300 leading-snug">
                {{ scanResult.micronutrients?.nakStatus === 'highSodium' ? t('scan.nakHighSodium') : (scanResult.micronutrients?.nakStatus === 'normal' ? t('scan.nakNormal') : t('scan.nakOptimal')) }}
              </p>
            </div>

            <!-- 5 Micronutrient Pillars -->
            <div class="grid grid-cols-3 gap-2 text-center text-xs">
              <!-- Sodium -->
              <div class="bg-white/5 rounded-xl p-2 border border-white/5">
                <div class="text-[10px] text-slate-400 font-medium">{{ t('scan.sodium') }}</div>
                <div class="font-bold text-slate-100 mt-0.5 text-xs">
                  {{ scanResult.micronutrients?.sodium || 360 }} <span class="text-[9px] font-normal text-slate-400">mg</span>
                </div>
                <div class="text-[9px] text-emerald-400 mt-0.5">
                  {{ scanResult.micronutrients?.sodiumDv || 16 }}% DV
                </div>
              </div>

              <!-- Potassium -->
              <div class="bg-white/5 rounded-xl p-2 border border-white/5">
                <div class="text-[10px] text-slate-400 font-medium">{{ t('scan.potassium') }}</div>
                <div class="font-bold text-slate-100 mt-0.5 text-xs">
                  {{ scanResult.micronutrients?.potassium || 720 }} <span class="text-[9px] font-normal text-slate-400">mg</span>
                </div>
                <div class="text-[9px] text-cyan-400 mt-0.5">
                  {{ scanResult.micronutrients?.potassiumDv || 21 }}% DV
                </div>
              </div>

              <!-- Calcium -->
              <div class="bg-white/5 rounded-xl p-2 border border-white/5">
                <div class="text-[10px] text-slate-400 font-medium">{{ t('scan.calcium') }}</div>
                <div class="font-bold text-slate-100 mt-0.5 text-xs">
                  {{ scanResult.micronutrients?.calcium || 190 }} <span class="text-[9px] font-normal text-slate-400">mg</span>
                </div>
                <div class="text-[9px] text-amber-400 mt-0.5">
                  {{ scanResult.micronutrients?.calciumDv || 19 }}% DV
                </div>
              </div>

              <!-- Iron -->
              <div class="bg-white/5 rounded-xl p-2 border border-white/5">
                <div class="text-[10px] text-slate-400 font-medium">{{ t('scan.iron') }}</div>
                <div class="font-bold text-slate-100 mt-0.5 text-xs">
                  {{ scanResult.micronutrients?.iron || 2.4 }} <span class="text-[9px] font-normal text-slate-400">mg</span>
                </div>
                <div class="text-[9px] text-rose-400 mt-0.5">
                  {{ scanResult.micronutrients?.ironDv || 13 }}% DV
                </div>
              </div>

              <!-- Vitamin C & Anti-Inflammatory -->
              <div class="bg-white/5 rounded-xl p-2 border border-white/5 col-span-2">
                <div class="text-[10px] text-slate-400 font-medium">{{ t('scan.vitaminC') }}</div>
                <div class="font-bold text-slate-100 mt-0.5 text-xs">
                  {{ scanResult.micronutrients?.vitaminC || 35 }} <span class="text-[9px] font-normal text-slate-400">mg</span>
                  <span class="text-[9px] text-emerald-400 ml-1">({{ scanResult.micronutrients?.vitaminCDv || 38 }}% DV)</span>
                </div>
                <div class="text-[9px] text-emerald-300 mt-0.5 leading-tight">
                  {{ t('scan.antiInflammatoryTitle') }}: {{ scanResult.micronutrients?.antiInflammatoryScore === 'high' ? t('scan.antiInflammatoryHigh') : t('scan.antiInflammatoryGood') }}
                </div>
              </div>
            </div>
          </div>

          <!-- Free User Gated / Frosted Blur Lock View -->
          <div
            v-else
            class="relative p-5 text-center overflow-hidden cursor-pointer group"
            @click="authStore.openPaywall()"
          >
            <!-- Blurred Simulated Background Elements -->
            <div class="absolute inset-0 filter blur-[3px] opacity-40 select-none pointer-events-none p-4 grid grid-cols-3 gap-2 bg-gradient-to-br from-emerald-50 to-slate-100">
              <div class="bg-white p-2 rounded-xl h-10 shadow-xs"></div>
              <div class="bg-white p-2 rounded-xl h-10 shadow-xs"></div>
              <div class="bg-white p-2 rounded-xl h-10 shadow-xs"></div>
              <div class="bg-white p-2 rounded-xl h-10 shadow-xs"></div>
              <div class="bg-white p-2 rounded-xl h-10 col-span-2 shadow-xs"></div>
            </div>

            <!-- Foreground Lock Overlay -->
            <div class="relative z-10 flex flex-col items-center justify-center py-1">
              <div class="w-10 h-10 rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-400 text-white flex items-center justify-center shadow-md shadow-emerald-500/30 mb-2 group-hover:scale-105 transition-transform">
                <Lock class="w-5 h-5" />
              </div>
              <div class="flex items-center gap-1.5 mb-1">
                <span class="px-2 py-0.5 rounded-full text-[9px] font-extrabold bg-emerald-100 text-emerald-800">
                  PRO EXCLUSIVE
                </span>
                <h3 class="text-xs font-black text-slate-900">
                  {{ t('scan.proMicronutrientsTitle') }}
                </h3>
              </div>
              <p class="text-[11px] text-slate-500 max-w-xs leading-relaxed mb-3">
                {{ t('scan.proLockedHint') }}
              </p>
              <button
                type="button"
                class="py-2 px-4 rounded-full bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-bold text-xs shadow-md shadow-emerald-500/25 active:scale-95 transition-all flex items-center gap-1.5"
              >
                <Sparkles class="w-3.5 h-3.5 text-amber-300" />
                <span>{{ t('scan.unlockProBtn') }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Cooking Oil & Sauce Calibrator -->
        <div class="pt-2">
          <div class="flex justify-between items-center text-xs mb-2">
            <span class="font-bold text-slate-700">{{ t('scan.oilLevelTitle') }}</span>
            <span class="font-semibold text-emerald-600">{{ oilLabel }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="level in ['LIGHT', 'NORMAL', 'HEAVY']"
              :key="level"
              @click="selectedOil = level"
              class="py-2 px-3 rounded-xl text-xs font-bold transition-all border"
              :class="selectedOil === level ? 'bg-emerald-500 text-white border-emerald-500 shadow-sm' : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300'"
            >
              {{ level === 'LIGHT' ? t('scan.light') : level === 'NORMAL' ? t('scan.standard') : t('scan.heavy') }}
            </button>
          </div>
          <p class="text-[11px] text-slate-400 mt-2">
            {{ t('scan.oilDesc') }}
          </p>
        </div>

        <!-- AI Nutrition Insight -->
        <div class="p-3 bg-emerald-50/50 rounded-2xl border border-emerald-100/60 text-xs text-slate-600 flex items-start gap-2">
          <Info class="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
          <p>{{ scanResult.advice }}</p>
        </div>

        <!-- Save Button -->
        <button
          @click="saveMeal"
          class="w-full py-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] text-white font-bold text-base shadow-lg shadow-emerald-500/25 flex items-center justify-center gap-2 transition-all"
        >
          <Check class="w-5 h-5" />
          <span>{{ t('scan.logToDiary') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Camera,
  ImageIcon,
  Sparkles,
  RotateCcw,
  Info,
  Check,
  UploadCloud,
  ChevronRight,
  Utensils,
  Lock,
  ShieldCheck,
  Droplets
} from 'lucide-vue-next'
import confetti from 'canvas-confetti'
import { useAuthStore } from '../stores/authStore'
import { useDietStore } from '../stores/dietStore'
import { useI18n } from '../i18n'

const router = useRouter()
const authStore = useAuthStore()
const dietStore = useDietStore()
const { t } = useI18n()

const cameraInput = ref(null)
const albumInput = ref(null)
const previewUrl = ref(null)
const scanResult = ref(null)
const selectedOil = ref('NORMAL')
const isDragging = ref(false)

// Sample demo dishes for instant testing
const sampleDishes = [
  {
    nameZh: '鲜虾藜麦沙拉',
    nameEn: 'Shrimp Quinoa Salad',
    tagZh: '🥗 鲜虾藜麦',
    tagEn: '🥗 Shrimp Salad',
    calories: 410,
    protein: 36,
    carbs: 32,
    fat: 12,
    netCarbs: 26,
    fiber: 6,
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600'
  },
  {
    nameZh: '慢烤安格斯牛排配芦笋',
    nameEn: 'Grilled Angus Steak',
    tagZh: '🥩 慢烤牛排',
    tagEn: '🥩 Angus Steak',
    calories: 540,
    protein: 52,
    carbs: 8,
    fat: 26,
    netCarbs: 5,
    fiber: 3,
    imageUrl: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=600'
  },
  {
    nameZh: '挪威三文鱼牛油果波奇饭',
    nameEn: 'Salmon Poke Bowl',
    tagZh: '🥑 三文鱼波奇',
    tagEn: '🥑 Salmon Poke',
    calories: 470,
    protein: 38,
    carbs: 45,
    fat: 16,
    netCarbs: 39,
    fiber: 6,
    imageUrl: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600'
  }
]

const triggerCamera = () => {
  if (!authStore.consumeScan()) return
  if (cameraInput.value) {
    cameraInput.value.value = ''
    cameraInput.value.click()
  }
}

const triggerAlbum = () => {
  if (!authStore.consumeScan()) return
  if (albumInput.value) {
    albumInput.value.value = ''
    albumInput.value.click()
  }
}

const compressImageFile = (file, maxWidth = 1280, quality = 0.82) => {
  return new Promise((resolve) => {
    if (!file || !file.type.startsWith('image/') || file.size < 200 * 1024) {
      resolve(file)
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let width = img.width
        let height = img.height
        if (width > maxWidth || height > maxWidth) {
          if (width > height) {
            height = Math.round((height * maxWidth) / width)
            width = maxWidth
          } else {
            width = Math.round((width * maxWidth) / height)
            height = maxWidth
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob(
          (blob) => {
            if (blob) {
              const compressed = new File([blob], file.name.replace(/\.[^/.]+$/, '.jpg'), {
                type: 'image/jpeg',
                lastModified: Date.now()
              })
              resolve(compressed)
            } else {
              resolve(file)
            }
          },
          'image/jpeg',
          quality
        )
      }
      img.onerror = () => resolve(file)
      img.src = e.target.result
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}

const processFile = async (file) => {
  if (!file) return
  previewUrl.value = URL.createObjectURL(file)
  const readyFile = await compressImageFile(file)
  const result = await dietStore.analyzeMealImage(readyFile, selectedOil.value)
  scanResult.value = result
}

const handleFileSelected = async (e) => {
  const file = e.target.files?.[0]
  if (file) {
    await processFile(file)
  }
}

const handleDrop = async (e) => {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    if (!authStore.consumeScan()) return
    await processFile(file)
  }
}

const getSampleDishName = (index) => {
  if (index === 0) return t('scan.sampleSalad')
  if (index === 1) return t('scan.sampleSteak')
  if (index === 2) return t('scan.sampleSalmon')
  return ''
}

const loadSampleDish = async (dish, index) => {
  if (!authStore.consumeScan()) return
  previewUrl.value = dish.imageUrl
  // Create dummy image file for backend API/analyzer
  const dummyFile = new File([new Blob()], `sample_${index}.jpg`, { type: 'image/jpeg' })
  const res = await dietStore.analyzeMealImage(dummyFile, selectedOil.value)
  if (res) {
    res.dishName = getSampleDishName(index)
    res.calories = dish.calories
    res.protein = dish.protein
    res.carbs = dish.carbs
    res.fat = dish.fat
    res.netCarbs = dish.netCarbs
    res.fiber = dish.fiber
  }
  scanResult.value = res
}

const adjustedCalories = computed(() => {
  if (!scanResult.value) return 0
  const base = scanResult.value.calories
  if (selectedOil.value === 'LIGHT') return Math.round(base * 0.9)
  if (selectedOil.value === 'HEAVY') return Math.round(base * 1.15)
  return base
})

const oilLabel = computed(() => {
  if (selectedOil.value === 'LIGHT') return t('scan.light')
  if (selectedOil.value === 'HEAVY') return t('scan.heavy')
  return t('scan.standard')
})

const resetScan = () => {
  previewUrl.value = null
  scanResult.value = null
  selectedOil.value = 'NORMAL'
}

const saveMeal = async () => {
  confetti({
    particleCount: 50,
    spread: 60,
    origin: { y: 0.7 }
  })
  await dietStore.saveMealRecord({
    dishName: scanResult.value.dishName,
    calories: adjustedCalories.value,
    protein: scanResult.value.protein,
    carbs: scanResult.value.netCarbs || scanResult.value.carbs,
    fat: scanResult.value.fat,
    imageUrl: previewUrl.value,
    oilModifier: selectedOil.value,
    foodItems: scanResult.value.foodItems
  })
  router.push('/')
}
</script>
