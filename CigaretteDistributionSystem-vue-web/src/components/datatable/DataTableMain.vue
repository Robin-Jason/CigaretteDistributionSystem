<template>
  <div class="data-table-main">
    <DataTableToolbar
      :export-loading="exportLoading"
      :has-data="tableData.length > 0"
      @refresh="handleRefresh"
      @export="handleExport"
    />
    
    <div class="table-content" v-if="tableData.length > 0">
      <el-table
        :data="tableData"
        style="width: 100%"
        height="100%"
        border
        stripe
        size="default"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        :row-class-name="getRowClassName"
        @row-click="handleRowClick"
        highlight-current-row
        v-loading="loading"
        element-loading-text="正在加载数据..."
      >
        <el-table-column prop="cigCode" label="卷烟代码" width="100" align="center" />
        <el-table-column prop="cigName" label="卷烟名称" width="180" />
        <el-table-column prop="dateDisplay" label="日期" width="150" align="center" />
        
        <!-- 编码表达列 -->
        <el-table-column prop="encodedExpression" label="编码表达" width="230" align="center">
          <template #default="scope">
            <div v-if="scope.row.encodedExpression" class="encoded-expression-cell">
              <el-tooltip :content="scope.row.decodedExpression || '暂无解码信息'" placement="top">
                <span class="encoded-expression">{{ scope.row.encodedExpression }}</span>
              </el-tooltip>
              <el-icon 
                class="encoded-copy-icon" 
                @click.stop="copyEncodedExpression(scope.row.encodedExpression)"
              >
                <DocumentCopy />
              </el-icon>
            </div>
            <span v-else class="no-encoding">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="deliveryArea" label="投放区域" width="200" />
        
        <!-- 投放组合列 -->
        <el-table-column label="投放组合" min-width="200">
          <template #default="scope">
            <div class="combo-info">
              <div class="combo-method">{{ scope.row.deliveryMethod || '—' }}</div>
              <div class="combo-extensions" v-if="scope.row.deliveryEtype">
                <el-tag
                  v-for="ext in splitPlusValues(scope.row.deliveryEtype)"
                  :key="`${scope.row.cigCode}-ext-${ext}`"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ ext }}
                </el-tag>
              </div>
              <div class="combo-tags" v-if="scope.row.tag">
                <el-tag
                  v-for="tag in splitPlusValues(scope.row.tag)"
                  :key="`${scope.row.cigCode}-tag-${tag}`"
                  size="small"
                  type="success"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <!-- 投放量列 -->
        <el-table-column label="投放量" width="150" align="center">
          <template #default="scope">
            <div class="metric-line">ADV：{{ formatNumber(scope.row.advAmount) }}</div>
            <div class="metric-line">实际：{{ formatNumber(scope.row.actualDelivery) }}</div>
          </template>
        </el-table-column>
        
        <!-- 30个档位列 -->
        <PositionColumns 
          :positions="reversedPositions"
          :get-cell-class="getCellClass"
        />
        
        <el-table-column prop="remark" label="备注" min-width="120" />
      </el-table>
    </div>
    
    <div v-else class="no-data-tip">
      <el-empty description="暂无数据" :image-size="100">
        <template #description>
          <span>请先输入查询条件进行查询</span>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { watch, onMounted } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import DataTableToolbar from './DataTableToolbar.vue'
import PositionColumns from './columns/PositionColumns.vue'

import { useDataTable } from '@/composables/datatable/useDataTable'
import { useTableExport } from '@/composables/datatable/useTableExport'
import { useTableFormatter } from '@/composables/datatable/useTableFormatter'

const props = defineProps({
  searchParams: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['row-selected', 'data-loaded'])

// Composables
const {
  selectedRow,
  tableData,
  loading,
  reversedPositions,
  loadData,
  handleRowClick: selectRow,
  getRowClassName,
  updatePositionData
} = useDataTable()

const { exportLoading, handleExport: exportData } = useTableExport()

const {
  getCellClass,
  copyEncodedExpression,
  splitPlusValues,
  formatNumber
} = useTableFormatter()

// 监听搜索参数变化
watch(() => props.searchParams, async (newParams) => {
  if (newParams && Object.keys(newParams).length > 0) {
    const result = await loadData(newParams)
    if (result.success) {
      emit('data-loaded', tableData.value)
      
      // 如果只有一条记录，自动选中
      if (tableData.value.length === 1) {
        handleRowClick(tableData.value[0])
        ElMessage.info('已自动选中唯一记录')
      }
    }
  }
}, { deep: true, immediate: true })

// 事件处理
const handleRowClick = (row) => {
  const selectedRecord = selectRow(row)
  emit('row-selected', selectedRecord)
}

const handleRefresh = () => {
  if (props.searchParams && Object.keys(props.searchParams).length > 0) {
    loadData(props.searchParams)
  } else {
    ElMessage.warning('请先设置查询条件')
  }
}

const handleExport = () => {
  exportData(tableData.value, props.searchParams)
}

// 暴露方法供父组件调用
defineExpose({
  loadData,
  updatePositionData
})
</script>

<style scoped>
.data-table-main {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.table-content {
  flex: 1;
  overflow: hidden;
  max-height: calc(35vh - 100px);
}

.no-data-tip {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table) {
  font-size: 11px;
}

:deep(.el-table th) {
  padding: 8px 0;
}

:deep(.el-table td) {
  padding: 4px 0;
}

.high-value {
  color: #e6a23c;
  font-weight: bold;
}

.medium-value {
  color: #409eff;
}

.low-value {
  color: #909399;
}

/* 选中组行样式 */
:deep(.el-table .el-table__row.selected-group-row) {
  background-color: #e6f3ff !important;
  border-left: 3px solid #409eff !important;
}

:deep(.el-table .el-table__row.selected-group-row:hover) {
  background-color: #d6edff !important;
}

:deep(.el-table .el-table__row.selected-group-row.current-selected) {
  background-color: #cce7ff !important;
  border-left: 5px solid #1976d2 !important;
  font-weight: 600;
}

:deep(.group-first-row) {
  border-top: 2px solid #e4e7ed !important;
}

:deep(.selected-group-row.group-first-row) {
  border-top: 2px solid #409eff !important;
}

.encoded-expression {
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.encoded-expression:hover {
  background: rgba(64, 158, 255, 0.2);
  color: #1976d2;
}

.encoded-expression-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.encoded-copy-icon {
  cursor: pointer;
  color: #909399;
  font-size: 14px;
  transition: color 0.2s;
}

.encoded-copy-icon:hover {
  color: #409eff;
}

.no-encoding {
  color: #c0c4cc;
  font-style: italic;
}

.combo-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
  line-height: 1.4;
}

.combo-method {
  font-weight: 600;
  color: #303133;
}

.combo-extensions,
.combo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.metric-line {
  line-height: 1.5;
}
</style>
