<script setup lang="ts">
import { computed, inject, onMounted, ref, Ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getHomeSummary, getRecordTypes, getStoredFamilyId, type HomeSummary, type RecordSummary, type RecordTypeDefinition, type ReportSummary } from '../../api/client'
import { locales, type Locale } from '../../i18n/messages'
import { recordPayloadPreview, recordTypeLabelKey, reportIndicatorsPreview } from '../../domain/structuredForms'

const locale = inject<Ref<Locale>>('locale')!
const t = inject<(key: string) => string>('t')!
const stages = ['fertility', 'pregnancy', 'delivery', 'postpartum', 'baby']
const defaultActions = ['addWeight', 'addBloodPressure', 'addSymptom', 'addMedication', 'addFetalMovement']
const summary = ref<HomeSummary | null>(null)
const recordTypes = ref<RecordTypeDefinition[]>([])
const loading = ref(false)
const errorMessage = ref('')
const hasLoadedOnce = ref(false)

const pregnancy = computed(() => summary.value?.pregnancy ?? null)
const weekDisplay = computed(() => pregnancy.value?.gestationalWeekDisplay ?? '6+0')
const dueInDays = computed(() => pregnancy.value?.daysUntilDue ?? 238)
const todoCount = computed(() => summary.value?.upcomingTodos.length ?? 2)
const reportCount = computed(() => summary.value?.recentReports.length ?? 0)
const recordCount = computed(() => summary.value?.recentRecords.length ?? 0)
const latestTodo = computed(() => summary.value?.upcomingTodos[0]?.title ?? t('prenatalPlan'))
const recentRecords = computed(() => summary.value?.recentRecords.slice(0, 3) ?? [])
const recentReports = computed(() => summary.value?.recentReports.slice(0, 3) ?? [])
const weightPoints = computed(() => summary.value?.keyMetrics.find((series) => series.metric === 'weight')?.points.length ?? 0)
const actions = computed(() => {
  if (recordTypes.value.length === 0) {
    return defaultActions
  }
  return recordTypes.value.slice(0, 5).map((recordType) => recordType.type)
})

function onLocaleChange(event: { detail: { value: number } }) {
  locale.value = locales[event.detail.value] ?? 'zh-CN'
}

function actionLabel(type: string): string {
  return t(recordTypeLabelKey(type))
}

function openRecordPage(type = '') {
  if (type) {
    uni.setStorageSync('pendingRecordType', type)
  }
  uni.navigateTo({ url: '/pages/records/index' })
}

function openStage(stage: string) {
  if (stage === 'pregnancy') {
    openRecordPage()
    return
  }
  uni.navigateTo({ url: `/pages/stage-records/index?stage=${encodeURIComponent(stage)}` })
}

function openAnalyticsPage() {
  uni.navigateTo({ url: '/pages/analytics/index' })
}

function openTimelinePage() {
  uni.navigateTo({ url: '/pages/timeline/index' })
}

function openRemindersPage() {
  uni.navigateTo({ url: '/pages/reminders/index' })
}

function openAiDraftPage() {
  uni.navigateTo({ url: '/pages/ai-draft/index' })
}

function openRecord(record: RecordSummary) {
  uni.navigateTo({ url: `/pages/record-detail/index?id=${encodeURIComponent(record.id)}` })
}

function openReport(report: ReportSummary) {
  uni.navigateTo({ url: `/pages/report-detail/index?id=${encodeURIComponent(report.id)}` })
}

function recordTitle(record: RecordSummary): string {
  return t(recordTypeLabelKey(record.recordType))
}

function recordPreview(record: RecordSummary): string {
  return recordPayloadPreview(record.payloadJson)
}

function reportPreview(report: ReportSummary): string {
  return reportIndicatorsPreview(report.indicatorsJson)
}

async function loadDashboard() {
  const familyId = getStoredFamilyId()
  if (!familyId) {
    errorMessage.value = t('needsInitialize')
    recordTypes.value = await getRecordTypes().catch(() => [])
    return
  }
  loading.value = true
  try {
    const [homeSummary, types] = await Promise.all([getHomeSummary(familyId), getRecordTypes()])
    summary.value = homeSummary
    recordTypes.value = types
    errorMessage.value = ''
    hasLoadedOnce.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

function retryDashboard() {
  void loadDashboard()
}

function goInitialize() {
  uni.navigateTo({ url: '/pages/mine/index' })
}

onMounted(loadDashboard)
onShow(loadDashboard)
onLoad(() => {
  uni.$on('records:changed', loadDashboard)
  uni.$on('reports:changed', loadDashboard)
  uni.$on('todos:changed', loadDashboard)
})
onUnload(() => {
  uni.$off('records:changed', loadDashboard)
  uni.$off('reports:changed', loadDashboard)
  uni.$off('todos:changed', loadDashboard)
})
</script>

<template>
  <view class="page">
    <view class="toolbar">
      <text class="brand">{{ t('appName') }}</text>
      <picker :value="locales.indexOf(locale)" :range="locales" @change="onLocaleChange">
        <text class="language">{{ t('language') }}: {{ locale }}</text>
      </picker>
    </view>

    <view class="hero">
      <text class="eyebrow">{{ t('pregnancy') }}</text>
      <text class="title">{{ t('week') }} {{ weekDisplay }}</text>
      <text class="summary">{{ t('dueIn') }} {{ dueInDays }} {{ t('days') }}</text>
      <text v-if="errorMessage" class="notice">{{ errorMessage }}</text>
      <button v-if="errorMessage && !getStoredFamilyId()" class="initButton" @click="goInitialize">{{ t('initializeSpace') }}</button>
      <button v-if="errorMessage && getStoredFamilyId()" class="secondaryButton" @click="retryDashboard">{{ t('retry') }}</button>
    </view>

    <view v-if="loading && !hasLoadedOnce" class="skeletonGroup">
      <view class="skeletonLine wide" />
      <view class="skeletonLine" />
      <view class="skeletonLine short" />
    </view>

    <view class="stageGrid">
      <view v-for="stage in stages" :key="stage" class="stage" @click="openStage(stage)">
        <text>{{ t(stage) }}</text>
      </view>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('quickRecord') }}</text>
      <view class="actionGrid">
        <button v-for="action in actions" :key="action" class="action" @click="openRecordPage(action)">{{ actionLabel(action) }}</button>
      </view>
    </view>

    <view class="section list">
      <view class="row">
        <text>{{ t('todayTodo') }}</text>
        <text @click="openRemindersPage">{{ todoCount }}</text>
      </view>
      <view class="row detail">
        <text>{{ latestTodo }}</text>
      </view>
      <view class="row">
        <text>{{ t('reminders') }}</text>
        <text @click="openRemindersPage">{{ t('view') }}</text>
      </view>
      <view class="row">
        <text>{{ t('aiDraft') }}</text>
        <text @click="openAiDraftPage">{{ t('view') }}</text>
      </view>
      <view class="row">
        <text>{{ t('reports') }}</text>
        <text @click="openAnalyticsPage">{{ reportCount }}</text>
      </view>
      <view class="row">
        <text>{{ t('records') }}</text>
        <text @click="openTimelinePage">{{ recordCount }}</text>
      </view>
      <view class="row">
        <text>{{ t('fullTimeline') }}</text>
        <text @click="openTimelinePage">{{ t('view') }}</text>
      </view>
      <view class="row">
        <text>{{ t('weightTrend') }}</text>
        <text>{{ weightPoints }}</text>
      </view>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('recentRecords') }}</text>
      <view v-for="record in recentRecords" :key="record.id" class="summaryCard" @click="openRecord(record)">
        <text class="itemTitle">{{ recordTitle(record) }}</text>
        <text class="itemMeta">{{ record.occurredAt }}</text>
        <text class="payload">{{ recordPreview(record) }}</text>
      </view>
      <text v-if="recentRecords.length === 0" class="notice">{{ t('empty') }}</text>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('recentReports') }}</text>
      <view v-for="report in recentReports" :key="report.id" class="summaryCard" @click="openReport(report)">
        <text class="itemTitle">{{ report.title }}</text>
        <text class="itemMeta">{{ report.reportType }} / {{ report.examinedAt }}</text>
        <text class="payload">{{ reportPreview(report) }}</text>
      </view>
      <text v-if="recentReports.length === 0" class="notice">{{ t('empty') }}</text>
    </view>
    <text v-if="loading" class="loading">{{ t('home') }}...</text>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20px 16px 32px;
  background: #f7f3ef;
  color: #27302d;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  font-size: 22px;
  font-weight: 700;
}

.language {
  font-size: 12px;
  color: #5f6b66;
}

.hero {
  margin-top: 20px;
  padding: 18px;
  border-radius: 8px;
  background: #ffffff;
}

.eyebrow {
  display: block;
  color: #2f7d68;
  font-size: 13px;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  font-weight: 760;
}

.summary {
  display: block;
  margin-top: 8px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 14px;
}

.notice,
.loading {
  display: block;
  margin-top: 10px;
  color: #8a5a32;
  font-size: 12px;
  line-height: 1.4;
}

.initButton {
  margin-top: 12px;
  min-height: 38px;
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

.stageGrid,
.actionGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.stage,
.action {
  min-height: 48px;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  color: #27302d;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1.25;
  font-size: 14px;
}

.section {
  margin-top: 20px;
}

.sectionTitle {
  font-size: 16px;
  font-weight: 700;
}

.list {
  border-radius: 8px;
  background: #ffffff;
}

.summaryCard {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 14px;
  border-bottom: 1px solid #edf0ee;
  gap: 12px;
}

.detail {
  color: #5f6b66;
  font-size: 13px;
  line-height: 1.35;
}

.itemTitle,
.itemMeta,
.payload {
  display: block;
}

.itemTitle {
  font-weight: 700;
  line-height: 1.35;
}

.itemMeta,
.payload {
  margin-top: 6px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}
</style>
