import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 客户信息导入 Composable
 */
export function useCustomerInfoImport() {
    const dialogVisible = ref(false)
    const fileList = ref([])
    const importing = ref(false)

    const importForm = ref({
        sheetIndex: 0,
        skipHeaderRows: 1,
        overwriteMode: 'APPEND'
    })

    const canImport = computed(() => {
        return fileList.value.length > 0 && !importing.value
    })

    const showDialog = () => {
        dialogVisible.value = true
    }

    const handleImport = async () => {
        if (!canImport.value) {
            ElMessage.warning('请先选择文件')
            return { success: false }
        }

        importing.value = true

        try {
            const file = fileList.value[0]
            if (!file || !(file instanceof File)) {
                ElMessage.error('文件对象无效')
                return { success: false }
            }

            const formData = new FormData()
            formData.append('file', file)
            formData.append('sheetIndex', importForm.value.sheetIndex.toString())
            formData.append('skipRows', importForm.value.skipHeaderRows.toString())
            formData.append('mode', importForm.value.overwriteMode)

            const response = await cigaretteDistributionAPI.importCustomerInfo(formData)

            if (response.data.success) {
                const count = response.data.importedCount || response.data.count || 0
                ElMessage.success(`客户信息导入成功！共处理 ${count} 条记录`)

                fileList.value = []
                dialogVisible.value = false

                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '导入失败')
            }
        } catch (error) {
            console.error('客户信息导入失败:', error)
            ElMessage.error(`导入失败: ${error.message}`)
            return { success: false, error }
        } finally {
            importing.value = false
        }
    }

    const resetForm = () => {
        importForm.value = {
            sheetIndex: 0,
            skipHeaderRows: 1,
            overwriteMode: 'APPEND'
        }
        fileList.value = []
    }

    return {
        dialogVisible,
        fileList,
        importing,
        importForm,
        canImport,
        showDialog,
        handleImport,
        resetForm
    }
}
