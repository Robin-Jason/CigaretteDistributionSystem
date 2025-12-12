<template>
  <div class="search-form-container">
    <el-form :model="searchForm" :inline="true" class="search-form">
      <!-- 基础搜索字段 -->
      <BasicSearchFields
        :searchForm="searchForm"
        :yearOptions="yearOptions"
        :isDateComplete="isDateComplete"
        :cigaretteNamePlaceholder="cigaretteNamePlaceholder"
        :tableData="tableData"
        @cigarette-name-matched="handleCigaretteNameMatched"
      />

      <!-- 投放类型选择器 -->
      <DistributionTypeSelector
        v-model:distributionType="searchForm.distributionType"
        v-model:extendedType="searchForm.extendedType"
        :disabled="isFormDisabled"
        @type-change="handleTypeChange"
      />

      <!-- 区域选择器 -->
      <AreaSelector
        v-model:distributionArea="searchForm.distributionArea"
        :areaOptions="deliveryAreaOptions"
        :areaPlaceholder="areaPlaceholder"
        :disabled="isFormDisabled"
        @change="handleAreaChange"
      />

      <!-- 投放量显示 -->
      <QuantityDisplay :selectedRecord="selectedRecord" />

      <!-- 档位设置面板 -->
      <PositionSettingsPanel
        v-if="selectedRecord && selectedRecord.cigCode"
        :selectedRecord="selectedRecord"
        :tableData="tableData"
        :positionData="positionData"
        v-model:viewMode="positionViewMode"
        :disabled="isFormDisabled"
        :encodingDisabled="isEncodingDisabled"
        v-model:encodedExpression="encodedExpressionInput"
        :updatingFromEncoded="updatingFromEncoded"
        :isEncodedExpressionChanged="isEncodedExpressionChanged"
        :validationResult="encodedExpressionValidation"
        :decodedExpression="decodedExpressionDisplay"
        :encodedExpressionHint="encodedExpressionHint"
        :editMode="editMode"
        :hasChanges="hasAnyChanges"
        @position-change="handlePositionChange"
        @encoding-input="handleEncodedExpressionInput"
        @encoding-change="handleEncodedExpressionChange"
        @encoding-update="handleUpdateFromEncodedExpression"
        @reset-edit-mode="resetEditMode"
      />

      <!-- 档位操作按钮 -->
      <PositionActions
        v-if="selectedRecord && selectedRecord.cigCode"
        :savingPositions="savingPositions"
        :isValid="isPositionDataValid"
        :showAreaActions="positionViewMode === 'grid'"
        :canAddArea="canAddNewArea"
        :canDeleteAreas="canDeleteAreas"
        @save="handleSavePositions"
        @reset="handleResetPositions"
        @add-area="handleAddNewArea"
        @delete-areas="handleDeleteAreas"
      />

      <!-- 删除区域面板 -->
      <DeleteAreaPanel
        v-if="positionViewMode === 'grid'"
        :selectedRecord="selectedRecord"
        v-model:areasToDelete="areasToDelete"
      />

      <!-- 搜索操作按钮 -->
      <SearchActions
        :canSearch="!!searchForm.year && !!searchForm.month && !!searchForm.week"
        :canSearchNext="!!searchForm.cigaretteName"
        :hasTableData="tableData && tableData.length > 0"
        :filteringDeviation="filteringDeviation"
        :deviationCount="largeDeviationRecords.length"
        :currentIndex="currentDeviationIndex"
        @search="handleSearch"
        @search-next="handleSearchNext"
        @filter-deviation="handleFilterLargeDeviation"
        @reset="handleReset"
        @export="handleExport"
      />
    </el-form>
  </div>
</template>

<script>
import BasicSearchFields from './BasicSearchFields.vue'
import DistributionTypeSelector from './DistributionTypeSelector.vue'
import AreaSelector from './area/AreaSelector.vue'
import QuantityDisplay from './QuantityDisplay.vue'
import PositionSettingsPanel from './position/PositionSettingsPanel.vue'
import PositionActions from './position/PositionActions.vue'
import DeleteAreaPanel from './area/DeleteAreaPanel.vue'
import SearchActions from './SearchActions.vue'

import { useSearchForm } from '@/composables/search/useSearchForm'
import { useEditMode } from '@/composables/search/useEditMode'
import { usePositionData } from '@/composables/search/usePositionData'
import { useAreaManagement } from '@/composables/search/useAreaManagement'
import { useDeviationFilter } from '@/composables/search/useDeviationFilter'
import { useEncodedExpression } from '@/composables/search/useEncodedExpression'
import { useEncodedExpressionValidator } from '@/composables/search/useEncodedExpressionValidator'

import { ElMessage } from 'element-plus'
import { computed, watch } from 'vue'

export default {
  name: 'SearchFormMain',
  components: {
    BasicSearchFields,
    DistributionTypeSelector,
    AreaSelector,
    QuantityDisplay,
    PositionSettingsPanel,
    PositionActions,
    DeleteAreaPanel,
    SearchActions
  },
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
  emits: [
    'search',
    'search-next',
    'reset',
    'export',
    'cigarette-name-matched',
    'position-updated',
    'area-added',
    'areas-deleted',
    'refresh-before-filter'
  ],
  setup(props, { emit }) {
    // 使用 composables
    const {
      searchForm,
      yearOptions,
      isDateComplete,
      cigaretteNamePlaceholder,
      resetSearchForm,
      updateSearchForm,
      validateSearchForm
    } = useSearchForm()

    const {
      editMode,
      hasAnyChanges,
      isEncodingDisabled,
      isFormDisabled,
      setEditMode,
      resetEditMode: resetEditModeLogic,
      saveOriginalFormState,
      clearOriginalFormState
    } = useEditMode()

    const {
      positionData,
      savingPositions,
      positionViewMode,
      isPositionDataValid,
      loadPositionData,
      resetPositionData,
      savePositionSettings
    } = usePositionData()

    const {
      areasToDelete,
      getDeliveryAreaOptions,
      getAreaPlaceholder,
      canAddNewArea: canAddNewAreaFn,
      canDeleteAreas: canDeleteAreasFn,
      addNewAreas,
      deleteAreas
    } = useAreaManagement()

    const {
      largeDeviationRecords,
      currentDeviationIndex,
      filteringDeviation,
      filterLargeDeviation,
      resetDeviationFilter
    } = useDeviationFilter()

    const {
      encodedExpressionInput,
      originalEncodedExpression,
      updatingFromEncoded,
      isEncodedExpressionChanged,
      loadEncodedExpression,
      generateRecordEncodedExpression
    } = useEncodedExpression()

    const {
      validateEncodedExpressions,
      updateFromEncodedExpression
    } = useEncodedExpressionValidator()

    // 计算属性
    const deliveryAreaOptions = computed(() =>
      getDeliveryAreaOptions(searchForm.value.distributionType, searchForm.value.extendedType)
    )

    const areaPlaceholder = computed(() =>
      getAreaPlaceholder(
        searchForm.value.distributionType,
        searchForm.value.extendedType,
        deliveryAreaOptions.value.length > 0
      )
    )

    const canAddNewArea = computed(() =>
      canAddNewAreaFn(
        props.selectedRecord,
        searchForm.value.distributionArea,
        isPositionDataValid.value
      )
    )

    const canDeleteAreas = computed(() =>
      canDeleteAreasFn(props.selectedRecord)
    )

    const encodedExpressionValidation = computed(() => {
      if (!encodedExpressionInput.value || !encodedExpressionInput.value.trim()) {
        return {
          isValid: true,
          type: 'info',
          title: '请输入编码表达式',
          message: '支持多行输入，每行一个编码表达式'
        }
      }

      const validation = validateEncodedExpressions(encodedExpressionInput.value)

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
    })

    const decodedExpressionDisplay = computed(() => {
      if (!props.selectedRecord) return ''

      const relatedRecords = props.tableData.filter(record =>
        record.cigCode === props.selectedRecord.cigCode &&
        record.year === props.selectedRecord.year &&
        record.month === props.selectedRecord.month &&
        record.weekSeq === props.selectedRecord.weekSeq
      )

      if (relatedRecords.length === 0) return ''

      const expressions = relatedRecords.map((record, index) => {
        const expr = record.decodedExpression || generateRecordEncodedExpression(record)
        return `${index + 1}. ${expr}`
      })

      return expressions.join('\n')
    })

    const encodedExpressionHint = computed(() => {
      if (!encodedExpressionInput.value) {
        return '请选中卷烟记录以显示编码表达'
      }
      if (isEncodedExpressionChanged.value) {
        return '编码表达已修改，点击"更新记录"按钮保存更改'
      }

      const lines = encodedExpressionInput.value.split('\n').filter(line => line.trim())
      if (lines.length > 1) {
        return `当前显示 ${lines.length} 条不同档位设置的区域聚合编码表达式`
      }
      return '编码格式：投放类型+扩展类型（区域编码）（档位投放量编码）'
    })

    // 监听选中记录变化
    watch(() => props.selectedRecord, (newRecord) => {
      if (newRecord) {
        searchForm.value.year = newRecord.year
        searchForm.value.month = newRecord.month
        searchForm.value.week = newRecord.weekSeq || newRecord.week
        searchForm.value.cigaretteName = newRecord.cigName || newRecord.cigaretteName

        if (newRecord.deliveryMethod) {
          searchForm.value.distributionType = newRecord.deliveryMethod
        }
        if (newRecord.deliveryEtype) {
          searchForm.value.extendedType = newRecord.deliveryEtype
        }
        if (newRecord.allAreas && Array.isArray(newRecord.allAreas)) {
          searchForm.value.distributionArea = [...newRecord.allAreas]
        } else if (newRecord.deliveryArea) {
          searchForm.value.distributionArea = [newRecord.deliveryArea]
        }

        loadPositionData(newRecord)
        loadEncodedExpression(newRecord, props.tableData)
        saveOriginalFormState(searchForm.value, positionData.value, encodedExpressionInput.value)
        resetEditModeLogic()
      } else {
        positionData.value = new Array(30).fill(0)
        areasToDelete.value = []
        encodedExpressionInput.value = ''
        originalEncodedExpression.value = ''
        clearOriginalFormState()
        resetEditModeLogic()
      }
    }, { immediate: true })

    return {
      // 状态
      searchForm,
      yearOptions,
      isDateComplete,
      cigaretteNamePlaceholder,
      deliveryAreaOptions,
      areaPlaceholder,
      positionData,
      savingPositions,
      positionViewMode,
      isPositionDataValid,
      areasToDelete,
      canAddNewArea,
      canDeleteAreas,
      editMode,
      hasAnyChanges,
      isEncodingDisabled,
      isFormDisabled,
      encodedExpressionInput,
      updatingFromEncoded,
      isEncodedExpressionChanged,
      encodedExpressionValidation,
      decodedExpressionDisplay,
      encodedExpressionHint,
      largeDeviationRecords,
      currentDeviationIndex,
      filteringDeviation,

      // 方法 - 在methods中定义
    }
  },
  methods: {
    handleTypeChange(type, value) {
      if (type === 'distribution') {
        if (!this.selectedRecord) {
          this.searchForm.extendedType = ''
          this.searchForm.distributionArea = []
        } else {
          this.setEditMode('form')
        }
      } else if (type === 'extended') {
        if (!this.selectedRecord) {
          this.searchForm.distributionArea = []
        } else {
          this.setEditMode('form')
        }
      }
    },

    handleAreaChange() {
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('form')
      }
    },

    handlePositionChange() {
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('form')
      }
    },

    handleEncodedExpressionInput(value) {
      if (this.selectedRecord && this.selectedRecord.cigCode) {
        this.setEditMode('encoding')
      }
    },

    handleEncodedExpressionChange() {
      if (this.isEncodedExpressionChanged) {
        ElMessage.info('编码表达已修改，可点击"更新记录"按钮保存')
      }
    },

    async handleUpdateFromEncodedExpression() {
      this.updatingFromEncoded = true
      const result = await this.updateFromEncodedExpression(
        this.encodedExpressionInput,
        this.selectedRecord
      )
      this.updatingFromEncoded = false

      if (result.success) {
        this.originalEncodedExpression = this.encodedExpressionInput
        this.$emit('position-updated', {
          cigCode: this.selectedRecord.cigCode,
          updateType: 'encodedExpression',
          result: result.result
        })
        this.resetEditModeLogic()
      }
    },

    handleCigaretteNameMatched(matchedRecords) {
      this.$emit('cigarette-name-matched', matchedRecords)
    },

    async handleSavePositions() {
      const result = await this.savePositionSettings(this.selectedRecord, this.searchForm)
      if (result.success) {
        this.$emit('position-updated', {
          cigCode: result.updateData.cigCode,
          updateData: result.updateData
        })
      }
    },

    handleResetPositions() {
      this.resetPositionData(this.selectedRecord)
    },

    async handleAddNewArea() {
      const result = await this.addNewAreas(
        this.selectedRecord,
        this.searchForm.distributionArea,
        this.searchForm.extendedType,
        this.positionData
      )
      if (result.success) {
        this.$emit('area-added', {
          cigCode: this.selectedRecord.cigCode,
          newAreas: result.newAreas,
          positionData: this.positionData
        })
      }
    },

    async handleDeleteAreas() {
      const result = await this.deleteAreas(this.selectedRecord)
      if (result.success) {
        this.$emit('areas-deleted', {
          cigCode: this.selectedRecord.cigCode,
          deletedAreas: result.deletedAreas,
          remainingAreas: result.remainingAreas
        })
      }
    },

    handleSearch() {
      if (!this.validateSearchForm()) return
      console.log('搜索条件:', this.searchForm)
      this.$emit('search', this.searchForm)
    },

    handleSearchNext() {
      if (!this.searchForm.cigaretteName.trim()) {
        ElMessage.warning('请先输入卷烟名称')
        return
      }
      this.$emit('search-next')
    },

    async handleFilterLargeDeviation() {
      await this.filterLargeDeviation(
        this.tableData,
        () => this.$emit('refresh-before-filter'),
        (records) => this.$emit('cigarette-name-matched', records)
      )
    },

    handleReset() {
      this.resetSearchForm()
      this.resetDeviationFilter()
      this.$emit('reset')
    },

    handleExport() {
      ElMessage.success('正在导出数据...')
      this.$emit('export', this.searchForm)
    },

    resetEditMode() {
      this.resetEditModeLogic()
      this.loadEncodedExpression(this.selectedRecord, this.tableData)

      if (this.selectedRecord) {
        if (this.selectedRecord.deliveryMethod) {
          this.searchForm.distributionType = this.selectedRecord.deliveryMethod
        }
        if (this.selectedRecord.deliveryEtype) {
          this.searchForm.extendedType = this.selectedRecord.deliveryEtype
        }
        if (this.selectedRecord.allAreas && Array.isArray(this.selectedRecord.allAreas)) {
          this.searchForm.distributionArea = [...this.selectedRecord.allAreas]
        } else if (this.selectedRecord.deliveryArea) {
          this.searchForm.distributionArea = [this.selectedRecord.deliveryArea]
        }

        this.loadPositionData(this.selectedRecord)
        this.saveOriginalFormState(this.searchForm, this.positionData, this.encodedExpressionInput)
      }

      ElMessage.success('已重置修改状态')
    },

    // 公开方法：外部更新搜索表单（供Home组件调用）
    updateSearchForm(searchParams) {
      this.updateSearchForm(searchParams)
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
</style>
