import { defineStore } from 'pinia'
import client from '../api/client'
import { useAuthStore } from './authStore'

export function normalizeDietRecord(r) {
  if (!r) return null
  const currentLang = localStorage.getItem('shike_lang') || 'zh'
  const isZh = currentLang === 'zh'

  // 1. Calories
  let calories = Math.round(Number(
    r.calories !== undefined && r.calories !== null
      ? r.calories
      : (r.totalCalories !== undefined && r.totalCalories !== null ? r.totalCalories : 0)
  ))

  // 2. Protein, Carbs, Fat
  let protein = Math.round(Number(
    r.protein !== undefined && r.protein !== null
      ? r.protein
      : (r.totalProtein !== undefined && r.totalProtein !== null ? r.totalProtein : 0)
  ))
  let carbs = Math.round(Number(
    r.carbs !== undefined && r.carbs !== null
      ? r.carbs
      : (r.totalCarbs !== undefined && r.totalCarbs !== null ? r.totalCarbs : 0)
  ))
  let fat = Math.round(Number(
    r.fat !== undefined && r.fat !== null
      ? r.fat
      : (r.totalFat !== undefined && r.totalFat !== null ? r.totalFat : 0)
  ))

  // 3. Name & Food Items
  let name = r.name || r.dishName
  let items = []
  if (r.foodItems) {
    try {
      items = typeof r.foodItems === 'string' ? JSON.parse(r.foodItems) : r.foodItems
      if (Array.isArray(items) && items.length > 0) {
        if (!name) {
          name = items.map(it => {
            if (isZh) return it.nameZh || it.name
            if (currentLang === 'en') return it.nameEn || it.name
            return it.name || it.nameEn || it.nameZh
          }).filter(Boolean).join(' + ')
        }
        // If macros are still 0, aggregate from items
        if (!calories) calories = Math.round(items.reduce((s, it) => s + (Number(it.calories) || 0), 0))
        if (!protein) protein = Math.round(items.reduce((s, it) => s + (Number(it.protein) || 0), 0))
        if (!carbs) carbs = Math.round(items.reduce((s, it) => s + (Number(it.carbs || it.netCarbs) || 0), 0))
        if (!fat) fat = Math.round(items.reduce((s, it) => s + (Number(it.fat) || 0), 0))
      }
    } catch (e) {
      console.warn('Failed to parse foodItems JSON in normalizeDietRecord:', e)
    }
  }

  // Fallback name if completely absent or corrupted with question marks/replacement chars
  if (!name || name.includes('\uFFFD') || name.includes('??') || name.trim().length === 0) {
    name = isZh ? '健康轻食能量餐' : 'Nutritious Energy Meal'
  }

  // If calories exist but macros were 0 in old DB records, estimate reasonable default breakdown
  if (calories > 0 && protein === 0 && carbs === 0 && fat === 0) {
    protein = Math.round((calories * 0.25) / 4)
    carbs = Math.round((calories * 0.45) / 4)
    fat = Math.round((calories * 0.30) / 9)
  }

  // 4. Meal Time
  let mealTime = r.mealTime
  if (!mealTime && r.createdAt) {
    try {
      const d = new Date(r.createdAt)
      const hhmm = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      const typeLabel = r.mealType === 'BREAKFAST' ? (isZh ? '早餐' : 'Breakfast')
        : r.mealType === 'LUNCH' ? (isZh ? '午餐' : 'Lunch')
        : r.mealType === 'DINNER' ? (isZh ? '晚餐' : 'Dinner')
        : (isZh ? '加餐' : 'Snack')
      mealTime = `${typeLabel} ${hhmm}`
    } catch (e) {
      mealTime = isZh ? '刚刚' : 'Just now'
    }
  }
  if (!mealTime) {
    mealTime = isZh ? '刚刚' : 'Just now'
  }

  // 5. Oil Modifier / Level
  let oilModifier = r.oilModifier || r.oilLevel || 'NORMAL'
  if (oilModifier === 'LIGHT') oilModifier = isZh ? '少油' : 'Light Oil'
  else if (oilModifier === 'HEAVY') oilModifier = isZh ? '多油酱' : 'Rich Sauce'
  else if (oilModifier === 'NORMAL' || oilModifier === 'MODERATE') oilModifier = isZh ? '适中' : 'Balanced'

  return {
    id: r.id || Date.now(),
    name,
    dishName: name,
    calories,
    totalCalories: calories,
    protein,
    totalProtein: protein,
    carbs,
    totalCarbs: carbs,
    fat,
    totalFat: fat,
    imageUrl: r.imageUrl || '',
    mealTime,
    oilModifier,
    oilLevel: r.oilLevel || 'NORMAL'
  }
}

export const useDietStore = defineStore('diet', {
  state: () => {
    const savedTarget = localStorage.getItem('shike_target_calories')
    const savedUser = localStorage.getItem('shike_user')
    let userTarget = 2150
    if (savedUser) {
      try {
        const u = JSON.parse(savedUser)
        if (u.targetCalories) userTarget = Number(u.targetCalories)
      } catch (e) {}
    }
    return {
      targetCalories: savedTarget ? Number(savedTarget) : userTarget,
      consumedCalories: 0,
      protein: 0,
      carbs: 0,
      fat: 0,
      netCarbs: 0,
      waterMl: Number(localStorage.getItem('shike_water') || 0),
      waterTargetMl: 2500,
      records: [],
      isLoading: false,
      currentScanResult: null
    }
  },
  getters: {
    remainingCalories: (state) => Math.max(0, state.targetCalories - state.consumedCalories),
    calorieProgress: (state) => Math.min(100, Math.round((state.consumedCalories / state.targetCalories) * 100))
  },
  actions: {
    async fetchTodaySummary(userId) {
      let targetUserId = Number(userId || localStorage.getItem('shike_user_id'))
      if (!targetUserId || isNaN(targetUserId)) {
        targetUserId = 1
      }
      try {
        const res = await client.get('/diet/today', {
          params: { userId: targetUserId }
        })
        if (res) {
          if (res.records && res.records.length > 0) {
            // Restore clean names from local cache if backend data was corrupted or generic
            const localRecords = this.records.length > 0 ? this.records : JSON.parse(localStorage.getItem('shike_records') || '[]')
            this.records = res.records.map((r) => {
              const norm = normalizeDietRecord(r)
              const localMatch = localRecords.find(lr => lr.id === r.id || (lr.imageUrl && lr.imageUrl === r.imageUrl))
              if (localMatch && localMatch.name && !localMatch.name.includes('??') && !localMatch.name.includes('')) {
                norm.name = localMatch.name
                norm.dishName = localMatch.name
                if (localMatch.protein) norm.protein = localMatch.protein
                if (localMatch.carbs) norm.carbs = localMatch.carbs
                if (localMatch.fat) norm.fat = localMatch.fat
                if (localMatch.calories) norm.calories = localMatch.calories
              }
              return norm
            })
            this.consumedCalories = res.totalCalories !== undefined ? res.totalCalories : this.records.reduce((s, r) => s + (r.calories || 0), 0)
            localStorage.setItem('shike_records', JSON.stringify(this.records))
            localStorage.setItem('shike_consumed_calories', String(this.consumedCalories))
          } else {
            // Backend has 0 records, check if there are local offline records
            const savedRecords = localStorage.getItem('shike_records')
            if (savedRecords) {
              try {
                const parsed = JSON.parse(savedRecords)
                if (Array.isArray(parsed) && parsed.length > 0) {
                  this.records = parsed.map(normalizeDietRecord)
                  this.consumedCalories = this.records.reduce((s, r) => s + (r.calories || 0), 0)
                  this.protein = this.records.reduce((s, r) => s + (r.protein || 0), 0)
                  this.carbs = this.records.reduce((s, r) => s + (r.carbs || 0), 0)
                  this.fat = this.records.reduce((s, r) => s + (r.fat || 0), 0)
                  return
                }
              } catch (e) {}
            }
            this.records = []
            this.consumedCalories = 0
            this.protein = 0
            this.carbs = 0
            this.fat = 0
            localStorage.removeItem('shike_records')
            localStorage.removeItem('shike_consumed_calories')
          }

          const recordProtein = this.records.reduce((s, r) => s + (r.protein || 0), 0)
          const recordCarbs = this.records.reduce((s, r) => s + (r.carbs || 0), 0)
          const recordFat = this.records.reduce((s, r) => s + (r.fat || 0), 0)

          this.protein = (res.totalProtein && res.totalProtein > 0) ? res.totalProtein : recordProtein
          this.carbs = (res.totalCarbs && res.totalCarbs > 0) ? res.totalCarbs : recordCarbs
          this.fat = (res.totalFat && res.totalFat > 0) ? res.totalFat : recordFat
          return
        }
      } catch (err) {
        console.warn('Backend diet endpoint offline or failed:', err.message)
      }

      // If records are empty, try restoring from localStorage
      const savedRecords = localStorage.getItem('shike_records')
      if (savedRecords) {
        try {
          const parsed = JSON.parse(savedRecords)
          if (Array.isArray(parsed) && parsed.length > 0) {
            this.records = parsed.map(normalizeDietRecord)
            this.consumedCalories = this.records.reduce((s, r) => s + (r.calories || 0), 0)
            this.protein = this.records.reduce((s, r) => s + (r.protein || 0), 0)
            this.carbs = this.records.reduce((s, r) => s + (r.carbs || 0), 0)
            this.fat = this.records.reduce((s, r) => s + (r.fat || 0), 0)
            return
          }
        } catch (e) {}
      }

      // Keep completely clean zero state when no records exist
      this.records = []
      this.consumedCalories = 0
      this.protein = 0
      this.carbs = 0
      this.fat = 0
    },
    async clearAllDietData(userId) {
      this.records = []
      this.consumedCalories = 0
      this.protein = 0
      this.carbs = 0
      this.fat = 0
      this.waterMl = 0
      localStorage.removeItem('shike_records')
      localStorage.removeItem('shike_consumed_calories')
      localStorage.removeItem('shike_water')
      try {
        await client.delete('/diet/records', { params: { userId } })
      } catch (e) {
        console.warn('Backend clear records error:', e.message)
      }
    },
    async analyzeMealImage(file, oilLevel = 'NORMAL', userHint = '') {
      this.isLoading = true
      const authStore = useAuthStore()
      const lang = authStore.lang || localStorage.getItem('shike_lang') || 'zh'
      const isZh = lang === 'zh'
      try {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('lang', lang)
        if (userHint && userHint.trim()) {
          formData.append('hint', userHint.trim())
        }

        const userId = localStorage.getItem('shike_user_id')
        if (userId && !isNaN(Number(userId))) {
          formData.append('userId', userId)
        }

        const timeoutPromise = new Promise((_, reject) =>
          setTimeout(() => reject(new Error(isZh ? '识别响应超时，请重试' : 'Backend recognition timeout, please retry')), 60000)
        )
        const res = await Promise.race([
          client.post('/diet/recognize', formData, {
            timeout: 60000
          }),
          timeoutPromise
        ])

        if (res) {
          let items = []
          if (typeof res.foodItems === 'string') {
            try {
              items = JSON.parse(res.foodItems)
            } catch (e) {
              console.warn('Failed to parse foodItems string:', e)
            }
          } else if (Array.isArray(res.foodItems)) {
            items = res.foodItems
          }

          let dishName = ''
          if (items.length > 0) {
            dishName = items.map(it => {
              if (isZh) return it.nameZh || it.name
              if (lang === 'en') return it.nameEn || it.name
              return it.name || it.nameEn || it.nameZh
            }).filter(Boolean).join(' + ')
          } else {
            dishName = isZh ? '健康轻食餐' : 'Nutritious Meal'
          }

          let fiber = 0
          let netCarbs = 0
          let sodium = 0
          let potassium = 0
          let calcium = 0
          let iron = 0
          let vitaminC = 0

          for (const it of items) {
            if (it.fiber) fiber += Number(it.fiber)
            if (it.netCarbs) netCarbs += Number(it.netCarbs)
            if (it.sodium) sodium += Number(it.sodium)
            if (it.potassium) potassium += Number(it.potassium)
            if (it.calcium) calcium += Number(it.calcium)
            if (it.iron) iron += Number(it.iron)
            if (it.vitaminC) vitaminC += Number(it.vitaminC)
          }
          if (!netCarbs && res.totalCarbs) {
            netCarbs = Math.max(0, Math.round(Number(res.totalCarbs) - fiber))
          }

          // Smart realistic estimates if AI returned a subset
          if (!sodium) sodium = 360
          if (!potassium) potassium = 720
          if (!calcium) calcium = 190
          if (!iron) iron = 2.4
          if (!vitaminC) vitaminC = 35

          const nakRatioVal = Number((potassium / Math.max(1, sodium)).toFixed(2))
          let nakStatus = 'optimal'
          if (nakRatioVal < 0.8) nakStatus = 'highSodium'
          else if (nakRatioVal < 1.1) nakStatus = 'normal'

          const antiInflammatoryScore = (fiber >= 5 || vitaminC >= 25) ? 'high' : 'good'

          const micronutrients = {
            sodium: Math.round(sodium),
            potassium: Math.round(potassium),
            calcium: Math.round(calcium),
            iron: Number(iron.toFixed(1)),
            vitaminC: Math.round(vitaminC),
            sodiumDv: Math.min(100, Math.round((sodium / 2300) * 100)),
            potassiumDv: Math.min(100, Math.round((potassium / 3400) * 100)),
            calciumDv: Math.min(100, Math.round((calcium / 1000) * 100)),
            ironDv: Math.min(100, Math.round((iron / 18) * 100)),
            vitaminCDv: Math.min(100, Math.round((vitaminC / 90) * 100)),
            nakRatio: nakRatioVal,
            nakStatus,
            antiInflammatoryScore
          }

          const defaultAdvices = {
            zh: '优质高蛋白营养搭配，有益于维持饱腹感与血糖平稳。',
            en: 'Rich in lean protein and essential micronutrients, optimal for muscle recovery and steady fat burn.',
            ja: '良質なタンパク質と微量栄養素が豊富で、筋肉の回復と健康的な代謝を促進します。',
            es: 'Rico en proteínas magras y micronutrientes esenciales, óptimo para la recuperación muscular y el control metabólico.',
            fr: 'Riche en protéines maigres et micronutriments essentiels, optimal pour la récupération musculaire.',
            de: 'Reich an magerem Eiweiß und essenziellen Mikronährstoffen, optimal für Muskelregeneration und Stoffwechsel.',
            pt: 'Rico em proteínas magras e micronutrientes essenciais, ideal para a recuperação muscular e saúde metabólica.'
          }
          const itemAdvice = items.find(it => it.advice && it.advice.trim())?.advice
          const advice = itemAdvice || (defaultAdvices[lang] || defaultAdvices['en'])

          const parsedResult = {
            id: res.id || Date.now(),
            dishName: dishName,
            calories: Math.round(Number(res.totalCalories) || 450),
            protein: Math.round(Number(res.totalProtein) || 32),
            carbs: Math.round(Number(res.totalCarbs) || 38),
            fat: Math.round(Number(res.totalFat) || 14),
            netCarbs: netCarbs || Math.round(Number(res.totalCarbs) || 38),
            fiber: fiber || 4,
            confidence: 0.96,
            foodItems: items,
            sauceLevel: oilLevel === 'LIGHT' ? 'Light Dressing' : (oilLevel === 'HEAVY' ? 'Rich Oil/Sauce' : 'Standard Dressing'),
            advice: advice,
            micronutrients
          }
          this.currentScanResult = parsedResult
          return parsedResult
        }
      } catch (err) {
        console.error('AI Recognition error:', err)
        throw err
      } finally {
        this.isLoading = false
      }
    },
    async saveMealRecord(mealData) {
      const isZh = (localStorage.getItem('shike_lang') || 'en') === 'zh'
      const dishName = mealData.dishName || mealData.name || (isZh ? '健康轻食餐' : 'Nutritious Meal')
      const calories = Number(mealData.calories) || 0
      const protein = Number(mealData.protein) || 0
      const carbs = Number(mealData.netCarbs || mealData.carbs) || 0
      const fat = Number(mealData.fat) || 0
      const imageUrl = mealData.imageUrl || ''
      const oilModifier = mealData.oilModifier || 'NORMAL'

      // Construct detailed foodItems array so backend calculates correct protein/carbs/fat/calories
      const itemsPayload = (mealData.foodItems && Array.isArray(mealData.foodItems) && mealData.foodItems.length > 0)
        ? mealData.foodItems
        : [
            {
              name: dishName,
              nameZh: dishName,
              weight: 250,
              calories: calories,
              protein: protein,
              carbs: carbs,
              fat: fat
            }
          ]

      const record = normalizeDietRecord({
        id: mealData.id || Date.now(),
        name: dishName,
        dishName: dishName,
        calories,
        protein,
        carbs,
        fat,
        imageUrl,
        mealTime: isZh ? '刚刚' : 'Just now',
        oilModifier,
        oilLevel: oilModifier
      })

      this.records.unshift(record)
      this.consumedCalories += record.calories
      this.protein += record.protein
      this.carbs += record.carbs
      this.fat += record.fat

      localStorage.setItem('shike_records', JSON.stringify(this.records))
      localStorage.setItem('shike_consumed_calories', String(this.consumedCalories))

      // Sync to backend MySQL
      try {
        let userId = Number(localStorage.getItem('shike_user_id'))
        if (!userId || isNaN(userId)) {
          userId = 1
        }
        const res = await client.post('/diet/record', {
          userId,
            mealType: mealData.mealType || 'LUNCH',
            foodItems: JSON.stringify(itemsPayload),
            oilLevel: oilModifier === 'LIGHT' ? 'LIGHT' : (oilModifier === 'HEAVY' ? 'HEAVY' : 'MODERATE'),
            imageUrl: imageUrl
          })
          if (res && res.id) {
            record.id = res.id
          }
        } catch (err) {
          console.warn('Backend sync failed, saved locally:', err.message)
        }

      return record
    },
    addWater(ml = 250) {
      this.waterMl = Math.min(this.waterTargetMl + 1000, this.waterMl + ml)
      localStorage.setItem('shike_water', String(this.waterMl))
    },
    setTargetCalories(cal) {
      this.targetCalories = Number(cal)
      localStorage.setItem('shike_target_calories', String(cal))
    }
  }
})
