<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { deleteRecord, getRecord, getRecordTypes, updateRecord, type RecordSummary, type RecordTypeDefinition } from '../../api/client'
import {
  buildRecordPayload,
  createRecordFormValues,
  fieldHintKey,
  isNumericRecordField,
  recordTypeLabelKey,
  requiredFieldsForRecord,
  toDateInput,
  toOffsetDateTime,
  toTimeInput,
  validateRecordValues
} from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const record = ref<RecordSummary | null>(null)
const recordTypes = ref<RecordTypeDefinition[]>([])
const occurredDate = ref('')
const occurredTime = ref('')
const originalPayloadJson = ref('{}')
const formValues = ref<Record<string, string>>({})
const errorMessage = ref('')
const loading = ref(false)
const submitting = ref(false)

const formFields = computed(() => {
  if (!record.value) {
    return []
  }
  return requiredFieldsForRecord(recordTypes.value, record.value.recordType)
})

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

async function loadRecord(recordId: string) {
  loading.value = true
  try {
    const [types, detail] = await Promise.all([getRecordTypes(), getRecord(recordId)])
    recordTypes.value = types
    record.value = detail
    occurredDate.value = toDateInput(record.value.occurredAt)
    occurredTime.value = toTimeInput(record.value.occurredAt)
    originalPayloadJson.value = record.value.payloadJson || '{}'
    formValues.value = createRecordFormValues(originalPayloadJson.value, requiredFieldsForRecord(types, record.value.recordType))
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

async function saveRecord() {
  if (!record.value || submitting.value) {
    return
  }
  const validation = validateRecordValues(formFields.value, formValues.value)
  if (!validation.valid) {
    errorMessage.value = t(validation.messageKey)
    return
  }
  submitting.value = true
  try {
    const payload = buildRecordPayload(originalPayloadJson.value, formFields.value, formValues.value)
    record.value = await updateRecord(record.value.id, {
      occurredAt: toOffsetDateTime(occurredDate.value, occurredTime.value),
      payloadJson: JSON.stringify(payload)
    })
    uni.$emit('records:changed')
    originalPayloadJson.value = record.value.payloadJson
    formValues.value = createRecordFormValues(originalPayloadJson.value, formFields.value)
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function removeRecord() {
  if (!record.value || submitting.value) {
    return
  }
  const confirmed = await confirmDelete()
  if (!confirmed) {
    return
  }
  submitting.value = true
  try {
    await deleteRecord(record.value.id)
    uni.$emit('records:changed')
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
  const recordId = typeof query?.id === 'string' ? query.id : ''
  if (recordId) {
    void loadRecord(recordId)
  }
})
</script>

<template>
  <view class="page">
    <text class="title">{{ t('recordDetail') }}</text>
    <view v-if="record" class="card">
      <text class="itemMeta">{{ labelForType(record.recordType) }}</text>
      <view class="field">
        <text class="fieldLabel">{{ t('occurredAt') }}</text>
        <view class="dateRow">
          <picker mode="date" :value="occurredDate" @change="occurredDate = String($event.detail.value)">
            <view class="pickerBox">{{ occurredDate }}</view>
          </picker>
          <picker mode="time" :value="occurredTime" @change="occurredTime = String($event.detail.value)">
            <view class="pickerBox">{{ occurredTime }}</view>
          </picker>
        </view>
      </view>
      <view v-for="field in formFields" :key="field" class="field">
        <text class="fieldLabel">{{ fieldLabel(field) }}</text>
        <input v-model="formValues[field]" class="input" :type="isNumericRecordField(field) ? 'digit' : 'text'" />
        <text v-if="fieldHint(field)" class="fieldHint">{{ fieldHint(field) }}</text>
      </view>
      <button class="primaryButton" :disabled="submitting" @click="saveRecord">{{ submitting ? t('saving') : t('save') }}</button>
      <button class="dangerButton" :disabled="submitting" @click="removeRecord">{{ t('delete') }}</button>
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

.dateRow {
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 8px;
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
  min-height: 140px;
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
