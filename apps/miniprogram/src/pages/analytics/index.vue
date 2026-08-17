<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import {
  createReport,
  exportPregnancyRecords,
  exportReports,
  getAnalyticsOverview,
  getHomeSummary,
  getIndicatorDefinitions,
  getMetricSeries,
  type ExportPackage,
  type AnalyticsOverview,
  type RecordExportRow,
  type ReportSummary,
  type ReportExportRow,
  type IndicatorDefinition,
  type MetricSeries
} from '../../api/client'
import { groupIndicatorsByReportType, reportIndicatorsPreview, validateIndicatorValues } from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const familyId = ref('')
const pregnancyId = ref('')
const seriesList = ref<MetricSeries[]>([])
const overview = ref<AnalyticsOverview | null>(null)
const recentReports = ref<ReportSummary[]>([])
const indicators = ref<IndicatorDefinition[]>([])
const loading = ref(false)
const submitting = ref(false)
const hasLoadedOnce = ref(false)
const errorMessage = ref('')
const saveMessage = ref('')
const exportMessage = ref('')
const exporting = ref(false)
const exportPreview = ref('')
const reportType = ref('blood')
const reportTitle = ref('')
const examinedAt = ref(new Date().toISOString().slice(0, 10))
const indicatorValues = ref<Record<string, string>>({ hcg: '1000', progesterone: '22' })
const metrics = ['weight', 'blood_pressure_systolic', 'blood_pressure_diastolic', 'baby_weight', 'baby_height']

const indicatorGroups = computed(() => {
  return groupIndicatorsByReportType(indicators.value)
})
const reportTypes = computed(() => Object.keys(indicatorGroups.value))
const reportIndicators = computed(() => indicatorGroups.value[reportType.value] ?? [])

function metricLabel(metric: string): string {
  const labels: Record<string, string> = {
    weight: t('weightTrend'),
    blood_pressure_systolic: t('systolic'),
    blood_pressure_diastolic: t('diastolic'),
    baby_weight: t('babyWeightTrend'),
    baby_height: t('babyHeightTrend')
  }
  return labels[metric] ?? metric
}

function pointHeight(series: MetricSeries, value: number): number {
  if (series.metric === 'baby_height') {
    return Math.max(8, Math.min(72, value))
  }
  if (series.metric === 'baby_weight') {
    return Math.max(8, Math.min(72, value * 2))
  }
  return Math.max(8, Math.min(72, value))
}

async function loadPage() {
  familyId.value = (uni.getStorageSync('familyId') as string | undefined) ?? ''
  loading.value = true
  try {
    indicators.value = await getIndicatorDefinitions()
    if (!familyId.value) {
      errorMessage.value = t('demoMode')
      return
    }
    const summary = await getHomeSummary(familyId.value)
    pregnancyId.value = summary.pregnancy?.id ?? ''
    recentReports.value = summary.recentReports
    const [overviewResult, seriesResult] = await Promise.all([
      getAnalyticsOverview(familyId.value),
      Promise.all(metrics.map((metric) => getMetricSeries(familyId.value, metric)))
    ])
    overview.value = overviewResult
    seriesList.value = seriesResult
    reportType.value = reportTypes.value[0] ?? 'blood'
    errorMessage.value = ''
    hasLoadedOnce.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

function openReport(reportId: string) {
  uni.navigateTo({ url: `/pages/report-detail/index?id=${encodeURIComponent(reportId)}` })
}

async function submitReport() {
  if (submitting.value) {
    return
  }
  if (!familyId.value || !pregnancyId.value) {
    errorMessage.value = t('demoMode')
    return
  }
  const values = reportIndicators.value
    .filter((indicator) => indicatorValues.value[indicator.code])
    .map((indicator) => ({
      code: indicator.code,
      value: Number(indicatorValues.value[indicator.code])
    }))
  if (values.length === 0) {
    errorMessage.value = t('atLeastOneIndicator')
    return
  }
  const validation = validateIndicatorValues(reportIndicators.value, indicatorValues.value)
  if (!validation.valid) {
    errorMessage.value = t(validation.messageKey)
    return
  }
  submitting.value = true
  try {
    await createReport({
      familyId: familyId.value,
      subjectType: 'pregnancy',
      subjectId: pregnancyId.value,
      reportType: reportType.value,
      title: reportTitle.value.trim() || t('reports'),
      examinedAt: examinedAt.value,
      indicatorsJson: JSON.stringify({ indicators: values })
    })
    saveMessage.value = t('saved')
    reportTitle.value = ''
    indicatorValues.value = {}
    uni.showToast({ title: t('saved'), icon: 'success' })
    errorMessage.value = ''
    const summary = await getHomeSummary(familyId.value)
    recentReports.value = summary.recentReports
    const [overviewResult, seriesResult] = await Promise.all([
      getAnalyticsOverview(familyId.value),
      Promise.all(metrics.map((metric) => getMetricSeries(familyId.value, metric)))
    ])
    overview.value = overviewResult
    seriesList.value = seriesResult
    uni.$emit('reports:changed')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

function compactExportPreview(result: ExportPackage<unknown>): string {
  return JSON.stringify({
    exportType: result.exportType,
    format: result.format,
    generatedAt: result.generatedAt,
    rowCount: result.rowCount,
    csvPreview: result.csvContent.split('\n').slice(0, 6).join('\n')
  }, null, 2)
}

async function runExport(type: 'pregnancy_records' | 'reports') {
  if (exporting.value) {
    return
  }
  if (!familyId.value) {
    errorMessage.value = t('needsInitialize')
    return
  }
  exporting.value = true
  try {
    const result: ExportPackage<RecordExportRow> | ExportPackage<ReportExportRow> = type === 'pregnancy_records'
      ? await exportPregnancyRecords(familyId.value)
      : await exportReports(familyId.value)
    exportPreview.value = result.csvContent || compactExportPreview(result as ExportPackage<unknown>)
    exportMessage.value = `${t('exportReady')} / ${result.rowCount} ${t('rows')}`
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    exporting.value = false
  }
}

function copyExportPreview() {
  if (!exportPreview.value) {
    return
  }
  uni.setClipboardData({
    data: exportPreview.value,
    success: () => uni.showToast({ title: t('copied'), icon: 'success' })
  })
}

function retryPage() {
  void loadPage()
}

onMounted(loadPage)
onShow(loadPage)
onLoad(() => {
  uni.$on('reports:changed', loadPage)
})
onUnload(() => {
  uni.$off('reports:changed', loadPage)
})
</script>

<template>
  <view class="page">
    <text class="title">{{ t('analytics') }}</text>
    <text class="hint">{{ t('chartsHint') }}</text>

    <view v-if="loading && !hasLoadedOnce" class="skeletonGroup">
      <view class="skeletonLine wide" />
      <view class="skeletonLine" />
      <view class="skeletonLine short" />
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('analyticsOverview') }}</text>
      <view class="overviewGrid">
        <view class="overviewCard">
          <text class="itemTitle">{{ t('prenatalCompletionRate') }}</text>
          <text class="metricValue">{{ overview?.prenatalPlan.completionRate ?? 0 }}%</text>
          <text class="itemMeta">{{ overview?.prenatalPlan.completed ?? 0 }} / {{ overview?.prenatalPlan.total ?? 0 }}</text>
        </view>
        <view class="overviewCard">
          <text class="itemTitle">{{ t('medicationSupplementStats') }}</text>
          <text class="metricValue">{{ overview?.medicationSupplement.totalRecords ?? 0 }}</text>
          <text class="itemMeta">{{ t('addMedication') }} {{ overview?.medicationSupplement.medicationRecords ?? 0 }} / {{ t('addSupplement') }} {{ overview?.medicationSupplement.supplementRecords ?? 0 }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('trendSeries') }}</text>
      <view v-for="series in seriesList" :key="series.metric" class="card">
        <view class="row">
          <text class="itemTitle">{{ metricLabel(series.metric) }}</text>
          <text class="itemMeta">{{ series.points.length }} {{ t('points') }}</text>
        </view>
        <view class="bars">
          <view v-for="point in series.points.slice(-12)" :key="point.occurredAt" class="bar" :style="{ height: `${pointHeight(series, Number(point.value))}px` }" />
        </view>
        <text v-if="series.points.length === 0" class="notice">{{ t('emptyChartHint') }}</text>
      </view>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('recentReports') }}</text>
      <view v-for="report in recentReports" :key="report.id" class="card" @click="openReport(report.id)">
        <text class="itemTitle">{{ report.title }}</text>
        <text class="itemMeta">{{ report.reportType }} / {{ report.examinedAt }}</text>
        <text class="indicator">{{ reportIndicatorsPreview(report.indicatorsJson) }}</text>
      </view>
      <text v-if="recentReports.length === 0" class="notice">{{ t('empty') }}</text>
    </view>

    <view class="section form">
      <text class="sectionTitle">{{ t('exportData') }}</text>
      <text class="hint">{{ t('exportDataHint') }}</text>
      <view class="exportActions">
        <button class="secondaryButton actionButton" :disabled="exporting" @click="runExport('pregnancy_records')">{{ t('exportPregnancyRecords') }}</button>
        <button class="secondaryButton actionButton" :disabled="exporting" @click="runExport('reports')">{{ t('exportReports') }}</button>
      </view>
      <text v-if="exportMessage" class="success">{{ exportMessage }}</text>
      <text v-if="exportPreview" class="exportPreview">{{ exportPreview }}</text>
      <button v-if="exportPreview" class="primaryButton" :disabled="exporting" @click="copyExportPreview">{{ t('copyExport') }}</button>
    </view>

    <view class="section form">
      <text class="sectionTitle">{{ t('newReport') }}</text>
      <picker :value="Math.max(0, reportTypes.indexOf(reportType))" :range="reportTypes" @change="reportType = reportTypes[$event.detail.value] ?? reportType">
        <view class="pickerBox">{{ reportType }}</view>
      </picker>
      <view class="field">
        <text class="fieldLabel">{{ t('reportTitle') }}</text>
        <input v-model="reportTitle" class="input" />
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
      <button class="primaryButton" :disabled="submitting" @click="submitReport">{{ submitting ? t('saving') : t('saveReport') }}</button>
      <text v-if="saveMessage" class="success">{{ saveMessage }}</text>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('indicatorDictionary') }}</text>
      <view v-for="(items, reportType) in indicatorGroups" :key="reportType" class="card">
        <text class="itemTitle">{{ reportType }}</text>
        <text v-for="indicator in items" :key="indicator.code" class="indicator">
          {{ indicator.name }} / {{ indicator.unit }}
        </text>
      </view>
    </view>

    <text v-if="errorMessage" class="notice">{{ errorMessage }}</text>
    <button v-if="errorMessage && familyId" class="secondaryButton" @click="retryPage">{{ t('retry') }}</button>
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

.sectionTitle {
  display: block;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
}

.card,
.form {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.overviewGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.overviewCard {
  min-height: 108px;
  padding: 14px;
  border-radius: 8px;
  background: #ffffff;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.itemTitle {
  display: block;
  font-weight: 700;
  line-height: 1.35;
}

.itemMeta,
.indicator {
  display: block;
  margin-top: 6px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.metricValue {
  display: block;
  margin-top: 10px;
  color: #2f7d68;
  font-size: 28px;
  font-weight: 760;
  line-height: 1.2;
}

.bars {
  height: 80px;
  margin-top: 12px;
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.bar {
  width: 12px;
  border-radius: 4px 4px 0 0;
  background: #2f7d68;
}

.pickerBox {
  min-height: 40px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f7f3ef;
  font-size: 14px;
}

.field {
  margin-top: 10px;
}

.fieldLabel {
  display: block;
  margin-bottom: 6px;
  color: #5f6b66;
  font-size: 12px;
}

.fieldHint {
  display: block;
  margin-top: 4px;
  color: #8a5a32;
  font-size: 11px;
  line-height: 1.35;
}

.input {
  min-height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f7f3ef;
}

.primaryButton {
  margin-top: 12px;
  min-height: 40px;
  border-radius: 8px;
  background: #2f7d68;
  color: #ffffff;
  font-size: 14px;
}

.secondaryButton {
  margin-top: 8px;
  min-height: 38px;
  border-radius: 8px;
  background: #edf0ee;
  color: #27302d;
  font-size: 14px;
}

.skeletonGroup {
  margin-top: 16px;
  padding: 14px;
  border-radius: 8px;
  background: #ffffff;
}

.skeletonLine {
  height: 14px;
  margin-top: 10px;
  border-radius: 8px;
  background: #edf0ee;
}

.skeletonLine.wide {
  width: 86%;
  margin-top: 0;
}

.skeletonLine.short {
  width: 46%;
}

.success {
  display: block;
  margin-top: 10px;
  color: #2f7d68;
  font-size: 12px;
}

.exportActions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.actionButton {
  flex: 1;
  min-width: 128px;
}

.exportPreview {
  display: block;
  max-height: 180px;
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #f7f3ef;
  color: #27302d;
  font-size: 11px;
  line-height: 1.45;
  overflow: hidden;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
