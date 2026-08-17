<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  bootstrapPregnancySpace,
  clearLocalSession,
  getStoredFamilyId,
  getStoredPregnancyId,
  useDemoSeedSession,
  type PregnancyProfile
} from '../../api/client'

const t = inject<(key: string) => string>('t')!

const nickname = ref('Mom')
const familyName = ref('Our family')
const lmpDate = ref('2026-04-13')
const fetusCount = ref(1)
const familyId = ref('')
const pregnancyId = ref('')
const pregnancy = ref<PregnancyProfile | null>(null)
const loading = ref(false)
const message = ref('')
const errorMessage = ref('')

const initialized = computed(() => Boolean(familyId.value && pregnancyId.value))

function refreshLocalState() {
  familyId.value = getStoredFamilyId()
  pregnancyId.value = getStoredPregnancyId()
}

async function initializeSpace() {
  if (!nickname.value.trim() || !familyName.value.trim() || !lmpDate.value.trim() || fetusCount.value < 1) {
    errorMessage.value = t('completeRequired')
    return
  }
  loading.value = true
  try {
    const result = await bootstrapPregnancySpace({
      nickname: nickname.value,
      familyName: familyName.value,
      lmpDate: lmpDate.value,
      fetusCount: fetusCount.value
    })
    pregnancy.value = result.pregnancy
    refreshLocalState()
    message.value = t('initialized')
    uni.showToast({ title: t('initialized'), icon: 'success' })
    errorMessage.value = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('initializeFailed')
  } finally {
    loading.value = false
  }
}

function resetLocal() {
  clearLocalSession()
  pregnancy.value = null
  refreshLocalState()
  message.value = t('localCleared')
}

function loadDemoData() {
  useDemoSeedSession()
  refreshLocalState()
  message.value = t('demoFamilyLoaded')
  errorMessage.value = ''
  uni.showToast({ title: t('demoDataLoaded'), icon: 'success' })
}

onMounted(refreshLocalState)
onShow(refreshLocalState)
</script>

<template>
  <view class="page">
    <text class="title">{{ t('mine') }}</text>
    <text class="hint">{{ t('mineHint') }}</text>

    <view class="card">
      <view class="statusRow">
        <text class="itemTitle">{{ t('spaceStatus') }}</text>
        <text class="badge" :class="{ ok: initialized }">{{ initialized ? t('ready') : t('notReady') }}</text>
      </view>
      <text class="itemMeta">familyId: {{ familyId || '-' }}</text>
      <text class="itemMeta">pregnancyId: {{ pregnancyId || '-' }}</text>
      <button class="secondaryButton compactButton" @click="loadDemoData">{{ t('loadDemoData') }}</button>
    </view>

    <view class="card">
      <text class="sectionTitle">{{ t('initializeSpace') }}</text>
      <view class="field">
        <text class="fieldLabel">{{ t('nickname') }}</text>
        <input v-model="nickname" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('familyName') }}</text>
        <input v-model="familyName" class="input" />
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('lmpDate') }}</text>
        <input v-model="lmpDate" class="input" />
        <text class="fieldHint">YYYY-MM-DD</text>
      </view>
      <view class="field">
        <text class="fieldLabel">{{ t('fetusCount') }}</text>
        <input v-model="fetusCount" class="input" type="number" />
      </view>
      <button class="primaryButton" :disabled="loading" @click="initializeSpace">{{ t('startUsing') }}</button>
      <button class="secondaryButton" @click="resetLocal">{{ t('clearLocal') }}</button>
    </view>

    <view v-if="pregnancy" class="card">
      <text class="sectionTitle">{{ t('pregnancy') }}</text>
      <text class="itemMeta">{{ t('lmpDate') }}: {{ pregnancy.lmpDate }}</text>
      <text class="itemMeta">{{ t('dueDate') }}: {{ pregnancy.dueDate }}</text>
    </view>

    <text v-if="message" class="success">{{ message }}</text>
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
.notice,
.success {
  display: block;
  margin-top: 10px;
  line-height: 1.5;
  color: #5f6b66;
  font-size: 13px;
}

.success {
  color: #2f7d68;
}

.card {
  padding: 14px;
  margin-top: 16px;
  border-radius: 8px;
  background: #ffffff;
}

.statusRow {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.sectionTitle,
.itemTitle {
  display: block;
  font-size: 16px;
  font-weight: 700;
}

.itemMeta {
  display: block;
  margin-top: 8px;
  color: #5f6b66;
  font-size: 12px;
  line-height: 1.45;
  word-break: break-all;
}

.badge {
  min-width: 72px;
  padding: 5px 8px;
  border-radius: 8px;
  text-align: center;
  background: #f2d7c4;
  color: #7a3d1e;
  font-size: 12px;
}

.ok {
  background: #d7eadf;
  color: #24624f;
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

.primaryButton,
.secondaryButton {
  margin-top: 12px;
  min-height: 40px;
  border-radius: 8px;
  font-size: 14px;
}

.primaryButton {
  background: #2f7d68;
  color: #ffffff;
}

.secondaryButton {
  background: #edf0ee;
  color: #27302d;
}
</style>
