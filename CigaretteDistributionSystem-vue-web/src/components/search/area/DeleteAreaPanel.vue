<template>
  <div 
    v-if="selectedRecord && selectedRecord.allAreas && selectedRecord.allAreas.length > 0" 
    class="delete-area-panel"
  >
    <el-divider content-position="left">
      <el-icon><Delete /></el-icon>
      选择要删除的投放区域
    </el-divider>

    <el-checkbox-group v-model="localAreasToDelete" class="delete-area-checkboxes">
      <el-checkbox 
        v-for="area in selectedRecord.allAreas" 
        :key="area" 
        :value="area"
        :disabled="selectedRecord.allAreas.length <= 1"
      >
        {{ area }}
      </el-checkbox>
    </el-checkbox-group>

    <div class="delete-area-tips">
      <el-alert
        v-if="selectedRecord.allAreas.length <= 1"
        title="无法删除：该卷烟至少需要保留一个投放区域"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else-if="localAreasToDelete.length === 0"
        title="请选择要删除的投放区域"
        type="info"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else
        :title="`已选择 ${localAreasToDelete.length} 个区域进行删除`"
        type="success"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<script>
import { Delete } from '@element-plus/icons-vue'

export default {
  name: 'DeleteAreaPanel',
  components: {
    Delete
  },
  props: {
    selectedRecord: {
      type: Object,
      default: null
    },
    areasToDelete: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:areasToDelete'],
  computed: {
    localAreasToDelete: {
      get() {
        return this.areasToDelete
      },
      set(value) {
        this.$emit('update:areasToDelete', value)
      }
    }
  }
}
</script>

<style scoped>
.delete-area-panel {
  margin-top: 20px;
  padding: 15px;
  background: #fff9f0;
  border-radius: 6px;
  border: 1px solid #ffe7ba;
}

.delete-area-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin: 15px 0;
}

.delete-area-tips {
  margin-top: 15px;
}
</style>
