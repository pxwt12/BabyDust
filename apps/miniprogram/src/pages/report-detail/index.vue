<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { deleteReport, getIndicatorDefinitions, getReport, updateReport, type IndicatorDefinition, type ReportSummary } from '../../api/client'
import { buildIndicatorsJson, createIndicatorValues, groupIndicatorsByReportType, validateIndicatorValues } from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const report = ref<ReportSummary | null>(null)
const indicatorDefinitions = ref<IndicatorDefinition[]>([])
const title = ref('')
const examinedAt = ref('')
const indicatorValues = ref<Record<string, string>>({})
const errorMessage = ref('')
const loading = ref(false)
const submitting = ref(false)

const indicatorGroups = computed(() => groupIndicatorsByReportType(indicatorDefinitions.value))
const reportIndicators = computed(() => report.value ? indicatorGroups.value[report.value.reportType] ?? [] : [])

async function loadReport(reportId: string) {
  loading.value = true
  try {
    const [definitions, detail] = await Promise.all([getIndicatorDefinitions(), getReport(reportId)])
    indicatorDefinitions.value = definitions
    report.value = detail
    title.value = report.value.title
    examinedAt.value = report.value.examinedAt
    indicatorValues.value = createIndicatorValues(report.value.indicatorsJson)
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

async function saveReport() {
  if (!report.value || submitting.value) {
    return
  }
  if (!title.value.trim() || !examinedAt.value) {
    errorMessage.value = t('completeRequired')
    return
  }
  const validation = validateIndicatorValues(reportIndicators.value, indicatorValues.value)
  if (!validation.valid) {
    errorMessage.value = t(validation.messageKey)
    return
  }
  submitting.value = true
  const indicatorsJson = buildIndicatorsJson(reportIndicators.value, indicatorValues.value)
  try {
    report.value = await updateReport(report.value.id, {
      title: title.value.trim(),
      examinedAt: examinedAt.value,
      indicatorsJson
    })
    uni.$emit('reports:changed')
    indicatorValues.value = createIndicatorValues(report.value.indicatorsJson)
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function removeReport() {
  if (!report.value || submitting.value) {
    return
  }
  const confirmed = await confirmDelete()
  if (!confirmed) {
    return
  }
  submitting.value = true
  try {
    await deleteReport(report.value.id)
    uni.$emit('reports:changed')
    uni.showToast({ title: t('deleted'), icon: 'success' })
    uni.navigateBack()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

function confirmDelete(): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title: t('deleteConfirmTitle'),
      content: t('deleteConfirmContent'),
      confirmText: t('delete'),
      cancelText: t('cancel'),
      success: (result) => resolve(Boolean(result.confirm)),
      fail: () => resolve(false)
    })
  })
}

onLoad((query) => {
  const reportId = typeof query?.id === 'string' ? query.id : ''
  if (reportId) {
    void loadReport(reportId)
  }
})
</script>

<template>
  <view class="page">
    <text class="title">{{ t('reportDetail') }}</text>
    <view v-if="report" class="card">
      <text class="itemMeta">{{ report.reportType }}</text>
      <view class="field">
        <text class="fieldLabel">{{ t('reportTitle') }}</text>
        <input v-model="title" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('examinedAt') }}</text>
        <picker mode="date" :value="examinedAt" @change="examinedAt = String($event.detail.value)">
          <view class="pickerBox">{{ examinedAt }}</view>
        </picker>
      </view>
      <view v-for="indicator in reportIndicators" :key="indicator.code" class="field">
        <text class="fieldLabel">{{ indicator.name }} / {{ indicator.unit }}</text>
        <input v-model="indicatorValues[indicator.code]" class="input" type="digit" />
      </view>
      <button class="primaryButton" :disabled="submitting" @click="saveReport">{{ submitting ? t('saving') : t('save') }}</button>
      <button class="dangerButton" :disabled="submitting" @click="removeReport">{{ t('delete') }}</button>
    </view>
    <text v-if="errorMessage" class="notice">{{ errorMessage }}</text>
    <text v-if="loading" class="notice">{{ t('loading') }}</text>
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

.card {
  padding: 14px;
  margin-top: 16px;
  border-radius: 8px;
  background: #ffffff;
}

.itemMeta,
.notice {
  display: block;
  margin-top: 8px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.field {
  margin-top: 12px;
}

.fieldLabel {
  display: block;
  margin-bottom: 6px;
  color: #5f6b66;
  font-size: 12px;
}

.pickerBox {
  min-height: 40px;
  box-sizing: border-box;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f7f3ef;
  font-size: 14px;
}

.input,
.textarea {
  width: 100%;
  box-sizing: border-box;
  border-radius: 8px;
  background: #f7f3ef;
}

.input {
  min-height: 40px;
  padding: 0 12px;
}

.fieldHint {
  display: block;
  margin-top: 4px;
  color: #8a5a32;
  font-size: 11px;
  line-height: 1.35;
}

.textarea {
  min-height: 160px;
  padding: 10px 12px;
  font-size: 13px;
}

.primaryButton,
.dangerButton {
  margin-top: 12px;
  min-height: 40px;
  border-radius: 8px;
  color: #ffffff;
  font-size: 14px;
}

.primaryButton {
  background: #2f7d68;
}

.dangerButton {
  background: #a33b2f;
}
</style>
