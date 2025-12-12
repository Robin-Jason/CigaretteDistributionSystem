import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 策略写回 Composable
 */
export function useWriteBack() {
    const dialogVisible = ref(false)
    const writingBack = ref(false)

    const writeBackForm = ref({
        year: null,
        month: null,
        weekSeq: null,
        enableRatio: false,
        urbanRatio: 40,
        ruralRatio: 60
    })

    const isTimeComplete = computed(() => {
        return writeBackForm.value.year &&
            writeBackForm.value.month &&
            writeBackForm.value.weekSeq
    })

    const isRatioValid = computed(() => {
        const total = writeBackForm.value.urbanRatio + writeBackForm.value.ruralRatio
        return total === 100
    })

    const ratioValidationMessage = computed(() => {
        const total = writeBackForm.value.urbanRatio + writeBackForm.value.ruralRatio
        if (total === 100) {
            return `比例设置正确：城网 ${writeBackForm.value.urbanRatio}% + 农网 ${writeBackForm.value.ruralRatio}% = 100%`
        }
        return `比例总和为 ${total}% ，请调整至 100%`
    })

    const ratioValidationType = computed(() => {
        return isRatioValid.value ? 'success' : 'warning'
    })

    const canWriteBack = computed(() => {
        return isTimeComplete.value &&
            (!writeBackForm.value.enableRatio || isRatioValid.value) &&
            !writingBack.value
    })

    const showDialog = () => {
        dialogVisible.value = true
    }

    const handleWriteBack = async () => {
        if (!canWriteBack.value) {
            ElMessage.warning('请检查时间选择和比例设置')
            return { success: false }
        }

        writingBack.value = true

        try {
            const params = {
                year: writeBackForm.value.year,
                month: writeBackForm.value.month,
                weekSeq: writeBackForm.value.weekSeq
            }

            if (writeBackForm.value.enableRatio) {
                params.urbanRatio = writeBackForm.value.urbanRatio
                params.ruralRatio = writeBackForm.value.ruralRatio
            }

            const response = await cigaretteDistributionAPI.executeWriteBack(params)

            if (response.data.success) {
                const count = response.data.processedCount || response.data.count || 0
                ElMessage.success(`策略写回成功！处理了 ${count} 条记录`)
                dialogVisible.value = false
                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '写回失败')
            }
        } catch (error) {
            console.error('策略写回失败:', error)
            ElMessage.error(`写回失败: ${error.message}`)
            return { success: false, error }
        } finally {
            writingBack.value = false
        }
    }

    const resetForm = () => {
        writeBackForm.value = {
            year: null,
            month: null,
            weekSeq: null,
            enableRatio: false,
            urbanRatio: 40,
            ruralRatio: 60
        }
    }

    return {
        dialogVisible,
        writingBack,
        writeBackForm,
        isTimeComplete,
        isRatioValid,
        ratioValidationMessage,
        ratioValidationType,
        canWriteBack,
        showDialog,
        handleWriteBack,
        resetForm
    }
}
