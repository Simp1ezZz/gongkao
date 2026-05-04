<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-content">
        <button class="modal-close" @click="$emit('close')">&times;</button>
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineEmits(['close'])
</script>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5); display: flex;
  align-items: center; justify-content: center; z-index: 1000;
}
.modal-content {
  background: var(--vp-c-bg); border-radius: 12px;
  padding: 24px; max-width: 480px; width: 90%;
  position: relative; max-height: 80vh; overflow-y: auto;
}
.modal-close {
  position: absolute; top: 12px; right: 16px;
  background: none; border: none; font-size: 24px;
  cursor: pointer; color: var(--vp-c-text-2); line-height: 1;
}
</style>
