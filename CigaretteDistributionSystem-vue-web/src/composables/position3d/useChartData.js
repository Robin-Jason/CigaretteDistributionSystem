import { ref, computed } from 'vue'

/**
 * 3D图表数据处理 Composable
 */
export function useChartData() {
    const chartData = ref([])
    const areaList = ref([])
    const uniqueAmounts = ref([])
    const colorMap = ref(new Map())

    // 处理图表数据
    const processData = (cigaretteData) => {
        const data = []
        const areas = new Set()
        const amounts = new Set()

        // 处理每个区域的数据
        cigaretteData.forEach(record => {
            const area = record.deliveryArea || '未知区域'
            areas.add(area)

            // 处理30个档位的数据 (D1-D30)
            for (let i = 1; i <= 30; i++) {
                const positionKey = `d${i}`
                const positionValue = record[positionKey] || 0

                if (positionValue > 0) {
                    data.push([
                        area,          // x轴：区域
                        `D${i}`,       // y轴：档位
                        positionValue  // z轴：投放量
                    ])
                    amounts.add(positionValue)
                }
            }
        })

        chartData.value = data
        areaList.value = Array.from(areas)

        // 处理唯一投放量并生成颜色映射
        uniqueAmounts.value = Array.from(amounts).sort((a, b) => b - a) // 从高到低排序
        generateColorMap()

        console.log('处理后的三维图表数据:', {
            data: chartData.value,
            areas: areaList.value,
            uniqueAmounts: uniqueAmounts.value,
            colorMap: Object.fromEntries(colorMap.value)
        })

        return {
            chartData: chartData.value,
            areaList: areaList.value,
            uniqueAmounts: uniqueAmounts.value
        }
    }

    // 生成颜色映射（冷暖色交替，从高到低由深到浅）
    const generateColorMap = () => {
        colorMap.value.clear()

        if (uniqueAmounts.value.length === 0) return

        // 定义暖色系（红、橙、黄色调）- 从深到浅
        const warmColors = [
            '#8B0000', '#B22222', '#DC143C', '#FF0000', '#FF4500',
            '#FF6347', '#FF7F50', '#FFA500', '#FFB347', '#FFD700'
        ]

        // 定义冷色系（蓝、绿、紫色调）- 从深到浅
        const coolColors = [
            '#000080', '#0000CD', '#0000FF', '#1E90FF', '#4169E1',
            '#6495ED', '#87CEEB', '#87CEFA', '#ADD8E6', '#E0F6FF'
        ]

        if (uniqueAmounts.value.length === 1) {
            // 只有一个投放量时使用深红色
            colorMap.value.set(uniqueAmounts.value[0], '#8B0000')
            return
        }

        // 为每个投放量分配颜色（冷暖色交替）
        uniqueAmounts.value.forEach((amount, index) => {
            let color

            // 计算在当前颜色系中的位置（深到浅）
            const colorIndex = Math.floor((index / 2) % 10) // 每个色系有10种深浅

            if (index % 2 === 0) {
                // 偶数索引使用暖色系
                color = warmColors[colorIndex] || warmColors[warmColors.length - 1]
            } else {
                // 奇数索引使用冷色系
                color = coolColors[colorIndex] || coolColors[coolColors.length - 1]
            }

            colorMap.value.set(amount, color)
        })

        console.log('生成的颜色映射:', Object.fromEntries(colorMap.value))
    }

    const hasData = computed(() => {
        return chartData.value && chartData.value.length > 0
    })

    return {
        chartData,
        areaList,
        uniqueAmounts,
        colorMap,
        hasData,
        processData
    }
}
