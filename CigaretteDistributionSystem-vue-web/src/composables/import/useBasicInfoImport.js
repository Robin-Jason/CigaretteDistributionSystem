import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 基本信息导入 Composable
 */
export function useBasicInfoImport() {
    // 对话框可见性
    const dialogVisible = ref(false)

    // 文件列表
    const fileList = ref([])

    // 导入中状态
    const importing = ref(false)

    // 时间表单
    const timeForm = ref({
        year: null,
        month: null,
        weekSeq: null,
        overwrite: false
    })

    // 时间表单是否完整
    const isTimeComplete = computed(() => {
        return timeForm.value.year &&
            timeForm.value.month &&
            timeForm.value.weekSeq
    })

    // 是否可以导入
    const canImport = computed(() => {
        return fileList.value.length > 0 &&
            isTimeComplete.value &&
            !importing.value
    })

    // 显示对话框
    const showDialog = () => {
        dialogVisible.value = true
    }

    // 检查Excel表结构
    const checkStructure = async (file) => {
        try {
            const requiredColumns = [
                'CIG_CODE', 'CIG_NAME', 'YEAR', 'MONTH', 'WEEK_SEQ',
                'URS', 'ADV', 'DELIVERY_METHOD', 'DELIVERY_ETYPE', 'DELIVERY_AREA', 'remark'
            ]

            const maxSize = 10 * 1024 * 1024 // 10MB
            if (file.size > maxSize) {
                return { valid: false, message: '文件大小超过限制（最大10MB）' }
            }

            const fileName = file.name.toLowerCase()
            const hasValidExtension = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')

            if (!hasValidExtension) {
                return { valid: false, message: `文件格式不正确，请上传Excel文件(.xlsx或.xls)` }
            }

            return { valid: true, message: '表结构检查通过', requiredColumns }
        } catch (error) {
            return { valid: false, message: `表结构检查失败: ${error.message}` }
        }
    }

    // 执行导入
    const handleImport = async () => {
        if (!canImport.value) {
            ElMessage.warning('请检查文件和时间选择')
            return { success: false }
        }

        if (!timeForm.value.year || !timeForm.value.month || !timeForm.value.weekSeq) {
            ElMessage.error('请选择完整的时间信息（年份、月份、周序号）')
            return { success: false }
        }

        importing.value = true

        try {
            const file = fileList.value[0]
            if (!file || !(file instanceof File)) {
                ElMessage.error('文件对象无效，请重新选择文件')
                return { success: false }
            }

            // 检查表结构
            const structureCheck = await checkStructure(file)
            if (!structureCheck.valid) {
                ElMessage.error(structureCheck.message)
                return { success: false }
            }

            // 构建FormData
            const formData = new FormData()
            formData.append('file', file)
            formData.append('year', timeForm.value.year.toString())
            formData.append('month', timeForm.value.month.toString())
            formData.append('weekSeq', timeForm.value.weekSeq.toString())
            formData.append('overwrite', timeForm.value.overwrite ? 'true' : 'false')

            // 调用API
            const response = await cigaretteDistributionAPI.importBasicInfo(formData)

            if (response.data.success) {
                const count = response.data.insertedCount || response.data.importCount
                ElMessage.success(`基本信息导入成功！共导入 ${count} 条记录`)

                // 显示警告信息
                const warnings = response.data.warnings || []
                if (warnings.length > 0) {
                    const warningHtml = `
            <div style="max-height: 260px; overflow-y: auto;">
              <p>以下为后端返回的警告信息，请及时处理：</p>
              <ul style="padding-left: 18px; line-height: 1.8;">
                ${warnings.map(item => `<li>${item}</li>`).join('')}
              </ul>
            </div>
          `
                    await ElMessageBox({
                        title: '导入警告',
                        message: warningHtml,
                        type: 'warning',
                        confirmButtonText: '我已知晓',
                        dangerouslyUseHTMLString: true
                    })
                }

                // 重置表单
                fileList.value = []
                dialogVisible.value = false

                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '导入失败')
            }
        } catch (error) {
            console.error('基本信息导入失败:', error)
            const errorMsg = (error.response && error.response.data && error.response.data.message) || error.message || '导入失败'
            ElMessage.error(`导入失败: ${errorMsg}`)
            return { success: false, error }
        } finally {
            importing.value = false
        }
    }

    // 重置表单
    const resetForm = () => {
        timeForm.value = {
            year: null,
            month: null,
            weekSeq: null,
            overwrite: false
        }
        fileList.value = []
    }

    return {
        // 状态
        dialogVisible,
        fileList,
        importing,
        timeForm,
        isTimeComplete,
        canImport,

        // 方法
        showDialog,
        checkStructure,
        handleImport,
        resetForm
    }
}
