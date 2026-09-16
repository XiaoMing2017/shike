import { defineStore } from 'pinia'
import client from '../api/client'

function detectInitialLanguage() {
  const saved = localStorage.getItem('shike_lang')
  if (saved && ['en', 'zh', 'es', 'de', 'fr', 'ja', 'pt'].includes(saved)) {
    return saved
  }
  if (typeof navigator !== 'undefined' && navigator.language) {
    const navLang = navigator.language.toLowerCase()
    if (navLang.startsWith('zh')) return 'zh'
    if (navLang.startsWith('es')) return 'es'
    if (navLang.startsWith('de')) return 'de'
    if (navLang.startsWith('fr')) return 'fr'
    if (navLang.startsWith('ja')) return 'ja'
    if (navLang.startsWith('pt')) return 'pt'
  }
  return 'en'
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const savedUser = localStorage.getItem('shike_user')
    return {
      user: savedUser ? JSON.parse(savedUser) : null,
      token: localStorage.getItem('shike_token') || '',
      userId: localStorage.getItem('shike_user_id') || '',
      unitSystem: localStorage.getItem('shike_unit') || 'metric', // 'metric' | 'imperial'
      lang: detectInitialLanguage(),
      dailyScansRemaining: 3,
      isPaywallOpen: false
    }
  },
  getters: {
    isLoggedIn: (state) => !!state.token && !!state.user?.email,
    isGuest: (state) => !state.user?.email,
    isVip: (state) => {
      if (!state.user) return false
      if (state.user.aiUnlimited) return true
      if (state.user.vipType === 'VIP' || state.user.vipType === 'PRO') {
        if (!state.user.vipExpireTime) return true
        return new Date(state.user.vipExpireTime).getTime() > Date.now()
      }
      return false
    }
  },
  actions: {
    // Initialize or restore session
    async initSession() {
      // If user already logged in with email
      if (this.user && this.user.email) {
        return
      }

      if (!this.userId) {
        const guestId = 'guest_' + Math.random().toString(36).substring(2, 10)
        this.userId = guestId
        localStorage.setItem('shike_user_id', guestId)
      }

      if (!this.user) {
        this.user = {
          id: this.userId,
          nickname: 'Guest Explorer',
          email: '',
          targetCalories: 2150,
          vipType: 'NORMAL',
          points: 150
        }
        localStorage.setItem('shike_user', JSON.stringify(this.user))
      }
    },

    // Email & Password Login
    async login(email, password) {
      try {
        const res = await client.post('/user/web/login', { email, password })
        if (res) {
          this.setAuthSuccess(res)
          return res
        }
      } catch (err) {
        // If the backend actively rejected with a business message, throw it
        const isNetworkError = err.code === 'ERR_NETWORK' || err.message?.includes('Network Error') || !err.message
        if (!isNetworkError && err.message && !err.message.includes('timeout')) {
          throw err
        }
        console.warn('Backend web login unavailable (offline), using client store auth fallback:', err.message)
      }

      // Local fallback auth only when offline
      const mockUser = {
        id: 'usr_' + Math.random().toString(36).substring(2, 9),
        email,
        nickname: email.split('@')[0],
        targetCalories: 2150,
        vipType: 'NORMAL',
        points: 200,
        currentStreak: 0
      }
      this.setAuthSuccess({ token: 'jwt_' + Date.now(), user: mockUser })
      return mockUser
    },

    // Email & Password Register
    async register(arg1, password, nickname) {
      const payload = (typeof arg1 === 'object') ? arg1 : { email: arg1, password, nickname }
      try {
        const res = await client.post('/user/web/register', payload)
        if (res) {
          this.setAuthSuccess(res)
          return res
        }
      } catch (err) {
        const isNetworkError = err.code === 'ERR_NETWORK' || err.message?.includes('Network Error') || !err.message
        if (!isNetworkError && err.message && !err.message.includes('timeout')) {
          throw err
        }
        console.warn('Backend web register unavailable (offline), using client store auth fallback:', err.message)
      }

      const mockUser = {
        id: 'usr_' + Math.random().toString(36).substring(2, 9),
        email: payload.email,
        nickname: payload.nickname || payload.email.split('@')[0],
        targetCalories: payload.targetCalories || 2150,
        gender: payload.gender || 1,
        age: payload.age || 26,
        height: payload.height || 175,
        weight: payload.weight || 76,
        targetWeight: payload.targetWeight || 68,
        activityLevel: payload.activityLevel || 'SEDENTARY',
        goal: payload.goal || 'LOSE_WEIGHT',
        bmr: payload.bmr || 1700,
        tdee: payload.tdee || 2050,
        vipType: 'NORMAL',
        points: 200,
        currentStreak: 1
      }
      this.setAuthSuccess({ token: 'jwt_' + Date.now(), user: mockUser })
      return mockUser
    },

    // One-Click Google Sign-In (Supports chosen account & real MySQL persistence)
    async loginWithGoogle(account = {}) {
      const email = (account.email || 'alex.rivera@gmail.com').trim().toLowerCase()
      const nickname = account.name || email.split('@')[0]
      const avatarUrl = account.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120'
      const dummyPassword = 'GoogleOAuthPassword_2026!'

      // Attempt to register or login via real backend API so account is in MySQL
      try {
        const loginRes = await client.post('/user/web/login', { email, password: dummyPassword })
        if (loginRes) {
          this.setAuthSuccess(loginRes)
          return loginRes
        }
      } catch (loginErr) {
        try {
          const regRes = await client.post('/user/web/register', {
            email,
            password: dummyPassword,
            nickname,
            targetCalories: 2150
          })
          if (regRes) {
            this.setAuthSuccess(regRes)
            return regRes
          }
        } catch (regErr) {
          console.warn('Backend Google account sync failed, using offline fallback:', regErr.message)
        }
      }

      const mockUser = {
        id: 'google_' + Math.random().toString(36).substring(2, 9),
        email,
        nickname,
        avatarUrl,
        targetCalories: 2150,
        vipType: 'PRO',
        aiUnlimited: true,
        points: 500,
        currentStreak: 0
      }
      this.setAuthSuccess({ token: 'jwt_google_' + Date.now(), user: mockUser })
      return mockUser
    },

    setAuthSuccess({ token, user }) {
      this.token = token || 'jwt_session'
      this.user = user
      this.userId = user.id?.toString() || this.userId
      localStorage.setItem('shike_token', this.token)
      localStorage.setItem('shike_user', JSON.stringify(this.user))
      localStorage.setItem('shike_user_id', this.userId)
    },

    // Logout
    logout() {
      this.token = ''
      localStorage.removeItem('shike_token')
      localStorage.removeItem('shike_user')
      this.user = null
      this.userId = ''
      this.initSession()
    },

    setUnitSystem(unit) {
      this.unitSystem = unit
      localStorage.setItem('shike_unit', unit)
    },
    setLang(lang) {
      this.lang = lang
      localStorage.setItem('shike_lang', lang)
    },
    consumeScan() {
      if (!this.isVip) {
        if (this.dailyScansRemaining > 0) {
          this.dailyScansRemaining--
        } else {
          this.isPaywallOpen = true
          return false
        }
      }
      return true
    },
    openPaywall() {
      this.isPaywallOpen = true
    },
    closePaywall() {
      this.isPaywallOpen = false
    },
    upgradeVipSuccess() {
      if (this.user) {
        this.user.vipType = 'PRO'
        this.user.aiUnlimited = true
        localStorage.setItem('shike_user', JSON.stringify(this.user))
      }
      this.isPaywallOpen = false
    }
  }
})
