import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 编码表达式管理 Composable (Part 1 - 核心功能)
 * 由于逻辑过于复杂，拆分为两部分
 */
export function useEncodedExpression() {
    // 编码化表达相关
    const encodedExpressionInput = ref('')
    const originalEncodedExpression = ref('')
    const updatingFromEncoded = ref(false)

    // 编码表达是否已变更
    const isEncodedExpressionChanged = computed(() => {
        return encodedExpressionInput.value.trim() !== originalEncodedExpression.value.trim()
    })

    // 加载编码化表达（支持多记录聚合显示）
    const loadEncodedExpression = (record, tableData) => {
        if (!record) {
            encodedExpressionInput.value = ''
            originalEncodedExpression.value = ''
            return
        }

        // 获取选中卷烟的所有相关记录
        const relatedRecords = getRelatedRecords(record, tableData)

        // 为每个记录生成独立的编码表达式
        const expressions = []
        relatedRecords.forEach(rec => {
            let expression = ''
            // 优先使用后端提供的编码表达式
            if (rec.encodedExpression) {
                expression = rec.encodedExpression
            } else {
                // 生成该记录的编码表达式
                expression = generateRecordEncodedExpression(rec)
            }
            expressions.push(expression)
        })

        // 将多个编码表达式按行显示
        const multiLineExpressions = expressions.join('\n')

        encodedExpressionInput.value = multiLineExpressions
        originalEncodedExpression.value = multiLineExpressions

        console.log('加载编码化表达:', {
            cigName: record.cigName,
            relatedRecordsCount: relatedRecords.length,
            expressionsCount: expressions.length
        })
    }

    // 获取选中卷烟的所有相关记录
    const getRelatedRecords = (selectedRecord, tableData) => {
        if (!tableData || !selectedRecord) {
            return [selectedRecord].filter(Boolean)
        }

        // 查找同一卷烟同一时间的所有记录
        const relatedRecords = tableData.filter(record =>
            record.cigCode === selectedRecord.cigCode &&
            record.cigName === selectedRecord.cigName &&
            record.year === selectedRecord.year &&
            record.month === selectedRecord.month &&
            record.weekSeq === selectedRecord.weekSeq
        )

        return relatedRecords.length > 0 ? relatedRecords : [selectedRecord]
    }

    // 生成单个记录的编码表达式
    const generateRecordEncodedExpression = (record) => {
        try {
            // 1. 生成投放类型编码
            let typeCode = getDeliveryTypeCode(record)

            // 2. 生成区域编码
            let areaCode = getAreaCode(record.deliveryArea, record.deliveryEtype)

            // 3. 生成档位投放量编码
            const positionCode = generatePositionCode(record)

            // 4. 组合完整编码
            if (record.deliveryMethod === '按档位统一投放') {
                return `${typeCode}（${positionCode}）`
            } else {
                return `${typeCode}（${areaCode}）（${positionCode}）`
            }
        } catch (error) {
            console.warn('生成编码表达式失败:', error)
            return ''
        }
    }

    // 获取投放类型编码
    const getDeliveryTypeCode = (record) => {
        if (record.deliveryMethod === '按档位统一投放') {
            return 'A'
        } else if (record.deliveryMethod === '按档位扩展投放') {
            let code = 'B'
            // 添加扩展类型编码
            if (record.deliveryEtype === '档位+区县') {
                code += '1'
            } else if (record.deliveryEtype === '档位+市场类型') {
                code += '2'
            } else if (record.deliveryEtype === '档位+城乡分类代码') {
                code += '4'
            } else if (record.deliveryEtype === '档位+业态') {
                code += '5'
            }
            return code
        }
        return 'A' // 默认
    }

    // 获取区域编码
    const getAreaCode = (deliveryArea, deliveryEtype) => {
        if (!deliveryArea) return ''

        const areaMapping = {
            '档位+区县': {
                '城区': '1', '丹江': '2', '房县': '3', '郧西': '4',
                '郧阳': '5', '竹山': '6', '竹溪': '7'
            },
            '档位+市场类型': {
                '城网': 'C', '农网': 'N'
            },
            '档位+城乡分类代码': {
                '主城区': '①', '城乡结合区': '②', '镇中心区': '③',
                '镇乡结合区': '④', '特殊区域': '⑤', '乡中心区': '⑥', '村庄': '⑦'
            },
            '档位+业态': {
                '便利店': 'a', '超市': 'b', '商场': 'c',
                '烟草专卖店': 'd', '娱乐服务类': 'e', '其他': 'f'
            }
        }

        const mapping = areaMapping[deliveryEtype]
        if (mapping && mapping[deliveryArea]) {
            return mapping[deliveryArea]
        }

        return deliveryArea.charAt(0)
    }

    // 生成档位投放量编码
    const generatePositionCode = (record) => {
        const positionGroups = []
        let currentValue = null
        let currentCount = 0

        // 从D30到D1遍历
        for (let i = 30; i >= 1; i--) {
            const value = record[`d${i}`] || 0

            if (value === currentValue) {
                currentCount++
            } else {
                if (currentValue !== null && currentCount > 0) {
                    positionGroups.push(`${currentCount}×${currentValue}`)
                }
                currentValue = value
                currentCount = 1
            }
        }

        if (currentValue !== null && currentCount > 0) {
            positionGroups.push(`${currentCount}×${currentValue}`)
        }

        return positionGroups.filter(group => !group.endsWith('×0')).join('+') || '无档位设置'
    }

    return {
        // 状态
        encodedExpressionInput,
        originalEncodedExpression,
        updatingFromEncoded,
        isEncodedExpressionChanged,

        // 方法
        loadEncodedExpression,
        getRelatedRecords,
        generateRecordEncodedExpression
    }
}
