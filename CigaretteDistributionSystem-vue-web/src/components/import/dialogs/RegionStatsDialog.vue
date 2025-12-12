<template>
  <el-dialog
    v-model="visible"
    title="重建区域客户统计"
    width="520px"
    :close-on-click-modal="false"
  >
    <div class="region-stats-content">
      <el-alert
        title="操作说明"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>将根据当前时间范围的卷烟及客户基础信息，重新构建 `region_customer_statistics` 矩阵。</p>
          <p>耗时操作，执行期间请勿重复提交。</p>
        </template>
      </el-alert>
      
      <el-divider />
      
      <el-form :model="statsForm" label-width="100px">
        <TimeSelector
          v-model:year="statsForm.year"
          v-model:month="statsForm.month"
          v-model:weekSeq="statsForm.weekSeq"
          :disabled="rebuilding"
        />
        
        <el-form-item label="覆盖已有矩阵">
          <el-switch
            v-model="statsForm.overwriteExisting"
            :disabled="rebuilding"
          />
          <div class="form-tip">
            开启后，若目标表已存在会先清空再重建；默认增量更新。
          </div>
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleRebuild"
          :loading="rebuilding"
          :disabled="!canRebuild"
        >
          {{ rebuilding ? '执行中...' : '开始重建' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import TimeSelector from '../components/TimeSelector.vue'
import { useRegionStats } from '@/composables/import/useRegionStats'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'rebuild-success'])

const {
  dialogVisible,
  rebuilding,
  statsForm,
  canRebuild,
  handleRebuild: executeRebuild
} = useRegionStats()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
    dialogVisible.value = value
  }
})

const handleRebuild = async () => {
  const result = await executeRebuild()
  if (result.success) {
    emit('rebuild-success', result.data)
    visible.value = false
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.region-stats-content {
  padding: 10px 0;
}

.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}
</style>
