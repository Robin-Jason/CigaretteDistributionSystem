import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'

/**
 * 图表渲染管理 Composable
 */
export function useChartRenderer() {
    const chartInstance = ref(null)
    const chartContainer = ref(null)

    // 初始化图表
    const initChart = () => {
        if (!chartContainer.value) {
            console.warn('Chart container not found')
            return false
        }

        // 检查DOM元素是否有尺寸
        const container = chartContainer.value
        if (container.clientWidth === 0 || container.clientHeight === 0) {
            console.warn('Chart container has no width or height, retrying...')
            // 如果没有尺寸，延迟重试
            setTimeout(() => {
                initChart()
            }, 200)
            return false
        }

        console.log('Initializing chart with container size:', {
            width: container.clientWidth,
            height: container.clientHeight
        })

        try {
            chartInstance.value = echarts.init(chartContainer.value)

            // 监听窗口大小变化
            window.addEventListener('resize', handleResize)

            return true
        } catch (error) {
            console.error('Error initializing chart:', error)
            return false
        }
    }

    // 重新初始化图表
    const reinitChart = () => {
        console.log('Reinitializing chart...')
        if (chartInstance.value) {
            chartInstance.value.dispose()
            chartInstance.value = null
        }

        setTimeout(() => {
            initChart()
        }, 50)
    }

    // 更新图表配置
    const updateChart = (option) => {
        if (!chartInstance.value) {
            console.warn('Chart not initialized')
            return false
        }

        try {
            chartInstance.value.setOption(option, true)
            return true
        } catch (error) {
            console.error('Error updating chart:', error)
            return false
        }
    }

    // 调整图表大小
    const handleResize = () => {
        if (chartInstance.value) {
            chartInstance.value.resize()
        }
    }

    // 销毁图表
    const disposeChart = () => {
        if (chartInstance.value) {
            window.removeEventListener('resize', handleResize)
            chartInstance.value.dispose()
            chartInstance.value = null
        }
    }

    return {
        chartInstance,
        chartContainer,
        initChart,
        reinitChart,
        updateChart,
        handleResize,
        disposeChart
    }
}
