<template>
  <!-- 30个档位列，从30档到1档 -->
  <el-table-column 
    v-for="position in positions" 
    :key="`d${position}`" 
    :prop="`d${position}`" 
    :label="`${position}档`" 
    width="50" 
    align="center"
  >
    <template #default="scope">
      <span :class="getCellClass(scope.row[`d${position}`])">
        {{ scope.row[`d${position}`] || 0 }}
      </span>
    </template>
  </el-table-column>
</template>

<script setup>
import { computed } from 'vue'

defineProps({
  positions: {
    type: Array,
    default: () => Array.from({ length: 30 }, (_, i) => 30 - i)
  },
  getCellClass: {
    type: Function,
    required: true
  }
})
</script>

<style scoped>
/* 样式由父组件提供 */
</style>
