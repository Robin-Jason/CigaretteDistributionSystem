<template>
  <el-dialog
    v-model="visible"
    title="导入卷烟投放基本信息"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form :model="timeForm" label-width="80px">
      <TimeSelector
        v-model:year="timeForm.year"
        v-model:month="timeForm.month"
        v-model:weekSeq="timeForm.weekSeq"
        :disabled="importing"
      />
      
      <el-form-item label="覆盖已有表">
        <el-switch 
          v-model="timeForm.overwrite"
          :disabled="importing"
        />
        <div class="form-tip">开启后将覆盖同年/月/周的旧表；默认保留旧表并在失败时提示。</div>
      </el-form-item>
      
      <FileUploader
        v-model:fileList="fileList"
        label="选择文件"
        :disabled="importing"
        @change="handleFileChange"
      />
      
      <!-- 表结构说明 -->
      <el-form-item label="表结构要求">
        <el-alert
          title="Excel文件必须包含以下列（大小写敏感）"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="structure-requirements">
              <p><strong>基本信息列：</strong></p>
              <ul>
                <li><code>CIG_CODE</code> - 卷烟代码</li>
                <li><code>CIG_NAME</code> - 卷烟名称</li>
                <li><code>YEAR</code> - 年份</li>
                <li><code>MONTH</code> - 月份</li>
                <li><code>WEEK_SEQ</code> - 周序号</li>
                <li><code>URS</code> - URS</li>
                <li><code>ADV</code> - ADV</li>
                <li><code>DELIVERY_METHOD</code> - 档位投放方式</li>
                <li><code>DELIVERY_ETYPE</code> - 扩展投放方式</li>
                <li><code>DELIVERY_AREA</code> - 投放区域</li>
                <li><code>remark</code> - 备注</li>
              </ul>
              <p><strong>可选列：</strong></p>
              <ul>
                <li><code>id</code> - 主键ID（可选，系统会自动生成）</li>
              </ul>
            </div>
          </template>
        </el-alert>
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

<script>
import TimeSelector from '../components/TimeSelector.vue'
import FileUploader from '../components/FileUploader.vue'
import { useBasicInfoImport } from '@/composables/import/useBasicInfoImport'

export default {
  name: 'BasicInfoImportDialog',
  components: {
    TimeSelector,
    FileUploader
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'import-success'],
  setup(props, { emit }) {
    const {
      dialogVisible,
      fileList,
      importing,
      timeForm,
      canImport,
      handleImport: executeImport
    } = useBasicInfoImport()

    // 同步props和composable的visible状态
    const visible = computed({
      get: () => props.modelValue,
      set: (value) => {
        emit('update:modelValue', value)
        dialogVisible.value = value
      }
    })

    const handleFileChange = (file) => {
      // 文件变化处理
    }

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

    return {
      visible,
      fileList,
      importing,
      timeForm,
      canImport,
      handleFileChange,
      handleImport,
      handleClose
    }
  }
}
</script>

<script setup>
import { computed } from 'vue'
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

.structure-requirements li {
  margin: 3px 0;
}

.structure-requirements code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e6a23c;
  font-family: monospace;
}
</style>
