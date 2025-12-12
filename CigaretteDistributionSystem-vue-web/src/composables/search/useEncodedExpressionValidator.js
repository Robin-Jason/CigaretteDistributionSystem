import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 编码表达式验证和更新 Composable (Part 2 - 验证和更新)
 * 这部分包含复杂的验证逻辑和批量更新功能
 */
export function useEncodedExpressionValidator() {

    // 验证编码表达式列表
    const validateEncodedExpressions = (expressions) => {
        const validation = {
            isValid: false,
            errors: [],
            warnings: []
        }

        try {
            const lines = expressions.split('\n')
                .map(line => line.trim())
                .filter(line => line.length > 0)

            if (lines.length === 0) {
                validation.errors.push('编码表达式列表不能为空')
                return validation
            }

            // 解析所有编码表达式
            const parsedExpressions = []
            for (const line of lines) {
                const parsed = parseEncodedExpression(line)
                if (!parsed.isValid) {
                    validation.errors.push(`编码表达式"${line}"解析失败: ${parsed.error}`)
                    return validation
                }
                parsedExpressions.push(parsed)
            }

            // 验证规则1：仅允许存在一种投放类型
            const deliveryTypes = [...new Set(parsedExpressions.map(p => p.deliveryTypeCode))]
            if (deliveryTypes.length > 1) {
                validation.errors.push(`不允许存在多种投放类型，当前包含: ${deliveryTypes.join(', ')}`)
                return validation
            }

            // 验证规则2：不允许存在重复的投放区域编号
            const allAreaCodes = []
            for (const parsed of parsedExpressions) {
                for (const areaCode of parsed.areaCodes) {
                    if (allAreaCodes.includes(areaCode)) {
                        validation.errors.push(`投放区域编号重复: ${areaCode}`)
                        return validation
                    }
                    allAreaCodes.push(areaCode)
                }
            }

            // 验证规则3：档位必须为30个
            for (const parsed of parsedExpressions) {
                const totalPositions = parsed.positionData.length
                if (totalPositions !== 30) {
                    validation.errors.push(`编码表达式的投放量部分必须包含30个完整档位，当前为: ${totalPositions}`)
                    return validation
                }
            }

            // 所有验证通过
            validation.isValid = true
            validation.parsedExpressions = parsedExpressions

            // 添加有用的信息
            validation.warnings.push(`共验证了 ${lines.length} 条编码表达式`)
            validation.warnings.push(`投放类型: ${parsedExpressions[0].deliveryType}`)
            if (parsedExpressions[0].extendedType) {
                validation.warnings.push(`扩展类型: ${parsedExpressions[0].extendedType}`)
            }
            validation.warnings.push(`涉及区域: ${allAreaCodes.length} 个`)

        } catch (error) {
            validation.errors.push(`验证过程异常: ${error.message}`)
        }

        return validation
    }

    // 解析编码表达式
    const parseEncodedExpression = (encoded) => {
        const parsed = {
            deliveryType: '',
            extendedType: '',
            deliveryTypeCode: '',
            extendedTypeCode: '',
            areaCodes: [],
            areaNames: [],
            positionCoding: '',
            positionData: new Array(30).fill(0),
            isValid: false,
            error: ''
        }

        try {
            const trimmed = encoded.trim()
            if (!trimmed) {
                parsed.error = '编码表达式不能为空'
                return parsed
            }

            // 解析投放类型
            const firstChar = trimmed.charAt(0).toUpperCase()
            if (!['A', 'B', 'C'].includes(firstChar)) {
                parsed.error = '投放类型编码错误，必须以A、B或C开头'
                return parsed
            }

            parsed.deliveryTypeCode = firstChar
            parsed.deliveryType = firstChar === 'A' ? '按档位统一投放' :
                firstChar === 'B' ? '按档位扩展投放' : '按需投放'

            // 解析扩展投放类型
            if (firstChar === 'B') {
                const secondChar = trimmed.charAt(1)
                if (!['1', '2', '3', '4', '5'].includes(secondChar)) {
                    parsed.error = 'B类型投放必须指定扩展类型编码（1-5）'
                    return parsed
                }

                parsed.extendedTypeCode = secondChar
                const extendedTypeMap = {
                    '1': '档位+区县',
                    '2': '档位+市场类型',
                    '3': '档位+区县+市场类型',
                    '4': '档位+城乡分类代码',
                    '5': '档位+业态'
                }
                parsed.extendedType = extendedTypeMap[secondChar]
            }

            // 解析区域编码和投放量编码
            const regex = /（([^）]+)）（([^）]+)）/
            const match = trimmed.match(regex)
            if (!match) {
                parsed.error = '编码格式错误，应为：类型+扩展类型（区域编码）（投放量编码）'
                return parsed
            }

            const areaCodeStr = match[1]
            const positionCodeStr = match[2]

            // 解析区域和档位
            const areaResult = parseAreaCodes(areaCodeStr, parsed.extendedTypeCode)
            if (!areaResult.isValid) {
                parsed.error = areaResult.error
                return parsed
            }

            const positionResult = parsePositionCoding(positionCodeStr)
            if (!positionResult.isValid) {
                parsed.error = positionResult.error
                return parsed
            }

            parsed.areaCodes = areaResult.areaCodes
            parsed.areaNames = areaResult.areaNames
            parsed.positionCoding = positionCodeStr
            parsed.positionData = positionResult.positionData
            parsed.isValid = true

        } catch (error) {
            parsed.error = `解析异常: ${error.message}`
        }

        return parsed
    }

    // 解析区域编码 (简化版)
    const parseAreaCodes = (areaCodeStr, extendedTypeCode) => {
        // 简化实现 - 实际应包含完整的区域映射逻辑
        return {
            areaCodes: areaCodeStr.split('+'),
            areaNames: areaCodeStr.split('+'),
            isValid: true,
            error: ''
        }
    }

    // 解析投放量编码 (简化版)
    const parsePositionCoding = (positionCodeStr) => {
        // 简化实现 - 实际应包含完整的档位解析逻辑
        return {
            positionData: new Array(30).fill(0),
            isValid: true,
            error: ''
        }
    }

    // 从编码表达更新记录
    const updateFromEncodedExpression = async (encodedExpressionInput, selectedRecord) => {
        if (!selectedRecord || !selectedRecord.cigCode) {
            ElMessage.error('请先选中一个卷烟记录')
            return { success: false }
        }

        if (!encodedExpressionInput.trim()) {
            ElMessage.error('编码表达不能为空')
            return { success: false }
        }

        try {
            // 验证编码表达式
            const validation = validateEncodedExpressions(encodedExpressionInput)

            if (!validation.isValid) {
                const errorMessage = validation.errors.join('\n')
                ElMessage.error({
                    dangerouslyUseHTMLString: false,
                    message: `编码表达式验证失败:\n${errorMessage}`,
                    duration: 5000
                })
                return { success: false }
            }

            if (validation.warnings.length > 0) {
                ElMessage.success({
                    message: validation.warnings.join(', '),
                    duration: 3000
                })
            }

            // 准备编码表达式列表
            const expressions = encodedExpressionInput.split('\n')
                .map(line => line.trim())
                .filter(line => line.length > 0)

            // 构建批量更新请求数据
            const batchUpdateData = {
                cigCode: selectedRecord.cigCode,
                cigName: selectedRecord.cigName,
                year: selectedRecord.year,
                month: selectedRecord.month,
                weekSeq: selectedRecord.weekSeq,
                encodedExpressions: expressions,
                remark: `基于编码表达式的批量更新，共${expressions.length}条表达式`
            }

            // 调用批量更新接口
            const response = await cigaretteDistributionAPI.batchUpdateFromExpressions(batchUpdateData)

            if (response.data.success) {
                let successMessage = response.data.message

                if (response.data.operation === '投放类型变更') {
                    successMessage += `\n删除了${response.data.deletedRecords}条记录，创建了${response.data.createdRecords}条记录`
                } else if (response.data.operation === '增量更新') {
                    successMessage += `\n新增${response.data.newAreas}个区域，更新${response.data.updatedAreas}个区域，删除${response.data.deletedAreas}个区域`
                }

                ElMessage.success({
                    message: successMessage,
                    duration: 4000
                })

                return { success: true, result: response.data }
            } else {
                throw new Error(response.data.message || '批量更新失败')
            }

        } catch (error) {
            console.error('编码表达式批量更新失败:', error)

            let errorMessage = error.message
            if (error.response && error.response.data) {
                errorMessage = error.response.data.message || errorMessage
            }

            ElMessage.error({
                message: `批量更新失败: ${errorMessage}`,
                duration: 5000
            })

            return { success: false, error }
        }
    }

    return {
        validateEncodedExpressions,
        updateFromEncodedExpression
    }
}
