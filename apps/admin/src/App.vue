<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

type AiConfig = {
  id: string
  configType: string
  configKey: string
  displayName: string
  provider: string
  status: string
  configJson: string
  versionLabel: string
  createdBy: string
}

type AiAuditLog = {
  id: string
  createdAt: string
  purpose: string
  provider: string
  model: string
  inputType: string
  inputLength: number
  inputPreview: string
  riskLevel: string
  blocked: boolean
  fallbackUsed: boolean
  errorCode: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  latencyMs: number
  costCurrency: string
  estimatedCost: number | string
  status: string
  policyVersion: string
  policyConfigured: boolean
  safetyPolicy: string
  riskReasons: string
}

type AiDraftConfirmation = {
  confirmationId: string
  familyId: string
  userId: string
  subjectType: string
  subjectId: string
  provider: string
  model: string
  purpose: string
  draftPreview: string
  recordIdsJson: string
  reportIdsJson: string
  todoIdsJson: string
  confirmedAt: string
}

type AiPreprocessAuditLog = {
  id: string
  createdAt: string
  purpose: string
  provider: string
  preprocessor: string
  fileUrlPreview: string
  textLength: number
  fallbackUsed: boolean
  errorCode: string
  latencyMs: number
  status: string
}

type ProviderForm = {
  model: string
  credentialRef: string
  baseUrl: string
  currency: string
  promptPer1K: string
  completionPer1K: string
}

type PreprocessorForm = {
  service: string
  preprocessor: string
  credentialRef: string
  region: string
  endpoint: string
  enabled: boolean
}

type ProviderSummary = {
  label: string
  value: string
}

const modules = [
  { title: 'Content Review', detail: 'Pregnancy weeks, postpartum guides and baby-care content approval.' },
  { title: 'Templates', detail: 'Prenatal plans, vaccine plans, reminders and report indicator dictionaries.' },
  { title: 'i18n', detail: 'Simplified Chinese, Traditional Chinese and English message keys.' },
  { title: 'AI Gateway', detail: 'DeepSeek provider, prompts, extraction schemas and audit logs.' },
  { title: 'Commercial Config', detail: 'Membership, paid content, toolkits and partner services hidden from users in v1.' }
]

const apiBase = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const configTypes = ['provider', 'prompt', 'schema', 'preprocessor', 'qa_policy']
const statuses = ['draft', 'active', 'archived']
const selectedType = ref('provider')
const configs = ref<AiConfig[]>([])
const auditLogs = ref<AiAuditLog[]>([])
const preprocessLogs = ref<AiPreprocessAuditLog[]>([])
const confirmationLogs = ref<AiDraftConfirmation[]>([])
const loading = ref(false)
const auditLoading = ref(false)
const preprocessLoading = ref(false)
const confirmationLoading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const auditErrorMessage = ref('')
const preprocessErrorMessage = ref('')
const confirmationErrorMessage = ref('')
const auditFilters = ref({
  purpose: '',
  provider: '',
  model: '',
  riskLevel: '',
  blocked: '',
  fallbackUsed: '',
  policyConfigured: '',
  safetyPolicy: '',
  errorCode: '',
  limit: '50'
})
const confirmationFilters = ref({
  familyId: '',
  userId: '',
  provider: '',
  model: '',
  purpose: '',
  subjectType: '',
  limit: '50'
})
const preprocessFilters = ref({
  purpose: '',
  provider: '',
  preprocessor: '',
  fallbackUsed: '',
  errorCode: '',
  limit: '50'
})
const form = ref({
  configType: 'provider',
  configKey: 'deepseek-public',
  displayName: 'DeepSeek public account',
  provider: 'deepseek',
  status: 'draft',
  versionLabel: 'v1',
  createdBy: 'admin',
  configJson: ''
})
const providerForm = ref<ProviderForm>({
  model: 'deepseek-chat',
  credentialRef: 'env:DEEPSEEK_API_KEY',
  baseUrl: 'https://api.deepseek.com',
  currency: 'CNY',
  promptPer1K: '0.002',
  completionPer1K: '0.008'
})
const preprocessorForm = ref<PreprocessorForm>({
  service: 'ocr',
  preprocessor: 'aliyun_ocr',
  credentialRef: 'env:ALIYUN_ACCESS_KEY',
  region: 'cn-shanghai',
  endpoint: 'https://ocr-api.cn-shanghai.aliyuncs.com',
  enabled: false
})

const filteredConfigs = computed(() => configs.value.filter((item) => item.configType === selectedType.value))

function buildProviderConfigJson() {
  let currentConfig: Record<string, unknown> = {}
  let currentPricing: Record<string, unknown> = {}
  try {
    const parsed = JSON.parse(form.value.configJson || '{}')
    if (parsed && !Array.isArray(parsed) && typeof parsed === 'object') {
      currentConfig = parsed
      if (parsed.pricing && !Array.isArray(parsed.pricing) && typeof parsed.pricing === 'object') {
        currentPricing = parsed.pricing
      }
    }
  } catch {
    currentConfig = {}
    currentPricing = {}
  }
  const config = {
    ...currentConfig,
    model: providerForm.value.model.trim(),
    credentialRef: providerForm.value.credentialRef.trim(),
    baseUrl: providerForm.value.baseUrl.trim(),
    pricing: {
      ...currentPricing,
      currency: providerForm.value.currency.trim() || 'CNY',
      promptPer1K: providerForm.value.promptPer1K.trim() || '0',
      completionPer1K: providerForm.value.completionPer1K.trim() || '0'
    }
  }
  return JSON.stringify(config, null, 2)
}

function syncProviderJson() {
  if (form.value.configType === 'provider') {
    form.value.configJson = buildProviderConfigJson()
  }
}

function buildPreprocessorConfigJson() {
  let currentConfig: Record<string, unknown> = {}
  try {
    const parsed = JSON.parse(form.value.configJson || '{}')
    if (parsed && !Array.isArray(parsed) && typeof parsed === 'object') {
      currentConfig = parsed
    }
  } catch {
    currentConfig = {}
  }
  return JSON.stringify(
    {
      ...currentConfig,
      service: preprocessorForm.value.service,
      preprocessor: preprocessorForm.value.preprocessor,
      credentialRef: preprocessorForm.value.credentialRef.trim(),
      region: preprocessorForm.value.region.trim(),
      endpoint: preprocessorForm.value.endpoint.trim(),
      enabled: preprocessorForm.value.enabled
    },
    null,
    2
  )
}

function syncPreprocessorJson() {
  if (form.value.configType === 'preprocessor') {
    form.value.configJson = buildPreprocessorConfigJson()
  }
}

function handlePreprocessorServiceChange() {
  if (preprocessorForm.value.service === 'ocr') {
    preprocessorForm.value.preprocessor = 'aliyun_ocr'
    preprocessorForm.value.endpoint = 'https://ocr-api.cn-shanghai.aliyuncs.com'
  } else {
    preprocessorForm.value.preprocessor = 'aliyun_asr'
    preprocessorForm.value.endpoint = 'https://nls-gateway-cn-shanghai.aliyuncs.com'
  }
  syncPreprocessorJson()
}

function syncProviderFieldsFromJson() {
  if (form.value.configType !== 'provider') {
    return
  }
  try {
    const parsed = JSON.parse(form.value.configJson)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return
    }
    providerForm.value = {
      model: typeof parsed.model === 'string' ? parsed.model : providerForm.value.model,
      credentialRef: typeof parsed.credentialRef === 'string' ? parsed.credentialRef : providerForm.value.credentialRef,
      baseUrl: typeof parsed.baseUrl === 'string' ? parsed.baseUrl : providerForm.value.baseUrl,
      currency: typeof parsed.pricing?.currency === 'string' ? parsed.pricing.currency : providerForm.value.currency,
      promptPer1K:
        typeof parsed.pricing?.promptPer1K === 'string' ? parsed.pricing.promptPer1K : providerForm.value.promptPer1K,
      completionPer1K:
        typeof parsed.pricing?.completionPer1K === 'string'
          ? parsed.pricing.completionPer1K
          : providerForm.value.completionPer1K
    }
  } catch {
    // Keep manual JSON errors visible through Save validation.
  }
}

function syncPreprocessorFieldsFromJson() {
  if (form.value.configType !== 'preprocessor') {
    return
  }
  try {
    const parsed = JSON.parse(form.value.configJson)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return
    }
    preprocessorForm.value = {
      service: typeof parsed.service === 'string' ? parsed.service : preprocessorForm.value.service,
      preprocessor: typeof parsed.preprocessor === 'string' ? parsed.preprocessor : preprocessorForm.value.preprocessor,
      credentialRef: typeof parsed.credentialRef === 'string' ? parsed.credentialRef : preprocessorForm.value.credentialRef,
      region: typeof parsed.region === 'string' ? parsed.region : preprocessorForm.value.region,
      endpoint: typeof parsed.endpoint === 'string' ? parsed.endpoint : preprocessorForm.value.endpoint,
      enabled: typeof parsed.enabled === 'boolean' ? parsed.enabled : preprocessorForm.value.enabled
    }
  } catch {
    // Keep manual JSON errors visible through Save validation.
  }
}

function handleTypeChange() {
  selectedType.value = form.value.configType
  if (form.value.configType === 'provider') {
    syncProviderJson()
  } else if (form.value.configType === 'preprocessor') {
    syncPreprocessorJson()
  }
}

function parseProviderConfig(configJson: string) {
  try {
    const parsed = JSON.parse(configJson)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function providerSummary(item: AiConfig): ProviderSummary[] {
  if (item.configType !== 'provider' && item.configType !== 'preprocessor') {
    return []
  }
  const parsed = parseProviderConfig(item.configJson)
  if (!parsed) {
    return []
  }
  if (item.configType === 'preprocessor') {
    return [
      { label: 'Service', value: typeof parsed.service === 'string' ? parsed.service : '-' },
      { label: 'Preprocessor', value: typeof parsed.preprocessor === 'string' ? parsed.preprocessor : '-' },
      { label: 'Credential', value: typeof parsed.credentialRef === 'string' ? parsed.credentialRef : '-' },
      { label: 'Region', value: typeof parsed.region === 'string' ? parsed.region : '-' },
      { label: 'Endpoint', value: typeof parsed.endpoint === 'string' ? parsed.endpoint : '-' },
      { label: 'Enabled', value: parsed.enabled === true ? 'true' : 'false' }
    ]
  }
  return [
    { label: 'Model', value: typeof parsed.model === 'string' ? parsed.model : '-' },
    { label: 'Credential', value: typeof parsed.credentialRef === 'string' ? parsed.credentialRef : '-' },
    { label: 'Base URL', value: typeof parsed.baseUrl === 'string' ? parsed.baseUrl : '-' },
    {
      label: 'Pricing',
      value: `${parsed.pricing?.currency || 'CNY'} / prompt ${parsed.pricing?.promptPer1K || '0'} / completion ${
        parsed.pricing?.completionPer1K || '0'
      } per 1K`
    }
  ]
}

async function loadConfigs() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetch(`${apiBase}/admin/ai-configs`)
    const body = await response.json()
    if (!response.ok || !body.success) {
      throw new Error(body.error?.message || 'Load failed')
    }
    configs.value = body.data
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Load failed'
  } finally {
    loading.value = false
  }
}

function auditQueryString() {
  const params = new URLSearchParams()
  Object.entries(auditFilters.value).forEach(([key, value]) => {
    if (value) {
      params.set(key, value)
    }
  })
  return params.toString()
}

function confirmationQueryString() {
  const params = new URLSearchParams()
  Object.entries(confirmationFilters.value).forEach(([key, value]) => {
    if (value) {
      params.set(key, value)
    }
  })
  return params.toString()
}

function preprocessQueryString() {
  const params = new URLSearchParams()
  Object.entries(preprocessFilters.value).forEach(([key, value]) => {
    if (value) {
      params.set(key, value)
    }
  })
  return params.toString()
}

async function loadAuditLogs() {
  auditLoading.value = true
  auditErrorMessage.value = ''
  try {
    const query = auditQueryString()
    const response = await fetch(`${apiBase}/admin/ai-audit-logs${query ? `?${query}` : ''}`)
    const body = await response.json()
    if (!response.ok || !body.success) {
      throw new Error(body.error?.message || 'Load audit logs failed')
    }
    auditLogs.value = body.data
  } catch (error) {
    auditErrorMessage.value = error instanceof Error ? error.message : 'Load audit logs failed'
  } finally {
    auditLoading.value = false
  }
}

async function loadConfirmations() {
  confirmationLoading.value = true
  confirmationErrorMessage.value = ''
  try {
    const query = confirmationQueryString()
    const response = await fetch(`${apiBase}/admin/ai-draft-confirmations${query ? `?${query}` : ''}`)
    const body = await response.json()
    if (!response.ok || !body.success) {
      throw new Error(body.error?.message || 'Load confirmation logs failed')
    }
    confirmationLogs.value = body.data
  } catch (error) {
    confirmationErrorMessage.value = error instanceof Error ? error.message : 'Load confirmation logs failed'
  } finally {
    confirmationLoading.value = false
  }
}

async function loadPreprocessLogs() {
  preprocessLoading.value = true
  preprocessErrorMessage.value = ''
  try {
    const query = preprocessQueryString()
    const response = await fetch(`${apiBase}/admin/ai-preprocess-audit-logs${query ? `?${query}` : ''}`)
    const body = await response.json()
    if (!response.ok || !body.success) {
      throw new Error(body.error?.message || 'Load preprocess logs failed')
    }
    preprocessLogs.value = body.data
  } catch (error) {
    preprocessErrorMessage.value = error instanceof Error ? error.message : 'Load preprocess logs failed'
  } finally {
    preprocessLoading.value = false
  }
}

function refreshAll() {
  loadConfigs()
  loadAuditLogs()
  loadPreprocessLogs()
  loadConfirmations()
}

function resetAuditFilters() {
  auditFilters.value = {
    purpose: '',
    provider: '',
    model: '',
    riskLevel: '',
    blocked: '',
    fallbackUsed: '',
    policyConfigured: '',
    safetyPolicy: '',
    errorCode: '',
    limit: '50'
  }
  loadAuditLogs()
}

function resetConfirmationFilters() {
  confirmationFilters.value = {
    familyId: '',
    userId: '',
    provider: '',
    model: '',
    purpose: '',
    subjectType: '',
    limit: '50'
  }
  loadConfirmations()
}

function resetPreprocessFilters() {
  preprocessFilters.value = {
    purpose: '',
    provider: '',
    preprocessor: '',
    fallbackUsed: '',
    errorCode: '',
    limit: '50'
  }
  loadPreprocessLogs()
}

function formatCost(log: AiAuditLog) {
  return `${log.costCurrency || 'CNY'} ${log.estimatedCost ?? 0}`
}

function formatTime(value: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function useTemplate(type: string) {
  form.value.configType = type
  selectedType.value = type
  if (type === 'provider') {
    form.value.configKey = 'deepseek-public'
    form.value.displayName = 'DeepSeek public account'
    form.value.provider = 'deepseek'
    providerForm.value = {
      model: 'deepseek-chat',
      credentialRef: 'env:DEEPSEEK_API_KEY',
      baseUrl: 'https://api.deepseek.com',
      currency: 'CNY',
      promptPer1K: '0.002',
      completionPer1K: '0.008'
    }
    syncProviderJson()
  } else if (type === 'prompt') {
    form.value.configKey = 'record-extraction-v1'
    form.value.displayName = 'Record extraction prompt'
    form.value.provider = 'deepseek'
    form.value.configJson = '{\n  "purpose": "record_extraction",\n  "locale": "zh-CN",\n  "safetyPolicy": "draft_only"\n}'
  } else if (type === 'schema') {
    form.value.configKey = 'report-draft-v1'
    form.value.displayName = 'Report draft schema'
    form.value.provider = 'deepseek'
    form.value.configJson = '{\n  "type": "object",\n  "required": ["reports"],\n  "properties": {\n    "records": { "type": "array" },\n    "todos": { "type": "array" },\n    "reports": { "type": "array" }\n  }\n}'
  } else if (type === 'preprocessor') {
    form.value.configKey = 'aliyun-ocr-v1'
    form.value.displayName = 'Aliyun OCR preprocessor'
    form.value.provider = 'aliyun'
    preprocessorForm.value = {
      service: 'ocr',
      preprocessor: 'aliyun_ocr',
      credentialRef: 'env:ALIYUN_ACCESS_KEY',
      region: 'cn-shanghai',
      endpoint: 'https://ocr-api.cn-shanghai.aliyuncs.com',
      enabled: false
    }
    syncPreprocessorJson()
  } else {
    form.value.configKey = 'qa-policy-v1'
    form.value.displayName = 'AI Q&A safety policy'
    form.value.provider = 'deepseek'
    form.value.configJson = JSON.stringify(
      {
        safetyPolicy: 'no_medical_decision',
        locales: {
          'zh-CN': {
            educationAnswer:
              '我可以帮你整理孕期相关信息、准备复诊问题清单，但不能判断是否正常、不能诊断，也不能建议用药或调整剂量。建议把发生时间、症状变化、检查结果、正在使用的药物或补剂记录下来，并在产检或需要时向医生确认下一步。',
            safetyAnswer: '这个问题可能涉及诊断、急症或用药决策。AI 不能给出医疗结论，请及时联系产检医生、医院或急诊。',
            suggestedQuestions: ['这件事下次产检需要问医生什么？', '我应该准备哪些记录给医生看？', '哪些情况需要及时联系医院？'],
            safetyQuestions: ['我现在需要立刻就医吗？', '这个症状需要做哪些检查？', '我正在使用的药物是否需要由医生复核？'],
            warnings: ['AI 仅提供科普整理和沟通准备，不能替代医生诊疗。']
          },
          'zh-TW': {
            educationAnswer:
              '我可以幫你整理孕期相關資訊、準備複診問題清單，但不能判斷是否正常、不能診斷，也不能建議用藥或調整劑量。建議把發生時間、症狀變化、檢查結果、正在使用的藥物或補劑記錄下來，並在產檢或需要時向醫生確認下一步。',
            safetyAnswer: '這個問題可能涉及診斷、急症或用藥決策。AI 不能給出醫療結論，請及時聯繫產檢醫生、醫院或急診。',
            suggestedQuestions: ['這件事下次產檢需要問醫生什麼？', '我應該準備哪些記錄給醫生看？', '哪些情況需要及時聯繫醫院？'],
            safetyQuestions: ['我現在需要立刻就醫嗎？', '這個症狀需要做哪些檢查？', '我正在使用的藥物是否需要由醫生複核？'],
            warnings: ['AI 僅提供科普整理和溝通準備，不能替代醫生診療。']
          },
          'en-US': {
            educationAnswer:
              'I can help organize pregnancy-related information and prepare questions for your clinician, but I cannot diagnose, judge whether a result is normal, or recommend medication changes. Keep a dated note, symptoms, test results and current medicines, then confirm the next step with your prenatal clinician.',
            safetyAnswer:
              'This question may involve diagnosis, urgent symptoms or medication decisions. AI cannot provide medical conclusions. Please contact your prenatal clinician, hospital or emergency care promptly.',
            suggestedQuestions: [
              'What should I ask at my next prenatal visit?',
              'Which records should I prepare for my clinician?',
              'What symptoms mean I should contact the hospital promptly?'
            ],
            safetyQuestions: [
              'Do I need urgent medical care now?',
              'Which checks should a clinician consider for this symptom?',
              'Should my current medicine be reviewed by a clinician?'
            ],
            warnings: ['AI provides education and organization only; medical decisions must be made with a clinician.']
          }
        }
      },
      null,
      2
    )
  }
}

async function saveConfig() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    JSON.parse(form.value.configJson)
    const response = await fetch(`${apiBase}/admin/ai-configs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    const body = await response.json()
    if (!response.ok || !body.success) {
      throw new Error(body.error?.message || 'Save failed')
    }
    successMessage.value = 'AI config saved as draft/control entry.'
    selectedType.value = form.value.configType
    await loadConfigs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Save failed'
  } finally {
    saving.value = false
  }
}

syncProviderJson()
onMounted(() => {
  refreshAll()
})
</script>

<template>
  <main class="shell">
    <aside class="sidebar">
      <h1>BabyDust Admin</h1>
      <nav>
        <a href="#content">Content</a>
        <a href="#templates">Templates</a>
        <a href="#i18n">i18n</a>
        <a href="#ai">AI</a>
        <a href="#ai-audit">AI Audit</a>
        <a href="#ai-preprocess">AI Preprocess</a>
        <a href="#ai-confirmations">AI Confirmations</a>
        <a href="#commerce">Commercial</a>
      </nav>
    </aside>
    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Operations Console</p>
          <h2>Public-release controls</h2>
        </div>
        <button type="button" @click="refreshAll">Refresh</button>
      </header>

      <section class="grid">
        <article v-for="module in modules" :key="module.title" class="card">
          <h3>{{ module.title }}</h3>
          <p>{{ module.detail }}</p>
        </article>
      </section>

      <section id="ai" class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">AI Gateway</p>
            <h2>Provider, Prompt and Schema Config</h2>
          </div>
          <div class="segmented">
            <button
              v-for="type in configTypes"
              :key="type"
              type="button"
              :class="{ active: selectedType === type }"
              @click="selectedType = type"
            >
              {{ type }}
            </button>
          </div>
        </div>

        <div class="ai-layout">
          <form class="config-form" @submit.prevent="saveConfig">
            <div class="form-row">
              <label>
                Type
                <select v-model="form.configType" @change="handleTypeChange">
                  <option v-for="type in configTypes" :key="type" :value="type">{{ type }}</option>
                </select>
              </label>
              <label>
                Status
                <select v-model="form.status">
                  <option v-for="status in statuses" :key="status" :value="status">{{ status }}</option>
                </select>
              </label>
            </div>
            <div class="form-row">
              <label>
                Key
                <input v-model="form.configKey" placeholder="record-extraction-v1" />
              </label>
              <label>
                Provider
                <input v-model="form.provider" placeholder="deepseek" />
              </label>
            </div>
            <label>
              Display name
              <input v-model="form.displayName" placeholder="Record extraction prompt" />
            </label>
            <div class="form-row">
              <label>
                Version
                <input v-model="form.versionLabel" placeholder="v1" />
              </label>
              <label>
                Created by
                <input v-model="form.createdBy" placeholder="admin" />
              </label>
            </div>
            <section v-if="form.configType === 'provider'" class="provider-builder">
              <div class="builder-heading">
                <div>
                  <h3>Provider details</h3>
                  <p>Use credential references only. Store real API keys in environment variables or KMS.</p>
                </div>
                <button type="button" class="secondary" @click="syncProviderFieldsFromJson">Read JSON</button>
              </div>
              <label>
                Model
                <input v-model="providerForm.model" placeholder="deepseek-chat" @input="syncProviderJson" />
              </label>
              <label>
                Credential reference
                <input v-model="providerForm.credentialRef" placeholder="env:DEEPSEEK_API_KEY" @input="syncProviderJson" />
              </label>
              <label>
                Base URL
                <input v-model="providerForm.baseUrl" placeholder="https://api.deepseek.com" @input="syncProviderJson" />
              </label>
              <div class="form-row">
                <label>
                  Currency
                  <input v-model="providerForm.currency" placeholder="CNY" @input="syncProviderJson" />
                </label>
                <label>
                  Prompt / 1K
                  <input v-model="providerForm.promptPer1K" inputmode="decimal" placeholder="0.002" @input="syncProviderJson" />
                </label>
              </div>
              <label>
                Completion / 1K
                <input
                  v-model="providerForm.completionPer1K"
                  inputmode="decimal"
                  placeholder="0.008"
                  @input="syncProviderJson"
                />
              </label>
            </section>
            <section v-if="form.configType === 'preprocessor'" class="provider-builder">
              <div class="builder-heading">
                <div>
                  <h3>Preprocessor details</h3>
                  <p>Use credential references only. Store real Alibaba Cloud keys outside this config.</p>
                </div>
                <button type="button" class="secondary" @click="syncPreprocessorFieldsFromJson">Read JSON</button>
              </div>
              <div class="form-row">
                <label>
                  Service
                  <select v-model="preprocessorForm.service" @change="handlePreprocessorServiceChange">
                    <option value="ocr">ocr</option>
                    <option value="asr">asr</option>
                  </select>
                </label>
                <label>
                  Preprocessor
                  <select v-model="preprocessorForm.preprocessor" @change="syncPreprocessorJson">
                    <option value="aliyun_ocr">aliyun_ocr</option>
                    <option value="aliyun_asr">aliyun_asr</option>
                  </select>
                </label>
              </div>
              <label>
                Credential reference
                <input v-model="preprocessorForm.credentialRef" placeholder="env:ALIYUN_ACCESS_KEY" @input="syncPreprocessorJson" />
              </label>
              <div class="form-row">
                <label>
                  Region
                  <input v-model="preprocessorForm.region" placeholder="cn-shanghai" @input="syncPreprocessorJson" />
                </label>
                <label>
                  Enabled
                  <select v-model="preprocessorForm.enabled" @change="syncPreprocessorJson">
                    <option :value="false">false</option>
                    <option :value="true">true</option>
                  </select>
                </label>
              </div>
              <label>
                Endpoint
                <input
                  v-model="preprocessorForm.endpoint"
                  placeholder="https://ocr-api.cn-shanghai.aliyuncs.com"
                  @input="syncPreprocessorJson"
                />
              </label>
            </section>
            <label>
              JSON config
              <textarea v-model="form.configJson" spellcheck="false" @blur="syncProviderFieldsFromJson(); syncPreprocessorFieldsFromJson()" />
            </label>
            <div class="quick-actions">
              <button v-for="type in configTypes" :key="type" type="button" @click="useTemplate(type)">
                {{ type }} template
              </button>
            </div>
            <button type="submit" :disabled="saving">{{ saving ? 'Saving...' : 'Save config' }}</button>
            <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
            <p v-if="successMessage" class="message success">{{ successMessage }}</p>
          </form>

          <section class="config-list">
            <div class="list-heading">
              <h3>{{ selectedType }} configs</h3>
              <span>{{ loading ? 'Loading' : `${filteredConfigs.length} items` }}</span>
            </div>
            <article v-for="item in filteredConfigs" :key="item.id" class="config-item">
              <div>
                <h4>{{ item.displayName }}</h4>
                <p>{{ item.configKey }} / {{ item.provider }} / {{ item.versionLabel }}</p>
              </div>
              <span class="status" :class="item.status">{{ item.status }}</span>
              <dl v-if="item.configType === 'provider' || item.configType === 'preprocessor'" class="provider-summary">
                <div v-for="entry in providerSummary(item)" :key="entry.label">
                  <dt>{{ entry.label }}</dt>
                  <dd>{{ entry.value }}</dd>
                </div>
              </dl>
              <pre>{{ item.configJson }}</pre>
            </article>
            <p v-if="!loading && filteredConfigs.length === 0" class="empty">No configs yet.</p>
          </section>
        </div>
      </section>

      <section id="ai-audit" class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">AI Audit</p>
            <h2>Draft Extraction Logs</h2>
          </div>
          <button type="button" @click="loadAuditLogs">Refresh logs</button>
        </div>

        <form class="audit-filters" @submit.prevent="loadAuditLogs">
          <label>
            Purpose
            <input v-model="auditFilters.purpose" placeholder="qa" />
          </label>
          <label>
            Provider
            <input v-model="auditFilters.provider" placeholder="deepseek" />
          </label>
          <label>
            Model
            <input v-model="auditFilters.model" placeholder="deepseek-chat" />
          </label>
          <label>
            Risk
            <select v-model="auditFilters.riskLevel">
              <option value="">All</option>
              <option value="low">low</option>
              <option value="high">high</option>
            </select>
          </label>
          <label>
            Blocked
            <select v-model="auditFilters.blocked">
              <option value="">All</option>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </label>
          <label>
            Fallback
            <select v-model="auditFilters.fallbackUsed">
              <option value="">All</option>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </label>
          <label>
            QA policy
            <select v-model="auditFilters.policyConfigured">
              <option value="">All</option>
              <option value="true">configured</option>
              <option value="false">default</option>
            </select>
          </label>
          <label>
            Safety policy
            <input v-model="auditFilters.safetyPolicy" placeholder="no_medical_decision" />
          </label>
          <label>
            Error code
            <input v-model="auditFilters.errorCode" placeholder="MODEL_CLIENT_NOT_CONFIGURED" />
          </label>
          <label>
            Limit
            <select v-model="auditFilters.limit">
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
          <div class="filter-actions">
            <button type="submit" :disabled="auditLoading">{{ auditLoading ? 'Loading...' : 'Apply' }}</button>
            <button type="button" class="secondary" @click="resetAuditFilters">Reset</button>
          </div>
        </form>

        <p v-if="auditErrorMessage" class="message error">{{ auditErrorMessage }}</p>

        <section class="audit-list">
          <div class="list-heading">
            <h3>Audit logs</h3>
            <span>{{ auditLoading ? 'Loading' : `${auditLogs.length} items` }}</span>
          </div>
          <article v-for="log in auditLogs" :key="log.id" class="audit-item">
            <div class="audit-main">
              <div>
                <h4>{{ log.purpose }} / {{ log.inputType }}</h4>
                <p>{{ formatTime(log.createdAt) }} / {{ log.provider }} / {{ log.model }}</p>
              </div>
              <div class="audit-badges">
                <span class="status" :class="{ active: !log.blocked, archived: log.blocked }">
                  {{ log.blocked ? 'blocked' : 'allowed' }}
                </span>
                <span class="status" :class="{ archived: log.fallbackUsed }">
                  {{ log.fallbackUsed ? 'fallback' : 'model' }}
                </span>
              </div>
            </div>
            <dl class="audit-metrics">
              <div>
                <dt>Risk</dt>
                <dd>{{ log.riskLevel }}</dd>
              </div>
              <div>
                <dt>Error</dt>
                <dd>{{ log.errorCode }}</dd>
              </div>
              <div>
                <dt>Tokens</dt>
                <dd>{{ log.totalTokens }} total / {{ log.promptTokens }} prompt / {{ log.completionTokens }} completion</dd>
              </div>
              <div>
                <dt>Cost</dt>
                <dd>{{ formatCost(log) }}</dd>
              </div>
              <div>
                <dt>Latency</dt>
                <dd>{{ log.latencyMs }} ms</dd>
              </div>
              <div>
                <dt>Policy</dt>
                <dd>{{ log.policyVersion || 'n/a' }} / {{ log.policyConfigured ? 'configured' : 'default' }}</dd>
              </div>
              <div>
                <dt>Safety</dt>
                <dd>{{ log.safetyPolicy || 'n/a' }}</dd>
              </div>
              <div>
                <dt>Risk reasons</dt>
                <dd>{{ log.riskReasons || '-' }}</dd>
              </div>
            </dl>
            <pre>{{ log.inputPreview }}</pre>
          </article>
          <p v-if="!auditLoading && auditLogs.length === 0" class="empty">No audit logs found.</p>
        </section>
      </section>

      <section id="ai-preprocess" class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">AI Preprocess</p>
            <h2>OCR and ASR Logs</h2>
          </div>
          <button type="button" @click="loadPreprocessLogs">Refresh preprocess logs</button>
        </div>

        <form class="audit-filters" @submit.prevent="loadPreprocessLogs">
          <label>
            Purpose
            <select v-model="preprocessFilters.purpose">
              <option value="">All</option>
              <option value="ocr_report">ocr_report</option>
              <option value="asr_record">asr_record</option>
            </select>
          </label>
          <label>
            Provider
            <input v-model="preprocessFilters.provider" placeholder="aliyun" />
          </label>
          <label>
            Preprocessor
            <select v-model="preprocessFilters.preprocessor">
              <option value="">All</option>
              <option value="aliyun_ocr">aliyun_ocr</option>
              <option value="aliyun_asr">aliyun_asr</option>
            </select>
          </label>
          <label>
            Fallback
            <select v-model="preprocessFilters.fallbackUsed">
              <option value="">All</option>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </label>
          <label>
            Error code
            <input v-model="preprocessFilters.errorCode" placeholder="ALIYUN_PREPROCESSOR_DISABLED" />
          </label>
          <label>
            Limit
            <select v-model="preprocessFilters.limit">
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
          <div class="filter-actions">
            <button type="submit" :disabled="preprocessLoading">{{ preprocessLoading ? 'Loading...' : 'Apply' }}</button>
            <button type="button" class="secondary" @click="resetPreprocessFilters">Reset</button>
          </div>
        </form>

        <p v-if="preprocessErrorMessage" class="message error">{{ preprocessErrorMessage }}</p>

        <section class="audit-list">
          <div class="list-heading">
            <h3>Preprocess logs</h3>
            <span>{{ preprocessLoading ? 'Loading' : `${preprocessLogs.length} items` }}</span>
          </div>
          <article v-for="log in preprocessLogs" :key="log.id" class="audit-item">
            <div class="audit-main">
              <div>
                <h4>{{ log.purpose }} / {{ log.preprocessor }}</h4>
                <p>{{ formatTime(log.createdAt) }} / {{ log.provider }} / {{ log.status }}</p>
              </div>
              <div class="audit-badges">
                <span class="status" :class="{ archived: log.fallbackUsed }">
                  {{ log.fallbackUsed ? 'fallback' : 'direct' }}
                </span>
              </div>
            </div>
            <dl class="audit-metrics">
              <div>
                <dt>Error</dt>
                <dd>{{ log.errorCode }}</dd>
              </div>
              <div>
                <dt>Text length</dt>
                <dd>{{ log.textLength }}</dd>
              </div>
              <div>
                <dt>Latency</dt>
                <dd>{{ log.latencyMs }} ms</dd>
              </div>
              <div>
                <dt>File</dt>
                <dd>{{ log.fileUrlPreview }}</dd>
              </div>
            </dl>
          </article>
          <p v-if="!preprocessLoading && preprocessLogs.length === 0" class="empty">No preprocess logs found.</p>
        </section>
      </section>

      <section id="ai-confirmations" class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">AI Confirmations</p>
            <h2>Draft Confirmation History</h2>
          </div>
          <button type="button" @click="loadConfirmations">Refresh confirmations</button>
        </div>

        <form class="audit-filters" @submit.prevent="loadConfirmations">
          <label>
            Family ID
            <input v-model="confirmationFilters.familyId" placeholder="UUID" />
          </label>
          <label>
            User ID
            <input v-model="confirmationFilters.userId" placeholder="UUID" />
          </label>
          <label>
            Provider
            <input v-model="confirmationFilters.provider" placeholder="deepseek" />
          </label>
          <label>
            Model
            <input v-model="confirmationFilters.model" placeholder="deepseek-chat" />
          </label>
          <label>
            Purpose
            <select v-model="confirmationFilters.purpose">
              <option value="">All</option>
              <option value="record_extraction">record_extraction</option>
              <option value="report_extraction">report_extraction</option>
              <option value="ocr_report">ocr_report</option>
              <option value="asr_record">asr_record</option>
            </select>
          </label>
          <label>
            Subject
            <select v-model="confirmationFilters.subjectType">
              <option value="">All</option>
              <option value="pregnancy">pregnancy</option>
              <option value="family">family</option>
              <option value="baby">baby</option>
            </select>
          </label>
          <label>
            Limit
            <select v-model="confirmationFilters.limit">
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
          <div class="filter-actions">
            <button type="submit" :disabled="confirmationLoading">
              {{ confirmationLoading ? 'Loading...' : 'Apply' }}
            </button>
            <button type="button" class="secondary" @click="resetConfirmationFilters">Reset</button>
          </div>
        </form>

        <p v-if="confirmationErrorMessage" class="message error">{{ confirmationErrorMessage }}</p>

        <section class="audit-list">
          <div class="list-heading">
            <h3>Confirmation logs</h3>
            <span>{{ confirmationLoading ? 'Loading' : `${confirmationLogs.length} items` }}</span>
          </div>
          <article v-for="log in confirmationLogs" :key="log.confirmationId" class="audit-item">
            <div class="audit-main">
              <div>
                <h4>{{ log.purpose }} / {{ log.subjectType }}</h4>
                <p>{{ formatTime(log.confirmedAt) }} / {{ log.provider }} / {{ log.model }}</p>
              </div>
              <div class="audit-badges">
                <span class="status active">confirmed</span>
              </div>
            </div>
            <dl class="audit-metrics">
              <div>
                <dt>Confirmation</dt>
                <dd>{{ log.confirmationId }}</dd>
              </div>
              <div>
                <dt>Family</dt>
                <dd>{{ log.familyId }}</dd>
              </div>
              <div>
                <dt>User</dt>
                <dd>{{ log.userId }}</dd>
              </div>
              <div>
                <dt>Subject</dt>
                <dd>{{ log.subjectType }} / {{ log.subjectId }}</dd>
              </div>
              <div>
                <dt>Records</dt>
                <dd>{{ log.recordIdsJson }}</dd>
              </div>
              <div>
                <dt>Reports</dt>
                <dd>{{ log.reportIdsJson }}</dd>
              </div>
              <div>
                <dt>Todos</dt>
                <dd>{{ log.todoIdsJson }}</dd>
              </div>
            </dl>
            <pre>{{ log.draftPreview }}</pre>
          </article>
          <p v-if="!confirmationLoading && confirmationLogs.length === 0" class="empty">No confirmation logs found.</p>
        </section>
      </section>
    </section>
  </main>
</template>
