import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 区域统计重建 Composable
 */
export function useRegionStats() {
    const dialogVisible = ref(false)
    const rebuilding = ref(false)

    const statsForm = ref({
        year: null,
        month: null,
        weekSeq: null,
        overwriteExisting: false
    })

    const isFormComplete = computed(() => {
        return statsForm.value.year &&
            statsForm.value.month &&
            statsForm.value.weekSeq
    })

    const canRebuild = computed(() => {
        return isFormComplete.value && !rebuilding.value
    })

    const showDialog = () => {
        dialogVisible.value = true
    }

    const handleRebuild = async () => {
        if (!canRebuild.value) {
            ElMessage.warning('请选择完整的时间信息')
            return { success: false }
        }

        rebuilding.value = true

        try {
            const response = await cigaretteDistributionAPI.rebuildRegionStats({
                year: statsForm.value.year,
                month: statsForm.value.month,
                weekSeq: statsForm.value.weekSeq,
                overwrite: statsForm.value.overwriteExisting
            })

            if (response.data.success) {
                ElMessage.success('区域客户统计重建成功！')
                dialogVisible.value = false
                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '重建失败')
            }
        } catch (error) {
            console.error('区域统计重建失败:', error)
            ElMessage.error(`重建失败: ${error.message}`)
            return { success: false, error }
        } finally {
            rebuilding.value = false
        }
    }

    const resetForm = () => {
        statsForm.value = {
            year: null,
            month: null,
            weekSeq: null,
            overwriteExisting: false
        }
    }

    return {
        dialogVisible,
        rebuilding,
        statsForm,
        isFormComplete,
        canRebuild,
        showDialog,
        handleRebuild,
        resetForm
    }
}
