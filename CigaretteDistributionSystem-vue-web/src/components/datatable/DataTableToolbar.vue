<template>
  <div class="table-toolbar">
    <h3>卷烟投放数据统计表</h3>
    <div class="toolbar-actions">
      <el-button type="primary" size="small" @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button 
        type="success" 
        size="small" 
        @click="handleExport"
        :loading="exportLoading"
        :disabled="!hasData"
      >
        <el-icon><Download /></el-icon>
        {{ exportLoading ? '导出中...' : '导出Excel' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { Refresh, Download } from '@element-plus/icons-vue'

defineProps({
  exportLoading: {
    type: Boolean,
    default: false
  },
  hasData: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh', 'export'])

const handleRefresh = () => {
  emit('refresh')
}

const handleExport = () => {
  emit('export')
}
</script>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.table-toolbar h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}
</style>
