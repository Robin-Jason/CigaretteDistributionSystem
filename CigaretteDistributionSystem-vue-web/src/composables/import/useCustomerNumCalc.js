import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

/**
 * 客户数计算 Composable
 */
export function useCustomerNumCalc() {
    const dialogVisible = ref(false)
    const calculating = ref(false)

    const calcForm = ref({
        year: null,
        month: null,
        weekSeq: null,
        customerTypes: [],
        workdays: []
    })

    const isFormComplete = computed(() => {
        return calcForm.value.year &&
            calcForm.value.month &&
            calcForm.value.weekSeq &&
            calcForm.value.customerTypes.length > 0 &&
            calcForm.value.workdays.length > 0
    })

    const canCalculate = computed(() => {
        return isFormComplete.value && !calculating.value
    })

    const showDialog = () => {
        dialogVisible.value = true
    }

    const handleCalculate = async () => {
        if (!canCalculate.value) {
            ElMessage.warning('请填写完整信息')
            return { success: false }
        }

        calculating.value = true

        try {
            const response = await cigaretteDistributionAPI.calculateCustomerNum({
                year: calcForm.value.year,
                month: calcForm.value.month,
                weekSeq: calcForm.value.weekSeq,
                customerTypes: calcForm.value.customerTypes,
                workdays: calcForm.value.workdays
            })

            if (response.data.success) {
                ElMessage.success('客户数计算完成！')
                dialogVisible.value = false
                return { success: true, data: response.data }
            } else {
                throw new Error(response.data.message || '计算失败')
            }
        } catch (error) {
            console.error('客户数计算失败:', error)
            ElMessage.error(`计算失败: ${error.message}`)
            return { success: false, error }
        } finally {
            calculating.value = false
        }
    }

    const resetForm = () => {
        calcForm.value = {
            year: null,
            month: null,
            weekSeq: null,
            customerTypes: [],
            workdays: []
        }
    }

    return {
        dialogVisible,
        calculating,
        calcForm,
        isFormComplete,
        canCalculate,
        showDialog,
        handleCalculate,
        resetForm
    }
}
