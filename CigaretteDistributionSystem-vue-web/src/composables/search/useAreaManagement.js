import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 区域管理 Composable
 */
export function useAreaManagement() {
    // 要删除的投放区域
    const areasToDelete = ref([])

    // 区域选项映射
    const areaOptions = {
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
    }

    // 获取投放区域选项
    const getDeliveryAreaOptions = (distributionType, extendedType) => {
        if (distributionType === '按档位统一投放') {
            return areaOptions['按档位统一投放'] || []
        } else if (distributionType === '按档位扩展投放') {
            if (extendedType) {
                return areaOptions[extendedType] || []
            } else {
                return []
            }
        }
        return []
    }

    // 区域占位符
    const getAreaPlaceholder = (distributionType, extendedType, hasOptions) => {
        if (distributionType === '按档位统一投放') {
            return '请选择投放区域（统一投放）'
        } else if (distributionType === '按档位扩展投放' && !extendedType) {
            return '请先选择扩展投放类型'
        } else if (hasOptions) {
            return '请选择投放区域'
        }
        return '请先选择投放类型'
    }

    // 检查是否可以新增投放区域
    const canAddNewArea = (selectedRecord, distributionArea, isPositionDataValid) => {
        return selectedRecord &&
            selectedRecord.cigCode &&
            distributionArea &&
            distributionArea.length > 0 &&
            isPositionDataValid
    }

    // 检查是否可以删除投放区域
    const canDeleteAreas = (selectedRecord) => {
        return selectedRecord &&
            selectedRecord.cigCode &&
            selectedRecord.allAreas &&
            selectedRecord.allAreas.length > 1 &&
            areasToDelete.value &&
            areasToDelete.value.length > 0
    }

    // 新增投放区域
    const addNewAreas = async (selectedRecord, distributionArea, extendedType, positionData) => {
        const originalAreas = selectedRecord.allAreas || [selectedRecord.deliveryArea]
        const selectedAreas = distributionArea
        const newAreas = selectedAreas.filter(area => !originalAreas.includes(area))

        if (newAreas.length === 0) {
            ElMessage.warning('没有选择新的投放区域')
            return { success: false }
        }

        try {
            const result = await ElMessageBox.confirm(
                `确定要为卷烟 "${selectedRecord.cigName}" 新增投放区域：${newAreas.join(', ')} 吗？`,
                '确认新增投放区域',
                {
                    confirmButtonText: '确定新增',
                    cancelButtonText: '取消',
                    type: 'info'
                }
            )

            if (result === 'confirm') {
                const addPromises = newAreas.map(area => {
                    const addData = {
                        cigCode: selectedRecord.cigCode,
                        cigName: selectedRecord.cigName,
                        year: selectedRecord.year,
                        month: selectedRecord.month,
                        weekSeq: selectedRecord.weekSeq,
                        deliveryMethod: selectedRecord.deliveryMethod,
                        deliveryEtype: extendedType,
                        deliveryArea: area,
                        distribution: [...positionData],
                        remark: `新增投放区域: ${area}`
                    }
                    return cigaretteDistributionAPI.updateCigaretteInfo(addData)
                })

                const responses = await Promise.all(addPromises)
                const successCount = responses.filter(res => res.data.success).length

                if (successCount === newAreas.length) {
                    ElMessage.success({
                        message: `成功新增 ${successCount} 个投放区域记录`,
                        duration: 2000
                    })
                    return { success: true, newAreas }
                } else {
                    ElMessage.warning(`部分新增成功：${successCount}/${newAreas.length}`)
                    return { success: false }
                }
            }
        } catch (error) {
            if (error === 'cancel') {
                return { success: false }
            }
            console.error('新增投放区域失败:', error)
            ElMessage.error(`新增失败: ${error.message}`)
            return { success: false, error }
        }
    }

    // 删除投放区域
    const deleteAreas = async (selectedRecord) => {
        if (!canDeleteAreas(selectedRecord)) {
            ElMessage.warning('请选择要删除的投放区域')
            return { success: false }
        }

        try {
            const areasToDeleteList = [...areasToDelete.value]
            const remainingAreas = selectedRecord.allAreas.filter(area => !areasToDeleteList.includes(area))

            if (remainingAreas.length === 0) {
                ElMessage.error('不能删除所有投放区域，至少需要保留一个')
                return { success: false }
            }

            const result = await ElMessageBox.confirm(
                `确定要删除卷烟 "${selectedRecord.cigName}" 的以下投放区域吗？\n\n${areasToDeleteList.join(', ')}\n\n删除后剩余区域：${remainingAreas.join(', ')}`,
                '确认删除投放区域',
                {
                    confirmButtonText: '确定删除',
                    cancelButtonText: '取消',
                    type: 'warning',
                    dangerouslyUseHTMLString: false
                }
            )

            if (result === 'confirm') {
                const deleteData = {
                    cigCode: selectedRecord.cigCode,
                    cigName: selectedRecord.cigName,
                    year: selectedRecord.year,
                    month: selectedRecord.month,
                    weekSeq: selectedRecord.weekSeq,
                    areasToDelete: areasToDeleteList
                }

                const response = await cigaretteDistributionAPI.deleteDeliveryAreas(deleteData)

                if (response.data.success) {
                    ElMessage.success({
                        message: `成功删除 ${areasToDeleteList.length} 个投放区域记录`,
                        duration: 2000
                    })
                    areasToDelete.value = []
                    return { success: true, deletedAreas: areasToDeleteList, remainingAreas }
                } else {
                    throw new Error(response.data.message || '删除失败')
                }
            }
        } catch (error) {
            if (error === 'cancel') {
                return { success: false }
            }
            console.error('删除投放区域失败:', error)
            ElMessage.error(`删除失败: ${error.message}`)
            return { success: false, error }
        }
    }

    return {
        // 状态
        areasToDelete,

        // 方法
        getDeliveryAreaOptions,
        getAreaPlaceholder,
        canAddNewArea,
        canDeleteAreas,
        addNewAreas,
        deleteAreas
    }
}
