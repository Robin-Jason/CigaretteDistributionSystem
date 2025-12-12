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

<script>
import * as echarts from 'echarts'
import 'echarts-gl'

export default {
  name: 'Position3DChart',
  props: {
    selectedRecord: {
      type: Object,
      default: () => null
    },
    tableData: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      chart: null,
      chartData: [],
      uniqueAmounts: [],
      colorMap: new Map()
    }
  },
  computed: {
    hasData() {
      return this.chartData && this.chartData.length > 0
    },
    
    // 获取选中卷烟在所有区域的投放数据
    cigaretteData() {
      if (!this.selectedRecord || !this.selectedRecord.cigCode) {
        return []
      }
      
      // 筛选出选中卷烟的所有记录
      return this.tableData.filter(item => 
        item.cigCode === this.selectedRecord.cigCode &&
        item.year === this.selectedRecord.year &&
        item.month === this.selectedRecord.month &&
        item.weekSeq === this.selectedRecord.weekSeq
      )
    }
  },
  watch: {
    selectedRecord: {
      handler() {
        this.updateChartData()
      },
      immediate: true
    },
    tableData: {
      handler() {
        this.updateChartData()
      },
      deep: true
    }
  },
  mounted() {
    // 延迟初始化，确保DOM完全渲染
    this.$nextTick(() => {
      setTimeout(() => {
        this.initChart()
      }, 100)
    })
  },
  beforeUnmount() {
    if (this.chart) {
      this.chart.dispose()
    }
  },
  methods: {
    initChart() {
      if (!this.$refs.chartContainer) {
        console.warn('Chart container not found')
        return
      }
      
      // 检查DOM元素是否有尺寸
      const container = this.$refs.chartContainer
      if (container.clientWidth === 0 || container.clientHeight === 0) {
        console.warn('Chart container has no width or height, retrying...')
        // 如果没有尺寸，延迟重试
        setTimeout(() => {
          this.initChart()
        }, 200)
        return
      }
      
      console.log('Initializing chart with container size:', {
        width: container.clientWidth,
        height: container.clientHeight
      })
      
      try {
        this.chart = echarts.init(this.$refs.chartContainer)
        this.updateChartData()
        
        // 监听窗口大小变化
        window.addEventListener('resize', this.handleResize)
      } catch (error) {
        console.error('Error initializing chart:', error)
      }
    },
    
    // 重新初始化图表 - 当弹窗完全打开后调用
    reinitChart() {
      console.log('Reinitializing chart...')
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
      
      this.$nextTick(() => {
        setTimeout(() => {
          this.initChart()
        }, 50)
      })
    },
    
    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    },
    
    updateChartData() {
      this.processData()
      this.renderChart()
    },
    
    processData() {
      const data = []
      const areas = new Set()
      const amounts = new Set()
      
      // 处理每个区域的数据
      this.cigaretteData.forEach(record => {
        const area = record.deliveryArea || '未知区域'
        areas.add(area)
        
        // 处理30个档位的数据 (D1-D30)
        for (let i = 1; i <= 30; i++) {
          const positionKey = `d${i}`
          const positionValue = record[positionKey] || 0
          
          if (positionValue > 0) {
            data.push([
              area,        // x轴：区域
              `D${i}`,     // y轴：档位
              positionValue // z轴：投放量
            ])
            amounts.add(positionValue)
          }
        }
      })
      
      this.chartData = data
      this.areaList = Array.from(areas)
      
      // 处理唯一投放量并生成颜色映射
      this.uniqueAmounts = Array.from(amounts).sort((a, b) => b - a) // 从高到低排序
      this.generateColorMap()
      
      console.log('处理后的三维图表数据:', {
        data: this.chartData,
        areas: this.areaList,
        uniqueAmounts: this.uniqueAmounts,
        colorMap: Object.fromEntries(this.colorMap),
        selectedRecord: this.selectedRecord
      })
    },
    
    // 生成颜色映射（冷暖色交替，从高到低由深到浅）
    generateColorMap() {
      this.colorMap.clear()
      
      if (this.uniqueAmounts.length === 0) return
      
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
      
      if (this.uniqueAmounts.length === 1) {
        // 只有一个投放量时使用深红色
        this.colorMap.set(this.uniqueAmounts[0], '#8B0000')
        return
      }
      
      // 为每个投放量分配颜色（冷暖色交替）
      this.uniqueAmounts.forEach((amount, index) => {
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
        
        this.colorMap.set(amount, color)
      })
      
      console.log('生成的颜色映射:', Object.fromEntries(this.colorMap))
    },
    
    renderChart() {
      if (!this.chart || !this.hasData) {
        console.warn('Chart not initialized or no data available')
        return
      }
      
      // 生成档位列表 (D1-D30)
      const positions = []
      for (let i = 1; i <= 30; i++) {
        positions.push(`D${i}`)
      }
      
      const option = {
        title: {
          text: `${(this.selectedRecord && this.selectedRecord.cigName) || '卷烟'} - 各区域档位投放分布`,
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold',
            color: '#333'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: ((colorMap) => {
            return function(params) {
              const [area, position, amount] = params.data
              const color = colorMap.get(amount) || '#8B0000'
              return `区域: ${area}<br/>档位: ${position}<br/>投放量: ${amount}<br/><span style="color: ${color};">■</span> 颜色: ${color}`
            }
          })(this.colorMap)
        },
        legend: {
          show: false
        },
        xAxis3D: {
          type: 'category',
          name: '投放区域',
          data: this.areaList,
          nameTextStyle: {
            fontSize: 12,
            color: '#666'
          },
          axisLabel: {
            fontSize: 10,
            color: '#666',
            interval: 0,
            rotate: 45
          }
        },
        yAxis3D: {
          type: 'category',
          name: '档位',
          data: positions,
          nameTextStyle: {
            fontSize: 12,
            color: '#666'
          },
          axisLabel: {
            fontSize: 10,
            color: '#666'
          }
        },
        zAxis3D: {
          type: 'value',
          name: '投放量',
          nameTextStyle: {
            fontSize: 12,
            color: '#666'
          },
          axisLabel: {
            fontSize: 10,
            color: '#666'
          }
        },
        grid3D: {
          boxWidth: 120,   // 增大3D网格宽度，提供更多空间
          boxDepth: 120,   // 增大3D网格深度，提供更多空间
          boxHeight: 60,
          alpha: 25,
          beta: 40,
          viewControl: {
            projection: 'orthographic',
            autoRotate: false,
            rotateSensitivity: 1,
            zoomSensitivity: 1,
            panSensitivity: 1,
            alpha: 25,
            beta: 40,
            distance: 250   // 增加观察距离，避免柱子显得过于密集
          },
          light: {
            main: {
              intensity: 1.5,
              alpha: 30,
              beta: 40
            },
            ambient: {
              intensity: 0.3
            }
          },
          environment: '#f8f9fa'
        },
        series: [{
          type: 'bar3D',
          data: this.chartData,
          shading: 'lambert',
          // 增加柱子间距配置，解决重叠问题
          barGap: 0.4,                // 同一类目下不同柱子的间距（40%间距）
          barCategoryGap: 0.5,        // 不同类目间柱子的间距（50%间距）
          emphasis: {
            label: {
              show: true,
              fontSize: 12,
              fontWeight: 'bold'
            },
            itemStyle: {
              color: '#FFD700' // 强调时使用金色
            }
          },
          itemStyle: {
            color: ((colorMap) => {
              return (params) => {
                // 根据投放量从颜色映射中获取对应颜色
                const amount = params.data[2]
                return colorMap.get(amount) || '#8B0000' // 默认深红色
              }
            })(this.colorMap),
            opacity: 0.8,
            // 增加边框，进一步区分不同颜色的柱子
            borderWidth: 1,
            borderColor: 'rgba(255,255,255,0.3)'
          },
          label: {
            show: false,
            fontSize: 10
          }
        }]
      }
      
      this.chart.setOption(option, true)
    }
  }
}
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
