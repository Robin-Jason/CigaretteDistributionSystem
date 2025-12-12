import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 数据表格状态管理 Composable
 */
export function useDataTable() {
    // 状态
    const selectedRow = ref(null)
    const tableData = ref([])
    const loading = ref(false)
    const total = ref(0)
    const originalTotal = ref(0)

    // 计算属性
    const reversedPositions = computed(() => {
        // 从30档到1档的顺序
        return Array.from({ length: 30 }, (_, i) => 30 - i)
    })

    // 加载数据
    const loadData = async (params) => {
        // 检查必要的查询参数
        if (!params.year || !params.month || !params.weekSeq) {
            return { success: false }
        }

        loading.value = true
        try {
            const response = await cigaretteDistributionAPI.queryDistribution({
                year: params.year,
                month: params.month,
                weekSeq: params.weekSeq
            })

            if (response.data.success) {
                let rawData = response.data.data || []
                total.value = response.data.total || 0
                originalTotal.value = response.data.originalTotal || 0

                // 为每条数据添加日期显示字段
                rawData = rawData.map(item => ({
                    ...item,
                    dateDisplay: formatDate(item.year, item.month, item.weekSeq)
                }))

                // 对数据进行分组排序
                tableData.value = sortDataForGrouping(rawData)

                ElMessage.success(`查询成功，共找到 ${total.value} 条记录（原始记录：${originalTotal.value} 条）`)

                return { success: true, data: tableData.value }
            } else {
                ElMessage.error(response.data.message || '查询失败')
                return { success: false }
            }
        } catch (error) {
            console.error('查询数据失败:', error)
            ElMessage.error('查询数据失败，请检查网络连接')
            return { success: false, error }
        } finally {
            loading.value = false
        }
    }

    // 行点击处理
    const handleRowClick = (row) => {
        selectedRow.value = { ...row }

        const selectedRecord = {
            ...row,
            cigCode: row.cigCode,
            cigName: row.cigName,
            year: row.year,
            month: row.month,
            weekSeq: row.weekSeq,
            advAmount: row.advAmount,
            actualDelivery: row.actualDelivery,
            deliveryArea: row.deliveryArea,
            deliveryMethod: row.deliveryMethod,
            deliveryEtype: row.deliveryEtype,
            remark: row.remark
        }

        return selectedRecord
    }

    // 行样式计算
    const getRowClassName = ({ row, rowIndex }) => {
        let className = ''

        if (selectedRow.value) {
            const selectedCigCode = String(selectedRow.value.cigCode || '').trim()
            const rowCigCode = String(row.cigCode || '').trim()
            const selectedYear = parseInt(selectedRow.value.year) || 0
            const rowYear = parseInt(row.year) || 0
            const selectedMonth = parseInt(selectedRow.value.month) || 0
            const rowMonth = parseInt(row.month) || 0
            const selectedWeekSeq = parseInt(selectedRow.value.weekSeq) || 0
            const rowWeekSeq = parseInt(row.weekSeq) || 0

            const isSameGroup = (
                selectedCigCode === rowCigCode &&
                selectedYear === rowYear &&
                selectedMonth === rowMonth &&
                selectedWeekSeq === rowWeekSeq
            )

            if (isSameGroup) {
                className += 'selected-group-row '

                const selectedArea = String(selectedRow.value.deliveryArea || '').trim()
                const rowArea = String(row.deliveryArea || '').trim()
                if (selectedArea === rowArea) {
                    className += 'current-selected '
                }
            }
        }

        if (isGroupFirstRow(row, rowIndex)) {
            className += 'group-first-row '
        }

        return className.trim()
    }

    // 更新档位数据
    const updatePositionData = (cigCode, year, month, weekSeq, positionData) => {
        const itemIndex = tableData.value.findIndex(row =>
            row.cigCode === cigCode &&
            row.year === year &&
            row.month === month &&
            row.weekSeq === weekSeq
        )

        if (itemIndex !== -1) {
            const updatedItem = { ...tableData.value[itemIndex] }

            Object.keys(positionData).forEach(key => {
                if (key.startsWith('position')) {
                    const positionNum = key.replace('position', '')
                    const dKey = `d${positionNum}`
                    updatedItem[dKey] = positionData[key]
                }
            })

            tableData.value.splice(itemIndex, 1, updatedItem)
            ElMessage.success('档位数据更新成功')
        }
    }

    // 辅助函数
    const formatDate = (year, month, weekSeq) => {
        if (year && month && weekSeq) {
            return `${year}年${month}月第${weekSeq}周`
        }
        return ''
    }

    const sortDataForGrouping = (data) => {
        return data.sort((a, b) => {
            if (a.cigName !== b.cigName) {
                return a.cigName.localeCompare(b.cigName, 'zh-CN')
            }
            if (a.year !== b.year) return a.year - b.year
            if (a.month !== b.month) return a.month - b.month
            if (a.weekSeq !== b.weekSeq) return a.weekSeq - b.weekSeq
            return (a.deliveryArea || '').localeCompare(b.deliveryArea || '', 'zh-CN')
        })
    }

    const isGroupFirstRow = (row, rowIndex) => {
        if (rowIndex === 0) return true

        const prevRow = tableData.value[rowIndex - 1]
        if (!prevRow) return true

        return (
            String(row.cigName || '') !== String(prevRow.cigName || '') ||
            Number(row.year) !== Number(prevRow.year) ||
            Number(row.month) !== Number(prevRow.month) ||
            Number(row.weekSeq) !== Number(prevRow.weekSeq)
        )
    }

    return {
        // 状态
        selectedRow,
        tableData,
        loading,
        total,
        originalTotal,
        reversedPositions,

        // 方法
        loadData,
        handleRowClick,
        getRowClassName,
        updatePositionData
    }
}
