import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 生成分配方案 Composable
 */
export function useGeneratePlan() {
    const dialogVisible = ref(false)
    const generating = ref(false)

    const planForm = ref({
        year: null,
        month: null,
        weekSeq: null,
        urbanRatio: 40,
        ruralRatio: 60
    })

    const isTimeComplete = computed(() => {
        return planForm.value.year &&
            planForm.value.month &&
            planForm.value.weekSeq
    })

    const isRatioValid = computed(() => {
        const total = planForm.value.urbanRatio + planForm.value.ruralRatio
        return total === 100
    })

    const ratioValidationMessage = computed(() => {
        const total = planForm.value.urbanRatio + planForm.value.ruralRatio
        if (total === 100) {
            return `比例设置正确：城网 ${planForm.value.urbanRatio}% + 农网 ${planForm.value.ruralRatio}% = 100%`
        } else if (total > 100) {
            return `比例总和超过100%，当前为 ${total}%，请调整`
        } else {
            return `比例总和不足100%，当前为 ${total}%，请调整`
        }
    })

    const ratioValidationType = computed(() => {
        return isRatioValid.value ? 'success' : 'warning'
    })

    const canGenerate = computed(() => {
        return isTimeComplete.value &&
            isRatioValid.value &&
            !generating.value
    })

    const showDialog = () => {
        dialogVisible.value = true
    }

    const handleGenerate = async () => {
        if (!canGenerate.value) {
            ElMessage.warning('请检查时间和比例设置')
            return { success: false }
        }

        generating.value = true

        try {
            const response = await cigaretteDistributionAPI.generatePlan({
                year: planForm.value.year,
                month: planForm.value.month,
                weekSeq: planForm.value.weekSeq,
                urbanRatio: planForm.value.urbanRatio,
                ruralRatio: planForm.value.ruralRatio
            })

            if (response.data.success) {
                const count = response.data.generatedCount || 0
                ElMessage.success(`分配方案生成成功！生成了 ${count} 条记录`)
                dialogVisible.value = false
                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '生成失败')
            }
        } catch (error) {
            console.error('生成分配方案失败:', error)
            ElMessage.error(`生成失败: ${error.message}`)
            return { success: false, error }
        } finally {
            generating.value = false
        }
    }

    const resetForm = () => {
        planForm.value = {
            year: null,
            month: null,
            weekSeq: null,
            urbanRatio: 40,
            ruralRatio: 60
        }
    }

    return {
        dialogVisible,
        generating,
        planForm,
        isTimeComplete,
        isRatioValid,
        ratioValidationMessage,
        ratioValidationType,
        canGenerate,
        showDialog,
        handleGenerate,
        resetForm
    }
}
