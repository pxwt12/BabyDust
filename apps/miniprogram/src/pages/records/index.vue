<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import {
  createRecord,
  getHomeSummary,
  getRecords,
  getRecordTypes,
  getTodos,
  updateTodoStatus,
  type RecordSummary,
  type RecordTypeDefinition,
  type TodoSummary
} from '../../api/client'
import {
  fieldHintKey,
  isNumericRecordField,
  parseRecordFieldValue,
  recordPayloadPreview,
  recordTypeLabelKey,
  validateRecordValues
} from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const records = ref<RecordSummary[]>([])
const recordTypes = ref<RecordTypeDefinition[]>([])
const todos = ref<TodoSummary[]>([])
const activeType = ref('')
const activeWeek = ref('')
const familyId = ref('')
const pregnancyId = ref('')
const loading = ref(false)
const submitting = ref(false)
const hasLoadedOnce = ref(false)
const errorMessage = ref('')
const saveMessage = ref('')
const formType = ref('weight')
const recordForm = ref<Record<string, string>>({
  weightKg: '56',
  systolic: '118',
  diastolic: '76',
  name: '',
  severity: '1',
  dose: '',
  count: '10',
  durationMinutes: '30',
  mood: '',
  text: ''
})

const filteredRecords = computed(() => {
  if (!activeWeek.value) {
    return records.value
  }
  return records.value.filter((record) => weekLabel(record.occurredAt) === activeWeek.value)
})
const weekFilters = computed(() => {
  return Array.from(new Set(records.value.map((record) => weekLabel(record.occurredAt))))
})
const groupedRecords = computed(() => {
  const groups: Array<{ week: string; items: RecordSummary[] }> = []
  for (const record of filteredRecords.value) {
    const week = weekLabel(record.occurredAt)
    const existing = groups.find((group) => group.week === week)
    if (existing) {
      existing.items.push(record)
    } else {
      groups.push({ week, items: [record] })
    }
  }
  return groups
})
const planProgress = computed(() => {
  const planTodos = todos.value.filter((todo) => todo.category === 'prenatal_checkup' || todo.category === 'delivery_prepare')
  const completed = planTodos.filter((todo) => todo.status === 'done').length
  return {
    total: planTodos.length,
    completed,
    rate: planTodos.length === 0 ? 0 : Math.round((completed * 100) / planTodos.length)
  }
})
const activeDefinition = computed(() => recordTypes.value.find((type) => type.type === formType.value))
const formFields = computed(() => activeDefinition.value?.requiredFields ?? ['weightKg'])

function weekLabel(occurredAt: string): string {
  const date = new Date(occurredAt)
  if (Number.isNaN(date.getTime())) {
    return t('unknownWeek')
  }
  const pregnancyStart = new Date(date)
  pregnancyStart.setDate(date.getDate() - 42)
  const storedLmp = uni.getStorageSync('lmpDate') as string | undefined
  const lmp = storedLmp ? new Date(storedLmp) : pregnancyStart
  const days = Math.max(0, Math.floor((date.getTime() - lmp.getTime()) / 86400000))
  return `${t('week')} ${Math.floor(days / 7)}+${days % 7}`
}

function labelForType(type: string): string {
  return t(recordTypeLabelKey(type))
}

function payloadPreview(record: RecordSummary): string {
  return recordPayloadPreview(record.payloadJson)
}

function openRecord(recordId: string) {
  uni.navigateTo({ url: `/pages/record-detail/index?id=${encodeURIComponent(recordId)}` })
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
    const value = recordForm.value[field] ?? ''
    payload[field] = parseRecordFieldValue(field, value)
  }
  return payload
}

function resetRecordForm() {
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
  if (!familyId.value || !pregnancyId.value) {
    errorMessage.value = t('demoMode')
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
      subjectType: 'pregnancy',
      subjectId: pregnancyId.value,
      recordType: formType.value,
      occurredAt: new Date().toISOString(),
      payloadJson: JSON.stringify(buildPayload())
    })
    saveMessage.value = t('saved')
    resetRecordForm()
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadRecords(activeType.value)
    uni.$emit('records:changed')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function loadRecords(type = activeType.value) {
  if (!familyId.value || !pregnancyId.value) {
    return
  }
  loading.value = true
  try {
    records.value = await getRecords(familyId.value, {
      subjectType: 'pregnancy',
      subjectId: pregnancyId.value,
      recordType: type || undefined
    })
    activeType.value = type
    activeWeek.value = ''
    errorMessage.value = ''
    hasLoadedOnce.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

async function markDone(todoId: string) {
  try {
    const updated = await updateTodoStatus(todoId, 'done')
    todos.value = todos.value.map((todo) => (todo.id === updated.id ? updated : todo))
    uni.$emit('todos:changed')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  }
}

async function loadPage() {
  familyId.value = (uni.getStorageSync('familyId') as string | undefined) ?? ''
  if (!familyId.value) {
    errorMessage.value = t('demoMode')
    recordTypes.value = await getRecordTypes().catch(() => [])
    return
  }
  try {
    const summary = await getHomeSummary(familyId.value)
    pregnancyId.value = summary.pregnancy?.id ?? ''
    recordTypes.value = await getRecordTypes()
    const pendingType = (uni.getStorageSync('pendingRecordType') as string | undefined) ?? ''
    if (pendingType) {
      uni.removeStorageSync('pendingRecordType')
    }
    formType.value = pendingType || formType.value || recordTypes.value[0]?.type || 'weight'
    if (pregnancyId.value) {
      await Promise.all([
        loadRecords(pendingType || activeType.value),
        getTodos(familyId.value, { subjectType: 'pregnancy', subjectId: pregnancyId.value }).then((items) => {
          todos.value = items
        })
      ])
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  }
}

function retryPage() {
  void loadPage()
}

onMounted(loadPage)
onShow(loadPage)
onLoad(() => {
  uni.$on('records:changed', loadPage)
})
onUnload(() => {
  uni.$off('records:changed', loadPage)
})
</script>

<template>
  <view class="page">
    <text class="title">{{ t('records') }}</text>
    <text class="hint">{{ t('recordsHint') }}</text>

    <scroll-view class="filters" scroll-x>
      <button class="chip" :class="{ active: activeType === '' }" @click="loadRecords('')">{{ t('all') }}</button>
      <button
        v-for="type in recordTypes"
        :key="type.type"
        class="chip"
        :class="{ active: activeType === type.type }"
        @click="loadRecords(type.type)"
      >
        {{ labelForType(type.type) }}
      </button>
    </scroll-view>

    <scroll-view v-if="weekFilters.length > 1" class="filters" scroll-x>
      <button class="chip" :class="{ active: activeWeek === '' }" @click="activeWeek = ''">{{ t('allWeeks') }}</button>
      <button
        v-for="week in weekFilters"
        :key="week"
        class="chip"
        :class="{ active: activeWeek === week }"
        @click="activeWeek = week"
      >
        {{ week }}
      </button>
    </scroll-view>

    <view class="section form">
      <text class="sectionTitle">{{ t('newRecord') }}</text>
      <picker :value="Math.max(0, recordTypes.findIndex((type) => type.type === formType))" :range="recordTypes.map((type) => labelForType(type.type))" @change="formType = recordTypes[$event.detail.value]?.type ?? formType">
        <view class="pickerBox">{{ labelForType(formType) }}</view>
      </picker>
      <view v-for="field in formFields" :key="field" class="field">
        <text class="fieldLabel">{{ fieldLabel(field) }}</text>
        <input v-model="recordForm[field]" class="input" :type="isNumericRecordField(field) ? 'digit' : 'text'" />
        <text v-if="fieldHint(field)" class="fieldHint">{{ fieldHint(field) }}</text>
      </view>
      <button class="primaryButton" :disabled="submitting" @click="submitRecord">{{ submitting ? t('saving') : t('saveRecord') }}</button>
      <text v-if="saveMessage" class="success">{{ saveMessage }}</text>
    </view>

    <view v-if="loading && !hasLoadedOnce" class="skeletonGroup">
      <view class="skeletonLine wide" />
      <view class="skeletonLine" />
      <view class="skeletonLine short" />
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('prenatalPlan') }}</text>
      <view class="progress">
        <text>{{ planProgress.completed }}/{{ planProgress.total }}</text>
        <text>{{ planProgress.rate }}%</text>
      </view>
      <view v-for="todo in todos" :key="todo.id" class="todoRow">
        <view>
          <text class="itemTitle">{{ todo.title }}</text>
          <text class="itemMeta">{{ todo.status }}</text>
        </view>
        <button v-if="todo.status === 'pending'" class="smallButton" @click="markDone(todo.id)">{{ t('done') }}</button>
      </view>
    </view>

    <view class="section">
      <text class="sectionTitle">{{ t('timeline') }}</text>
      <view v-for="group in groupedRecords" :key="group.week" class="weekGroup">
        <text class="weekTitle">{{ group.week }}</text>
        <view v-for="record in group.items" :key="record.id" class="recordItem" @click="openRecord(record.id)">
          <text class="itemTitle">{{ labelForType(record.recordType) }}</text>
          <text class="itemMeta">{{ record.occurredAt }}</text>
          <text class="payload">{{ payloadPreview(record) }}</text>
        </view>
      </view>
      <view v-if="!loading && filteredRecords.length === 0" class="emptyState">
        <text class="emptyTitle">{{ t('emptyRecordsTitle') }}</text>
        <text class="empty">{{ t('emptyRecordsHint') }}</text>
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
.notice,
.empty {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 13px;
}

.emptyState {
  padding: 14px;
  border-radius: 8px;
  background: #ffffff;
}

.emptyTitle {
  display: block;
  font-weight: 700;
  line-height: 1.35;
}

.filters {
  margin-top: 16px;
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

.section {
  margin-top: 20px;
}

.sectionTitle {
  display: block;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
}

.todoRow,
.recordItem,
.form {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.weekGroup {
  margin-bottom: 14px;
}

.weekTitle {
  display: block;
  margin: 4px 0 8px;
  color: #2f7d68;
  font-size: 14px;
  font-weight: 700;
}

.progress {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #d7eadf;
  color: #24624f;
  font-size: 13px;
}

.todoRow {
  display: flex;
  justify-content: space-between;
  gap: 12px;
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

.smallButton {
  min-width: 64px;
  height: 34px;
  border-radius: 8px;
  background: #2f7d68;
  color: #ffffff;
  font-size: 13px;
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
</style>
