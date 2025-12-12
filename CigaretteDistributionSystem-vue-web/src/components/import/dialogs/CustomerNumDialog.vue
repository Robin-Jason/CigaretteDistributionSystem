<template>
  <el-dialog
    v-model="visible"
    title="计算区域客户数"
    width="520px"
    :close-on-click-modal="false"
  >
    <div class="customer-num-content">
      <el-alert
        title="操作说明"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>根据客户类型和工作日计算各区域的客户数量。</p>
        </template>
      </el-alert>
      
      <el-divider />
      
      <el-form :model="calcForm" label-width="100px">
        <TimeSelector
          v-model:year="calcForm.year"
          v-model:month="calcForm.month"
          v-model:weekSeq="calcForm.weekSeq"
          :disabled="calculating"
        />
        
        <el-form-item label="客户类型" required>
          <el-select
            v-model="calcForm.customerTypes"
            placeholder="请选择客户类型"
            multiple
            style="width: 100%"
            :disabled="calculating"
          >
            <el-option label="城网" value="城网" />
            <el-option label="农网" value="农网" />
            <el-option label="便利店" value="便利店" />
            <el-option label="超市" value="超市" />
          </el-select>
          <div class="form-tip">选择需要计算的客户类型</div>
        </el-form-item>
        
        <el-form-item label="工作日" required>
          <el-select
            v-model="calcForm.workdays"
            placeholder="请选择工作日"
            multiple
            style="width: 100%"
            :disabled="calculating"
          >
            <el-option 
              v-for="day in 7" 
              :key="day" 
              :label="`星期${day === 7 ? '日' : day}`" 
              :value="day"
            />
          </el-select>
          <div class="form-tip">选择统计的工作日范围</div>
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleCalculate"
          :loading="calculating"
          :disabled="!canCalculate"
        >
          {{ calculating ? '计算中...' : '开始计算' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import TimeSelector from '../components/TimeSelector.vue'
import { useCustomerNumCalc } from '@/composables/import/useCustomerNumCalc'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'calc-success'])

const {
  dialogVisible,
  calculating,
  calcForm,
  canCalculate,
  handleCalculate: executeCalculate
} = useCustomerNumCalc()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
    dialogVisible.value = value
  }
})

const handleCalculate = async () => {
  const result = await executeCalculate()
  if (result.success) {
    emit('calc-success', result.data)
    visible.value = false
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.customer-num-content {
  padding: 10px 0;
}

.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}
</style>
