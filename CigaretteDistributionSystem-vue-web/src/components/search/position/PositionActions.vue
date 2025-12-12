<template>
  <div class="position-actions">
    <el-button 
      type="primary" 
      size="small"
      @click="handleSave"
      :loading="savingPositions"
      :disabled="!isValid"
    >
      <el-icon><Check /></el-icon>
      保存档位设置
    </el-button>

    <el-button 
      type="info" 
      size="small"
      @click="handleReset"
    >
      <el-icon><RefreshLeft /></el-icon>
      重置档位
    </el-button>

    <el-button 
      v-if="showAreaActions"
      type="success" 
      size="small"
      @click="handleAddArea"
      :disabled="!canAddArea"
    >
      <el-icon><Plus /></el-icon>
      新增投放区域
    </el-button>

    <el-button 
      v-if="showAreaActions"
      type="danger" 
      size="small"
      @click="handleDeleteAreas"
      :disabled="!canDeleteAreas"
    >
      <el-icon><Delete /></el-icon>
      删除投放区域
    </el-button>
  </div>
</template>

<script>
import { Check, RefreshLeft, Plus, Delete } from '@element-plus/icons-vue'

export default {
  name: 'PositionActions',
  components: {
    Check,
    RefreshLeft,
    Plus,
    Delete
  },
  props: {
    savingPositions: {
      type: Boolean,
      default: false
    },
    isValid: {
      type: Boolean,
      default: false
    },
    showAreaActions: {
      type: Boolean,
      default: false
    },
    canAddArea: {
      type: Boolean,
      default: false
    },
    canDeleteAreas: {
      type: Boolean,
      default: false
    }
  },
  emits: ['save', 'reset', 'add-area', 'delete-areas'],
  methods: {
    handleSave() {
      this.$emit('save')
    },
    handleReset() {
      this.$emit('reset')
    },
    handleAddArea() {
      this.$emit('add-area')
    },
    handleDeleteAreas() {
      this.$emit('delete-areas')
    }
  }
}
</script>

<style scoped>
.position-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}
</style>
