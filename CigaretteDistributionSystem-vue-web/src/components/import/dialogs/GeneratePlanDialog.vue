<template>
  <el-dialog
    v-model="visible"
    title="生成分配方案"
    width="520px"
    :close-on-click-modal="false"
  >
    <div class="generate-plan-content">
      <el-alert
        title="操作说明"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>根据设定的城网/农网比例生成卷烟分配方案。</p>
        </template>
      </el-alert>
      
      <el-divider />
      
      <el-form :model="planForm" label-width="100px">
        <TimeSelector
          v-model:year="planForm.year"
          v-model:month="planForm.month"
          v-model:weekSeq="planForm.weekSeq"
          :disabled="generating"
        />
        
        <el-form-item label="城网比例(%)">
          <el-input-number
            v-model="planForm.urbanRatio"
            :min="0"
            :max="100"
            :step="5"
            :disabled="generating"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="农网比例(%)">
          <el-input-number
            v-model="planForm.ruralRatio"
            :min="0"
            :max="100"
            :step="5"
            :disabled="generating"
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
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleGenerate"
          :loading="generating"
          :disabled="!canGenerate"
        >
          {{ generating ? '生成中...' : '开始生成' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import TimeSelector from '../components/TimeSelector.vue'
import { useGeneratePlan } from '@/composables/import/useGeneratePlan'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'generate-success'])

const {
  dialogVisible,
  generating,
  planForm,
  canGenerate,
  ratioValidationMessage,
  ratioValidationType,
  handleGenerate: executeGenerate
} = useGeneratePlan()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
    dialogVisible.value = value
  }
})

const handleGenerate = async () => {
  const result = await executeGenerate()
  if (result.success) {
    emit('generate-success', result.data)
    visible.value = false
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.generate-plan-content {
  padding: 10px 0;
}

.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}
</style>
