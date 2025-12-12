import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ExcelExporter } from '@/utils/excelExport'

/**
 * 表格导出功能 Composable
 */
export function useTableExport() {
    const exportLoading = ref(false)

    const handleExport = (tableData, searchParams) => {
        if (!tableData || tableData.length === 0) {
            ElMessage.warning('暂无数据可导出')
            return { success: false }
        }

        exportLoading.value = true

        try {
            // 调用Excel导出工具类
            const result = ExcelExporter.exportCigaretteData(
                tableData,
                searchParams
            )

            if (result.success) {
                ElMessage.success(`Excel文件导出成功：${result.filename}`)
                return { success: true, filename: result.filename }
            } else {
                ElMessage.error(result.message)
                return { success: false, message: result.message }
            }
        } catch (error) {
            console.error('导出失败:', error)
            ElMessage.error('导出失败，请稍后重试')
            return { success: false, error }
        } finally {
            exportLoading.value = false
        }
    }

    return {
        exportLoading,
        handleExport
    }
}
