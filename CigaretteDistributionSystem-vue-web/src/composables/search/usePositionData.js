import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 档位数据管理 Composable
 */
export function usePositionData() {
    // 档位数据（D30到D1，30个档位）
    const positionData = ref(new Array(30).fill(0))

    // 保存状态
    const savingPositions = ref(false)

    // 档位视图模式：'grid'(表格视图)、'encoding'(编码视图)、'3d'(三维图视图)
    const positionViewMode = ref('grid')

    // 验证档位数据是否有效
    const isPositionDataValid = computed(() => {
        if (!positionData.value || positionData.value.length !== 30) {
            return false
        }
        // 只检查是否有数值
        return positionData.value.some(val => val > 0)
    })

    // 加载档位数据
    const loadPositionData = (record) => {
        if (!record) {
            positionData.value = new Array(30).fill(0)
            return
        }

        // 从记录中提取D30到D1的数据
        const positions = []
        for (let i = 30; i >= 1; i--) {
            const key = `d${i}`
            const value = record[key] || 0
            positions.push(Number(value))
        }

        positionData.value = positions.length === 30 ? positions : new Array(30).fill(0)

        console.log('=== 档位数据加载调试 ===')
        console.log('从后端接收的原始数据字段:', Object.keys(record).filter(key => key.startsWith('d')).sort())
        console.log('转换后的positionData数组:', positionData.value)
        console.log('界面将显示的顺序:', positionData.value.map((val, idx) => `D${30 - idx}=${val}`).join(', '))
    }

    // 重置档位数据
    const resetPositionData = (record) => {
        if (record) {
            loadPositionData(record)
            ElMessage.info('已重置档位数据')
        } else {
            positionData.value = new Array(30).fill(0)
            ElMessage.info('已清空档位数据')
        }
    }

    // 保存档位设置
    const savePositionSettings = async (selectedRecord, searchForm) => {
        if (!selectedRecord || !selectedRecord.cigCode) {
            ElMessage.error('请先选中一个卷烟记录')
            return { success: false }
        }

        if (!isPositionDataValid.value) {
            ElMessage.error('请检查档位数据，至少设置一个档位值')
            return { success: false }
        }

        try {
            savingPositions.value = true

            // 构建更新请求数据
            const updateData = {
                cigCode: selectedRecord.cigCode,
                cigName: selectedRecord.cigName,
                year: selectedRecord.year,
                month: selectedRecord.month,
                weekSeq: selectedRecord.weekSeq,
                deliveryMethod: searchForm.distributionType,
                deliveryEtype: searchForm.extendedType,
                deliveryArea: searchForm.distributionArea.join(','),
                distribution: [...positionData.value],
                remark: selectedRecord.remark || '档位设置更新'
            }

            console.log('=== 档位设置数据传输调试 ===')
            console.log('界面显示顺序:', positionData.value.map((val, idx) => `D${30 - idx}=${val}`).join(', '))
            console.log('发送给后端的distribution数组:', updateData.distribution)

            const response = await cigaretteDistributionAPI.updateCigaretteInfo(updateData)

            if (response.data.success) {
                ElMessage.success({
                    message: `档位设置保存成功！更新了${response.data.updatedRecords || 1}条记录`,
                    duration: 2000
                })
                return { success: true, updateData }
            } else {
                throw new Error(response.data.message || '保存失败')
            }
        } catch (error) {
            console.error('保存档位设置失败:', error)
            ElMessage.error(`保存失败: ${error.message}`)
            return { success: false, error }
        } finally {
            savingPositions.value = false
        }
    }

    // 切换档位显示视图
    const switchPositionView = (mode) => {
        positionViewMode.value = mode
        console.log(`切换档位显示模式: ${mode}`)
    }

    return {
        // 状态
        positionData,
        savingPositions,
        positionViewMode,
        isPositionDataValid,

        // 方法
        loadPositionData,
        resetPositionData,
        savePositionSettings,
        switchPositionView
    }
}
