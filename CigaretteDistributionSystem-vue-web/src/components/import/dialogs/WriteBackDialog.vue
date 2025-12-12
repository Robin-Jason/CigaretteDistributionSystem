<template>
  <el-dialog
    v-model="visible"
    title="执行策略并写回预测表"
    width="520px"
    :close-on-click-modal="false"
  >
    <div class="write-back-content">
      <el-alert
        title="操作说明"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>执行分配策略算法，并将计算结果写回到预测表。</p>
          <p>此操作会修改数据库，请谨慎执行。</p>
        </template>
      </el-alert>
      
      <el-divider />
      
      <el-form :model="writeBackForm" label-width="100px">
        <TimeSelector
          v-model:year="writeBackForm.year"
          v-model:month="writeBackForm.month"
          v-model:weekSeq="writeBackForm.weekSeq"
          :disabled="writingBack"
        />
        
        <el-form-item label="启用比例分配">
          <el-switch
            v-model="writeBackForm.enableRatio"
            :disabled="writingBack"
          />
          <div class="form-tip">启用后将按城网/农网比例分配投放量</div>
        </el-form-item>
        
        <template v-if="writeBackForm.enableRatio">
          <el-form-item label="城网比例(%)">
            <el-input-number
              v-model="writeBackForm.urbanRatio"
              :min="0"
              :max="100"
              :step="5"
              :disabled="writingBack"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="农网比例(%)">
            <el-input-number
              v-model="writeBackForm.ruralRatio"
              :min="0"
              :max="100"
              :step="5"
              :disabled="writingBack"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item label="比例验证">
            <el-alert
              :title="ratioValidationMessage"
              :type="ratioValidationType"
              :closable="false"
              show-icon
            />
          </el-form-item>
        </template>
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleWriteBack"
          :loading="writingBack"
          :disabled="!canWriteBack"
        >
          {{ writingBack ? '执行中...' : '执行写回' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import TimeSelector from '../components/TimeSelector.vue'
import { useWriteBack } from '@/composables/import/useWriteBack'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'writeback-success'])

const {
  dialogVisible,
  writingBack,
  writeBackForm,
  canWriteBack,
  ratioValidationMessage,
  ratioValidationType,
  handleWriteBack: executeWriteBack
} = useWriteBack()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
    dialogVisible.value = value
  }
})

const handleWriteBack = async () => {
  const result = await executeWriteBack()
  if (result.success) {
    emit('writeback-success', result.data)
    visible.value = false
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.write-back-content {
  padding: 10px 0;
}

.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}
</style>
