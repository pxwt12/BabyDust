<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import {
  createBaby,
  createRecord,
  getRecords,
  getRecordTypes,
  getStoredFamilyId,
  getStoredPregnancyId,
  listBabies,
  type BabyProfile,
  type RecordSummary,
  type RecordTypeDefinition
} from '../../api/client'
import {
  fieldHintKey,
  isNumericRecordField,
  parseRecordFieldValue,
  parseJsonObject,
  recordPayloadPreview,
  recordTypeLabelKey,
  stageRecordTypes,
  type StageKey,
  validateRecordValues
} from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const stage = ref<StageKey>('fertility')
const familyId = ref('')
const pregnancyId = ref('')
const recordTypes = ref<RecordTypeDefinition[]>([])
const records = ref<RecordSummary[]>([])
const babies = ref<BabyProfile[]>([])
const selectedBabyId = ref('')
const formType = ref('')
const recordForm = ref<Record<string, string>>({
  cycleDay: '12',
  result: '',
  note: '',
  temperatureC: '36.6',
  event: '',
  durationSeconds: '45',
  intervalMinutes: '5',
  level: '',
  mood: '',
  amountMl: '60',
  durationMinutes: '90',
  type: '',
  babyWeightKg: '3.2',
  heightCm: '50'
})
const babyForm = ref({
  name: '',
  gender: 'unknown',
  birthDateTime: '',
  birthWeightKg: '',
  birthLengthCm: ''
})
const loading = ref(false)
const submitting = ref(false)
const message = ref('')
const errorMessage = ref('')
const stagePageStages: StageKey[] = ['fertility', 'delivery', 'postpartum', 'baby']
const contractionStartedAt = ref<Date | null>(null)
const contractionEndedAt = ref<Date | null>(null)
const contractionElapsedSeconds = ref(0)
const contractionTimer = ref<ReturnType<typeof setInterval> | null>(null)
const quickFeeding = ref({
  amountMl: '60',
  side: 'bottle'
})
const sleepStartedAt = ref<Date | null>(null)
const sleepElapsedSeconds = ref(0)
const sleepTimer = ref<ReturnType<typeof setInterval> | null>(null)

const availableTypes = computed(() => {
  const allowed = stageRecordTypes[stage.value] ?? []
  return recordTypes.value.filter((type) => allowed.includes(type.type))
})
const activeDefinition = computed(() => availableTypes.value.find((type) => type.type === formType.value))
const formFields = computed(() => activeDefinition.value?.requiredFields ?? [])
const contractionDisplay = computed(() => formatDuration(contractionElapsedSeconds.value))
const sleepDisplay = computed(() => formatDuration(sleepElapsedSeconds.value))
const babyGrowthPoints = computed(() => {
  return records.value
    .filter((record) => record.recordType === 'baby_growth')
    .map((record) => {
      const payload = parseJsonObject(record.payloadJson)
      return {
        occurredAt: record.occurredAt,
        weight: Number(payload.babyWeightKg ?? 0),
        height: Number(payload.heightCm ?? 0)
      }
    })
    .filter((point) => point.weight > 0 || point.height > 0)
    .reverse()
})

function stageTitle(): string {
  return t(stage.value)
}

function labelForType(type: string): string {
  return t(recordTypeLabelKey(type))
}

function fieldLabel(field: string): string {
  return t(field)
}

function fieldHint(field: string): string {
  const key = fieldHintKey(field)
  return key ? t(key) : ''
}

function buildPayload(): Record<string, number | string> {
  const payload: Record<string, number | string> = {}
  for (const field of formFields.value) {
    payload[field] = parseRecordFieldValue(field, recordForm.value[field] ?? '')
  }
  return payload
}

function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function clearContractionTimer() {
  if (contractionTimer.value) {
    clearInterval(contractionTimer.value)
    contractionTimer.value = null
  }
}

function clearSleepTimer() {
  if (sleepTimer.value) {
    clearInterval(sleepTimer.value)
    sleepTimer.value = null
  }
}

function startContraction() {
  if (contractionStartedAt.value) {
    return
  }
  contractionStartedAt.value = new Date()
  contractionElapsedSeconds.value = 0
  clearContractionTimer()
  contractionTimer.value = setInterval(() => {
    if (contractionStartedAt.value) {
      contractionElapsedSeconds.value = Math.max(0, Math.floor((Date.now() - contractionStartedAt.value.getTime()) / 1000))
    }
  }, 1000)
}

async function stopAndSaveContraction() {
  if (!familyId.value || !contractionStartedAt.value) {
    errorMessage.value = t('completeRequired')
    return
  }
  const startedAt = contractionStartedAt.value
  const endedAt = new Date()
  const durationSeconds = Math.max(1, Math.floor((endedAt.getTime() - startedAt.getTime()) / 1000))
  const intervalMinutes = contractionEndedAt.value
    ? Math.max(0, Math.round((startedAt.getTime() - contractionEndedAt.value.getTime()) / 60000))
    : 0
  clearContractionTimer()
  contractionStartedAt.value = null
  submitting.value = true
  try {
    await createRecord({
      familyId: familyId.value,
      subjectType: 'family',
      subjectId: familyId.value,
      recordType: 'contraction',
      occurredAt: startedAt.toISOString(),
      payloadJson: JSON.stringify({ durationSeconds, intervalMinutes })
    })
    contractionEndedAt.value = endedAt
    contractionElapsedSeconds.value = 0
    message.value = t('saved')
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadRecords()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function submitQuickFeeding() {
  if (!familyId.value || !selectedBabyId.value) {
    errorMessage.value = t('selectBabyFirst')
    return
  }
  const amountMl = Number(quickFeeding.value.amountMl)
  if (!Number.isFinite(amountMl) || amountMl < 0 || amountMl > 300) {
    errorMessage.value = t('rangeAmountMl')
    return
  }
  submitting.value = true
  try {
    await createRecord({
      familyId: familyId.value,
      subjectType: 'baby',
      subjectId: selectedBabyId.value,
      recordType: 'baby_feeding',
      occurredAt: new Date().toISOString(),
      payloadJson: JSON.stringify({ amountMl, side: quickFeeding.value.side })
    })
    message.value = t('saved')
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadRecords()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

function startBabySleep() {
  if (sleepStartedAt.value) {
    return
  }
  sleepStartedAt.value = new Date()
  sleepElapsedSeconds.value = 0
  clearSleepTimer()
  sleepTimer.value = setInterval(() => {
    if (sleepStartedAt.value) {
      sleepElapsedSeconds.value = Math.max(0, Math.floor((Date.now() - sleepStartedAt.value.getTime()) / 1000))
    }
  }, 1000)
}

async function stopAndSaveBabySleep() {
  if (!familyId.value || !selectedBabyId.value || !sleepStartedAt.value) {
    errorMessage.value = t('selectBabyFirst')
    return
  }
  const startedAt = sleepStartedAt.value
  const durationMinutes = Math.max(1, Math.round((Date.now() - startedAt.getTime()) / 60000))
  clearSleepTimer()
  sleepStartedAt.value = null
  submitting.value = true
  try {
    await createRecord({
      familyId: familyId.value,
      subjectType: 'baby',
      subjectId: selectedBabyId.value,
      recordType: 'baby_sleep',
      occurredAt: startedAt.toISOString(),
      payloadJson: JSON.stringify({ durationMinutes, startedAt: startedAt.toISOString() })
    })
    sleepElapsedSeconds.value = 0
    message.value = t('saved')
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadRecords()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

function openBabyGrowthAnalytics() {
  formType.value = 'baby_growth'
  uni.pageScrollTo({ selector: '.recordForm', duration: 180 })
}

function resetTextFields() {
  for (const field of formFields.value) {
    if (!isNumericRecordField(field)) {
      recordForm.value[field] = ''
    }
  }
}

async function submitRecord() {
  if (submitting.value) {
    return
  }
  if (!familyId.value || !formType.value) {
    errorMessage.value = t('demoMode')
    return
  }
  if (stage.value === 'baby' && !selectedBabyId.value) {
    errorMessage.value = t('selectBabyFirst')
    return
  }
  const validation = validateRecordValues(formFields.value, recordForm.value)
  if (!validation.valid) {
    errorMessage.value = t(validation.messageKey)
    return
  }
  submitting.value = true
  try {
    await createRecord({
      familyId: familyId.value,
      subjectType: stage.value === 'baby' ? 'baby' : 'family',
      subjectId: stage.value === 'baby' ? selectedBabyId.value : familyId.value,
      recordType: formType.value,
      occurredAt: new Date().toISOString(),
      payloadJson: JSON.stringify(buildPayload())
    })
    resetTextFields()
    message.value = t('saved')
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadRecords()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function submitBaby() {
  if (submitting.value) {
    return
  }
  if (!familyId.value || !babyForm.value.name.trim()) {
    errorMessage.value = t('completeRequired')
    return
  }
  submitting.value = true
  try {
    await createBaby({
      familyId: familyId.value,
      pregnancyId: pregnancyId.value || undefined,
      name: babyForm.value.name.trim(),
      gender: babyForm.value.gender || 'unknown',
      birthDateTime: babyForm.value.birthDateTime || undefined,
      birthWeightKg: babyForm.value.birthWeightKg ? Number(babyForm.value.birthWeightKg) : undefined,
      birthLengthCm: babyForm.value.birthLengthCm ? Number(babyForm.value.birthLengthCm) : undefined
    })
    babyForm.value.name = ''
    babyForm.value.birthDateTime = ''
    babyForm.value.birthWeightKg = ''
    babyForm.value.birthLengthCm = ''
    message.value = t('saved')
    errorMessage.value = ''
    await loadBabies()
    selectedBabyId.value = babies.value[0]?.id ?? selectedBabyId.value
    await loadRecords()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function loadRecords() {
  if (!familyId.value) {
    return
  }
  if (stage.value === 'baby') {
    if (!selectedBabyId.value) {
      records.value = []
      return
    }
    records.value = await getRecords(familyId.value, {
      subjectType: 'baby',
      subjectId: selectedBabyId.value
    })
    const allowed = stageRecordTypes[stage.value] ?? []
    records.value = records.value.filter((record) => allowed.includes(record.recordType))
    return
  }
  records.value = await getRecords(familyId.value, {
    subjectType: 'family',
    subjectId: familyId.value
  })
  const allowed = stageRecordTypes[stage.value] ?? []
  records.value = records.value.filter((record) => allowed.includes(record.recordType))
}

async function loadBabies() {
  if (!familyId.value) {
    return
  }
  babies.value = await listBabies(familyId.value).catch(() => [])
  if (!selectedBabyId.value || !babies.value.some((baby) => baby.id === selectedBabyId.value)) {
    selectedBabyId.value = babies.value[0]?.id ?? ''
  }
}

async function loadPage() {
  familyId.value = getStoredFamilyId()
  pregnancyId.value = getStoredPregnancyId()
  if (!familyId.value) {
    errorMessage.value = t('needsInitialize')
    return
  }
  loading.value = true
  try {
    recordTypes.value = await getRecordTypes()
    await Promise.all([loadRecords(), loadBabies()])
    formType.value = availableTypes.value[0]?.type ?? ''
    await loadRecords()
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  const queryStage = typeof query?.stage === 'string' ? query.stage : 'fertility'
  stage.value = stagePageStages.includes(queryStage as StageKey) ? queryStage as StageKey : 'fertility'
  void loadPage()
})
onShow(loadPage)
onUnload(() => {
  clearContractionTimer()
  clearSleepTimer()
})
</script>

<template>
  <view class="page">
    <text class="title">{{ stageTitle() }}</text>
    <text class="hint">{{ t('stageRecordsHint') }}</text>

    <view v-if="stage === 'baby'" class="section form">
      <text class="sectionTitle">{{ t('babyProfile') }}</text>
      <view class="field">
        <text class="fieldLabel">{{ t('name') }}</text>
        <input v-model="babyForm.name" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('gender') }}</text>
        <input v-model="babyForm.gender" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('birthDateTime') }}</text>
        <input v-model="babyForm.birthDateTime" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('babyWeightKg') }}</text>
        <input v-model="babyForm.birthWeightKg" class="input" type="digit" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('heightCm') }}</text>
        <input v-model="babyForm.birthLengthCm" class="input" type="digit" />
      </view>
      <button class="primaryButton" :disabled="submitting" @click="submitBaby">{{ t('saveBaby') }}</button>
    </view>

    <view v-if="stage === 'baby' && babies.length > 0" class="section">
      <text class="sectionTitle">{{ t('selectBaby') }}</text>
      <scroll-view class="filters" scroll-x>
        <button
          v-for="baby in babies"
          :key="baby.id"
          class="chip"
          :class="{ active: selectedBabyId === baby.id }"
          @click="selectedBabyId = baby.id; loadRecords()"
        >
          {{ baby.name }}
        </button>
      </scroll-view>
    </view>

    <view v-if="stage === 'delivery'" class="section form">
      <text class="sectionTitle">{{ t('contractionTimer') }}</text>
      <view class="timerBox">
        <text class="timerValue">{{ contractionDisplay }}</text>
        <text class="timerHint">{{ t('contractionTimerHint') }}</text>
      </view>
      <view class="buttonRow">
        <button class="primaryButton halfButton" :disabled="Boolean(contractionStartedAt) || submitting" @click="startContraction">{{ t('start') }}</button>
        <button class="secondaryButton halfButton" :disabled="!contractionStartedAt || submitting" @click="stopAndSaveContraction">{{ t('stopAndSave') }}</button>
      </view>
    </view>

    <view v-if="stage === 'baby' && selectedBabyId" class="section form">
      <text class="sectionTitle">{{ t('quickBabyTools') }}</text>
      <view class="toolBlock">
        <text class="itemTitle">{{ t('quickFeeding') }}</text>
        <view class="field">
          <text class="fieldLabel">{{ t('amountMl') }}</text>
          <input v-model="quickFeeding.amountMl" class="input" type="digit" />
        </view>
        <view class="buttonRow">
          <button class="chip" :class="{ active: quickFeeding.side === 'left' }" @click="quickFeeding.side = 'left'">{{ t('leftSide') }}</button>
          <button class="chip" :class="{ active: quickFeeding.side === 'right' }" @click="quickFeeding.side = 'right'">{{ t('rightSide') }}</button>
          <button class="chip" :class="{ active: quickFeeding.side === 'bottle' }" @click="quickFeeding.side = 'bottle'">{{ t('bottle') }}</button>
        </view>
        <button class="primaryButton" :disabled="submitting" @click="submitQuickFeeding">{{ t('saveFeeding') }}</button>
      </view>
      <view class="toolBlock">
        <text class="itemTitle">{{ t('sleepTimer') }}</text>
        <view class="timerBox compact">
          <text class="timerValue">{{ sleepDisplay }}</text>
        </view>
        <view class="buttonRow">
          <button class="primaryButton halfButton" :disabled="Boolean(sleepStartedAt) || submitting" @click="startBabySleep">{{ t('start') }}</button>
          <button class="secondaryButton halfButton" :disabled="!sleepStartedAt || submitting" @click="stopAndSaveBabySleep">{{ t('stopAndSave') }}</button>
        </view>
      </view>
      <view class="toolBlock">
        <view class="row">
          <text class="itemTitle">{{ t('babyGrowthTrend') }}</text>
          <text class="linkText" @click="openBabyGrowthAnalytics">{{ t('addBabyGrowth') }}</text>
        </view>
        <view v-if="babyGrowthPoints.length > 0" class="growthBars">
          <view v-for="point in babyGrowthPoints.slice(-8)" :key="point.occurredAt" class="growthPoint">
            <view class="growthBar weightBar" :style="{ height: `${Math.max(8, Math.min(68, point.weight * 2))}px` }" />
            <view class="growthBar heightBar" :style="{ height: `${Math.max(8, Math.min(68, point.height))}px` }" />
          </view>
        </view>
        <text v-if="babyGrowthPoints.length === 0" class="empty">{{ t('emptyChartHint') }}</text>
      </view>
    </view>

    <view class="section form recordForm">
      <text class="sectionTitle">{{ t('newRecord') }}</text>
      <picker :value="Math.max(0, availableTypes.findIndex((type) => type.type === formType))" :range="availableTypes.map((type) => labelForType(type.type))" @change="formType = availableTypes[$event.detail.value]?.type ?? formType">
        <view class="pickerBox">{{ labelForType(formType) }}</view>
      </picker>
      <view v-for="field in formFields" :key="field" class="field">
        <text class="fieldLabel">{{ fieldLabel(field) }}</text>
        <input v-model="recordForm[field]" class="input" :type="isNumericRecordField(field) ? 'digit' : 'text'" />
        <text v-if="fieldHint(field)" class="fieldHint">{{ fieldHint(field) }}</text>
      </view>
      <button class="primaryButton" :disabled="submitting" @click="submitRecord">{{ submitting ? t('saving') : t('saveRecord') }}</button>
    </view>

    <view v-if="stage === 'baby'" class="section">
      <text class="sectionTitle">{{ t('baby') }}</text>
      <view v-for="baby in babies" :key="baby.id" class="recordItem">
        <text class="itemTitle">{{ baby.name }}</text>
        <text class="itemMeta">{{ baby.gender }}</text>
      </view>
      <text v-if="babies.length === 0" class="empty">{{ t('empty') }}</text>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('timeline') }}</text>
      <view v-for="record in records" :key="record.id" class="recordItem">
        <text class="itemTitle">{{ labelForType(record.recordType) }}</text>
        <text class="itemMeta">{{ record.occurredAt }}</text>
        <text class="payload">{{ recordPayloadPreview(record.payloadJson) }}</text>
      </view>
      <text v-if="!loading && records.length === 0" class="empty">{{ t('empty') }}</text>
    </view>

    <text v-if="message" class="success">{{ message }}</text>
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

.hint,
.notice,
.success,
.empty {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 13px;
}

.success {
  color: #2f7d68;
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

.form,
.recordItem {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.timerBox {
  padding: 16px;
  border-radius: 8px;
  background: #f7f3ef;
}

.timerBox.compact {
  margin-top: 10px;
  padding: 12px;
}

.timerValue {
  display: block;
  font-size: 32px;
  font-weight: 760;
  line-height: 1.2;
}

.timerHint {
  display: block;
  margin-top: 6px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.buttonRow,
.row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.buttonRow {
  margin-top: 10px;
  flex-wrap: wrap;
}

.halfButton {
  flex: 1;
  min-width: 120px;
}

.toolBlock {
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #edf0ee;
}

.toolBlock:first-of-type {
  padding-top: 0;
  margin-top: 0;
  border-top: 0;
}

.filters {
  white-space: nowrap;
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

.input,
.pickerBox {
  min-height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f7f3ef;
}

.pickerBox {
  box-sizing: border-box;
  padding-top: 10px;
  font-size: 14px;
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
  margin-top: 12px;
  min-height: 40px;
  border-radius: 8px;
  background: #edf0ee;
  color: #27302d;
  font-size: 14px;
}

.linkText {
  color: #2f7d68;
  font-size: 12px;
  line-height: 1.4;
}

.growthBars {
  height: 76px;
  margin-top: 12px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.growthPoint {
  display: flex;
  align-items: flex-end;
  gap: 3px;
}

.growthBar {
  width: 8px;
  border-radius: 4px 4px 0 0;
}

.weightBar {
  background: #2f7d68;
}

.heightBar {
  background: #b06b3b;
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
