<template>
  <div v-if="selectedRecord && selectedRecord.cigCode" class="position-settings-panel">
    <div class="position-header">
      <el-divider content-position="left">
        <el-icon><Setting /></el-icon>
        档位设置
      </el-divider>

      <div class="position-view-toggle">
        <el-button-group>
          <el-button 
            :type="viewMode === 'grid' ? 'primary' : ''"
            size="small"
            @click="handleViewChange('grid')"
          >
            <el-icon><DataBoard /></el-icon>
            表格视图
          </el-button>
          <el-button 
            :type="viewMode === 'encoding' ? 'primary' : ''"
            size="small"
            @click="handleViewChange('encoding')"
          >
            <el-icon><Document /></el-icon>
            编码视图
          </el-button>
          <el-button 
            size="small"
            @click="handleViewChange('3d')"
            :disabled="!hasChartData"
          >
            <el-icon><TrendCharts /></el-icon>
            三维图表
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 表格视图 -->
    <PositionGridView
      v-if="viewMode === 'grid'"
      :positionData="positionData"
      :disabled="disabled"
      @change="$emit('position-change')"
    />

    <!-- 编码视图 -->
    <PositionEncodingView
      v-if="viewMode === 'encoding'"
      v-model:encodedExpression="localEncodedExpression"
      :selectedRecord="selectedRecord"
      :disabled="encodingDisabled"
      :updating="updatingFromEncoded"
      :isChanged="isEncodedExpressionChanged"
      :validationResult="validationResult"
      :decodedExpression="decodedExpression"
      :hint="encodedExpressionHint"
      :editMode="editMode"
      :hasChanges="hasChanges"
      @input="$emit('encoding-input', $event)"
      @change="$emit('encoding-change')"
      @update="$emit('encoding-update')"
      @reset="$emit('reset-edit-mode')"
    />

    <!-- 3D图表弹窗 -->
    <Position3DChartDialog
      v-model="show3DChart"
      :selectedRecord="selectedRecord"
      :tableData="tableData"
    />
  </div>
</template>

<script>
import { Setting, DataBoard, Document, TrendCharts } from '@element-plus/icons-vue'
import PositionGridView from './PositionGridView.vue'
import PositionEncodingView from './PositionEncodingView.vue'
import Position3DChartDialog from './Position3DChartDialog.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'PositionSettingsPanel',
  components: {
    Setting,
    DataBoard,
    Document,
    TrendCharts,
    PositionGridView,
    PositionEncodingView,
    Position3DChartDialog
  },
  props: {
    selectedRecord: {
      type: Object,
      default: null
    },
    tableData: {
      type: Array,
      default: () => []
    },
    positionData: {
      type: Array,
      required: true
    },
    viewMode: {
      type: String,
      default: 'grid'
    },
    disabled: {
      type: Boolean,
      default: false
    },
    encodingDisabled: {
      type: Boolean,
      default: false
    },
    encodedExpression: {
      type: String,
      default: ''
    },
    updatingFromEncoded: {
      type: Boolean,
      default: false
    },
    isEncodedExpressionChanged: {
      type: Boolean,
      default: false
    },
    validationResult: {
      type: Object,
      default: () => ({})
    },
    decodedExpression: {
      type: String,
      default: ''
    },
    encodedExpressionHint: {
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
  emits: [
    'update:viewMode', 
    'update:encodedExpression',
    'position-change',
    'encoding-input',
    'encoding-change',
    'encoding-update',
    'reset-edit-mode'
  ],
  data() {
    return {
      show3DChart: false
    }
  },
  computed: {
    localEncodedExpression: {
      get() {
        return this.encodedExpression
      },
      set(value) {
        this.$emit('update:encodedExpression', value)
      }
    },
    hasChartData() {
      return this.selectedRecord && this.selectedRecord.cigCode && this.tableData.length > 0
    }
  },
  methods: {
    handleViewChange(mode) {
      if (mode === '3d') {
        if (!this.hasChartData) {
          ElMessage.warning('请先选择一个卷烟记录')
          return
        }
        this.show3DChart = true
      } else {
        this.$emit('update:viewMode', mode)
      }
    }
  }
}
</script>

<style scoped>
.position-settings-panel {
  margin: 20px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.position-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.position-header .el-divider {
  margin: 0;
  flex: 1;
}

.position-view-toggle {
  margin-left: 20px;
}

.position-view-toggle .el-button-group .el-button {
  font-size: 12px;
  padding: 6px 12px;
}
</style>
