import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 误差筛选 Composable
 */
export function useDeviationFilter() {
    // 误差筛选相关
    const largeDeviationRecords = ref([])  // 绝对误差>200的记录列表
    const currentDeviationIndex = ref(-1)  // 当前选中的误差记录索引
    const filteringDeviation = ref(false)  // 筛选加载状态

    // 筛选绝对误差大于200的卷烟
    const filterLargeDeviation = async (tableData, emitRefresh, emitSelectRecord) => {
        if (!tableData || tableData.length === 0) {
            ElMessage.warning('暂无数据可筛选')
            return
        }

        filteringDeviation.value = true

        try {
            // 1. 先触发数据刷新，确保获取最新数据
            console.log('刷新表格数据以获取最新的投放量信息...')
            if (emitRefresh) {
                emitRefresh()
            }

            // 等待数据刷新完成
            await new Promise(resolve => setTimeout(resolve, 500))

            // 2. 筛选出绝对误差大于200的记录
            const deviationRecords = tableData.filter(record => {
                const advAmount = parseFloat(record.advAmount) || 0
                const actualDelivery = parseFloat(record.actualDelivery) || 0
                const deviation = Math.abs(actualDelivery - advAmount)
                return deviation > 200
            })

            console.log('筛选出的误差>200记录:', deviationRecords)

            // 3. 如果没有符合条件的记录
            if (deviationRecords.length === 0) {
                ElMessage.info('没有找到绝对误差大于200的卷烟')
                largeDeviationRecords.value = []
                currentDeviationIndex.value = -1
                return
            }

            // 4. 第一次筛选或者记录列表已改变，重置索引
            if (largeDeviationRecords.value.length === 0 ||
                largeDeviationRecords.value.length !== deviationRecords.length) {
                largeDeviationRecords.value = deviationRecords
                currentDeviationIndex.value = 0
            } else {
                // 5. 循环选择下一个
                currentDeviationIndex.value = (currentDeviationIndex.value + 1) % deviationRecords.length
                largeDeviationRecords.value = deviationRecords
            }

            // 6. 获取当前要选中的记录
            const currentRecord = largeDeviationRecords.value[currentDeviationIndex.value]

            // 7. 计算误差信息
            const advAmount = parseFloat(currentRecord.advAmount) || 0
            const actualDelivery = parseFloat(currentRecord.actualDelivery) || 0
            const deviation = Math.abs(actualDelivery - advAmount)

            // 8. 触发选中事件
            if (emitSelectRecord) {
                emitSelectRecord([currentRecord])
            }

            // 9. 显示提示信息
            ElMessage.success({
                message: `已选中第 ${currentDeviationIndex.value + 1}/${largeDeviationRecords.value.length} 个误差记录\n卷烟：${currentRecord.cigName}\n预投放量：${advAmount.toFixed(2)}\n实际投放量：${actualDelivery.toFixed(2)}\n绝对误差：${deviation.toFixed(2)}`,
                duration: 3000,
                dangerouslyUseHTMLString: false
            })

            console.log('已选中误差记录:', {
                index: currentDeviationIndex.value + 1,
                total: largeDeviationRecords.value.length,
                record: currentRecord,
                deviation: deviation
            })

        } catch (error) {
            console.error('筛选误差记录失败:', error)
            ElMessage.error('筛选失败，请重试')
        } finally {
            filteringDeviation.value = false
        }
    }

    // 重置误差筛选状态
    const resetDeviationFilter = () => {
        largeDeviationRecords.value = []
        currentDeviationIndex.value = -1
    }

    return {
        // 状态
        largeDeviationRecords,
        currentDeviationIndex,
        filteringDeviation,

        // 方法
        filterLargeDeviation,
        resetDeviationFilter
    }
}
