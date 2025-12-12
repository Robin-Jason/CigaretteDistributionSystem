import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 文件上传通用逻辑 Composable
 */
export function useFileUpload() {
    // 文件列表
    const fileList = ref([])

    // 文件上传前的验证
    const validateFile = (file, maxSizeMB = 10) => {
        const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
            file.type === 'application/vnd.ms-excel' ||
            file.name.endsWith('.xlsx') ||
            file.name.endsWith('.xls')

        if (!isExcel) {
            ElMessage.error('只能上传 Excel 文件 (.xlsx 或 .xls)')
            return false
        }

        const isLt10M = file.size / 1024 / 1024 < maxSizeMB
        if (!isLt10M) {
            ElMessage.error(`文件大小不能超过 ${maxSizeMB}MB`)
            return false
        }

        return true
    }

    // 处理文件变更
    const handleFileChange = (file, fileListRef) => {
        if (!file) return

        // 验证文件
        const isValid = validateFile(file.raw || file)
        if (!isValid) {
            fileListRef.value = []
            return false
        }

        fileListRef.value = [file]
        return true
    }

    // 处理文件移除
    const handleFileRemove = (fileListRef) => {
        fileListRef.value = []
    }

    // 文件上传前的钩子
    const beforeUpload = (file) => {
        return validateFile(file)
    }

    // 重置文件列表
    const resetFileList = (fileListRef) => {
        fileListRef.value = []
    }

    return {
        fileList,
        validateFile,
        handleFileChange,
        handleFileRemove,
        beforeUpload,
        resetFileList
    }
}
