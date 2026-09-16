<template>
  <div class="fixed bottom-0 left-0 right-0 z-40 max-w-md mx-auto">
    <div class="glass-nav px-6 py-2 pb-6 flex items-center justify-between shadow-lg">
      <!-- Dashboard -->
      <router-link
        to="/"
        class="flex flex-col items-center gap-1 transition-colors py-1"
        :class="isActive('/') ? 'text-emerald-500 font-semibold' : 'text-slate-400 hover:text-slate-600'"
      >
        <Flame class="w-6 h-6" :stroke-width="isActive('/') ? 2.5 : 2" />
        <span class="text-xs">{{ t('nav.daily') }}</span>
      </router-link>

      <!-- Habit Squad -->
      <router-link
        to="/team"
        class="flex flex-col items-center gap-1 transition-colors py-1"
        :class="isActive('/team') ? 'text-emerald-500 font-semibold' : 'text-slate-400 hover:text-slate-600'"
      >
        <Users class="w-6 h-6" :stroke-width="isActive('/team') ? 2.5 : 2" />
        <span class="text-xs">{{ t('nav.squad') }}</span>
      </router-link>

      <!-- Center Snap Button (Floating Camera) -->
      <div class="relative -top-5 flex flex-col items-center">
        <router-link
          to="/scan"
          class="w-14 h-14 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white shadow-xl shadow-emerald-500/30 flex items-center justify-center transition-transform active:scale-95 border-4 border-white"
        >
          <Camera class="w-7 h-7" :stroke-width="2.2" />
        </router-link>
        <span class="text-[11px] font-medium text-emerald-600 mt-1">{{ t('nav.snap') }}</span>
      </div>

      <!-- History -->
      <router-link
        to="/history"
        class="flex flex-col items-center gap-1 transition-colors py-1"
        :class="isActive('/history') ? 'text-emerald-500 font-semibold' : 'text-slate-400 hover:text-slate-600'"
      >
        <CalendarDays class="w-6 h-6" :stroke-width="isActive('/history') ? 2.5 : 2" />
        <span class="text-xs">{{ t('nav.history') }}</span>
      </router-link>

      <!-- Profile -->
      <router-link
        to="/profile"
        class="flex flex-col items-center gap-1 transition-colors py-1"
        :class="isActive('/profile') ? 'text-emerald-500 font-semibold' : 'text-slate-400 hover:text-slate-600'"
      >
        <User class="w-6 h-6" :stroke-width="isActive('/profile') ? 2.5 : 2" />
        <span class="text-xs">{{ t('nav.profile') }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { Flame, Users, Camera, CalendarDays, User } from 'lucide-vue-next'
import { useI18n } from '../i18n'

const route = useRoute()
const { t } = useI18n()
const isActive = (path) => route.path === path
</script>
