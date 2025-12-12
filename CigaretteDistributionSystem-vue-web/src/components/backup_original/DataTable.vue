<template>
  <div class="data-table-container">
    <div class="table-header">
      <h3>卷烟投放数据统计表</h3>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button 
          type="success" 
          size="small" 
          @click="handleExport"
          :loading="exportLoading"
          :disabled="tableData.length === 0"
        >
          <el-icon><Download /></el-icon>
          {{ exportLoading ? '导出中...' : '导出Excel' }}
        </el-button>
      </div>
    </div>
    
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
        <el-table-column label="投放量" width="150" align="center">
          <template #default="scope">
            <div class="metric-line">ADV：{{ formatNumber(scope.row.advAmount) }}</div>
            <div class="metric-line">实际：{{ formatNumber(scope.row.actualDelivery) }}</div>
          </template>
        </el-table-column>
        
        <!-- 30个档位列，从30档开始到1档 -->
        <el-table-column 
          v-for="position in reversedPositions" 
          :key="`d${position}`" 
          :prop="`d${position}`" 
          :label="`${position}档`" 
          width="50" 
          align="center"
        >
          <template #default="scope">
            <span :class="getCellClass(scope.row[`d${position}`])">
              {{ scope.row[`d${position}`] || 0 }}
            </span>
          </template>
        </el-table-column>
        
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

<script>
import { Download, Refresh, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '../services/api'
import { ExcelExporter } from '../utils/excelExport'

export default {
  name: 'DataTable',
  components: {
    Download,
    Refresh,
    DocumentCopy
  },
  props: {
    searchParams: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['row-selected', 'data-loaded'],
  data() {
    return {
      selectedRow: null,
      tableData: [],
      loading: false,
      total: 0,
      originalTotal: 0,
      exportLoading: false
    }
  },
  computed: {
    reversedPositions() {
      // 从30档到1档的顺序
      return Array.from({length: 30}, (_, i) => 30 - i)
    }
  },
  watch: {
    searchParams: {
      handler(newParams) {
        if (newParams && Object.keys(newParams).length > 0) {
          this.loadData(newParams)
        }
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    async loadData(params) {
      // 检查必要的查询参数
      if (!params.year || !params.month || !params.weekSeq) {
        return
      }
      
      this.loading = true
      try {
        const response = await cigaretteDistributionAPI.queryDistribution({
          year: params.year,
          month: params.month,
          weekSeq: params.weekSeq
        })
        
        if (response.data.success) {
          let rawData = response.data.data || []
          this.total = response.data.total || 0
          this.originalTotal = response.data.originalTotal || 0
          
          // 为每条数据添加日期显示字段
          rawData = rawData.map(item => ({
            ...item,
            dateDisplay: this.formatDate(item.year, item.month, item.weekSeq)
          }))
          
          // 对数据进行分组排序，使同一卷烟同一日期的记录聚集在一起
          this.tableData = this.sortDataForGrouping(rawData)
          
          ElMessage.success(`查询成功，共找到 ${this.total} 条记录（原始记录：${this.originalTotal} 条）`)
          this.$emit('data-loaded', this.tableData)
          
          // 如果有数据且只有一条记录，自动选中该记录并显示投放量信息
          if (this.tableData.length === 1) {
            this.handleRowClick(this.tableData[0])
            ElMessage.info('已自动选中唯一记录')
          }
        } else {
          ElMessage.error(response.data.message || '查询失败')
        }
      } catch (error) {
        console.error('查询数据失败:', error)
        ElMessage.error('查询数据失败，请检查网络连接')
      } finally {
        this.loading = false
      }
    },
    
    getCellClass(value) {
      if (!value || value === 0) return ''
      if (value > 30) return 'high-value'
      if (value > 10) return 'medium-value'
      return 'low-value'
    },
    
    getRowClassName({ row, rowIndex }) {
      let className = ''
      
      // 如果是同一卷烟同一日期的记录，使用统一的高亮样式
      if (this.selectedRow) {
        // 更安全的比较逻辑
        const selectedCigCode = String(this.selectedRow.cigCode || '').trim()
        const rowCigCode = String(row.cigCode || '').trim()
        const selectedYear = parseInt(this.selectedRow.year) || 0
        const rowYear = parseInt(row.year) || 0
        const selectedMonth = parseInt(this.selectedRow.month) || 0
        const rowMonth = parseInt(row.month) || 0
        const selectedWeekSeq = parseInt(this.selectedRow.weekSeq) || 0
        const rowWeekSeq = parseInt(row.weekSeq) || 0
        
        const isSameGroup = (
          selectedCigCode === rowCigCode &&
          selectedYear === rowYear &&
          selectedMonth === rowMonth &&
          selectedWeekSeq === rowWeekSeq
        )
        
        
        if (isSameGroup) {
          className += 'selected-group-row '
          
          // 如果是当前精确选中的行，添加额外的选中标识
          const selectedArea = String(this.selectedRow.deliveryArea || '').trim()
          const rowArea = String(row.deliveryArea || '').trim()
          if (selectedArea === rowArea) {
            className += 'current-selected '
          }
        }
      }
      
      // 检查是否是分组的第一行（用于添加上边距分隔）
      if (this.isGroupFirstRow(row, rowIndex)) {
        className += 'group-first-row '
      }
      
      return className.trim()
    },
    
    handleRowClick(row) {
      // 确保selectedRow保存的是原始行数据的完整副本
      this.selectedRow = { ...row }
      
      
      // 确保传递完整的记录信息，包括投放相关信息
      const selectedRecord = {
        ...row,
        // 保持原有的字段映射
        cigCode: row.cigCode,
        cigName: row.cigName,
        year: row.year,
        month: row.month,
        weekSeq: row.weekSeq,
        // 添加投放量信息
        advAmount: row.advAmount,
        actualDelivery: row.actualDelivery,
        // 修正字段名：使用deliveryArea（根据实际API返回）
        deliveryArea: row.deliveryArea,
        // 添加投放类型信息
        deliveryMethod: row.deliveryMethod,
        deliveryEtype: row.deliveryEtype,
        remark: row.remark
      }
      
      // 滚动到选中的行组区域
      this.scrollToSelectedGroup()
      
      console.log('选中行详细信息:', {
        selectedRow: this.selectedRow,
        selectedRecord: selectedRecord
      })
      this.$emit('row-selected', selectedRecord)
    },
    
    handleRefresh() {
      if (this.searchParams && Object.keys(this.searchParams).length > 0) {
        this.loadData(this.searchParams)
      } else {
        ElMessage.warning('请先设置查询条件')
      }
    },
    
    handleExport() {
      if (this.tableData.length === 0) {
        ElMessage.warning('暂无数据可导出')
        return
      }
      
      this.exportLoading = true
      
      try {
        // 调用Excel导出工具类
        const result = ExcelExporter.exportCigaretteData(
          this.tableData, 
          this.searchParams
        )
        
        if (result.success) {
          ElMessage.success(`Excel文件导出成功：${result.filename}`)
        } else {
          ElMessage.error(result.message)
        }
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败，请稍后重试')
      } finally {
        this.exportLoading = false
      }
    },
    
    async copyEncodedExpression(encodedExpression) {
      if (!encodedExpression) {
        ElMessage.warning('暂无编码表达可复制')
        return
      }
      try {
        if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(encodedExpression)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = encodedExpression
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.focus()
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        ElMessage.success('编码表达已复制到剪贴板')
      } catch (error) {
        console.error('复制编码表达失败:', error)
        ElMessage.error('复制失败，请手动选择文本复制')
      }
    },
    
    splitPlusValues(value) {
      if (!value) return []
      return value
        .split('+')
        .map(item => item.trim())
        .filter(Boolean)
    },
    
    formatNumber(value) {
      if (value === null || value === undefined || value === '') {
        return '-'
      }
      return value
    },
    
    formatDate(year, month, weekSeq) {
      // 格式化日期显示：××年××月第×周
      if (year && month && weekSeq) {
        return `${year}年${month}月第${weekSeq}周`
      }
      return ''
    },
    
    sortDataForGrouping(data) {
      // 对数据进行分组排序，使同一卷烟同一日期的记录聚集在一起
      return data.sort((a, b) => {
        // 首先按卷烟名称排序
        if (a.cigName !== b.cigName) {
          return a.cigName.localeCompare(b.cigName, 'zh-CN')
        }
        
        // 然后按年份排序
        if (a.year !== b.year) {
          return a.year - b.year
        }
        
        // 然后按月份排序
        if (a.month !== b.month) {
          return a.month - b.month
        }
        
        // 然后按周序号排序
        if (a.weekSeq !== b.weekSeq) {
          return a.weekSeq - b.weekSeq
        }
        
        // 最后按投放区域排序，确保同组内的顺序稳定
        return (a.deliveryArea || '').localeCompare(b.deliveryArea || '', 'zh-CN')
      })
    },
    
    isGroupFirstRow(row, rowIndex) {
      // 第一行肯定是分组第一行
      if (rowIndex === 0) return true
      
      // 检查当前行与前一行是否属于不同的分组
      const prevRow = this.tableData[rowIndex - 1]
      if (!prevRow) return true
      
      // 比较分组关键字段：卷烟名称、年份、月份、周序号（使用严格类型比较）
      return (
        String(row.cigName || '') !== String(prevRow.cigName || '') ||
        Number(row.year) !== Number(prevRow.year) ||
        Number(row.month) !== Number(prevRow.month) ||
        Number(row.weekSeq) !== Number(prevRow.weekSeq)
      )
    },
    
    scrollToSelectedGroup() {
      // 延迟执行，确保DOM更新完成
      this.$nextTick(() => {
        // 查找第一个选中组的行
        const selectedGroupRow = document.querySelector('.selected-group-row')
        if (selectedGroupRow) {
          // 平滑滚动到该行
          selectedGroupRow.scrollIntoView({
            behavior: 'smooth',
            block: 'center'
          })
        }
      })
    },
    
    // 外部调用的方法，用于从搜索选中时滚动
    scrollToSelectedRecord(record) {
      // 确保selectedRow保存的是记录的完整副本
      this.selectedRow = { ...record }
      this.scrollToSelectedGroup()
    },
    
    // 更新档位数据的方法
    updatePositionData(cigCode, year, month, weekSeq, positionData) {
      const itemIndex = this.tableData.findIndex(row => 
        row.cigCode === cigCode && 
        row.year === year && 
        row.month === month && 
        row.weekSeq === weekSeq
      )
      
      if (itemIndex !== -1) {
        // 创建新的数据对象，确保响应式更新
        const updatedItem = { ...this.tableData[itemIndex] }
        
        // 更新档位数据，将position格式转换为d格式
        Object.keys(positionData).forEach(key => {
          if (key.startsWith('position')) {
            const positionNum = key.replace('position', '')
            const dKey = `d${positionNum}`
            updatedItem[dKey] = positionData[key]
          }
        })
        
        // 使用Vue 3的响应式方式更新数组元素
        this.tableData.splice(itemIndex, 1, updatedItem)
        
        console.log(`表格数据已更新: ${cigCode}`, updatedItem)
        ElMessage.success('档位数据更新成功')
      }
    }
  }
}
</script>

<style scoped>
.data-table-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.table-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
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

/* 高亮行样式 */
:deep(.highlight-row) {
  background-color: #e6f3ff !important;
}

:deep(.highlight-row:hover) {
  background-color: #cce7ff !important;
}

/* 选中组行样式（同一卷烟同一日期的所有记录统一背景） */
:deep(.el-table .el-table__row.selected-group-row) {
  background-color: #e6f3ff !important;
  border-left: 3px solid #409eff !important;
}

:deep(.el-table .el-table__row.selected-group-row:hover) {
  background-color: #d6edff !important;
}

:deep(.el-table .el-table__row--striped.selected-group-row) {
  background-color: #e6f3ff !important;
  border-left: 3px solid #409eff !important;
}

:deep(.el-table .el-table__row--striped.selected-group-row:hover) {
  background-color: #d6edff !important;
}

/* 当前精确选中的行样式（在组内突出显示） */
:deep(.el-table .el-table__row.selected-group-row.current-selected) {
  background-color: #cce7ff !important;
  border-left: 5px solid #1976d2 !important;
  font-weight: 600;
}

:deep(.el-table .el-table__row.selected-group-row.current-selected:hover) {
  background-color: #b3d9ff !important;
}

:deep(.el-table .el-table__row--striped.selected-group-row.current-selected) {
  background-color: #cce7ff !important;
  border-left: 5px solid #1976d2 !important;
  font-weight: 600;
}

:deep(.el-table .el-table__row--striped.selected-group-row.current-selected:hover) {
  background-color: #b3d9ff !important;
}

/* 分组第一行样式（添加上边距分隔） */
:deep(.group-first-row) {
  border-top: 2px solid #e4e7ed !important;
}

/* 如果分组第一行同时是选中组，则使用蓝色分隔线 */
:deep(.selected-group-row.group-first-row) {
  border-top: 2px solid #409eff !important;
}

/* 强制覆盖Element Plus表格的默认背景样式 */
:deep(.el-table__body-wrapper .el-table__body .selected-group-row td) {
  background-color: #e6f3ff !important;
}

:deep(.el-table__body-wrapper .el-table__body .selected-group-row.current-selected td) {
  background-color: #cce7ff !important;
}

/* 编码表达列样式 */
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