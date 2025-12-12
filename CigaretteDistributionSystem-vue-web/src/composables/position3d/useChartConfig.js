/**
 * ECharts 3D配置生成 Composable
 */
export function useChartConfig() {

    // 生成完整的3D图表配置
    const generate3DChartConfig = (options) => {
        const {
            chartData,
            areaList,
            colorMap,
            selectedRecord,
            title = '卷烟档位投放分布'
        } = options

        // 生成档位列表 (D1-D30)
        const positions = []
        for (let i = 1; i <= 30; i++) {
            positions.push(`D${i}`)
        }

        return {
            title: getTitleConfig(selectedRecord, title),
            tooltip: getTooltipConfig(colorMap),
            legend: { show: false },
            xAxis3D: getXAxis3DConfig(areaList),
            yAxis3D: getYAxis3DConfig(positions),
            zAxis3D: getZAxis3DConfig(),
            grid3D: getGrid3DConfig(),
            series: getSeriesConfig(chartData, colorMap)
        }
    }

    // 标题配置
    const getTitleConfig = (selectedRecord, title) => {
        const cigName = (selectedRecord && selectedRecord.cigName) || '卷烟'
        return {
            text: `${cigName} - 各区域档位投放分布`,
            left: 'center',
            top: 10,
            textStyle: {
                fontSize: 16,
                fontWeight: 'bold',
                color: '#333'
            }
        }
    }

    // 提示框配置
    const getTooltipConfig = (colorMap) => {
        return {
            trigger: 'item',
            formatter: (params) => {
                const [area, position, amount] = params.data
                const color = colorMap.get(amount) || '#8B0000'
                return `区域: ${area}<br/>档位: ${position}<br/>投放量: ${amount}<br/><span style="color: ${color};">■</span> 颜色: ${color}`
            }
        }
    }

    // X轴配置（投放区域）
    const getXAxis3DConfig = (areaList) => {
        return {
            type: 'category',
            name: '投放区域',
            data: areaList,
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
        }
    }

    // Y轴配置（档位）
    const getYAxis3DConfig = (positions) => {
        return {
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
        }
    }

    // Z轴配置（投放量）
    const getZAxis3DConfig = () => {
        return {
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
        }
    }

    // 3D网格配置
    const getGrid3DConfig = () => {
        return {
            boxWidth: 120,
            boxDepth: 120,
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
                distance: 250
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
        }
    }

    // 系列配置
    const getSeriesConfig = (chartData, colorMap) => {
        return [{
            type: 'bar3D',
            data: chartData,
            shading: 'lambert',
            barGap: 0.4,
            barCategoryGap: 0.5,
            emphasis: {
                label: {
                    show: true,
                    fontSize: 12,
                    fontWeight: 'bold'
                },
                itemStyle: {
                    color: '#FFD700'
                }
            },
            itemStyle: {
                color: (params) => {
                    const amount = params.data[2]
                    return colorMap.get(amount) || '#8B0000'
                },
                opacity: 0.8,
                borderWidth: 1,
                borderColor: 'rgba(255,255,255,0.3)'
            },
            label: {
                show: false,
                fontSize: 10
            }
        }]
    }

    return {
        generate3DChartConfig
    }
}
