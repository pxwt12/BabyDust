<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import {
  apiErrorCode,
  askAiQuestion,
  asrRecordDraft,
  confirmAiDraft,
  extractRecordDraft,
  extractReportDraft,
  getStoredFamilyId,
  getStoredPregnancyId,
  ocrReportDraft,
  type AiDraftConfirmationResult,
  type AiDraftResponse,
  type AiPreprocessResponse,
  type AiQaResponse
} from '../../api/client'
import type { Locale } from '../../i18n/messages'

const t = inject<(key: string) => string>('t')!
const locale = inject<Ref<Locale>>('locale')!

const mode = ref<'record' | 'report' | 'ocr' | 'asr' | 'qa'>('record')
const inputText = ref('')
const fileUrl = ref('')
const draft = ref<AiDraftResponse | null>(null)
const preprocess = ref<AiPreprocessResponse | null>(null)
const qa = ref<AiQaResponse | null>(null)
const confirmation = ref<AiDraftConfirmationResult | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')

const draftJson = computed(() => draft.value ? JSON.stringify(draft.value, null, 2) : '')
const preprocessJson = computed(() => preprocess.value ? JSON.stringify(preprocess.value, null, 2) : '')
const qaJson = computed(() => qa.value ? JSON.stringify(qa.value, null, 2) : '')
const resultJson = computed(() => preprocessJson.value || draftJson.value || qaJson.value)
const preprocessFallbackMessage = computed(() => {
  if (!preprocess.value?.fallbackUsed) {
    return ''
  }
  const key = `aiError_${preprocess.value.errorCode}`
  const message = t(key)
  return message === key ? t('aiPreprocessFallback') : message
})

function aiErrorMessage(error: unknown) {
  const code = apiErrorCode(error)
  if (code) {
    const key = `aiError_${code}`
    const message = t(key)
    if (message !== key) {
      return message
    }
  }
  return error instanceof Error ? error.message : t('loadFailed')
}

async function generateDraft() {
  if (loading.value) {
    return
  }
  if (!inputText.value.trim()) {
    errorMessage.value = t('completeRequired')
    return
  }
  loading.value = true
  try {
    preprocess.value = null
    qa.value = null
    confirmation.value = null
    if (mode.value === 'record') {
      draft.value = await extractRecordDraft({ text: inputText.value.trim(), inputType: 'text' })
    } else if (mode.value === 'report') {
      draft.value = await extractReportDraft({ text: inputText.value.trim(), inputType: 'text' })
    } else if (mode.value === 'ocr') {
      preprocess.value = await ocrReportDraft({ fileUrl: fileUrl.value.trim() || 'oss://demo/report.jpg', text: inputText.value.trim() })
      draft.value = preprocess.value.draft
    } else if (mode.value === 'asr') {
      preprocess.value = await asrRecordDraft({ fileUrl: fileUrl.value.trim() || 'oss://demo/voice.m4a', text: inputText.value.trim() })
      draft.value = preprocess.value.draft
    } else {
      draft.value = null
      qa.value = await askAiQuestion({ question: inputText.value.trim(), locale: locale.value })
    }
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = aiErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function confirmDraft() {
  if (!draft.value || saving.value) {
    return
  }
  if (draft.value.blocked) {
    errorMessage.value = t('aiDraftBlocked')
    return
  }
  const familyId = getStoredFamilyId()
  const pregnancyId = getStoredPregnancyId()
  if (!familyId || !pregnancyId) {
    errorMessage.value = t('needsInitialize')
    return
  }
  saving.value = true
  try {
    confirmation.value = await confirmAiDraft({
      familyId,
      subjectType: 'pregnancy',
      subjectId: pregnancyId,
      draft: draft.value
    })
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
  } catch (error) {
    errorMessage.value = aiErrorMessage(error)
  } finally {
    saving.value = false
  }
}

function copyDraft() {
  if (!resultJson.value) {
    return
  }
  uni.setClipboardData({
    data: resultJson.value,
    success: () => uni.showToast({ title: t('copied'), icon: 'success' })
  })
}
</script>

<template>
  <view class="page">
    <text class="title">{{ t('aiDraft') }}</text>
    <text class="hint">{{ t('aiDraftHint') }}</text>

    <view class="section form">
      <view class="modeRow">
        <button class="chip" :class="{ active: mode === 'record' }" @click="mode = 'record'">{{ t('recordDraft') }}</button>
        <button class="chip" :class="{ active: mode === 'report' }" @click="mode = 'report'">{{ t('reportDraft') }}</button>
        <button class="chip" :class="{ active: mode === 'ocr' }" @click="mode = 'ocr'">{{ t('ocrDraft') }}</button>
        <button class="chip" :class="{ active: mode === 'asr' }" @click="mode = 'asr'">{{ t('asrDraft') }}</button>
        <button class="chip" :class="{ active: mode === 'qa' }" @click="mode = 'qa'">{{ t('aiQa') }}</button>
      </view>
      <input v-if="mode === 'ocr' || mode === 'asr'" v-model="fileUrl" class="input" :placeholder="t('fileUrlPlaceholder')" />
      <textarea v-model="inputText" class="textarea" :placeholder="mode === 'qa' ? t('aiQaPlaceholder') : t('aiInputPlaceholder')" />
      <button class="primaryButton" :disabled="loading" @click="generateDraft">
        {{ loading ? t('loading') : (mode === 'qa' ? t('askAi') : t('generateDraft')) }}
      </button>
    </view>

    <view v-if="qa" class="section card">
      <view class="row">
        <text class="itemTitle">{{ t('aiQaResult') }}</text>
        <text class="statusBadge" :class="{ blocked: qa.blocked }">{{ qa.riskLevel }}</text>
      </view>
      <text class="itemMeta">{{ qa.provider }} / {{ qa.model }} / {{ qa.generatedAt }}</text>
      <text class="itemMeta">{{ qa.answerType }} / {{ qa.errorCode }}</text>
      <text class="answer">{{ qa.answer }}</text>
      <view v-if="qa.suggestedQuestions.length" class="questionList">
        <text class="itemTitle">{{ t('suggestedQuestions') }}</text>
        <text v-for="question in qa.suggestedQuestions" :key="question" class="questionItem">{{ question }}</text>
      </view>
      <text v-for="warning in qa.warnings" :key="warning" class="warning">{{ warning }}</text>
      <button class="primaryButton" @click="copyDraft">{{ t('copyAnswer') }}</button>
    </view>

    <view v-if="draft" class="section card">
      <view class="row">
        <text class="itemTitle">{{ t('draftResult') }}</text>
        <text class="statusBadge">{{ draft.riskLevel }}</text>
      </view>
      <text class="itemMeta">{{ draft.provider }} / {{ draft.model }} / {{ draft.generatedAt }}</text>
      <text class="itemMeta">{{ draft.providerConfigKey }} / {{ draft.promptVersion }} / {{ draft.schemaVersion }}</text>
      <text class="itemMeta">{{ draft.errorCode }} / fallback={{ draft.fallbackUsed }}</text>
      <text class="itemMeta">tokens={{ draft.totalTokens }} / cost={{ draft.estimatedCost }} {{ draft.costCurrency }}</text>
      <text v-if="preprocess" class="itemMeta">{{ preprocess.preprocessor }} / {{ preprocess.fileUrl }}</text>
      <text v-if="preprocess" class="itemMeta">
        preprocess={{ preprocess.errorCode }} / fallback={{ preprocess.fallbackUsed }} / {{ preprocess.processedAt }}
      </text>
      <text v-if="preprocessFallbackMessage" class="warning">{{ preprocessFallbackMessage }}</text>
      <text v-for="warning in draft.warnings" :key="warning" class="warning">{{ warning }}</text>
      <text v-for="warning in preprocess?.warnings ?? []" :key="warning" class="warning">{{ warning }}</text>
      <text class="payload">{{ resultJson }}</text>
      <view v-if="confirmation" class="confirmationBox">
        <text class="itemMeta">{{ t('aiDraftConfirmed') }}</text>
        <text class="itemMeta">confirmationId={{ confirmation.confirmationId }}</text>
        <text class="itemMeta">
          records={{ confirmation.records.length }} / reports={{ confirmation.reports.length }} / todos={{ confirmation.todos.length }}
        </text>
      </view>
      <button class="primaryButton" :disabled="saving || draft.blocked" @click="confirmDraft">
        {{ saving ? t('saving') : t('confirmDraft') }}
      </button>
      <button class="primaryButton" @click="copyDraft">{{ t('copyDraft') }}</button>
    </view>

    <text v-if="errorMessage" class="notice">{{ errorMessage }}</text>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20px 16px 32px;
  background: #f7f3ef;
  color: #27302d;
}

.title {
  display: block;
  font-size: 22px;
  font-weight: 700;
}

.hint,
.notice {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 13px;
}

.section {
  margin-top: 20px;
}

.form,
.card {
  padding: 14px;
  border-radius: 8px;
  background: #ffffff;
}

.modeRow,
.row {
  display: flex;
  gap: 10px;
}

.modeRow {
  flex-wrap: wrap;
}

.row {
  justify-content: space-between;
}

.chip {
  flex: 1 0 30%;
  min-height: 36px;
  border-radius: 8px;
  background: #edf0ee;
  color: #27302d;
  font-size: 13px;
}

.active {
  background: #2f7d68;
  color: #ffffff;
}

.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 132px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f7f3ef;
  font-size: 13px;
  line-height: 1.5;
}

.input {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  margin-top: 12px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f7f3ef;
  font-size: 13px;
}

.primaryButton {
  margin-top: 12px;
  min-height: 40px;
  border-radius: 8px;
  background: #2f7d68;
  color: #ffffff;
  font-size: 14px;
}

.itemTitle,
.itemMeta,
.warning,
.answer,
.questionItem,
.payload {
  display: block;
}

.itemTitle {
  font-weight: 700;
  line-height: 1.35;
}

.itemMeta,
.warning,
.payload {
  margin-top: 8px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.warning {
  color: #8a5a32;
}

.answer {
  margin-top: 12px;
  color: #27302d;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.questionList {
  margin-top: 12px;
  padding: 10px;
  border-radius: 8px;
  background: #f7f3ef;
}

.questionItem {
  margin-top: 8px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.payload {
  max-height: 260px;
  padding: 10px;
  border-radius: 8px;
  background: #f7f3ef;
  overflow: hidden;
  white-space: pre-wrap;
  word-break: break-all;
}

.confirmationBox {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #eef7f4;
}

.statusBadge {
  flex: none;
  padding: 3px 8px;
  border-radius: 8px;
  background: #edf0ee;
  color: #2f7d68;
  font-size: 11px;
  line-height: 1.3;
}

.blocked {
  background: #f2d7c4;
  color: #7a3d1e;
}
</style>
