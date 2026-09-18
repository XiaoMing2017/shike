<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-slate-900/60 backdrop-blur-sm transition-opacity"
  >
    <div
      class="w-full max-w-lg bg-white rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl border border-slate-100 max-h-[90vh] flex flex-col animate-slide-up"
    >
      <!-- Header -->
      <div class="flex justify-between items-center pb-3 border-b border-slate-100">
        <div class="flex items-center gap-2">
          <ShieldCheck class="w-5 h-5 text-emerald-500" />
          <h2 class="text-base font-bold text-slate-900">
            {{ headerTitle }}
          </h2>
        </div>
        <button
          @click="$emit('close')"
          class="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 flex items-center justify-center text-slate-500 transition-colors"
        >
          <X class="w-4 h-4" />
        </button>
      </div>

      <!-- Segmented Tabs -->
      <div class="grid grid-cols-4 gap-1 p-1 bg-slate-100 rounded-xl my-3 text-xs font-bold text-slate-500">
        <button
          @click="activeTab = 'refund'"
          class="py-1.5 rounded-lg transition-all text-center truncate"
          :class="activeTab === 'refund' ? 'bg-white text-slate-900 shadow-xs' : 'hover:text-slate-800'"
        >
          {{ isZh ? '退款政策' : 'Refunds' }}
        </button>
        <button
          @click="activeTab = 'terms'"
          class="py-1.5 rounded-lg transition-all text-center truncate"
          :class="activeTab === 'terms' ? 'bg-white text-slate-900 shadow-xs' : 'hover:text-slate-800'"
        >
          {{ isZh ? '服务条款' : 'Terms' }}
        </button>
        <button
          @click="activeTab = 'privacy'"
          class="py-1.5 rounded-lg transition-all text-center truncate"
          :class="activeTab === 'privacy' ? 'bg-white text-slate-900 shadow-xs' : 'hover:text-slate-800'"
        >
          {{ isZh ? '隐私政策' : 'Privacy' }}
        </button>
        <button
          @click="activeTab = 'contact'"
          class="py-1.5 rounded-lg transition-all text-center truncate"
          :class="activeTab === 'contact' ? 'bg-white text-slate-900 shadow-xs' : 'hover:text-slate-800'"
        >
          {{ isZh ? '联系支持' : 'Contact' }}
        </button>
      </div>

      <!-- Scrollable Policy Content -->
      <div class="flex-1 overflow-y-auto pr-1 text-slate-600 text-xs leading-relaxed space-y-4 no-scrollbar">
        <!-- 1. REFUND POLICY TAB -->
        <div v-if="activeTab === 'refund'" class="space-y-3">
          <div class="p-3 bg-emerald-50 border border-emerald-200/80 rounded-2xl flex items-start gap-2.5">
            <Sparkles class="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            <div>
              <div class="font-bold text-emerald-900 text-xs">
                {{ isZh ? '14 天无忧满意保障 (14-Day Money-Back Guarantee)' : '14-Day Satisfaction Guarantee' }}
              </div>
              <p class="text-[11px] text-emerald-700 mt-0.5">
                {{ isZh ? '在首次扣款后 14 天内，若对 AI 测算体验不满意，我们承诺为您全额原路退款。' : 'If you are unsatisfied with ShiKe within 14 days of your initial purchase, you are eligible for a full refund.' }}
              </p>
            </div>
          </div>

          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">
              {{ isZh ? '1. 退款申请条件' : '1. Eligibility for Refunds' }}
            </h3>
            <p>
              {{ isZh ? '通过 ShiKe Web/PWA 官方购买的周订阅 (Weekly) 或年订阅 (Annual)，自初始付款扣费之日起 14 个自然日内均可申请全额退款。' : 'Subscriptions purchased through the ShiKe Web App / Lemon Squeezy are eligible for a full refund within 14 calendar days from the initial transaction date.' }}
            </p>
          </div>

          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">
              {{ isZh ? '2. 如何快速申请退款' : '2. How to Request a Refund' }}
            </h3>
            <p class="mb-2">
              {{ isZh ? '您无需复杂流程，仅需提供您的注册邮箱或订单收据：' : 'You can request a refund directly with no hassle:' }}
            </p>
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-1 text-[11px]">
              <div><strong>{{ isZh ? '客服邮箱：' : 'Support Email:' }}</strong> support@shike.store</div>
              <div><strong>{{ isZh ? '邮件主题：' : 'Subject:' }}</strong> Refund Request - [Your Order No / Email]</div>
              <div><strong>{{ isZh ? '处理时效：' : 'SLA:' }}</strong> {{ isZh ? '24~48 小时内确认并原路退回至支付卡（3~5 个工作日到账）。' : 'Processed within 24-48 hours back to your original payment method.' }}</div>
            </div>
          </div>

          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">
              {{ isZh ? '3. 自主取消下期续订 (Cancel Renewal)' : '3. Self-Service Subscription Cancellation' }}
            </h3>
            <p>
              {{ isZh ? '若您仅希望不再自动扣费，可随时在个人中心点击「管理订阅」或访问 Lemon Squeezy 客户中枢一键关闭续订，当前付费周期权益将保留至到期日。' : 'You may cancel automatic renewals anytime in your Account Settings or via the Lemon Squeezy Customer Portal without contacting support.' }}
            </p>
          </div>

          <!-- Direct mail action button -->
          <a
            href="mailto:support@shike.store?subject=ShiKe%20PRO%20Refund%20Request&body=Hello%20ShiKe%20Support%2C%0A%0AI%20would%20like%20to%20request%20a%20refund%20for%20my%20subscription.%0A%0AMy%20account%20email%3A%20%0AMy%20order%20number%20(if%20available)%3A%20%0AReason%20(optional)%3A%20%0A%0AThank%20you!"
            class="w-full py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all mt-2"
          >
            <Mail class="w-3.5 h-3.5" />
            <span>{{ isZh ? '一键向客服发送退款申请邮件' : 'Email Support for Refund' }}</span>
          </a>
        </div>

        <!-- 2. TERMS OF SERVICE TAB -->
        <div v-else-if="activeTab === 'terms'" class="space-y-3">
          <p class="text-[11px] text-slate-400">Last updated: September 18, 2026</p>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">1. Acceptance of Terms</h3>
            <p>By accessing or using ShiKe (the "App"), you agree to be bound by these Terms of Service. If you do not agree, please do not use the service.</p>
          </div>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">2. Health & Medical Disclaimer</h3>
            <p>ShiKe provides AI-assisted dietary estimation, caloric approximations, and lifestyle tracking for educational and informational purposes only. ShiKe is not a medical device, licensed dietitian, or healthcare provider. Always consult a healthcare professional before starting any extreme diet.</p>
          </div>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">3. Subscriptions & Billing</h3>
            <p>ShiKe PRO subscriptions are billed on a recurring basis (Weekly or Annually) via our Merchant of Record, Lemon Squeezy. Subscriptions automatically renew unless cancelled at least 24 hours prior to the end of the billing period.</p>
          </div>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">4. Termination</h3>
            <p>You may terminate your account at any time via the "Delete Account" button in your profile settings, which permanently wipes all stored personal records.</p>
          </div>
        </div>

        <!-- 3. PRIVACY POLICY TAB -->
        <div v-else-if="activeTab === 'privacy'" class="space-y-3">
          <p class="text-[11px] text-slate-400">Last updated: September 18, 2026 (GDPR & CCPA Compliant)</p>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">1. Information We Collect</h3>
            <p>We collect your email address for account authentication, voluntary physical metrics (weight, height, activity level) to calculate daily caloric goals, and uploaded meal photographs.</p>
          </div>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">2. How We Use Data & AI Processing</h3>
            <p>Meal photos are securely submitted to multi-modal visual models solely for real-time food classification and macronutrient estimation. We do not sell or monetize personal data to third-party ad networks.</p>
          </div>
          <div>
            <h3 class="font-bold text-slate-900 text-sm mb-1">3. Data Retention & Right to Erasure</h3>
            <p>Under GDPR/CCPA regulations, you hold the right to access and permanently erase your data. Executing "Delete Account" immediately destroys all your records on our servers.</p>
          </div>
        </div>

        <!-- 4. CONTACT US TAB -->
        <div v-else-if="activeTab === 'contact'" class="space-y-3">
          <div class="text-center py-2">
            <div class="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto mb-2">
              <Mail class="w-6 h-6" />
            </div>
            <h3 class="font-bold text-slate-900 text-sm">
              {{ isZh ? '官方客服与帮助中枢' : 'Official Support Desk' }}
            </h3>
            <p class="text-[11px] text-slate-500 mt-1 max-w-xs mx-auto">
              {{ isZh ? '遇到订阅扣款、AI 识别疑问或技术故障？我们随时在此为您解答。' : 'Have questions about billing, subscriptions, or AI accuracy? We are here to help 24/7.' }}
            </p>
          </div>

          <div class="p-3 bg-slate-50 rounded-2xl border border-slate-200 space-y-2 text-xs">
            <div class="flex items-center justify-between">
              <span class="text-slate-500 font-medium">{{ isZh ? '客服邮箱' : 'Direct Email' }}:</span>
              <a href="mailto:support@shike.store" class="font-bold text-emerald-600 hover:underline">support@shike.store</a>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-500 font-medium">{{ isZh ? '服务时间' : 'Operating Hours' }}:</span>
              <span class="font-semibold text-slate-700">Mon - Sun (24h Ticket Response)</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-500 font-medium">{{ isZh ? '支付服务商' : 'Merchant of Record' }}:</span>
              <span class="font-semibold text-slate-700">Lemon Squeezy, LLC</span>
            </div>
          </div>

          <a
            href="mailto:support@shike.store?subject=ShiKe%20User%20Inquiry"
            class="w-full py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-sm transition-all"
          >
            <Mail class="w-4 h-4" />
            <span>{{ isZh ? '直接发送邮件联系我们' : 'Send an Email' }}</span>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { X, ShieldCheck, Sparkles, Mail } from 'lucide-vue-next'
import { useAuthStore } from '../stores/authStore'

const props = defineProps({
  isOpen: Boolean,
  initialTab: {
    type: String,
    default: 'refund' // 'refund' | 'terms' | 'privacy' | 'contact'
  }
})

defineEmits(['close'])

const authStore = useAuthStore()
const activeTab = ref('refund')

const isZh = computed(() => authStore.lang === 'zh')

const headerTitle = computed(() => {
  if (activeTab.value === 'refund') return isZh.value ? '退款政策与保障' : 'Refund Policy'
  if (activeTab.value === 'terms') return isZh.value ? '服务条款' : 'Terms of Service'
  if (activeTab.value === 'privacy') return isZh.value ? '隐私政策' : 'Privacy Policy'
  return isZh.value ? '联系客服与支持' : 'Contact & Support'
})

watch(() => props.initialTab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab
  }
}, { immediate: true })
</script>
