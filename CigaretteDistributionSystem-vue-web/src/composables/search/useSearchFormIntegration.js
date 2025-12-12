import { computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * SearchFormMain 逻辑整合 Composable
 * 整合所有业务逻辑，减少主组件代码量
 */
export function useSearchFormIntegration(props, emit, {
    searchForm,
    positionData,
    areasToDelete,
    encodedExpressionInput,
    originalEncodedExpression,
    updatingFromEncoded,
    loadPositionData,
    loadEncodedExpression,
    saveOriginalFormState,
    clearOriginalFormState,
    resetEditModeLogic,
    setEditMode,
    getDeliveryAreaOptions,
    getAreaPlaceholder,
    canAddNewAreaFn,
    canDeleteAreasFn,
    validateEncodedExpressions,
    generateRecordEncodedExpression,
    updateFromEncodedExpression: updateFromEncodedExpressionFn,
    savePositionSettings,
    addNewAreas,
    deleteAreas,
    resetPositionData,
    validateSearchForm,
    resetSearchForm,
    filterLargeDeviation,
    resetDeviationFilter
}) {

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
        canAddNewAreaFn(props.selectedRecord, searchForm.value.distributionArea, true)
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
        return validation.isValid
            ? { isValid: true, type: 'success', title: '编码表达式验证通过', message: validation.warnings.join(', ') }
            : { isValid: false, type: 'error', title: '编码表达式验证失败', message: validation.errors.join('; ') }
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

        return relatedRecords.map((record, index) => {
            const expr = record.decodedExpression || generateRecordEncodedExpression(record)
            return `${index + 1}. ${expr}`
        }).join('\n')
    })

    const encodedExpressionHint = computed(() => {
        if (!encodedExpressionInput.value) return '请选中卷烟记录以显示编码表达'
        if (encodedExpressionInput.value.trim() !== originalEncodedExpression.value.trim()) {
            return '编码表达已修改，点击"更新记录"按钮保存更改'
        }

        const lines = encodedExpressionInput.value.split('\n').filter(line => line.trim())
        return lines.length > 1
            ? `当前显示 ${lines.length} 条不同档位设置的区域聚合编码表达式`
            : '编码格式：投放类型+扩展类型（区域编码）（档位投放量编码）'
    })

    // 监听选中记录变化
    watch(() => props.selectedRecord, (newRecord) => {
        if (newRecord) {
            searchForm.value.year = newRecord.year
            searchForm.value.month = newRecord.month
            searchForm.value.week = newRecord.weekSeq || newRecord.week
            searchForm.value.cigaretteName = newRecord.cigName || newRecord.cigaretteName
            searchForm.value.distributionType = newRecord.deliveryMethod || ''
            searchForm.value.extendedType = newRecord.deliveryEtype || ''
            searchForm.value.distributionArea = newRecord.allAreas && Array.isArray(newRecord.allAreas)
                ? [...newRecord.allAreas]
                : newRecord.deliveryArea ? [newRecord.deliveryArea] : []

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

    // 事件处理方法
    const handleTypeChange = (type, value) => {
        if (!props.selectedRecord) {
            if (type === 'distribution') {
                searchForm.value.extendedType = ''
                searchForm.value.distributionArea = []
            } else if (type === 'extended') {
                searchForm.value.distributionArea = []
            }
        } else {
            setEditMode('form')
        }
    }

    const handleSavePositions = async () => {
        const result = await savePositionSettings(props.selectedRecord, searchForm.value)
        if (result.success) {
            emit('position-updated', {
                cigCode: result.updateData.cigCode,
                updateData: result.updateData
            })
        }
    }

    const handleAddNewArea = async () => {
        const result = await addNewAreas(
            props.selectedRecord,
            searchForm.value.distributionArea,
            searchForm.value.extendedType,
            positionData.value
        )
        if (result.success) {
            emit('area-added', {
                cigCode: props.selectedRecord.cigCode,
                newAreas: result.newAreas,
                positionData: positionData.value
            })
        }
    }

    const handleDeleteAreas = async () => {
        const result = await deleteAreas(props.selectedRecord)
        if (result.success) {
            emit('areas-deleted', {
                cigCode: props.selectedRecord.cigCode,
                deletedAreas: result.deletedAreas,
                remainingAreas: result.remainingAreas
            })
        }
    }

    const handleUpdateFromEncodedExpression = async () => {
        updatingFromEncoded.value = true
        const result = await updateFromEncodedExpressionFn(encodedExpressionInput.value, props.selectedRecord)
        updatingFromEncoded.value = false

        if (result.success) {
            originalEncodedExpression.value = encodedExpressionInput.value
            emit('position-updated', {
                cigCode: props.selectedRecord.cigCode,
                updateType: 'encodedExpression',
                result: result.result
            })
            resetEditModeLogic()
        }
    }

    return {
        // 计算属性
        deliveryAreaOptions,
        areaPlaceholder,
        canAddNewArea,
        canDeleteAreas,
        encodedExpressionValidation,
        decodedExpressionDisplay,
        encodedExpressionHint,

        // 方法
        handleTypeChange,
        handleSavePositions,
        handleAddNewArea,
        handleDeleteAreas,
        handleUpdateFromEncodedExpression
    }
}
