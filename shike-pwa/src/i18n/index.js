import { computed } from 'vue'
import { useAuthStore } from '../stores/authStore'
import en from './locales/en'
import zh from './locales/zh'
import es from './locales/es'
import de from './locales/de'
import fr from './locales/fr'
import ja from './locales/ja'
import pt from './locales/pt'

export const SUPPORTED_LANGUAGES = [
  { code: 'en', name: 'English', nativeName: 'English', flag: '🇺🇸' },
  { code: 'es', name: 'Spanish', nativeName: 'Español', flag: '🇪🇸' },
  { code: 'de', name: 'German', nativeName: 'Deutsch', flag: '🇩🇪' },
  { code: 'fr', name: 'French', nativeName: 'Français', flag: '🇫🇷' },
  { code: 'ja', name: 'Japanese', nativeName: '日本語', flag: '🇯🇵' },
  { code: 'pt', name: 'Portuguese', nativeName: 'Português', flag: '🇧🇷' },
  { code: 'zh', name: 'Chinese', nativeName: '简体中文', flag: '🇨🇳' }
]

const messages = { en, zh, es, de, fr, ja, pt }

export function useI18n() {
  const authStore = useAuthStore()

  const t = (path, params = {}) => {
    const keys = path.split('.')
    const lang = authStore.lang || 'en'
    let current = messages[lang] || messages['en']

    for (const key of keys) {
      if (current && current[key] !== undefined) {
        current = current[key]
      } else {
        // Fallback to English
        let fallback = messages['en']
        for (const fbKey of keys) {
          fallback = fallback?.[fbKey]
        }
        current = fallback !== undefined ? fallback : path
        break
      }
    }

    // Param interpolation: replace {paramName} with value
    if (typeof current === 'string' && params && typeof params === 'object') {
      let interpolated = current
      for (const [paramKey, paramVal] of Object.entries(params)) {
        interpolated = interpolated.replace(new RegExp(`\\{${paramKey}\\}`, 'g'), String(paramVal))
      }
      return interpolated
    }

    return current
  }

  return {
    t,
    currentLang: computed(() => authStore.lang)
  }
}
