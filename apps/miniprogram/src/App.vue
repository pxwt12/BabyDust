<script setup lang="ts">
import { provide, ref } from 'vue'
import { messages, type Locale } from './i18n/messages'

const locale = ref<Locale>('zh-CN')

function t(key: string): string {
  return messages[locale.value][key] ?? messages['zh-CN'][key] ?? key
}

provide('locale', locale)
provide('t', t)
</script>

<template>
  <slot />
</template>
