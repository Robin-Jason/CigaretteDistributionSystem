<template>
  <div class="search-form-container">
    <el-form :model="searchForm" :inline="true" class="search-form">
      <el-form-item label="年份">
        <el-select
          v-model="searchForm.year"
          placeholder="选择或输入年份"
          style="width: 120px"
          clearable
          filterable
          allow-create
          default-first-option
        >
          <el-option 
            v-for="year in yearOptions" 
            :key="year" 
            :label="year" 
            :value="year"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="月份">
        <el-select
          v-model="searchForm.month"
          placeholder="请选择月份"
          style="width: 120px"
          clearable
        >
          <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="month" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="周序号">
        <el-input-number
          v-model="searchForm.week"
          placeholder="请输入周序号"
          style="width: 120px"
          :min="1"
          :max="5"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="卷烟名称">
        <el-input
          v-model="searchForm.cigaretteName"
          :placeholder="cigaretteNamePlaceholder"
          style="width: 200px"
          clearable
          :disabled="!isDateComplete"
          @input="handleCigaretteNameInput"
          @change="handleCigaretteNameChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      
      <el-form-item label="投放类型">
        <el-select
          v-model="searchForm.distributionType"
          placeholder="请选择投放类型"
          style="width: 180px"
          clearable
          :disabled="isFormDisabled"
          @change="handleDistributionTypeChange"
        >
          <el-option label="按档位统一投放" value="按档位统一投放" />
          <el-option label="按档位扩展投放" value="按档位扩展投放" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="扩展投放类型" v-if="searchForm.distributionType === '按档位扩展投放'">
        <el-select
          v-model="searchForm.extendedType"
          placeholder="请选择扩展类型"
          style="width: 160px"
          clearable
          :disabled="isFormDisabled"
          @change="handleExtendedTypeChange"
        >
          <el-option label="档位+区县" value="档位+区县" />
          <el-option label="档位+市场类型" value="档位+市场类型" />
          <el-option label="档位+城乡分类代码" value="档位+城乡分类代码" />
          <el-option label="档位+业态" value="档位+业态" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="投放区域">
        <el-select
          v-model="searchForm.distributionArea"
          :placeholder="areaPlaceholder"
          style="width: 300px"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          :disabled="isFormDisabled"
          @change="handleDistributionAreaChange"
        >
          <el-option
            v-for="option in deliveryAreaOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      
      <!-- 新增：预投放量和实际投放量显示框（仅在选中卷烟后显示） -->
      <el-form-item label="预投放量" v-if="selectedRecord && selectedRecord.cigCode">
        <el-input
          :value="formatQuantity(selectedRecord.advAmount)"
          style="width: 150px"
          readonly
          suffix-icon="el-icon-info"
        />
      </el-form-item>
      
      <el-form-item label="实际投放量" v-if="selectedRecord && selectedRecord.cigCode">
        <el-input
          :value="formatQuantity(selectedRecord.actualDelivery)"
          style="width: 150px"
          readonly
          suffix-icon="el-icon-info"
        />
      </el-form-item>
      
      <!-- 档位设置区域（仅在选中卷烟后显示） -->
      <div v-if="selectedRecord && selectedRecord.cigCode" class="position-settings-section">
        <div class="position-header">
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          档位设置
        </el-divider>
        
          <div class="position-view-toggle">
            <el-button-group>
              <el-button 
                :type="positionViewMode === 'grid' ? 'primary' : ''"
                size="small"
                @click="switchPositionView('grid')"
              >
                <el-icon><DataBoard /></el-icon>
                表格视图
              </el-button>
              <el-button 
                :type="positionViewMode === 'encoding' ? 'primary' : ''"
                size="small"
                @click="switchPositionView('encoding')"
              >
                <el-icon><Document /></el-icon>
                编码视图
              </el-button>
              <el-button 
                size="small"
                @click="switchPositionView('3d')"
                :disabled="!hasChartData"
              >
                <el-icon><TrendCharts /></el-icon>
                三维图表
              </el-button>
            </el-button-group>
          </div>
        </div>
        
        <!-- 表格视图 -->
        <div v-if="positionViewMode === 'grid'" class="position-grid">
          <div v-for="(position, index) in positionData" :key="`d${30 - index}`" class="position-item">
            <span class="position-label">D{{ 30 - index }}:</span>
            <el-input-number
              v-model="positionData[index]"
              :min="0"
              :precision="0"
              :step="1"
              size="small"
              style="width: 90px"
              :disabled="isFormDisabled"
              @change="handlePositionChange"
            />
          </div>
        </div>
        
        <!-- 三维图表弹窗 -->
        <el-dialog
          v-model="show3DChart"
          :title="`${(selectedRecord && selectedRecord.cigName) || '卷烟'} - 各区域档位投放分布`"
          width="95%"
          height="85vh"
          :destroy-on-close="true"
          :close-on-click-modal="false"
          :close-on-press-escape="true"
          :show-close="true"
          draggable
          class="chart-dialog"
          @opened="handleDialogOpened"
        >
          <Position3DChart 
            ref="position3DChart"
            :selectedRecord="selectedRecord"
            :tableData="tableData"
            style="height: 1125px;"
          />
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="show3DChart = false">关闭</el-button>
              <el-button 
                type="primary" 
                @click="exportChart"
                :disabled="!hasChartData"
              >
                导出图表
              </el-button>
            </div>
          </template>
        </el-dialog>
        
        <!-- 编码化表达输入区域 (仅编码视图时显示) -->
        <div v-if="positionViewMode === 'encoding'" class="encoded-expression-section">
          <el-divider content-position="left">
            <el-icon><Document /></el-icon>
            编码化表达
          </el-divider>
          
          <div class="encoded-expression-input-container">
            <el-form-item label="编码表达" class="encoded-expression-form-item">
              <el-input
                v-model="encodedExpressionInput"
                type="textarea"
                :rows="6"
                placeholder="显示选中卷烟的所有区域聚合编码表达，每行一个不同的档位设置组合"
                style="width: 600px"
                :readonly="!selectedRecord || !selectedRecord.cigCode || isEncodingDisabled"
                :disabled="isEncodingDisabled"
                @input="handleEncodedExpressionInput"
                @change="handleEncodedExpressionChange"
                resize="vertical"
              />
              
              <el-button 
                type="primary" 
                size="small"
                @click="handleUpdateFromEncodedExpression"
                :loading="updatingFromEncoded"
                :disabled="!isEncodedExpressionChanged || !selectedRecord || !encodedExpressionValidation.isValid"
                style="margin-left: 10px"
              >
                <el-icon><Check /></el-icon>
                更新记录
              </el-button>
              
              <el-button 
                v-if="hasAnyChanges"
                size="small"
                @click="resetEditMode"
                style="margin-left: 10px"
              >
                <el-icon><RefreshLeft /></el-icon>
                重置修改
              </el-button>
            </el-form-item>
            
            <!-- 解码信息显示 -->
            <div v-if="decodedExpressionDisplay" class="decoded-expression-display">
              <el-alert
                title="解码表达式"
                type="info"
                :closable="false"
                show-icon
                :description="decodedExpressionDisplay"
              />
            </div>
            
            <!-- 编码表达式实时验证状态 -->
            <div class="encoded-expression-validation">
              <el-alert
                :title="encodedExpressionValidation.title"
                :type="encodedExpressionValidation.type"
                :description="encodedExpressionValidation.message"
                :closable="false"
                show-icon
              />
            </div>
            
            <!-- 编码表达提示信息 -->
            <div v-if="encodedExpressionHint" class="encoded-expression-hint">
              <el-alert
                :title="encodedExpressionHint"
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
        
        <div class="position-actions">
          <el-button 
            type="primary" 
            size="small"
            @click="handleSavePositions"
            :loading="savingPositions"
            :disabled="!isPositionDataValid"
          >
            <el-icon><Check /></el-icon>
            保存档位设置
          </el-button>
          <el-button 
            type="info" 
            size="small"
            @click="handleResetPositions"
          >
            <el-icon><RefreshLeft /></el-icon>
            重置档位
          </el-button>
          <el-button 
            v-if="positionViewMode === 'grid'"
            type="success" 
            size="small"
            @click="handleAddNewArea"
            :disabled="!canAddNewArea"
          >
            <el-icon><Plus /></el-icon>
            新增投放区域
          </el-button>
          <el-button 
            v-if="positionViewMode === 'grid'"
            type="danger" 
            size="small"
            @click="handleDeleteAreas"
            :disabled="!canDeleteAreas"
          >
            <el-icon><Delete /></el-icon>
            删除投放区域
          </el-button>
        </div>
        
        <!-- 删除投放区域选择框（仅在表格视图且有现有区域时显示） -->
        <div v-if="positionViewMode === 'grid' && selectedRecord && selectedRecord.allAreas && selectedRecord.allAreas.length > 0" class="delete-area-section">
          <el-divider content-position="left">
            <el-icon><Delete /></el-icon>
            选择要删除的投放区域
          </el-divider>
          <el-checkbox-group v-model="areasToDelete" class="delete-area-checkboxes">
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
              v-else-if="areasToDelete.length === 0"
              title="请选择要删除的投放区域"
              type="info"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else
              :title="`已选择 ${areasToDelete.length} 个区域进行删除`"
              type="success"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>
      
      <el-form-item>
        <el-button 
          type="primary" 
          @click="handleSearch"
          :disabled="!searchForm.year || !searchForm.month || !searchForm.week"
        >
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button 
          type="info" 
          @click="handleSearchNext"
          :disabled="!searchForm.cigaretteName"
        >
          <el-icon><ArrowDown /></el-icon>
          下一个
        </el-button>
        <el-button 
          type="warning" 
          @click="handleFilterLargeDeviation"
          :disabled="!tableData || tableData.length === 0"
          :loading="filteringDeviation"
        >
          <el-icon><Filter /></el-icon>
          筛选误差>200
          <span v-if="largeDeviationRecords.length > 0" style="margin-left: 5px;">
            ({{ currentDeviationIndex + 1 }}/{{ largeDeviationRecords.length }})
          </span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { Search, RefreshLeft, Download, ArrowDown, LocationInformation, Setting, Check, Plus, Delete, Document, DataBoard, TrendCharts, Filter } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'
import Position3DChart from './Position3DChart.vue'

export default {
  name: 'SearchForm',
  props: {
    selectedRecord: {
      type: Object,
      default: null
    },
    tableData: {
      type: Array,
      default: () => []
    }
  },
  components: {
    Search,
    RefreshLeft,
    Download,
    ArrowDown,
    LocationInformation,
    Setting,
    Check,
    Plus,
    Delete,
    Document,
    DataBoard,
    TrendCharts,
    Filter,
    Position3DChart
  },
  data() {
    return {
      searchForm: {
        year: null,
        month: null,
        week: null,
        cigaretteName: '',
        distributionType: '',
        extendedType: '',
        distributionArea: []
      },
      // 不同扩展类型对应的区域选项
      areaOptions: {
        '按档位统一投放': [
          { label: '全市', value: '全市' }
        ],
        '档位+区县': [
          { label: '丹江', value: '丹江' },
          { label: '房县', value: '房县' },
          { label: '郧西', value: '郧西' },
          { label: '郧阳', value: '郧阳' },
          { label: '竹山', value: '竹山' },
          { label: '竹溪', value: '竹溪' },
          { label: '城区', value: '城区' }
        ],
        '档位+城乡分类代码': [
          { label: '主城区', value: '主城区' },
          { label: '城乡结合区', value: '城乡结合区' },
          { label: '镇中心区', value: '镇中心区' },
          { label: '镇乡结合区', value: '镇乡结合区' },
          { label: '特殊区域', value: '特殊区域' },
          { label: '乡中心区', value: '乡中心区' },
          { label: '村庄', value: '村庄' }
        ],
        '档位+市场类型': [
          { label: '城网', value: '城网' },
          { label: '农网', value: '农网' }
        ],
        '档位+业态': [
          { label: '便利店', value: '便利店' },
          { label: '超市', value: '超市' },
          { label: '商场', value: '商场' },
          { label: '烟草专卖店', value: '烟草专卖店' },
          { label: '娱乐服务类', value: '娱乐服务类' },
          { label: '其他', value: '其他' }
        ]
      },
      // 档位数据（D30到D1，30个档位）
      positionData: new Array(30).fill(0),
      // 保存状态
      savingPositions: false,
      // 要删除的投放区域
      areasToDelete: [],
      // 编码化表达相关
      encodedExpressionInput: '',
      originalEncodedExpression: '',
      updatingFromEncoded: false,
      // 一致性检查相关
      originalFormState: null,
      isConsistencyCheckEnabled: true,
      // 编辑模式：'none'(无修改)、'encoding'(只能修改编码)、'form'(只能修改表单)
      editMode: 'none',
      // 档位设置显示模式：'grid'(表格视图)、'encoding'(编码视图)、'3d'(三维图视图)
      positionViewMode: 'grid',
      // 三维图表弹窗显示状态
      show3DChart: false,
      // 误差筛选相关
      largeDeviationRecords: [],  // 绝对误差>200的记录列表
      currentDeviationIndex: -1,  // 当前选中的误差记录索引
      filteringDeviation: false   // 筛选加载状态
    }
  },
  computed: {
    // 年份选项（当前年份往前2年，往后10年）
    yearOptions() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear - 2; year <= currentYear + 10; year++) {
        years.push(year)
      }
      return years
    },
    isDateComplete() {
      return this.searchForm.year && this.searchForm.month && this.searchForm.week
    },
    cigaretteNamePlaceholder() {
      if (!this.isDateComplete) {
        return '请先填充年份、月份和周序号'
      }
      return '请输入卷烟名称'
    },
    deliveryAreaOptions() {
      // 根据投放类型和扩展投放类型确定可选的投放区域
      if (this.searchForm.distributionType === '按档位统一投放') {
        return this.areaOptions['按档位统一投放'] || []
      } else if (this.searchForm.distributionType === '按档位扩展投放') {
        if (this.searchForm.extendedType) {
          return this.areaOptions[this.searchForm.extendedType] || []
        } else {
          return []
        }
      }
      return []
    },
    areaPlaceholder() {
      if (this.searchForm.distributionType === '按档位统一投放') {
        return '请选择投放区域（统一投放）'
      } else if (this.searchForm.distributionType === '按档位扩展投放' && !this.searchForm.extendedType) {
        return '请先选择扩展投放类型'
      } else if (this.deliveryAreaOptions.length > 0) {
        return '请选择投放区域'
      }
      return '请先选择投放类型'
    },
    // 验证档位数据是否有效
    isPositionDataValid() {
      if (!this.positionData || this.positionData.length !== 30) {
        return false
      }
      
      // 只检查是否有数值，不再检查约束条件
      return this.positionData.some(val => val > 0)
    },
    // 检查是否可以新增投放区域
    canAddNewArea() {
      return this.selectedRecord && 
             this.selectedRecord.cigCode && 
             this.searchForm.distributionArea && 
             this.searchForm.distributionArea.length > 0 &&
             this.isPositionDataValid
    },
    // 检查是否可以删除投放区域
    canDeleteAreas() {
      return this.selectedRecord && 
             this.selectedRecord.cigCode && 
             this.selectedRecord.allAreas && 
             this.selectedRecord.allAreas.length > 1 && // 至少要保留一个区域
             this.areasToDelete && 
             this.areasToDelete.length > 0
    },
    // 检查编码表达是否已变更
    isEncodedExpressionChanged() {
      return this.encodedExpressionInput.trim() !== this.originalEncodedExpression.trim()
    },
    // 解码表达显示（显示每个记录的独立解码）
    decodedExpressionDisplay() {
      if (!this.selectedRecord) {
        return ''
      }
      
      // 获取选中卷烟的所有相关记录
      const relatedRecords = this.getRelatedRecords(this.selectedRecord)
      
      if (relatedRecords.length === 0) {
        return ''
      }
      
      // 为每个记录生成解码表达式
      const decodedExpressions = []
      
      relatedRecords.forEach((record, index) => {
        let decodedExpression = ''
        
        // 优先使用后端提供的解码表达式
        if (record.decodedExpression) {
          decodedExpression = record.decodedExpression
        } else {
          // 生成该记录的解码表达式
          decodedExpression = this.generateRecordDecodedExpression(record)
        }
        
        decodedExpressions.push(`${index + 1}. ${decodedExpression}`)
      })
      
      return decodedExpressions.join('\n')
    },
    
    // 生成单个记录的解码表达式
    generateRecordDecodedExpression(record) {
      try {
        let decoded = ''
        
        // 投放方式
        decoded += record.deliveryMethod || '未知投放方式'
        
        // 扩展投放类型
        if (record.deliveryEtype && record.deliveryEtype !== 'NULL') {
          decoded += `、${record.deliveryEtype}`
        }
        
        // 投放区域
        if (record.deliveryArea) {
          decoded += `、${record.deliveryArea}`
        }
        
        // 档位设置摘要
        const positionSummary = this.getPositionSummary(record)
        if (positionSummary) {
          decoded += `、${positionSummary}`
        }
        
        return decoded
      } catch (error) {
        console.warn('生成解码表达式失败:', error)
        return '解码失败'
      }
    },
    
    // 获取档位设置摘要
    getPositionSummary(record) {
      const positions = []
      for (let i = 30; i >= 1; i--) {
        const value = record[`d${i}`] || 0
        if (value > 0) {
          positions.push(`D${i}=${value}`)
        }
      }
      
      if (positions.length === 0) {
        return '无档位设置'
      }
      
      // 简化显示
      if (positions.length > 3) {
        return `档位设置：${positions.slice(0, 3).join('、')}等${positions.length}个档位`
      } else {
        return `档位设置：${positions.join('、')}`
      }
    },
    // 编码表达式实时验证状态
    encodedExpressionValidation() {
      if (!this.encodedExpressionInput || !this.encodedExpressionInput.trim()) {
        return {
          isValid: true,
          type: 'info',
          title: '请输入编码表达式',
          message: '支持多行输入，每行一个编码表达式'
        }
      }
      
      // 执行实时验证
      const validation = this.validateEncodedExpressions(this.encodedExpressionInput)
      
      if (validation.isValid) {
        return {
          isValid: true,
          type: 'success',
          title: '编码表达式验证通过',
          message: validation.warnings.join(', ')
        }
      } else {
        return {
          isValid: false,
          type: 'error',
          title: '编码表达式验证失败',
          message: validation.errors.join('; ')
        }
      }
    },
    
    // 编码表达提示信息
    encodedExpressionHint() {
      if (!this.encodedExpressionInput) {
        return '请选中卷烟记录以显示编码表达'
      }
      if (this.isEncodedExpressionChanged) {
        if (this.hasOtherFormChanges) {
          return '编码表达和其他设置均已修改，需要通过一致性验证'
        }
        return '编码表达已修改，点击"更新记录"按钮保存更改'
      }
      
      // 统计显示的编码表达数量
      const lines = this.encodedExpressionInput.split('\n').filter(line => line.trim())
      const lineCount = lines.length
      
      if (lineCount > 1) {
        return `当前显示 ${lineCount} 条不同档位设置的区域聚合编码表达式`
      } else {
        return '编码格式：投放类型+扩展类型（区域编码）（档位投放量编码）'
      }
    },
    
    // 检查是否有其他表单变更（档位、投放类型、区域等）
    hasOtherFormChanges() {
      return this.hasPositionChanges || this.hasDeliverySettingsChanges
    },
    
    // 检查档位数据是否发生变更
    hasPositionChanges() {
      if (!this.originalFormState || !this.selectedRecord) return false
      
      return this.positionData.some((val, index) => 
        val !== (this.originalFormState.positionData[index] || 0)
      )
    },
    
    // 检查投放设置是否发生变更（投放类型、扩展类型、投放区域）
    hasDeliverySettingsChanges() {
      if (!this.originalFormState || !this.selectedRecord) return false
      
      // 检查投放类型变更
      const hasDeliveryMethodChange = this.searchForm.distributionType !== this.originalFormState.distributionType
      
      // 检查扩展投放类型变更
      const hasExtendedTypeChange = this.searchForm.extendedType !== this.originalFormState.extendedType
      
      // 检查投放区域变更
      const currentAreas = (this.searchForm.distributionArea || []).sort()
      const originalAreas = (this.originalFormState.distributionArea || []).sort()
      const hasAreaChanges = JSON.stringify(currentAreas) !== JSON.stringify(originalAreas)
      
      return hasDeliveryMethodChange || hasExtendedTypeChange || hasAreaChanges
    },
    
    // 是否需要一致性验证
    needsConsistencyCheck() {
      return this.isEncodedExpressionChanged && this.hasOtherFormChanges && this.isConsistencyCheckEnabled
    },
    
    // 获取变更摘要
    changesSummary() {
      const changes = []
      
      if (this.isEncodedExpressionChanged) {
        changes.push('编码表达')
      }
      
      if (this.hasPositionChanges) {
        changes.push('档位设置')
      }
      
      if (this.hasDeliverySettingsChanges) {
        const deliveryChanges = []
        if (this.originalFormState && this.searchForm.distributionType !== this.originalFormState.distributionType) {
          deliveryChanges.push('投放类型')
        }
        if (this.originalFormState && this.searchForm.extendedType !== this.originalFormState.extendedType) {
          deliveryChanges.push('扩展投放类型')  
        }
        if (this.originalFormState && JSON.stringify((this.searchForm.distributionArea || []).sort()) !== JSON.stringify((this.originalFormState.distributionArea || []).sort())) {
          deliveryChanges.push('投放区域')
        }
        changes.push(...deliveryChanges)
      }
      
      return changes
    },
    
    // 编码表达字段是否禁用（当处于表单修改模式时禁用）
    isEncodingDisabled() {
      return this.editMode === 'form'
    },
    
    // 表单字段是否禁用（当处于编码修改模式时禁用）
    isFormDisabled() {
      return this.editMode === 'encoding'
    },
    
    // 是否有任何修改
    hasAnyChanges() {
      return this.editMode !== 'none'
    },
    
    // 是否有图表数据
    hasChartData() {
      return this.selectedRecord && this.selectedRecord.cigCode && this.tableData.length > 0
    }
  },
  watch: {
    selectedRecord: {
      handler(newRecord) {
        if (newRecord) {
          // 当选中记录变化时，自动填充表单
          this.searchForm.year = newRecord.year
          this.searchForm.month = newRecord.month
          this.searchForm.week = newRecord.weekSeq || newRecord.week
          this.searchForm.cigaretteName = newRecord.cigName || newRecord.cigaretteName
          
          // 自动填充投放类型信息
          if (newRecord.deliveryMethod) {
            this.searchForm.distributionType = newRecord.deliveryMethod
          }
          
          // 自动填充扩展投放类型
          if (newRecord.deliveryEtype) {
            this.searchForm.extendedType = newRecord.deliveryEtype
          }
          
          // 自动填充投放区域
          if (newRecord.allAreas && Array.isArray(newRecord.allAreas)) {
            // 如果传入的是多个记录（通过搜索选中），则自动勾选所有投放区域
            this.searchForm.distributionArea = [...newRecord.allAreas]
          } else if (newRecord.deliveryArea) {
            // 如果是单个记录，则勾选该记录的投放区域
            this.searchForm.distributionArea = [newRecord.deliveryArea]
          }
          
          // 加载档位数据
          this.loadPositionData(newRecord)
          
          // 加载编码化表达
          this.loadEncodedExpression(newRecord)
          
          // 保存原始表单状态用于一致性检查
          this.saveOriginalFormState()
          
          // 重置编辑模式
          this.editMode = 'none'
          
          console.log('选中记录更新并自动填充:', {
            record: newRecord,
            form: this.searchForm
          })
        } else {
          // 清空档位数据和删除选择
          this.positionData = new Array(30).fill(0)
          this.areasToDelete = []
          // 清空编码化表达
          this.encodedExpressionInput = ''
          this.originalEncodedExpression = ''
          // 清空原始状态
          this.originalFormState = null
          // 重置编辑模式
          this.editMode = 'none'
        }
      },
      immediate: true
    }
  },
  methods: {
    handleDistributionTypeChange(value) {
      // 投放类型变化时重置相关字段（仅当非自动填充时）
      if (!this.selectedRecord) {
        this.searchForm.extendedType = ''
        this.searchForm.distributionArea = []
      } else {
        // 当有选中记录时，设置为表单修改模式
        this.setEditMode('form')
      }
    },
    handleExtendedTypeChange() {
      // 扩展投放类型变化时重置区域选择（仅当非自动填充时）
      if (!this.selectedRecord) {
        this.searchForm.distributionArea = []
      } else {
        // 当有选中记录时，设置为表单修改模式
        this.setEditMode('form')
      }
    },
    
    // 处理投放区域变化
    handleDistributionAreaChange() {
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('form')
      }
    },
    
    // 处理档位数据变化
    handlePositionChange() {
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('form')
      }
    },
    
    // 设置编辑模式
    setEditMode(mode) {
      if (this.editMode === 'none') {
        this.editMode = mode
        console.log(`切换到${mode === 'encoding' ? '编码修改' : '表单修改'}模式`)
      }
    },
    
    // 重置编辑模式
    resetEditMode() {
      this.editMode = 'none'
      
      // 重置编码表达
      this.loadEncodedExpression(this.selectedRecord)
      
      // 重置表单数据
      if (this.selectedRecord) {
        // 重新填充投放类型信息
        if (this.selectedRecord.deliveryMethod) {
          this.searchForm.distributionType = this.selectedRecord.deliveryMethod
        }
        
        // 重新填充扩展投放类型
        if (this.selectedRecord.deliveryEtype) {
          this.searchForm.extendedType = this.selectedRecord.deliveryEtype
        }
        
        // 重新填充投放区域
        if (this.selectedRecord.allAreas && Array.isArray(this.selectedRecord.allAreas)) {
          this.searchForm.distributionArea = [...this.selectedRecord.allAreas]
        } else if (this.selectedRecord.deliveryArea) {
          this.searchForm.distributionArea = [this.selectedRecord.deliveryArea]
        }
        
        // 重新加载档位数据
        this.loadPositionData(this.selectedRecord)
        
        // 重新保存原始表单状态
        this.saveOriginalFormState()
      }
      
      ElMessage.success('已重置修改状态')
      console.log('重置编辑模式')
    },
    
    // 切换档位显示视图
    switchPositionView(mode) {
      if (mode === '3d') {
        // 如果选择三维图表，直接打开弹窗
        this.open3DChart()
      } else {
        // 其他模式正常切换
        this.positionViewMode = mode
        console.log(`切换档位显示模式: ${mode}`)
      }
    },
    
    // 打开三维图表弹窗
    open3DChart() {
      if (!this.hasChartData) {
        ElMessage.warning('请先选择一个卷烟记录')
        return
      }
      this.show3DChart = true
      console.log('打开三维图表弹窗')
    },
    
    // 对话框打开完成事件
    handleDialogOpened() {
      console.log('Dialog opened, reinitializing chart...')
      // 对话框完全打开后重新初始化图表
      if (this.$refs.position3DChart) {
        this.$refs.position3DChart.reinitChart()
      }
    },
    
    // 导出图表功能
    exportChart() {
      ElMessage.info('图表导出功能正在开发中...')
    },
    handleSearch() {
      // 至少需要选择一个时间条件或卷烟名称
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请至少选择一个时间条件')
        return
      }
      
      // 如果选择了投放类型，需要完整填写相关信息
      if (this.searchForm.distributionType) {
        if (this.searchForm.distributionType === '按档位扩展投放' && !this.searchForm.extendedType) {
          ElMessage.warning('请选择扩展投放类型')
          return
        }
        
        if (!this.searchForm.distributionArea) {
          ElMessage.warning('请选择投放区域')
          return
        }
      }
      
      console.log('搜索条件:', this.searchForm)
      
      let message = '查询条件：'
      if (this.searchForm.year) message += `${this.searchForm.year}年 `
      if (this.searchForm.month) message += `${this.searchForm.month}月 `
      if (this.searchForm.week) message += `第${this.searchForm.week}周 `
      if (this.searchForm.cigaretteName) message += `卷烟：${this.searchForm.cigaretteName}`
      
      ElMessage.success(message)
      this.$emit('search', this.searchForm)
    },
    handleSearchNext() {
      if (!this.searchForm.cigaretteName.trim()) {
        ElMessage.warning('请先输入卷烟名称')
        return
      }
      
      this.$emit('search-next')
    },
    
    // 筛选绝对误差大于200的卷烟
    async handleFilterLargeDeviation() {
      if (!this.tableData || this.tableData.length === 0) {
        ElMessage.warning('暂无数据可筛选')
        return
      }
      
      this.filteringDeviation = true
      
      try {
        // 1. 先触发数据刷新，确保获取最新数据（用户可能修改了档位）
        console.log('刷新表格数据以获取最新的投放量信息...')
        this.$emit('refresh-before-filter')
        
        // 等待数据刷新完成（给一点时间让表格刷新）
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // 2. 筛选出绝对误差大于200的记录
        const deviationRecords = this.tableData.filter(record => {
          const advAmount = parseFloat(record.advAmount) || 0
          const actualDelivery = parseFloat(record.actualDelivery) || 0
          const deviation = Math.abs(actualDelivery - advAmount)
          return deviation > 200
        })
        
        console.log('筛选出的误差>200记录:', deviationRecords)
        
        // 3. 如果没有符合条件的记录
        if (deviationRecords.length === 0) {
          ElMessage.info('没有找到绝对误差大于200的卷烟')
          this.largeDeviationRecords = []
          this.currentDeviationIndex = -1
          return
        }
        
        // 4. 第一次筛选或者记录列表已改变，重置索引
        if (this.largeDeviationRecords.length === 0 || 
            this.largeDeviationRecords.length !== deviationRecords.length) {
          this.largeDeviationRecords = deviationRecords
          this.currentDeviationIndex = 0
        } else {
          // 5. 循环选择下一个
          this.currentDeviationIndex = (this.currentDeviationIndex + 1) % deviationRecords.length
          this.largeDeviationRecords = deviationRecords
        }
        
        // 6. 获取当前要选中的记录
        const currentRecord = this.largeDeviationRecords[this.currentDeviationIndex]
        
        // 7. 计算误差信息
        const advAmount = parseFloat(currentRecord.advAmount) || 0
        const actualDelivery = parseFloat(currentRecord.actualDelivery) || 0
        const deviation = Math.abs(actualDelivery - advAmount)
        
        // 8. 触发选中事件（通过父组件）
        this.$emit('cigarette-name-matched', [currentRecord])
        
        // 9. 显示提示信息
        ElMessage.success({
          message: `已选中第 ${this.currentDeviationIndex + 1}/${this.largeDeviationRecords.length} 个误差记录\n卷烟：${currentRecord.cigName}\n预投放量：${advAmount.toFixed(2)}\n实际投放量：${actualDelivery.toFixed(2)}\n绝对误差：${deviation.toFixed(2)}`,
          duration: 3000,
          dangerouslyUseHTMLString: false
        })
        
        console.log('已选中误差记录:', {
          index: this.currentDeviationIndex + 1,
          total: this.largeDeviationRecords.length,
          record: currentRecord,
          deviation: deviation
        })
        
      } catch (error) {
        console.error('筛选误差记录失败:', error)
        ElMessage.error('筛选失败，请重试')
      } finally {
        this.filteringDeviation = false
      }
    },
    handleReset() {
      this.searchForm = {
        year: null,
        month: null,
        week: null,
        cigaretteName: '',
        distributionType: '',
        extendedType: '',
        distributionArea: ''
      }
      // 重置误差筛选状态
      this.largeDeviationRecords = []
      this.currentDeviationIndex = -1
      
      ElMessage.info('已重置搜索条件')
      this.$emit('reset')
    },
    handleExport() {
      ElMessage.success('正在导出数据...')
      this.$emit('export', this.searchForm)
    },
    handleCigaretteNameInput(value) {
      // 实时搜索时去除前后空格
      this.searchForm.cigaretteName = value.trim()
    },
    handleCigaretteNameChange() {
      // 检查是否已填充日期表单
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请先填充年份、月份和周序号，然后再搜索卷烟名称')
        return
      }
      
      // 当卷烟名称输入完成时，尝试匹配表格中的记录
      if (this.searchForm.cigaretteName && this.tableData.length > 0) {
        // 找到该卷烟在当前日期的所有投放记录
        const matchedRecords = this.tableData.filter(record => 
          record.cigName && 
          record.cigName.includes(this.searchForm.cigaretteName) &&
          record.year === this.searchForm.year &&
          record.month === this.searchForm.month &&
          record.weekSeq === this.searchForm.week
        )
        
        if (matchedRecords.length > 0) {
          // 找到匹配记录，触发选中事件（传递所有匹配的记录）
          this.$emit('cigarette-name-matched', matchedRecords)
          ElMessage.success(`已自动选中匹配的卷烟：${matchedRecords[0].cigName}，共找到 ${matchedRecords.length} 个投放区域`)
        } else {
          ElMessage.info('未找到匹配的卷烟记录')
        }
      }
    },
    formatQuantity(value) {
      if (value === null || value === undefined || value === '') {
        return '未设置'
      }
      
      // 如果是数字，格式化为带小数点的形式
      const numValue = parseFloat(value)
      if (!isNaN(numValue)) {
        return numValue.toFixed(2)
      }
      
      return value
    },
    
    // 加载档位数据
    loadPositionData(record) {
      if (!record) {
        this.positionData = new Array(30).fill(0)
        return
      }
      
      // 从记录中提取D30到D1的数据
      const positions = []
      for (let i = 30; i >= 1; i--) {
        const key = `d${i}`
        const value = record[key] || 0
        positions.push(Number(value))
      }
      
      this.positionData = positions.length === 30 ? positions : new Array(30).fill(0)
      
      // 详细的调试信息
      console.log('=== 档位数据加载调试 ===')
      console.log('从后端接收的原始数据字段:', Object.keys(record).filter(key => key.startsWith('d')).sort())
      console.log('转换后的positionData数组:', this.positionData)
      console.log('界面将显示的顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
      console.log('数组第一个值 (对应D30):', this.positionData[0])
      console.log('数组最后一个值 (对应D1):', this.positionData[29])
    },
    
    // 档位数据验证（已移除约束检查）
    validatePositionConstraints() {
      // 不再进行约束条件检查，允许任意档位值
      console.log('档位数据已更新，不再检查约束条件')
    },
    
    // 保存档位设置
    async handleSavePositions() {
      if (!this.selectedRecord || !this.selectedRecord.cigCode) {
        ElMessage.error('请先选中一个卷烟记录')
        return
      }
      
      if (!this.isPositionDataValid) {
        ElMessage.error('请检查档位数据，至少设置一个档位值')
        return
      }
      
      // 一致性检查：如果同时修改了编码表达和档位设置
      if (this.needsConsistencyCheck) {
        const checkResult = await this.performConsistencyCheck()
        if (!checkResult.passed) {
          return // 一致性检查未通过，终止操作
        }
      }
      
      try {
        this.savingPositions = true
        
        // 构建更新请求数据
        const updateData = {
          cigCode: this.selectedRecord.cigCode,
          cigName: this.selectedRecord.cigName,
          year: this.selectedRecord.year,
          month: this.selectedRecord.month,
          weekSeq: this.selectedRecord.weekSeq,
          deliveryMethod: this.searchForm.distributionType,
          deliveryEtype: this.searchForm.extendedType,
          deliveryArea: this.searchForm.distributionArea.join(','),
          distribution: [...this.positionData], // D30到D1的分配值数组
          remark: this.selectedRecord.remark || '档位设置更新'
        }
        
        // 详细的调试信息
        console.log('=== 档位设置数据传输调试 ===')
        console.log('界面显示顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
        console.log('发送给后端的distribution数组:', updateData.distribution)
        console.log('数组长度:', updateData.distribution.length)
        console.log('数组第一个值 (应该是D30):', updateData.distribution[0])
        console.log('数组最后一个值 (应该是D1):', updateData.distribution[29])
        console.log('完整请求数据:', updateData)
        
        const response = await cigaretteDistributionAPI.updateCigaretteInfo(updateData)
        
        if (response.data.success) {
          ElMessage.success({
            message: `档位设置保存成功！更新了${response.data.updatedRecords || 1}条记录`,
            duration: 2000
          })
          
          // 触发数据刷新
          this.$emit('position-updated', {
            cigCode: updateData.cigCode,
            updateData: updateData
          })
        } else {
          throw new Error(response.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存档位设置失败:', error)
        ElMessage.error(`保存失败: ${error.message}`)
      } finally {
        this.savingPositions = false
      }
    },
    
    // 重置档位数据
    handleResetPositions() {
      if (this.selectedRecord) {
        this.loadPositionData(this.selectedRecord)
        ElMessage.info('已重置档位数据')
      } else {
        this.positionData = new Array(30).fill(0)
        ElMessage.info('已清空档位数据')
      }
    },
    
    // 新增投放区域
    async handleAddNewArea() {
      if (!this.canAddNewArea) {
        ElMessage.warning('请确保已选中卷烟并设置了有效的档位数据')
        return
      }
      
      try {
        // 获取当前选中的投放区域中，不在原记录中的新区域
        const originalAreas = this.selectedRecord.allAreas || [this.selectedRecord.deliveryArea]
        const selectedAreas = this.searchForm.distributionArea
        const newAreas = selectedAreas.filter(area => !originalAreas.includes(area))
        
        if (newAreas.length === 0) {
          ElMessage.warning('没有选择新的投放区域')
          return
        }
        
        const result = await ElMessageBox.confirm(
          `确定要为卷烟 "${this.selectedRecord.cigName}" 新增投放区域：${newAreas.join(', ')} 吗？`,
          '确认新增投放区域',
          {
            confirmButtonText: '确定新增',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
        
        if (result === 'confirm') {
          // 为每个新区域创建记录
          const addPromises = newAreas.map(area => {
            const addData = {
              cigCode: this.selectedRecord.cigCode,
              cigName: this.selectedRecord.cigName,
              year: this.selectedRecord.year,
              month: this.selectedRecord.month,
              weekSeq: this.selectedRecord.weekSeq,
              deliveryMethod: this.searchForm.distributionType,
              deliveryEtype: this.searchForm.extendedType,
              deliveryArea: area,
              distribution: [...this.positionData], // 使用当前的档位设置
              remark: `新增投放区域: ${area}`
            }
            
            // 新增区域的调试信息
            console.log(`=== 新增区域 ${area} 数据传输调试 ===`)
            console.log('界面显示顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
            console.log('发送给后端的distribution数组:', addData.distribution)
            console.log('数组第一个值 (应该是D30):', addData.distribution[0])
            console.log('数组最后一个值 (应该是D1):', addData.distribution[29])
            
            return cigaretteDistributionAPI.updateCigaretteInfo(addData)
          })
          
          const responses = await Promise.all(addPromises)
          const successCount = responses.filter(res => res.data.success).length
          
          if (successCount === newAreas.length) {
            ElMessage.success({
              message: `成功新增 ${successCount} 个投放区域记录`,
              duration: 2000
            })
            
            // 触发数据刷新
            this.$emit('area-added', {
              cigCode: this.selectedRecord.cigCode,
              newAreas: newAreas,
              positionData: this.positionData
            })
          } else {
            ElMessage.warning(`部分新增成功：${successCount}/${newAreas.length}`)
          }
        }
      } catch (error) {
        if (error === 'cancel') {
          return // 用户取消操作
        }
        console.error('新增投放区域失败:', error)
        ElMessage.error(`新增失败: ${error.message}`)
      }
    },
    
    // 删除投放区域
    async handleDeleteAreas() {
      if (!this.canDeleteAreas) {
        ElMessage.warning('请选择要删除的投放区域')
        return
      }
      
      try {
        const areasToDeleteList = [...this.areasToDelete]
        const remainingAreas = this.selectedRecord.allAreas.filter(area => !areasToDeleteList.includes(area))
        
        if (remainingAreas.length === 0) {
          ElMessage.error('不能删除所有投放区域，至少需要保留一个')
          return
        }
        
        const result = await ElMessageBox.confirm(
          `确定要删除卷烟 "${this.selectedRecord.cigName}" 的以下投放区域吗？\n\n${areasToDeleteList.join(', ')}\n\n删除后剩余区域：${remainingAreas.join(', ')}`,
          '确认删除投放区域',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        
        if (result === 'confirm') {
          // 构建删除请求数据
          const deleteData = {
            cigCode: this.selectedRecord.cigCode,
            cigName: this.selectedRecord.cigName,
            year: this.selectedRecord.year,
            month: this.selectedRecord.month,
            weekSeq: this.selectedRecord.weekSeq,
            areasToDelete: areasToDeleteList
          }
          
          console.log('删除投放区域请求数据:', deleteData)
          
          // 调用后端删除接口
          const response = await cigaretteDistributionAPI.deleteDeliveryAreas(deleteData)
          
          if (response.data.success) {
            ElMessage.success({
              message: `成功删除 ${areasToDeleteList.length} 个投放区域记录`,
              duration: 2000
            })
            
            // 清空删除选择
            this.areasToDelete = []
            
            // 触发数据刷新
            this.$emit('areas-deleted', {
              cigCode: this.selectedRecord.cigCode,
              deletedAreas: areasToDeleteList,
              remainingAreas: remainingAreas
            })
          } else {
            throw new Error(response.data.message || '删除失败')
          }
        }
      } catch (error) {
        if (error === 'cancel') {
          return // 用户取消操作
        }
        console.error('删除投放区域失败:', error)
        ElMessage.error(`删除失败: ${error.message}`)
      }
    },
    
    // =================== 编码化表达功能方法 ===================
    
    // 保存原始表单状态
    saveOriginalFormState() {
      this.originalFormState = {
        distributionType: this.searchForm.distributionType,
        extendedType: this.searchForm.extendedType,
        distributionArea: [...(this.searchForm.distributionArea || [])],
        positionData: [...this.positionData],
        encodedExpression: this.encodedExpressionInput
      }
      
      console.log('保存原始表单状态:', this.originalFormState)
    },
    
    // 加载编码化表达（支持多记录聚合显示）
    loadEncodedExpression(record) {
      if (!record) {
        this.encodedExpressionInput = ''
        this.originalEncodedExpression = ''
        return
      }
      
      // 获取选中卷烟的所有相关记录
      const relatedRecords = this.getRelatedRecords(record)
      
      // 按档位设置分组，生成聚合编码表达
      const groupedExpressions = this.generateAggregatedExpressions(relatedRecords)
      
      // 将多个编码表达式按行显示
      const multiLineExpressions = groupedExpressions.join('\n')
      
      this.encodedExpressionInput = multiLineExpressions
      this.originalEncodedExpression = multiLineExpressions
      
      console.log('加载编码化表达:', {
        cigName: record.cigName,
        relatedRecordsCount: relatedRecords.length,
        groupedExpressionsCount: groupedExpressions.length,
        multiLineExpressions: multiLineExpressions
      })
    },
    
    // 获取选中卷烟的所有相关记录
    getRelatedRecords(selectedRecord) {
      if (!this.tableData || !selectedRecord) {
        return [selectedRecord].filter(Boolean)
      }
      
      // 查找同一卷烟同一时间的所有记录
      const relatedRecords = this.tableData.filter(record => 
        record.cigCode === selectedRecord.cigCode &&
        record.cigName === selectedRecord.cigName &&
        record.year === selectedRecord.year &&
        record.month === selectedRecord.month &&
        record.weekSeq === selectedRecord.weekSeq
      )
      
      return relatedRecords.length > 0 ? relatedRecords : [selectedRecord]
    },
    
    // 生成每个记录的独立编码表达式
    generateAggregatedExpressions(records) {
      if (!records || records.length === 0) {
        return []
      }
      
      // 为每个记录生成独立的编码表达式
      const expressions = []
      
      records.forEach(record => {
        let expression = ''
        
        // 优先使用后端提供的编码表达式
        if (record.encodedExpression) {
          expression = record.encodedExpression
        } else {
          // 生成该记录的编码表达式
          expression = this.generateRecordEncodedExpression(record)
        }
        
        expressions.push(expression)
      })
      
      return expressions
    },
    
    // 生成单个记录的编码表达式
    generateRecordEncodedExpression(record) {
      try {
        // 1. 生成投放类型编码
        let typeCode = this.getDeliveryTypeCode(record)
        
        // 2. 生成区域编码
        let areaCode = this.getAreaCode(record.deliveryArea, record.deliveryEtype)
        
        // 3. 生成档位投放量编码
        const positionCode = this.generatePositionCode(record)
        
        // 4. 组合完整编码
        if (record.deliveryMethod === '按档位统一投放') {
          return `${typeCode}（${positionCode}）`
        } else {
          return `${typeCode}（${areaCode}）（${positionCode}）`
        }
      } catch (error) {
        console.warn('生成编码表达式失败:', error)
        return ''
      }
    },
    
    // 获取投放类型编码
    getDeliveryTypeCode(record) {
      if (record.deliveryMethod === '按档位统一投放') {
        return 'A'
      } else if (record.deliveryMethod === '按档位扩展投放') {
        let code = 'B'
        // 添加扩展类型编码
        if (record.deliveryEtype === '档位+区县') {
          code += '1'
        } else if (record.deliveryEtype === '档位+市场类型') {
          code += '2'
        } else if (record.deliveryEtype === '档位+城乡分类代码') {
          code += '4'
        } else if (record.deliveryEtype === '档位+业态') {
          code += '5'
        }
        return code
      }
      return 'A' // 默认
    },
    
    // 获取区域编码
    getAreaCode(deliveryArea, deliveryEtype) {
      if (!deliveryArea) return ''
      
      // 根据扩展投放类型映射区域编码
      const areaMapping = {
        '档位+区县': {
          '城区': '1',
          '丹江': '2', 
          '房县': '3',
          '郧西': '4',
          '郧阳': '5',
          '竹山': '6',
          '竹溪': '7'
        },
        '档位+市场类型': {
          '城网': 'C',
          '农网': 'N'
        },
        '档位+城乡分类代码': {
          '主城区': '①',
          '城乡结合区': '②', 
          '镇中心区': '③',
          '镇乡结合区': '④',
          '特殊区域': '⑤',
          '乡中心区': '⑥',
          '村庄': '⑦'
        },
        '档位+业态': {
          '便利店': 'a',
          '超市': 'b',
          '商场': 'c',
          '烟草专卖店': 'd',
          '娱乐服务类': 'e',
          '其他': 'f'
        }
      }
      
      const mapping = areaMapping[deliveryEtype]
      if (mapping && mapping[deliveryArea]) {
        return mapping[deliveryArea]
      }
      
      // 如果没有找到映射，返回区域名称首字母或简化表示
      return deliveryArea.charAt(0)
    },
    
    // 生成档位投放量编码
    generatePositionCode(record) {
      const positionGroups = []
      let currentValue = null
      let currentCount = 0
      
      // 从D30到D1遍历，统计连续相同值的档位数量
      for (let i = 30; i >= 1; i--) {
        const value = record[`d${i}`] || 0
        
        if (value === currentValue) {
          currentCount++
        } else {
          // 处理前一组
          if (currentValue !== null && currentCount > 0) {
            positionGroups.push(`${currentCount}×${currentValue}`)
          }
          
          // 开始新的一组
          currentValue = value
          currentCount = 1
        }
      }
      
      // 处理最后一组
      if (currentValue !== null && currentCount > 0) {
        positionGroups.push(`${currentCount}×${currentValue}`)
      }
      
      return positionGroups.filter(group => !group.endsWith('×0')).join('+') || '无档位设置'
    },
    
    // 按档位设置分组记录
    groupRecordsByPositions(records) {
      const grouped = {}
      
      records.forEach(record => {
        // 生成档位设置的唯一标识
        const positionValues = []
        for (let i = 30; i >= 1; i--) {
          const value = record[`d${i}`] || 0
          if (value > 0) {
            positionValues.push(`D${i}:${value}`)
          }
        }
        
        const positionKey = positionValues.length > 0 ? positionValues.join(',') : '无档位设置'
        
        if (!grouped[positionKey]) {
          grouped[positionKey] = []
        }
        grouped[positionKey].push(record)
      })
      
      return grouped
    },
    
    // 处理编码表达输入
    handleEncodedExpressionInput(value) {
      // 输入过程中的实时处理
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('encoding')
      }
      console.log('编码表达输入变化:', value)
    },
    
    // 处理编码表达变化
    handleEncodedExpressionChange() {
      // 输入完成后的处理
      if (this.isEncodedExpressionChanged) {
        ElMessage.info('编码表达已修改，可点击"更新记录"按钮保存')
      }
    },
    
    // 从编码表达更新记录（基于新的批量更新接口）
    async handleUpdateFromEncodedExpression() {
      if (!this.selectedRecord || !this.selectedRecord.cigCode) {
        ElMessage.error('请先选中一个卷烟记录')
        return
      }
      
      if (!this.isEncodedExpressionChanged) {
        ElMessage.warning('编码表达未发生变化')
        return
      }
      
      if (!this.encodedExpressionInput.trim()) {
        ElMessage.error('编码表达不能为空')
        return
      }
      
      try {
        this.updatingFromEncoded = true
        
        // 步骤1：验证编码表达式
        console.log('开始验证编码表达式...')
        const validation = this.validateEncodedExpressions(this.encodedExpressionInput)
        
        if (!validation.isValid) {
          // 显示验证错误
          const errorMessage = validation.errors.join('\n')
          ElMessage.error({
            dangerouslyUseHTMLString: false,
            message: `编码表达式验证失败:\n${errorMessage}`,
            duration: 5000
          })
          return
        }
        
        // 显示验证信息
        if (validation.warnings.length > 0) {
          ElMessage.success({
            message: validation.warnings.join(', '),
            duration: 3000
          })
        }
        
        // 步骤2：准备编码表达式列表
        const expressions = this.encodedExpressionInput.split('\n')
          .map(line => line.trim())
          .filter(line => line.length > 0)
        
        console.log('准备批量更新，编码表达式列表:', expressions)
        
        // 步骤3：构建批量更新请求数据
        const batchUpdateData = {
          cigCode: this.selectedRecord.cigCode,
          cigName: this.selectedRecord.cigName,
          year: this.selectedRecord.year,
          month: this.selectedRecord.month,
          weekSeq: this.selectedRecord.weekSeq,
          encodedExpressions: expressions,
          remark: `基于编码表达式的批量更新，共${expressions.length}条表达式`
        }
        
        console.log('批量更新请求数据:', batchUpdateData)
        
        // 步骤4：调用新的批量更新接口
        const response = await cigaretteDistributionAPI.batchUpdateFromExpressions(batchUpdateData)
        
        if (response.data.success) {
          // 根据操作类型显示不同的成功消息
          let successMessage = response.data.message
          
          if (response.data.operation === '投放类型变更') {
            successMessage += `\n删除了${response.data.deletedRecords}条记录，创建了${response.data.createdRecords}条记录`
          } else if (response.data.operation === '增量更新') {
            successMessage += `\n新增${response.data.newAreas}个区域，更新${response.data.updatedAreas}个区域，删除${response.data.deletedAreas}个区域`
          }
          
          ElMessage.success({
            message: successMessage,
            duration: 4000
          })
          
          // 更新本地状态
          this.originalEncodedExpression = this.encodedExpressionInput
          
          // 触发数据刷新
          this.$emit('position-updated', {
            cigCode: batchUpdateData.cigCode,
            updateData: batchUpdateData,
            updateType: 'batchEncodedExpression',
            operationType: response.data.operation,
            result: response.data
          })
          
          // 重置编辑模式
          this.editMode = 'none'
          
        } else {
          throw new Error(response.data.message || '批量更新失败')
        }
        
      } catch (error) {
        console.error('编码表达式批量更新失败:', error)
        
        // 根据错误类型显示不同的错误消息
        let errorMessage = error.message
        if (error.response && error.response.data) {
          errorMessage = error.response.data.message || errorMessage
        }
        
        ElMessage.error({
          message: `批量更新失败: ${errorMessage}`,
          duration: 5000
        })
      } finally {
        this.updatingFromEncoded = false
      }
    },
    
    // 公开方法：外部更新搜索表单（供Home组件调用）
    updateSearchForm(searchParams) {
      if (searchParams) {
        this.searchForm.year = searchParams.year
        this.searchForm.month = searchParams.month
        this.searchForm.week = searchParams.week
        console.log('外部更新搜索表单:', searchParams)
      }
    },
    
    // =================== 一致性检查方法 ===================
    
    // 执行一致性检查
    async performConsistencyCheck() {
      try {
        console.log('开始执行一致性检查...')
        
        // 解析编码表达
        const parsedEncoding = this.parseEncodedExpression(this.encodedExpressionInput)
        
        // 检查一致性
        const inconsistencies = this.checkConsistency(parsedEncoding)
        
        if (inconsistencies.length === 0) {
          console.log('一致性检查通过')
          return { passed: true }
        }
        
        // 显示一致性冲突对话框
        const result = await this.showConsistencyDialog(inconsistencies)
        return result
        
      } catch (error) {
        console.error('一致性检查过程中发生错误:', error)
        ElMessage.warning('一致性检查失败，建议分别保存各项设置')
        return { passed: false }
      }
    },
    
    // 解析编码表达式（基于编码规则表的完整实现）
    parseEncodedExpression(encoded) {
      const parsed = {
        deliveryType: '',
        extendedType: '',
        deliveryTypeCode: '',
        extendedTypeCode: '',
        areaCodes: [],
        areaNames: [],
        positionCoding: '',
        positionData: new Array(30).fill(0),
        isValid: false,
        error: ''
      }
      
      try {
        // 基本格式：B1（2+3+4）（5×9+5×8+10×7+10×6）
        const trimmed = encoded.trim()
        if (!trimmed) {
          parsed.error = '编码表达式不能为空'
          return parsed
        }
        
        // 第一步：解析投放类型
        const firstChar = trimmed.charAt(0).toUpperCase()
        if (!['A', 'B', 'C'].includes(firstChar)) {
          parsed.error = '投放类型编码错误，必须以A、B或C开头'
          return parsed
        }
        
        parsed.deliveryTypeCode = firstChar
        switch (firstChar) {
          case 'A':
            parsed.deliveryType = '按档位统一投放'
            break
          case 'B':
            parsed.deliveryType = '按档位扩展投放'
            break
          case 'C':
            parsed.deliveryType = '按需投放'
            break
        }
        
        // 第二步：解析扩展投放类型（仅B类型需要）
        if (firstChar === 'B') {
          const secondChar = trimmed.charAt(1)
          if (!['1', '2', '3', '4', '5'].includes(secondChar)) {
            parsed.error = 'B类型投放必须指定扩展类型编码（1-5）'
            return parsed
          }
          
          parsed.extendedTypeCode = secondChar
          switch (secondChar) {
            case '1':
              parsed.extendedType = '档位+区县'
              break
            case '2':
              parsed.extendedType = '档位+市场类型'
              break
            case '3':
              parsed.extendedType = '档位+区县+市场类型'
              break
            case '4':
              parsed.extendedType = '档位+城乡分类代码'
              break
            case '5':
              parsed.extendedType = '档位+业态'
              break
          }
        }
        
        // 第三步：解析区域编码和投放量编码
        const regex = /（([^）]+)）（([^）]+)）/
        const match = trimmed.match(regex)
        if (!match) {
          parsed.error = '编码格式错误，应为：类型+扩展类型（区域编码）（投放量编码）'
          return parsed
        }
        
        const areaCodeStr = match[1]
        const positionCodeStr = match[2]
        
        // 解析区域编码
        const areaResult = this.parseAreaCodes(areaCodeStr, parsed.extendedTypeCode)
        if (!areaResult.isValid) {
          parsed.error = areaResult.error
          return parsed
        }
        parsed.areaCodes = areaResult.areaCodes
        parsed.areaNames = areaResult.areaNames
        
        // 解析投放量编码
        const positionResult = this.parsePositionCoding(positionCodeStr)
        if (!positionResult.isValid) {
          parsed.error = positionResult.error
          return parsed
        }
        parsed.positionCoding = positionCodeStr
        parsed.positionData = positionResult.positionData
        
        parsed.isValid = true
        
      } catch (error) {
        console.warn('编码表达解析失败:', error)
        parsed.error = `解析异常: ${error.message}`
      }
      
      return parsed
    },
    
    // 解析区域编码
    parseAreaCodes(areaCodeStr, extendedTypeCode) {
      const result = {
        areaCodes: [],
        areaNames: [],
        isValid: false,
        error: ''
      }
      
      try {
        // 区域编码映射表
        const areaMapping = {
          '1': { // 档位+区县
            '1': '城区', '2': '丹江', '3': '房县', '4': '郧西',
            '5': '郧阳', '6': '竹山', '7': '竹溪'
          },
          '2': { // 档位+市场类型
            'C': '城网', 'N': '农网'
          },
          '3': { // 档位+区县+市场类型（使用区县编码）
            '1': '城区', '2': '丹江', '3': '房县', '4': '郧西',
            '5': '郧阳', '6': '竹山', '7': '竹溪'
          },
          '4': { // 档位+城乡分类代码
            '①': '主城区', '②': '城乡结合区', '③': '镇中心区', '④': '镇乡接合区',
            '⑤': '特殊区域', '⑥': '乡中心区', '⑦': '村庄'
          },
          '5': { // 档位+业态
            'a': '便利店', 'b': '超市', 'c': '商场', 'd': '烟草专业店',
            'e': '娱乐服务类', 'f': '其他'
          }
        }
        
        const mapping = areaMapping[extendedTypeCode] || areaMapping['1'] // 默认使用区县编码
        
        // 拆分区域编码（用+分隔）
        const codes = areaCodeStr.split('+').map(code => code.trim()).filter(code => code)
        
        if (codes.length === 0) {
          result.error = '区域编码不能为空'
          return result
        }
        
        // 验证每个区域编码
        for (const code of codes) {
          if (!mapping[code]) {
            result.error = `无效的区域编码: ${code}`
            return result
          }
          result.areaCodes.push(code)
          result.areaNames.push(mapping[code])
        }
        
        result.isValid = true
        
      } catch (error) {
        result.error = `区域编码解析异常: ${error.message}`
      }
      
      return result
    },
    
    // 解析投放量编码
    parsePositionCoding(positionCodeStr) {
      const result = {
        positionData: new Array(30).fill(0),
        isValid: false,
        error: ''
      }
      
      try {
        // 投放量编码格式：5×9+5×8+10×7+10×6
        // 表示：5个档位投放量9，5个档位投放量8，10个档位投放量7，10个档位投放量6
        
        const segments = positionCodeStr.split('+').map(seg => seg.trim()).filter(seg => seg)
        
        if (segments.length === 0) {
          result.error = '投放量编码不能为空'
          return result
        }
        
        let totalPositions = 0
        
        // 解析每个分段
        for (const segment of segments) {
          const match = segment.match(/^(\d+)×(\d+)$/)
          if (!match) {
            result.error = `投放量编码格式错误: ${segment}，应为"档位数×投放量"格式`
            return result
          }
          
          const positionCount = parseInt(match[1])
          const deliveryAmount = parseInt(match[2])
          
          if (positionCount <= 0 || positionCount > 30) {
            result.error = `档位数量错误: ${positionCount}，应在1-30之间`
            return result
          }
          
          if (deliveryAmount < 0) {
            result.error = `投放量不能为负数: ${deliveryAmount}`
            return result
          }
          
          totalPositions += positionCount
        }
        
        // 验证总档位数必须为30
        if (totalPositions !== 30) {
          result.error = `总档位数必须为30，当前为: ${totalPositions}`
          return result
        }
        
        // 填充档位数据（从D30开始往下填）
        let currentIndex = 0
        for (const segment of segments) {
          const match = segment.match(/^(\d+)×(\d+)$/)
          const positionCount = parseInt(match[1])
          const deliveryAmount = parseInt(match[2])
          
          for (let i = 0; i < positionCount; i++) {
            result.positionData[currentIndex + i] = deliveryAmount
          }
          currentIndex += positionCount
        }
        
        result.isValid = true
        
      } catch (error) {
        result.error = `投放量编码解析异常: ${error.message}`
      }
      
      return result
    },
    
    // 验证编码表达式列表（用户要求的三个验证规则）
    validateEncodedExpressions(expressions) {
      const validation = {
        isValid: false,
        errors: [],
        warnings: []
      }
      
      try {
        // 处理输入：按行分割并过滤空行
        const lines = expressions.split('\n')
          .map(line => line.trim())
          .filter(line => line.length > 0)
        
        if (lines.length === 0) {
          validation.errors.push('编码表达式列表不能为空')
          return validation
        }
        
        // 解析所有编码表达式
        const parsedExpressions = []
        for (const line of lines) {
          const parsed = this.parseEncodedExpression(line)
          if (!parsed.isValid) {
            validation.errors.push(`编码表达式"${line}"解析失败: ${parsed.error}`)
            return validation
          }
          parsedExpressions.push(parsed)
        }
        
        // 验证规则1：仅允许存在一种投放类型（第一个字母一致）
        const deliveryTypes = [...new Set(parsedExpressions.map(p => p.deliveryTypeCode))]
        if (deliveryTypes.length > 1) {
          validation.errors.push(`不允许存在多种投放类型，当前包含: ${deliveryTypes.join(', ')}`)
          return validation
        }
        
        // 验证规则2：不允许存在重复的投放区域编号
        const allAreaCodes = []
        for (const parsed of parsedExpressions) {
          for (const areaCode of parsed.areaCodes) {
            if (allAreaCodes.includes(areaCode)) {
              validation.errors.push(`投放区域编号重复: ${areaCode}`)
              return validation
            }
            allAreaCodes.push(areaCode)
          }
        }
        
        // 验证规则3：编码表达式的投放量部分必须为30个完整档位（这个在parsePositionCoding中已经验证）
        // 这里再次检查以确保所有表达式都符合要求
        for (const parsed of parsedExpressions) {
          const totalPositions = parsed.positionData.length
          if (totalPositions !== 30) {
            validation.errors.push(`编码表达式的投放量部分必须包含30个完整档位，当前为: ${totalPositions}`)
            return validation
          }
        }
        
        // 所有验证通过
        validation.isValid = true
        validation.parsedExpressions = parsedExpressions
        
        // 添加一些有用的信息
        validation.warnings.push(`共验证了 ${lines.length} 条编码表达式`)
        validation.warnings.push(`投放类型: ${parsedExpressions[0].deliveryType}`)
        if (parsedExpressions[0].extendedType) {
          validation.warnings.push(`扩展类型: ${parsedExpressions[0].extendedType}`)
        }
        validation.warnings.push(`涉及区域: ${allAreaCodes.length} 个`)
        
      } catch (error) {
        validation.errors.push(`验证过程异常: ${error.message}`)
      }
      
      return validation
    },
    
    // 检查一致性
    checkConsistency(parsedEncoding) {
      const inconsistencies = []
      
      // 检查投放类型一致性
      if (parsedEncoding.deliveryType && 
          this.searchForm.distributionType && 
          parsedEncoding.deliveryType !== this.searchForm.distributionType) {
        inconsistencies.push({
          type: '投放类型',
          encoded: parsedEncoding.deliveryType,
          current: this.searchForm.distributionType
        })
      }
      
      // 检查扩展投放类型一致性
      if (parsedEncoding.extendedType && 
          this.searchForm.extendedType && 
          parsedEncoding.extendedType !== this.searchForm.extendedType) {
        inconsistencies.push({
          type: '扩展投放类型',
          encoded: parsedEncoding.extendedType,
          current: this.searchForm.extendedType
        })
      }
      
      // 这里可以添加更多的一致性检查逻辑
      // 如档位数据一致性、区域一致性等
      
      return inconsistencies
    },
    
    // 显示一致性检查对话框
    async showConsistencyDialog(inconsistencies) {
      const inconsistencyText = inconsistencies.map(item => 
        `• ${item.type}: 编码表达为"${item.encoded}"，当前设置为"${item.current}"`
      ).join('\n')
      
      const message = `检测到以下不一致项：\n\n${inconsistencyText}\n\n请选择处理方式：`
      
      try {
        const result = await ElMessageBox.confirm(
          message,
          '数据一致性检查',
          {
            confirmButtonText: '强制更新（忽略冲突）',
            cancelButtonText: '取消操作',
            type: 'warning',
            dangerouslyUseHTMLString: false,
            customClass: 'consistency-check-dialog',
            beforeClose: (action, instance, done) => {
              if (action === 'confirm') {
                instance.confirmButtonText = '强制更新中...'
                instance.confirmButtonLoading = true
                setTimeout(() => {
                  done()
                }, 300)
              } else {
                done()
              }
            }
          }
        )
        
        if (result === 'confirm') {
          ElMessage.warning('已选择强制更新，将忽略一致性冲突')
          return { passed: true, forced: true }
        }
        
      } catch (error) {
        if (error === 'cancel') {
          ElMessage.info('已取消更新操作')
        }
      }
      
      return { passed: false }
    }
  }
}
</script>

<style scoped>
.search-form-container {
  background: #fafbfc;
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.search-form {
  margin: 0;
}

:deep(.el-form-item) {
  margin-bottom: 15px;
  margin-right: 15px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

:deep(.el-select .el-input__wrapper.is-disabled) {
  background: #f5f7fa;
  color: #c0c4cc;
}

/* 档位设置区域样式 */
.position-settings-section {
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

/* 三维图表弹窗样式 */
.chart-dialog :deep(.el-dialog__header) {
  background: linear-gradient(90deg, #409eff, #67c23a);
  color: white;
  padding: 20px 24px;
  border-radius: 8px 8px 0 0;
}

.chart-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.chart-dialog :deep(.el-dialog__close) {
  color: white;
  font-size: 18px;
}

.chart-dialog :deep(.el-dialog__close:hover) {
  color: #f0f0f0;
}

.chart-dialog :deep(.el-dialog__body) {
  padding: 0;
  background: #f8f9fa;
  min-height: 1125px;
}

.chart-dialog :deep(.el-dialog) {
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  max-height: 95vh;
  display: flex;
  flex-direction: column;
}

.chart-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #e4e7ed;
  background: white;
  padding: 16px 24px;
  border-radius: 0 0 8px 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.position-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin: 15px 0;
}

.position-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  transition: all 0.2s ease;
}

.position-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.position-label {
  font-weight: 600;
  color: #409eff;
  min-width: 32px;
  font-size: 12px;
  text-align: center;
}

.position-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

/* 档位输入框样式优化 */
:deep(.position-item .el-input-number) {
  width: 90px !important;
}

:deep(.position-item .el-input-number .el-input__wrapper) {
  border-radius: 4px;
  font-size: 12px;
}

:deep(.position-item .el-input-number--small .el-input__wrapper) {
  padding: 4px 8px;
}

/* 档位约束错误提示 */
.position-item.error {
  border-color: #f56c6c;
  background: #fef0f0;
}

.position-item.error .position-label {
  color: #f56c6c;
}

/* 删除投放区域样式 */
.delete-area-section {
  margin: 20px 0;
  padding: 15px;
  background: #fef9f9;
  border-radius: 6px;
  border: 1px solid #f5c6cb;
}

.delete-area-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin: 15px 0;
}

.delete-area-checkboxes .el-checkbox {
  margin: 0;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.delete-area-checkboxes .el-checkbox:hover {
  border-color: #f56c6c;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.1);
}

.delete-area-checkboxes .el-checkbox.is-checked {
  background: #fef0f0;
  border-color: #f56c6c;
}

.delete-area-checkboxes .el-checkbox.is-disabled {
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
}

.delete-area-tips {
  margin-top: 10px;
}

.delete-area-tips .el-alert {
  margin: 8px 0;
}

/* 编码化表达区域样式 */
.encoded-expression-section {
  margin: 20px 0;
  padding: 20px;
  background: #f0f7ff;
  border-radius: 8px;
  border: 1px solid #409eff;
}

.encoded-expression-input-container {
  margin-top: 15px;
}

.encoded-expression-form-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.encoded-expression-form-item :deep(.el-form-item__label) {
  font-weight: 600;
  color: #409eff;
}

.encoded-expression-form-item :deep(.el-input-group__prepend) {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.encoded-expression-form-item :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #fafbfc;
  border: 1px solid #409eff;
}

.encoded-expression-form-item :deep(.el-textarea__inner:focus) {
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.encoded-expression-form-item :deep(.el-textarea__inner[readonly]) {
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
}

.decoded-expression-display {
  margin-top: 15px;
}

.decoded-expression-display :deep(.el-alert__title) {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.5;
}

.decoded-expression-display :deep(.el-alert__description) {
  font-size: 12px;
  color: #606266;
  margin-top: 8px;
  white-space: pre-line;
  line-height: 1.6;
}

/* 编码表达式实时验证状态样式 */
.encoded-expression-validation {
  margin-top: 15px;
}

.encoded-expression-validation :deep(.el-alert__title) {
  font-size: 14px;
  font-weight: 600;
}

.encoded-expression-validation :deep(.el-alert__description) {
  white-space: pre-line;
  line-height: 1.6;
  font-size: 13px;
}

/* 成功状态 */
.encoded-expression-validation :deep(.el-alert--success) {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

/* 错误状态 */
.encoded-expression-validation :deep(.el-alert--error) {
  border-color: #f56c6c;
  background-color: #fef0f0;
}

/* 信息状态 */
.encoded-expression-validation :deep(.el-alert--info) {
  border-color: #909399;
  background-color: #f4f4f5;
}

/* 编码表达提示信息样式 */
.encoded-expression-hint {
  margin-top: 15px;
}

.encoded-expression-hint :deep(.el-alert__title) {
  font-size: 13px;
  font-weight: 500;
}

.encoded-expression-hint :deep(.el-alert) {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

/* 编辑模式状态样式 */
.edit-mode-status {
  margin-top: 15px;
}

.edit-mode-status :deep(.el-alert__description) {
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
  margin-top: 5px;
}

.edit-mode-status :deep(.el-alert__title) {
  font-weight: 600;
  font-size: 14px;
}

/* 一致性检查警告样式 */
.consistency-warning {
  margin-top: 15px;
}

.consistency-warning :deep(.el-alert) {
  border-color: #e6a23c;
}

.consistency-warning :deep(.el-alert__title) {
  font-weight: 600;
  color: #e6a23c;
}

.consistency-warning :deep(.el-alert__description) p {
  margin: 8px 0;
  line-height: 1.5;
}

/* 一致性检查对话框样式 */
:deep(.consistency-check-dialog) {
  .el-message-box {
    width: 500px;
    border-radius: 12px;
  }
  
  .el-message-box__title {
    font-size: 18px;
    font-weight: 600;
    color: #e6a23c;
  }
  
  .el-message-box__content {
    padding: 20px 20px 30px;
  }
  
  .el-message-box__message {
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-line;
  }
}

</style>