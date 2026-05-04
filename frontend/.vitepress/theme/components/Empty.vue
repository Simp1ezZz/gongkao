<template>
  <div class="empty-state">
    <p>{{ text || '暂无数据' }}</p>
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '' }
})
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--vp-c-text-2);
  font-size: 14px;
}
</style>
