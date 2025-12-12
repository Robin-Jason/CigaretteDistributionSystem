<template>
  <div class="import-table-main">
    <!-- 导入功能按钮区域 -->
    <div class="import-buttons-row">
      <el-button 
        type="primary" 
        size="default"
        @click="showBasicInfoDialog"
      >
        <el-icon><DocumentAdd /></el-icon>
        导入卷烟投放基本信息
      </el-button>
      
      <el-button 
        type="success" 
        size="default"
        @click="showCustomerInfoDialog"
      >
        <el-icon><DataAnalysis /></el-icon>
        导入客户基础信息表
      </el-button>
      
      <el-button 
        type="info" 
        size="default"
        @click="calculateCustomerNumDialogVisible = true"
      >
        <el-icon><Operation /></el-icon>
        计算区域客户数
      </el-button>
      
      <el-button 
        type="primary" 
        plain
        size="default"
        @click="showRegionStatsDialog"
      >
        <el-icon><Histogram /></el-icon>
        重建区域客户统计
      </el-button>
      
      <el-button 
        type="danger" 
        size="default"
        @click="writeBackDialogVisible = true"
      >
        <el-icon><UploadFilled /></el-icon>
        执行策略写回
      </el-button>
      
      <el-button 
        type="warning" 
        size="default"
        @click="generatePlanDialogVisible = true"
      >
        <el-icon><Cpu /></el-icon>
        生成分配方案
      </el-button>
    </div>

    <!-- 对话框组件 -->
    <BasicInfoImportDialog
      v-model="basicInfoDialogVisible"
      @import-success="handleImportSuccess"
    />

    <CustomerInfoImportDialog
      v-model="customerInfoDialogVisible"
      @import-success="handleImportSuccess"
    />

    <RegionStatsDialog
      v-model="regionStatsDialogVisible"
      @rebuild-success="handleRebuildSuccess"
    />

    <WriteBackDialog
      v-model="writeBackDialogVisible"
      @writeback-success="handleWriteBackSuccess"
    />

    <CustomerNumDialog
      v-model="calculateCustomerNumDialogVisible"
      @calc-success="handleCalcSuccess"
    />

    <GeneratePlanDialog
      v-model="generatePlanDialogVisible"
      @generate-success="handleGenerateSuccess"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  DocumentAdd, 
  DataAnalysis, 
  Operation, 
  Histogram, 
  UploadFilled, 
  Cpu 
} from '@element-plus/icons-vue'

import BasicInfoImportDialog from './dialogs/BasicInfoImportDialog.vue'
import CustomerInfoImportDialog from './dialogs/CustomerInfoImportDialog.vue'
import RegionStatsDialog from './dialogs/RegionStatsDialog.vue'
import WriteBackDialog from './dialogs/WriteBackDialog.vue'
import CustomerNumDialog from './dialogs/CustomerNumDialog.vue'
import GeneratePlanDialog from './dialogs/GeneratePlanDialog.vue'

const emit = defineEmits([
  'import-success', 
  'rebuild-success', 
  'writeback-success',
  'calc-success',
  'generate-success',
  'operation-complete'
])

// 对话框可见性状态
const basicInfoDialogVisible = ref(false)
const customerInfoDialogVisible = ref(false)
const regionStatsDialogVisible = ref(false)
const calculateCustomerNumDialogVisible = ref(false)
const writeBackDialogVisible = ref(false)
const generatePlanDialogVisible = ref(false)

// 显示对话框
const showBasicInfoDialog = () => {
  basicInfoDialogVisible.value = true
}

const showCustomerInfoDialog = () => {
  customerInfoDialogVisible.value = true
}

const showRegionStatsDialog = () => {
  regionStatsDialogVisible.value = true
}

// 事件处理
const handleImportSuccess = (data) => {
  emit('import-success', data)
}

const handleRebuildSuccess = (data) => {
  emit('rebuild-success', data)
}

const handleWriteBackSuccess = (data) => {
  emit('writeback-success', data)
}

const handleCalcSuccess = (data) => {
  emit('calc-success', data)
}

const handleGenerateSuccess = (data) => {
  emit('generate-success', data)
}
</script>

<style scoped>
.import-table-main {
  padding: 15px;
}

.import-buttons-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}
</style>
