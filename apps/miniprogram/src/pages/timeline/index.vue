<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  getRecords,
  getStoredFamilyId,
  getStoredPregnancyId,
  listBabies,
  type BabyProfile,
  type RecordSummary
} from '../../api/client'
import {
  recordPayloadPreview,
  recordStage,
  recordTypeLabelKey,
  stageRecordTypes,
  type StageKey
} from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const stages: Array<StageKey | 'all'> = ['all', 'fertility', 'pregnancy', 'delivery', 'postpartum', 'baby']
const familyId = ref('')
const pregnancyId = ref('')
const selectedStage = ref<StageKey | 'all'>('all')
const records = ref<RecordSummary[]>([])
const babies = ref<BabyProfile[]>([])
const loading = ref(false)
const errorMessage = ref('')

const babyNameById = computed(() => {
  return babies.value.reduce<Record<string, string>>((names, baby) => {
    names[baby.id] = baby.name
    return names
  }, {})
})

const filteredRecords = computed(() => {
  const visibleRecords = selectedStage.value === 'all'
    ? records.value
    : records.value.filter((record) => recordStage(record.recordType) === selectedStage.value)
  return [...visibleRecords].sort((left, right) => {
    return new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime()
  })
})

function stageLabel(stage: StageKey | 'all'): string {
  return stage === 'all' ? t('allStages') : t(stage)
}

function recordTitle(record: RecordSummary): string {
  return t(recordTypeLabelKey(record.recordType))
}

function recordStageLabel(record: RecordSummary): string {
  const stage = recordStage(record.recordType)
  return stage === 'unknown' ? t('unknownStage') : t(stage)
}

function recordSubjectLabel(record: RecordSummary): string {
  if (record.subjectType === 'baby') {
    return babyNameById.value[record.subjectId] || t('baby')
  }
  if (record.subjectType === 'pregnancy') {
    return t('pregnancy')
  }
  return t('familySpace')
}

function recordPreview(record: RecordSummary): string {
  return recordPayloadPreview(record.payloadJson)
}

function openRecord(record: RecordSummary) {
  uni.navigateTo({ url: `/pages/record-detail/index?id=${encodeURIComponent(record.id)}` })
}

async function loadTimeline() {
  familyId.value = getStoredFamilyId()
  pregnancyId.value = getStoredPregnancyId()
  if (!familyId.value) {
    errorMessage.value = t('needsInitialize')
    records.value = []
    return
  }

  loading.value = true
  try {
    babies.value = await listBabies(familyId.value).catch(() => [])
    const queries: Array<Promise<RecordSummary[]>> = [
      getRecords(familyId.value, { subjectType: 'family', subjectId: familyId.value })
    ]
    if (pregnancyId.value) {
      queries.push(getRecords(familyId.value, { subjectType: 'pregnancy', subjectId: pregnancyId.value }))
    }
    for (const baby of babies.value) {
      queries.push(getRecords(familyId.value, { subjectType: 'baby', subjectId: baby.id }))
    }

    const batches = await Promise.all(queries)
    const knownTypes = new Set(Object.values(stageRecordTypes).flat())
    records.value = batches.flat().filter((record) => knownTypes.has(record.recordType))
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  const queryStage = typeof query?.stage === 'string' ? query.stage : 'all'
  selectedStage.value = queryStage === 'all' || Object.prototype.hasOwnProperty.call(stageRecordTypes, queryStage)
    ? queryStage as StageKey | 'all'
    : 'all'
  void loadTimeline()
})
onShow(loadTimeline)
</script>

<template>
  <view class="page">
    <view class="header">
      <text class="title">{{ t('fullTimeline') }}</text>
      <text class="hint">{{ t('fullTimelineHint') }}</text>
    </view>

    <scroll-view class="filters" scroll-x>
      <button
        v-for="stage in stages"
        :key="stage"
        class="chip"
        :class="{ active: selectedStage === stage }"
        @click="selectedStage = stage"
      >
        {{ stageLabel(stage) }}
      </button>
    </scroll-view>

    <view class="section">
      <view v-for="record in filteredRecords" :key="record.id" class="recordItem" @click="openRecord(record)">
        <view class="itemHeader">
          <text class="itemTitle">{{ recordTitle(record) }}</text>
          <text class="stageBadge">{{ recordStageLabel(record) }}</text>
        </view>
        <text class="itemMeta">{{ recordSubjectLabel(record) }} / {{ record.occurredAt }}</text>
        <text class="payload">{{ recordPreview(record) }}</text>
      </view>
      <text v-if="!loading && filteredRecords.length === 0" class="empty">{{ t('empty') }}</text>
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

.header {
  margin-bottom: 14px;
}

.title {
  display: block;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.hint,
.notice,
.empty {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 13px;
}

.filters {
  white-space: nowrap;
  margin-top: 12px;
}

.chip {
  display: inline-flex;
  margin-right: 8px;
  min-height: 34px;
  border-radius: 8px;
  background: #ffffff;
  color: #27302d;
  font-size: 13px;
}

.active {
  background: #2f7d68;
  color: #ffffff;
}

.section {
  margin-top: 18px;
}

.recordItem {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.itemHeader {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.itemTitle,
.itemMeta,
.payload {
  display: block;
}

.itemTitle {
  min-width: 0;
  font-weight: 700;
  line-height: 1.35;
}

.stageBadge {
  flex: none;
  max-width: 112px;
  padding: 3px 8px;
  border-radius: 8px;
  background: #edf0ee;
  color: #2f7d68;
  font-size: 11px;
  line-height: 1.3;
  text-align: center;
}

.itemMeta,
.payload {
  margin-top: 6px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}
</style>
