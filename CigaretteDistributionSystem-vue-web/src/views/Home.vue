<template>
  <div class="layout-container">
    <!-- 左侧菜单栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>卷烟投放管理系统</h2>
      </div>
      <el-menu
        default-active="1"
        class="sidebar-menu"
        background-color="#2c3e50"
        text-color="#ffffff"
        active-text-color="#409EFF"
      >
        <el-menu-item index="1">
          <el-icon><Grid /></el-icon>
          <span>模型投放</span>
        </el-menu-item>
      </el-menu>
    </aside>

     <!-- 主内容区域 -->
     <main class="main-content">
       <!-- 数据导入功能区域 -->
       <section class="import-section">
         <ImportTable 
           @import-success="handleImportSuccess"
           @data-refresh="handleDataRefresh"
         />
       </section>
       
       <!-- 上方数据表格区域 -->
       <section class="data-table-section">
        <DataTable 
          ref="dataTable"
          :search-params="searchParams"
          @row-selected="handleRowSelected"
          @data-loaded="handleDataLoaded"
        />
      </section>

      <!-- 中间表单区域 -->
      <section class="form-section">
        <SearchForm 
          ref="searchForm"
          :selected-record="selectedRecord"
          :table-data="tableData"
          @search="handleSearch"
          @search-next="handleSearchNext"
          @reset="handleReset"
          @export="handleExport"
          @cigarette-name-matched="handleCigaretteNameMatched"
          @position-updated="handlePositionUpdated"
          @area-added="handleAreaAdded"
          @areas-deleted="handleAreasDeleted"
          @refresh-before-filter="handleRefreshBeforeFilter"
        />
      </section>

    </main>
    
  </div>
</template>

<script>
import { Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
// 使用重构后的新组件
import DataTableMain from '../components/datatable/DataTableMain.vue'
import SearchFormMain from '../components/search/SearchFormMain.vue'
import ImportTableMain from '../components/import/ImportTableMain.vue'

export default {
  name: 'Home',
  components: {
    Grid,
    DataTable: DataTableMain,
    SearchForm: SearchFormMain,
    ImportTable: ImportTableMain
  },
  data() {
    return {
      searchParams: {},
      selectedCigaretteName: '',
      currentPositionData: {},
      selectedRecord: null,
      tableData: []
    }
  },
  computed: {},
  methods: {
    handleSearch(searchForm) {
      console.log('搜索参数:', searchForm)
      
      // 更新搜索参数，触发表格和档位设置的更新
      this.searchParams = { 
        year: searchForm.year,
        month: searchForm.month,
        weekSeq: searchForm.week
      }
      
      ElMessage.success(`已查询：${searchForm.year}年${searchForm.month}月第${searchForm.week}周`)
    },
    
    handleDataLoaded(data) {
      // 当表格数据加载完成时
      this.tableData = data
      console.log('表格数据已加载:', data)
      
      // 注意：不在这里清除选中状态，让DataTable组件处理自动选中逻辑
    },
    
    handleRowSelected(row) {
      // 行被选中时，查找该卷烟在当前日期的所有投放记录
      const relatedRecords = this.tableData.filter(record => 
        record.cigName === row.cigName &&
        record.year === row.year &&
        record.month === row.month &&
        record.weekSeq === row.weekSeq
      )
      
      // 收集所有投放区域
      const allAreas = relatedRecords.map(record => record.deliveryArea).filter(area => area)
      
      // 更新选中的卷烟名称和记录
      this.selectedCigaretteName = row.cigName
      
      // 创建包含所有区域信息的选中记录
      this.selectedRecord = {
        ...row,
        allAreas: allAreas,
        totalRecords: relatedRecords.length
      }
      
      
      console.log('选中行及相关记录:', {
        selectedRow: row,
        relatedRecords: relatedRecords,
        allAreas: allAreas
      })
      
      ElMessage.info(`已选中：${row.cigName}，该日期共有 ${relatedRecords.length} 个投放区域`)
    },
    
    
    handleReset() {
      // 重置所有搜索条件和状态
      this.searchParams = {}
      this.selectedCigaretteName = ''
      this.currentPositionData = {}
      this.selectedRecord = null
      this.tableData = []
      
      // 通知DataTable组件清除选中状态
      if (this.$refs.dataTable) {
        this.$refs.dataTable.selectedRow = null
      }
      
      ElMessage.info('已重置查询条件')
    },
    
    handleExport(searchForm) {
      console.log('导出参数:', searchForm)
      
      // 检查是否有数据可导出
      if (!this.tableData || this.tableData.length === 0) {
        ElMessage.warning('暂无数据可导出，请先查询数据')
        return
      }
      
      // 调用DataTable组件的导出功能
      if (this.$refs.dataTable) {
        this.$refs.dataTable.handleExport()
      } else {
        ElMessage.error('导出组件未找到')
      }
    },
    
    handleSearchNext() {
      // 调用DataTable组件的searchNext方法
      if (this.$refs.dataTable) {
        this.$refs.dataTable.searchNext()
      }
    },
    
    handleCigaretteNameMatched(matchedRecords) {
      // 当卷烟名称搜索匹配到记录时，处理多个记录
      console.log('卷烟名称匹配到记录:', matchedRecords)
      
      if (Array.isArray(matchedRecords) && matchedRecords.length > 0) {
        // 取第一个记录作为主要记录
        const primaryRecord = matchedRecords[0]
        
        // 收集所有投放区域
        const allAreas = matchedRecords.map(record => record.deliveryArea).filter(area => area)
        
        // 更新选中的卷烟名称
        this.selectedCigaretteName = primaryRecord.cigName
        
        // 创建包含所有区域信息的选中记录
        this.selectedRecord = {
          ...primaryRecord,
          allAreas: allAreas,
          totalRecords: matchedRecords.length
        }
        
        
        // 同步更新DataTable的选中状态（选中第一个记录）
        if (this.$refs.dataTable) {
          this.$refs.dataTable.scrollToSelectedRecord(primaryRecord)
        }
        
        console.log('搜索匹配选中记录:', {
          primaryRecord: primaryRecord,
          allMatchedRecords: matchedRecords,
          allAreas: allAreas
        })
      }
    },
    
    // 处理档位设置更新
    async handlePositionUpdated(updateInfo) {
      console.log('档位设置已更新:', updateInfo)
      
      try {
        // 刷新表格数据以显示更新后的结果
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 保持当前选中状态
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === updateInfo.cigCode) {
              // 找到更新后的记录并重新选中
              const updatedRecord = this.tableData.find(record => 
                record.cigCode === updateInfo.cigCode &&
                record.year === updateInfo.updateData.year &&
                record.month === updateInfo.updateData.month &&
                record.weekSeq === updateInfo.updateData.weekSeq
              )
              
              if (updatedRecord) {
                this.handleRowSelected(updatedRecord)
              }
            }
          }, 500)
        }
        
        ElMessage.success('表格数据已刷新，显示最新的档位设置')
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('档位设置已保存，但表格刷新失败，请手动刷新')
      }
    },
    
    // 处理新增投放区域
    async handleAreaAdded(addInfo) {
      console.log('新增投放区域:', addInfo)
      
      try {
        // 刷新表格数据以显示新增的记录
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 等待表格数据更新后，重新选中该卷烟的所有记录
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === addInfo.cigCode) {
              // 找到该卷烟在当前日期的所有记录（包括新增的）
              const allRecordsForCigarette = this.tableData.filter(record => 
                record.cigCode === addInfo.cigCode &&
                record.year === this.selectedRecord.year &&
                record.month === this.selectedRecord.month &&
                record.weekSeq === this.selectedRecord.weekSeq
              )
              
              if (allRecordsForCigarette.length > 0) {
                // 选中第一个记录，这会触发所有相关记录的高亮显示
                this.handleRowSelected(allRecordsForCigarette[0])
                
                // 滚动到选中的记录组
                if (this.$refs.dataTable) {
                  this.$refs.dataTable.scrollToSelectedRecord(allRecordsForCigarette[0])
                }
              }
            }
          }, 500)
        }
        
        ElMessage.success(`表格已刷新，新增的投放区域记录已显示并集中相邻排列`)
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('投放区域已新增，但表格刷新失败，请手动刷新')
      }
    },
    
    // 处理删除投放区域
    async handleAreasDeleted(deleteInfo) {
      console.log('删除投放区域:', deleteInfo)
      
      try {
        // 刷新表格数据以移除删除的记录
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 等待表格数据更新后，重新选中该卷烟的剩余记录
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === deleteInfo.cigCode) {
              // 找到该卷烟在当前日期的剩余记录
              const remainingRecordsForCigarette = this.tableData.filter(record => 
                record.cigCode === deleteInfo.cigCode &&
                record.year === this.selectedRecord.year &&
                record.month === this.selectedRecord.month &&
                record.weekSeq === this.selectedRecord.weekSeq
              )
              
              if (remainingRecordsForCigarette.length > 0) {
                // 选中第一个剩余记录，这会触发所有相关记录的高亮显示
                this.handleRowSelected(remainingRecordsForCigarette[0])
                
                // 滚动到选中的记录组
                if (this.$refs.dataTable) {
                  this.$refs.dataTable.scrollToSelectedRecord(remainingRecordsForCigarette[0])
                }
              } else {
                // 如果没有剩余记录，清除选中状态
                this.selectedRecord = null
                this.selectedCigaretteName = ''
              }
            }
          }, 500)
        }
        
        ElMessage.success(`表格已刷新，已删除的投放区域记录已移除`)
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('投放区域已删除，但表格刷新失败，请手动刷新')
      }
    },
    
    // =================== 导入组件事件处理 ===================
    
    // 处理导入成功事件
    handleImportSuccess(event) {
      console.log('导入成功事件:', event)
      
      if (event.type === 'generate-plan' && event.searchParams) {
        // 如果是生成分配方案，自动刷新数据并设置搜索条件
          setTimeout(() => {
            console.log('自动刷新卷烟投放数据统计表，使用生成方案的时间范围...')
            
            const searchParams = {
            year: event.searchParams.year,
            month: event.searchParams.month,
            week: event.searchParams.weekSeq
            }
            
            console.log('搜索参数:', searchParams)
            
            // 更新SearchForm组件的搜索条件，确保界面显示正确的时间范围
            if (this.$refs.searchForm && this.$refs.searchForm.updateSearchForm) {
              this.$refs.searchForm.updateSearchForm(searchParams)
            }
            
            this.handleSearch(searchParams)
            
            // 同时直接刷新DataTable组件作为备用方案
            setTimeout(() => {
              if (this.$refs.dataTable && this.$refs.dataTable.handleRefresh) {
                console.log('直接刷新DataTable组件...')
                this.$refs.dataTable.handleRefresh()
                
                // 显示数据刷新完成的提示
                ElMessage.info({
                  message: '数据已自动刷新，显示最新的分配方案',
                  duration: 2000
                })
              }
            }, 200) // 在搜索后再等200ms刷新表格
            
          }, 1000) // 增加到1秒，确保后端数据已保存
      }
    },
    
    // 处理数据刷新事件
    handleDataRefresh() {
      console.log('数据刷新事件')
      
      // 刷新表格数据
      if (this.$refs.dataTable) {
        this.$refs.dataTable.handleRefresh()
      }
    },
    
    // 处理筛选误差前的刷新事件
    handleRefreshBeforeFilter() {
      console.log('筛选误差前刷新数据')
      
      // 刷新表格数据以获取最新的实际投放量（用户可能修改了档位）
      if (this.$refs.dataTable) {
        this.$refs.dataTable.handleRefresh()
      }
    }
  },
  
  beforeUnmount() {
    // 清理定时器
    if (this.updateTimer) {
      clearTimeout(this.updateTimer)
    }
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #34495e 0%, #2c3e50 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #34495e;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.sidebar-menu {
  flex: 1;
  border: none;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: auto;
  padding: 15px;
  gap: 15px;
  height: calc(100vh - 0px);
}

/* 导入功能区域样式 */
.import-section {
  flex: 0 0 auto;
}

.data-table-section {
  flex: 1;
  min-height: 200px;
  max-height: 25vh;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.form-section {
  flex: 1.5;
  min-height: 300px;
  max-height: 60vh;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow-y: auto;
}

</style>