<template>
  <div class="position-encoding-view">
    <el-divider content-position="left">
      <el-icon><Document /></el-icon>
      编码化表达
    </el-divider>

    <div class="encoded-expression-input-container">
      <el-form-item label="编码表达" class="encoded-expression-form-item">
        <el-input
          v-model="localEncodedExpression"
          type="textarea"
          :rows="6"
          placeholder="显示选中卷烟的所有区域聚合编码表达，每行一个不同的档位设置组合"
          style="width: 600px"
          :readonly="!selectedRecord || !selectedRecord.cigCode || disabled"
          :disabled="disabled"
          @input="handleInput"
          @change="handleChange"
          resize="vertical"
        />

        <el-button 
          type="primary" 
          size="small"
          @click="handleUpdate"
          :loading="updating"
          :disabled="!isChanged || !selectedRecord || !validationResult.isValid"
          style="margin-left: 10px"
        >
          <el-icon><Check /></el-icon>
          更新记录
        </el-button>

        <el-button 
          v-if="hasChanges"
          size="small"
          @click="handleReset"
          style="margin-left: 10px"
        >
          <el-icon><RefreshLeft /></el-icon>
          重置修改
        </el-button>
      </el-form-item>

      <!-- 解码信息显示 -->
      <div v-if="decodedExpression" class="decoded-expression-display">
        <el-alert
          title="解码表达式"
          type="info"
          :closable="false"
          show-icon
          :description="decodedExpression"
        />
      </div>

      <!-- 编码表达式实时验证状态 -->
      <div class="encoded-expression-validation">
        <el-alert
          :title="validationResult.title"
          :type="validationResult.type"
          :description="validationResult.message"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 编码表达提示信息 -->
      <div v-if="hint" class="encoded-expression-hint">
        <el-alert
          :title="hint"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 编辑模式状态显示 -->
      <div v-if="editMode !== 'none'" class="edit-mode-status">
        <el-alert
          v-if="editMode === 'encoding'"
          title="编码修改模式"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>当前正在修改编码表达式，其他表单字段已被锁定</p>
            <p>要修改其他字段，请先点击"重置修改"按钮</p>
          </template>
        </el-alert>

        <el-alert
          v-if="editMode === 'form'"
          title="表单修改模式"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>当前正在修改表单设置，编码表达式字段已被锁定</p>
            <p>要修改编码表达式，请先点击"重置修改"按钮</p>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script>
import { Document, Check, RefreshLeft } from '@element-plus/icons-vue'

export default {
  name: 'PositionEncodingView',
  components: {
    Document,
    Check,
    RefreshLeft
  },
  props: {
    encodedExpression: {
      type: String,
      default: ''
    },
    selectedRecord: {
      type: Object,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    },
    updating: {
      type: Boolean,
      default: false
    },
    isChanged: {
      type: Boolean,
      default: false
    },
    validationResult: {
      type: Object,
      default: () => ({ isValid: true, type: 'info', title: '', message: '' })
    },
    decodedExpression: {
      type: String,
      default: ''
    },
    hint: {
      type: String,
      default: ''
    },
    editMode: {
      type: String,
      default: 'none'
    },
    hasChanges: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:encodedExpression', 'input', 'change', 'update', 'reset'],
  computed: {
    localEncodedExpression: {
      get() {
        return this.encodedExpression
      },
      set(value) {
        this.$emit('update:encodedExpression', value)
      }
    }
  },
  methods: {
    handleInput(value) {
      this.$emit('input', value)
    },
    handleChange() {
      this.$emit('change')
    },
    handleUpdate() {
      this.$emit('update')
    },
    handleReset() {
      this.$emit('reset')
    }
  }
}
</script>

<style scoped>
.position-encoding-view {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.encoded-expression-input-container {
  margin-top: 15px;
}

.encoded-expression-form-item {
  margin-bottom: 15px;
}

.decoded-expression-display,
.encoded-expression-validation,
.encoded-expression-hint,
.edit-mode-status {
  margin-top: 15px;
}
</style>
