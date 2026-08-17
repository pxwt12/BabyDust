<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  createReminder,
  getReminders,
  getStoredFamilyId,
  updateReminderStatus,
  type ReminderSummary
} from '../../api/client'
import { toOffsetDateTime } from '../../domain/structuredForms'

const t = inject<(key: string) => string>('t')!

const familyId = ref('')
const reminders = ref<ReminderSummary[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const message = ref('')
const selectedStatus = ref<'scheduled' | 'done' | 'cancelled' | 'all'>('scheduled')
const title = ref('')
const scene = ref('custom')
const triggerDate = ref(new Date().toISOString().slice(0, 10))
const triggerTime = ref('09:00')

const statusFilters: Array<'scheduled' | 'done' | 'cancelled' | 'all'> = ['scheduled', 'done', 'cancelled', 'all']
const scenes = ['custom', 'prenatal_checkup', 'supplement', 'postpartum_review', 'vaccine']

const visibleReminders = computed(() => {
  const filtered = selectedStatus.value === 'all'
    ? reminders.value
    : reminders.value.filter((reminder) => reminder.status === selectedStatus.value)
  return [...filtered].sort((left, right) => new Date(left.triggerAt).getTime() - new Date(right.triggerAt).getTime())
})

function sceneLabel(value: string): string {
  const keyByScene: Record<string, string> = {
    custom: 'customReminder',
    prenatal_checkup: 'prenatalCheckup',
    supplement: 'supplementReminder',
    postpartum_review: 'postpartumReview',
    vaccine: 'vaccineReminder'
  }
  return t(keyByScene[value] ?? value)
}

function statusLabel(value: string): string {
  const keyByStatus: Record<string, string> = {
    scheduled: 'scheduled',
    done: 'done',
    cancelled: 'cancelled',
    all: 'all'
  }
  return t(keyByStatus[value] ?? value)
}

async function loadReminders() {
  familyId.value = getStoredFamilyId()
  if (!familyId.value) {
    errorMessage.value = t('needsInitialize')
    return
  }
  loading.value = true
  try {
    reminders.value = await getReminders(familyId.value)
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

async function submitReminder() {
  if (submitting.value) {
    return
  }
  if (!familyId.value || !title.value.trim()) {
    errorMessage.value = t('completeRequired')
    return
  }
  submitting.value = true
  try {
    await createReminder({
      familyId: familyId.value,
      title: title.value.trim(),
      scene: scene.value,
      subjectType: 'family',
      subjectId: familyId.value,
      triggerAt: toOffsetDateTime(triggerDate.value, triggerTime.value)
    })
    title.value = ''
    message.value = t('saved')
    errorMessage.value = ''
    uni.showToast({ title: t('saved'), icon: 'success' })
    await loadReminders()
    uni.$emit('reminders:changed')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

async function markReminder(reminder: ReminderSummary, status: 'scheduled' | 'done' | 'cancelled') {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    await updateReminderStatus(reminder.id, status)
    await loadReminders()
    uni.$emit('reminders:changed')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('saveFailed')
  } finally {
    submitting.value = false
  }
}

onLoad(loadReminders)
onShow(loadReminders)
</script>

<template>
  <view class="page">
    <text class="title">{{ t('reminders') }}</text>
    <text class="hint">{{ t('remindersHint') }}</text>

    <view class="section form">
      <text class="sectionTitle">{{ t('newReminder') }}</text>
      <view class="field">
        <text class="fieldLabel">{{ t('title') }}</text>
        <input v-model="title" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('scene') }}</text>
        <picker :value="Math.max(0, scenes.indexOf(scene))" :range="scenes.map(sceneLabel)" @change="scene = scenes[$event.detail.value] ?? scene">
          <view class="pickerBox">{{ sceneLabel(scene) }}</view>
        </picker>
      </view>
      <view class="fieldRow">
        <view class="field halfField">
          <text class="fieldLabel">{{ t('date') }}</text>
          <picker mode="date" :value="triggerDate" @change="triggerDate = String($event.detail.value)">
            <view class="pickerBox">{{ triggerDate }}</view>
          </picker>
        </view>
        <view class="field halfField">
          <text class="fieldLabel">{{ t('time') }}</text>
          <picker mode="time" :value="triggerTime" @change="triggerTime = String($event.detail.value)">
            <view class="pickerBox">{{ triggerTime }}</view>
          </picker>
        </view>
      </view>
      <button class="primaryButton" :disabled="submitting" @click="submitReminder">{{ submitting ? t('saving') : t('saveReminder') }}</button>
      <text v-if="message" class="success">{{ message }}</text>
    </view>

    <view class="section">
      <scroll-view class="filters" scroll-x>
        <button
          v-for="status in statusFilters"
          :key="status"
          class="chip"
          :class="{ active: selectedStatus === status }"
          @click="selectedStatus = status"
        >
          {{ statusLabel(status) }}
        </button>
      </scroll-view>
      <view v-for="reminder in visibleReminders" :key="reminder.id" class="reminderItem">
        <view class="row">
          <text class="itemTitle">{{ reminder.title }}</text>
          <text class="statusBadge">{{ statusLabel(reminder.status) }}</text>
        </view>
        <text class="itemMeta">{{ sceneLabel(reminder.scene) }} / {{ reminder.triggerAt }}</text>
        <view class="buttonRow">
          <button v-if="reminder.status !== 'done'" class="miniButton" :disabled="submitting" @click="markReminder(reminder, 'done')">{{ t('done') }}</button>
          <button v-if="reminder.status !== 'cancelled'" class="miniButton secondary" :disabled="submitting" @click="markReminder(reminder, 'cancelled')">{{ t('cancel') }}</button>
          <button v-if="reminder.status !== 'scheduled'" class="miniButton secondary" :disabled="submitting" @click="markReminder(reminder, 'scheduled')">{{ t('restore') }}</button>
        </view>
      </view>
      <text v-if="!loading && visibleReminders.length === 0" class="empty">{{ t('empty') }}</text>
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
.reminderItem {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.field {
  margin-top: 10px;
}

.fieldRow,
.row,
.buttonRow {
  display: flex;
  gap: 10px;
}

.fieldRow,
.row {
  align-items: center;
}

.row {
  justify-content: space-between;
}

.halfField {
  flex: 1;
  min-width: 0;
}

.fieldLabel {
  display: block;
  margin-bottom: 6px;
  color: #5f6b66;
  font-size: 12px;
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

.filters {
  white-space: nowrap;
  margin-bottom: 10px;
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

.itemTitle,
.itemMeta {
  display: block;
}

.itemTitle {
  min-width: 0;
  font-weight: 700;
  line-height: 1.35;
}

.itemMeta {
  margin-top: 6px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
}

.statusBadge {
  flex: none;
  max-width: 96px;
  padding: 3px 8px;
  border-radius: 8px;
  background: #edf0ee;
  color: #2f7d68;
  font-size: 11px;
  line-height: 1.3;
  text-align: center;
}

.buttonRow {
  margin-top: 10px;
  flex-wrap: wrap;
}

.miniButton {
  min-height: 32px;
  min-width: 76px;
  border-radius: 8px;
  background: #2f7d68;
  color: #ffffff;
  font-size: 12px;
}

.miniButton.secondary {
  background: #edf0ee;
  color: #27302d;
}
</style>
