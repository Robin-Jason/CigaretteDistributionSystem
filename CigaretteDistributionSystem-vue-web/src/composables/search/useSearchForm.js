import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 搜索表单状态管理 Composable
 */
export function useSearchForm() {
    // 搜索表单状态
    const searchForm = ref({
        year: null,
        month: null,
        week: null,
        cigaretteName: '',
        distributionType: '',
        extendedType: '',
        distributionArea: []
    })

    // 年份选项（当前年份往前2年，往后10年）
    const yearOptions = computed(() => {
        const currentYear = new Date().getFullYear()
        const years = []
        for (let year = currentYear - 2; year <= currentYear + 10; year++) {
            years.push(year)
        }
        return years
    })

    // 月份选项
    const monthOptions = computed(() => {
        return Array.from({ length: 12 }, (_, i) => i + 1)
    })

    // 周选项
    const weekOptions = computed(() => {
        return [1, 2, 3, 4, 5]
    })

    // 日期是否完整
    const isDateComplete = computed(() => {
        return searchForm.value.year && searchForm.value.month && searchForm.value.week
    })

    // 卷烟名称占位符
    const cigaretteNamePlaceholder = computed(() => {
        if (!isDateComplete.value) {
            return '请先填充年份、月份和周序号'
        }
        return '请输入卷烟名称'
    })

    // 重置搜索表单
    const resetSearchForm = () => {
        searchForm.value = {
            year: null,
            month: null,
            week: null,
            cigaretteName: '',
            distributionType: '',
            extendedType: '',
            distributionArea: []
        }
        ElMessage.info('已重置搜索条件')
    }

    // 更新搜索表单（供外部调用）
    const updateSearchForm = (params) => {
        if (params) {
            searchForm.value.year = params.year
            searchForm.value.month = params.month
            searchForm.value.week = params.week
            console.log('外部更新搜索表单:', params)
        }
    }

    // 处理卷烟名称输入
    const handleCigaretteNameInput = (value) => {
        // 实时搜索时去除前后空格
        searchForm.value.cigaretteName = value.trim()
    }

    // 验证搜索表单
    const validateSearchForm = () => {
        if (!searchForm.value.year || !searchForm.value.month || !searchForm.value.week) {
            ElMessage.warning('请至少选择一个时间条件')
            return false
        }

        if (searchForm.value.distributionType) {
            if (searchForm.value.distributionType === '按档位扩展投放' && !searchForm.value.extendedType) {
                ElMessage.warning('请选择扩展投放类型')
                return false
            }

            if (!searchForm.value.distributionArea || searchForm.value.distributionArea.length === 0) {
                ElMessage.warning('请选择投放区域')
                return false
            }
        }

        return true
    }

    // 格式化数量显示
    const formatQuantity = (value) => {
        if (value === null || value === undefined || value === '') {
            return '未设置'
        }

        const numValue = parseFloat(value)
        if (!isNaN(numValue)) {
            return numValue.toFixed(2)
        }

        return value
    }

    return {
        // 状态
        searchForm,
        yearOptions,
        monthOptions,
        weekOptions,
        isDateComplete,
        cigaretteNamePlaceholder,

        // 方法
        resetSearchForm,
        updateSearchForm,
        handleCigaretteNameInput,
        validateSearchForm,
        formatQuantity
    }
}
