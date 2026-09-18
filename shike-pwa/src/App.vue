<template>
  <div class="min-h-screen bg-slate-50 text-slate-800 antialiased selection:bg-emerald-100 flex flex-col justify-between">
    <!-- Main Router View -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Bottom Navigation Bar (Hidden on Scan, Auth, and Onboarding pages) -->
    <BottomNav v-if="route.path !== '/scan' && route.path !== '/auth' && route.path !== '/onboarding'" />

    <!-- Global Coach Push Notification Banner -->
    <CoachNotificationBanner />

    <!-- Global Paywall Modal -->
    <PaywallModal
      :isOpen="authStore.isPaywallOpen"
      @close="authStore.closePaywall"
    />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BottomNav from './components/BottomNav.vue'
import PaywallModal from './components/PaywallModal.vue'
import CoachNotificationBanner from './components/CoachNotificationBanner.vue'
import { useAuthStore } from './stores/authStore'

const route = useRoute()
const authStore = useAuthStore()

onMounted(async () => {
  await authStore.initSession()
  if (authStore.user?.id) {
    await authStore.refreshProfile()
  }
})
</script>
