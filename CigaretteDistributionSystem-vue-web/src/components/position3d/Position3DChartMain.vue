<template>
  <div class="position-3d-chart-container">
    <div v-if="!hasData" class="no-data-message">
      <el-empty description="暂无数据可显示" />
    </div>
    <div v-else class="chart-wrapper">
      <div ref="chartContainer" class="chart-container"></div>
      <div v-if="uniqueAmounts.length > 1" class="chart-legend">
        <div class="legend-title">投放量颜色映射</div>
        <div class="legend-items">
          <div 
            v-for="(amount, index) in uniqueAmounts" 
            :key="amount"
            class="legend-item"
          >
            <span 
              class="legend-color-box"
              :style="{ backgroundColor: colorMap.get(amount) }"
            ></span>
            <span class="legend-text">{{ amount }}</span>
            <span class="legend-type">{{ index % 2 === 0 ? '(暖)' : '(冷)' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useChartData } from '@/composables/position3d/useChartData'
import { useChartConfig } from '@/composables/position3d/useChartConfig'
import { useChartRenderer } from '@/composables/position3d/useChartRenderer'

const props = defineProps({
  selectedRecord: {
    type: Object,
    default: () => null
  },
  tableData: {
    type: Array,
    default: () => []
  }
})

// 使用composables
const {
  chartData,
  areaList,
  uniqueAmounts,
  colorMap,
  hasData,
  processData
} = useChartData()

const { generate3DChartConfig } = useChartConfig()

const {
  chartContainer,
  initChart,
  reinitChart,
  updateChart,
  disposeChart
} = useChartRenderer()

// 获取选中卷烟在所有区域的投放数据
const cigaretteData = computed(() => {
  if (!props.selectedRecord || !props.selectedRecord.cigCode) {
    return []
  }

  return props.tableData.filter(item =>
    item.cigCode === props.selectedRecord.cigCode &&
    item.year === props.selectedRecord.year &&
    item.month === props.selectedRecord.month &&
    item.weekSeq === props.selectedRecord.weekSeq
  )
})

// 更新图表数据
const updateChartData = () => {
  processData(cigaretteData.value)
  renderChart()
}

// 渲染图表
const renderChart = () => {
  if (!hasData.value) {
    console.warn('No data available for chart')
    return
  }

  const option = generate3DChartConfig({
    chartData: chartData.value,
    areaList: areaList.value,
    colorMap: colorMap.value,
    selectedRecord: props.selectedRecord
  })

  updateChart(option)
}

// 监听数据变化
watch(() => props.selectedRecord, () => {
  updateChartData()
}, { immediate: true })

watch(() => props.tableData, () => {
  updateChartData()
}, { deep: true })

// 生命周期
onMounted(() => {
  nextTick(() => {
    setTimeout(() => {
      initChart()
      updateChartData()
    }, 100)
  })
})

onBeforeUnmount(() => {
  disposeChart()
})

// 暴露方法供父组件调用
defineExpose({
  reinitChart
})
</script>

<style scoped>
.position-3d-chart-container {
  width: 100%;
  height: 100%;
  min-height: 1125px;
}

.chart-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 1125px;
}

.chart-container {
  flex: 1;
  height: 100%;
  min-height: 1125px;
}

.chart-legend {
  width: 160px;
  padding: 15px;
  background: #f8f9fa;
  border-left: 1px solid #e4e7ed;
  border-radius: 0 6px 6px 0;
  overflow-y: auto;
}

.legend-title {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
  text-align: center;
  border-bottom: 1px solid #ddd;
  padding-bottom: 8px;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.legend-item:hover {
  background-color: #e8f4fd;
}

.legend-color-box {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid #ccc;
  flex-shrink: 0;
}

.legend-text {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  min-width: 30px;
}

.legend-type {
  font-size: 11px;
  color: #666;
  font-style: italic;
}

.no-data-message {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 1125px;
  background: #fafafa;
  border-radius: 6px;
}

:deep(.el-empty__description) {
  color: #999;
  font-size: 14px;
}
</style>
