import { ElMessage } from 'element-plus'

/**
 * 表格格式化工具 Composable
 */
export function useTableFormatter() {

    // 单元格样式类
    const getCellClass = (value) => {
        if (!value || value === 0) return ''
        if (value > 30) return 'high-value'
        if (value > 10) return 'medium-value'
        return 'low-value'
    }

    // 复制编码表达式
    const copyEncodedExpression = async (encodedExpression) => {
        if (!encodedExpression) {
            ElMessage.warning('暂无编码表达可复制')
            return false
        }

        try {
            if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(encodedExpression)
            } else {
                const textarea = document.createElement('textarea')
                textarea.value = encodedExpression
                textarea.style.position = 'fixed'
                textarea.style.opacity = '0'
                document.body.appendChild(textarea)
                textarea.focus()
                textarea.select()
                document.execCommand('copy')
                document.body.removeChild(textarea)
            }
            ElMessage.success('编码表达已复制到剪贴板')
            return true
        } catch (error) {
            console.error('复制编码表达失败:', error)
            ElMessage.error('复制失败，请手动选择文本复制')
            return false
        }
    }

    // 分割加号值
    const splitPlusValues = (value) => {
        if (!value) return []
        return value
            .split('+')
            .map(item => item.trim())
            .filter(Boolean)
    }

    // 格式化数字
    const formatNumber = (value) => {
        if (value === null || value === undefined || value === '') {
            return '-'
        }
        return value
    }

    // 格式化日期
    const formatDate = (year, month, weekSeq) => {
        if (year && month && weekSeq) {
            return `${year}年${month}月第${weekSeq}周`
        }
        return ''
    }

    return {
        getCellClass,
        copyEncodedExpression,
        splitPlusValues,
        formatNumber,
        formatDate
    }
}
