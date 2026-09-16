import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ScanView from '../views/ScanView.vue'
import TeamView from '../views/TeamView.vue'
import HistoryView from '../views/HistoryView.vue'
import ProfileView from '../views/ProfileView.vue'
import AuthView from '../views/AuthView.vue'
import OnboardingView from '../views/OnboardingView.vue'

const routes = [
  { path: '/', name: 'Home', component: HomeView },
  { path: '/scan', name: 'Scan', component: ScanView },
  { path: '/team', name: 'Team', component: TeamView },
  { path: '/history', name: 'History', component: HistoryView },
  { path: '/profile', name: 'Profile', component: ProfileView },
  { path: '/auth', name: 'Auth', component: AuthView },
  { path: '/onboarding', name: 'Onboarding', component: OnboardingView }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
