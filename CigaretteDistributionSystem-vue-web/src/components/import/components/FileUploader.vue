<template>
  <div class="file-uploader">
    <el-form-item :label="label" required>
      <el-upload
        ref="upload"
        class="file-upload"
        :auto-upload="false"
        :show-file-list="true"
        accept=".xlsx,.xls"
        :limit="1"
        :file-list="localFileList"
        :before-upload="handleBeforeUpload"
        :on-change="handleChange"
        :on-remove="handleRemove"
        :disabled="disabled"
      >
        <el-button type="primary">
          <el-icon><Plus /></el-icon>
          选择Excel文件
        </el-button>
      </el-upload>
      <div class="upload-tip">{{ tip }}</div>
    </el-form-item>
  </div>
</template>

<script>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'FileUploader',
  components: {
    Plus
  },
  props: {
    fileList: {
      type: Array,
      default: () => []
    },
    label: {
      type: String,
      default: '选择文件'
    },
    tip: {
      type: String,
      default: '支持Excel格式(.xlsx, .xls)，文件大小不超过10MB'
    },
    disabled: {
      type: Boolean,
      default: false
    },
    maxSize: {
      type: Number,
      default: 10 // MB
    }
  },
  emits: ['update:fileList', 'change'],
  computed: {
    localFileList: {
      get() {
        return this.fileList
      },
      set(value) {
        this.$emit('update:fileList', value)
      }
    }
  },
  methods: {
    handleBeforeUpload(file) {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel' ||
                      file.name.endsWith('.xlsx') ||
                      file.name.endsWith('.xls')

      if (!isExcel) {
        ElMessage.error('只能上传 Excel 文件 (.xlsx 或 .xls)')
        return false
      }

      const isLtMaxSize = file.size / 1024 / 1024 < this.maxSize
      if (!isLtMaxSize) {
        ElMessage.error(`文件大小不能超过 ${this.maxSize}MB`)
        return false
      }

      return false // 阻止自动上传
    },

    handleChange(file, fileList) {
      if (fileList && fileList.length > 0) {
        const originalFile = file.raw || file
        this.localFileList = [originalFile]
        this.$emit('change', originalFile)
      } else {
        this.localFileList = []
        this.$emit('change', null)
      }
    },

    handleRemove() {
      this.localFileList = []
      this.$emit('change', null)
    }
  }
}
</script>

<style scoped>
.file-uploader {
  margin: 10px 0;
}

.upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
