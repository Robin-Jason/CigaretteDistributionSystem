<template>
  <el-dialog
    v-model="visible"
    title="导入客户基础信息表"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form :model="importForm" label-width="120px">
      <FileUploader
        v-model:fileList="fileList"
        label="选择文件"
        tip="仅需上传客户基础信息Excel文件，大小≤10MB"
        :disabled="importing"
      />
      
      <!-- 表结构说明 -->
      <el-form-item label="导入要求">
        <el-alert
          title="Excel首行需为表头，系统自动规范列名"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="structure-requirements">
              <p><strong>必填列：</strong></p>
              <ul>
                <li><code>CUST_CODE</code> - 客户唯一编码，缺失将导致整行被忽略</li>
              </ul>
              <p><strong>可选列：</strong>可自定义客户属性列（如 <code>MARKET_TYPE</code>、<code>CUST_FORMAT</code> 等），系统会自动扩展表结构并写入。</p>
              <p><strong>处理策略：</strong>以 <code>CUST_CODE</code> 作为唯一键执行 upsert，空白行/缺失编码记录会跳过并记录日志。</p>
            </div>
          </template>
        </el-alert>
      </el-form-item>
      
      <el-form-item label="工作表索引">
        <el-input-number
          v-model="importForm.sheetIndex"
          :min="0"
          :step="1"
          :disabled="importing"
          style="width: 100%"
        />
        <div class="form-tip">默认读取首个工作表，如需切换请填写对应索引（从0开始）。</div>
      </el-form-item>
      
      <el-form-item label="跳过表头行数">
        <el-input-number
          v-model="importForm.skipHeaderRows"
          :min="0"
          :step="1"
          :disabled="importing"
          style="width: 100%"
        />
        <div class="form-tip">用于忽略多余标题或说明行，默认跳过1行。</div>
      </el-form-item>
      
      <el-form-item label="覆盖策略">
        <el-select
          v-model="importForm.overwriteMode"
          placeholder="请选择覆盖策略"
          :disabled="importing"
          style="width: 100%"
        >
          <el-option label="追加导入（APPEND）" value="APPEND" />
          <el-option label="覆盖重建（REPLACE）" value="REPLACE" />
        </el-select>
        <div class="form-tip">
          APPEND：对现有数据做增量 upsert；REPLACE：先清空再全量写入（谨慎使用）。
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleImport"
          :loading="importing"
          :disabled="!canImport"
        >
          确定导入
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import FileUploader from '../components/FileUploader.vue'
import { useCustomerInfoImport } from '@/composables/import/useCustomerInfoImport'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'import-success'])

const {
  dialogVisible,
  fileList,
  importing,
  importForm,
  canImport,
  handleImport: executeImport
} = useCustomerInfoImport()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
    dialogVisible.value = value
  }
})

const handleImport = async () => {
  const result = await executeImport()
  if (result.success) {
    emit('import-success', result.data)
    visible.value = false
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.structure-requirements {
  font-size: 13px;
}

.structure-requirements ul {
  margin: 5px 0;
  padding-left: 20px;
}

.structure-requirements code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e6a23c;
}
</style>
