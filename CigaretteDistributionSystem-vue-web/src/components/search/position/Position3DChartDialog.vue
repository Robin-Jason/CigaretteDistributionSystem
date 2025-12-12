<template>
  <el-dialog
    v-model="visible"
    :title="`${cigaretteName} - 各区域档位投放分布`"
    width="95%"
    height="85vh"
    :destroy-on-close="true"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    :show-close="true"
    draggable
    class="chart-dialog"
    @opened="handleOpened"
  >
    <Position3DChart 
      ref="position3DChart"
      :selectedRecord="selectedRecord"
      :tableData="tableData"
      style="height: 1125px;"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button 
          type="primary" 
          @click="handleExport"
          :disabled="!hasChartData"
        >
          导出图表
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import Position3DChart from '@/components/Position3DChart.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'Position3DChartDialog',
  components: {
    Position3DChart
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    selectedRecord: {
      type: Object,
      default: null
    },
    tableData: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  computed: {
    visible: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    },
    cigaretteName() {
      return (this.selectedRecord && this.selectedRecord.cigName) || '卷烟'
    },
    hasChartData() {
      return this.selectedRecord && this.selectedRecord.cigCode && this.tableData.length > 0
    }
  },
  methods: {
    handleOpened() {
      console.log('Dialog opened, reinitializing chart...')
      // 对话框完全打开后重新初始化图表
      if (this.$refs.position3DChart) {
        this.$refs.position3DChart.reinitChart()
      }
    },
    handleClose() {
      this.visible = false
    },
    handleExport() {
      ElMessage.info('图表导出功能正在开发中...')
    }
  }
}
</script>

<style scoped>
.chart-dialog :deep(.el-dialog__header) {
  background: linear-gradient(90deg, #409eff, #67c23a);
  color: white;
  padding: 20px 24px;
  border-radius: 8px 8px 0 0;
}

.chart-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.chart-dialog :deep(.el-dialog__close) {
  color: white;
  font-size: 18px;
}

.chart-dialog :deep(.el-dialog__close:hover) {
  color: #f0f0f0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
