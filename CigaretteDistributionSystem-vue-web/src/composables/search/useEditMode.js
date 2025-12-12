import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 编辑模式管理 Composable
 * 管理编码修改模式和表单修改模式的互斥逻辑
 */
export function useEditMode() {
    // 编辑模式：'none'(无修改)、'encoding'(只能修改编码)、'form'(只能修改表单)
    const editMode = ref('none')

    // 原始表单状态
    const originalFormState = ref(null)

    // 是否有任何修改
    const hasAnyChanges = computed(() => {
        return editMode.value !== 'none'
    })

    // 编码表达字段是否禁用（当处于表单修改模式时禁用）
    const isEncodingDisabled = computed(() => {
        return editMode.value === 'form'
    })

    // 表单字段是否禁用（当处于编码修改模式时禁用）
    const isFormDisabled = computed(() => {
        return editMode.value === 'encoding'
    })

    // 设置编辑模式
    const setEditMode = (mode) => {
        if (editMode.value === 'none') {
            editMode.value = mode
            console.log(`切换到${mode === 'encoding' ? '编码修改' : '表单修改'}模式`)
        }
    }

    // 重置编辑模式
    const resetEditMode = () => {
        editMode.value = 'none'
        console.log('重置编辑模式')
    }

    // 保存原始表单状态
    const saveOriginalFormState = (formData, positionData, encodedExpression) => {
        originalFormState.value = {
            distributionType: formData.distributionType,
            extendedType: formData.extendedType,
            distributionArea: [...(formData.distributionArea || [])],
            positionData: [...positionData],
            encodedExpression: encodedExpression
        }
        console.log('保存原始表单状态:', originalFormState.value)
    }

    // 清空原始状态
    const clearOriginalFormState = () => {
        originalFormState.value = null
    }

    return {
        // 状态
        editMode,
        originalFormState,
        hasAnyChanges,
        isEncodingDisabled,
        isFormDisabled,

        // 方法
        setEditMode,
        resetEditMode,
        saveOriginalFormState,
        clearOriginalFormState
    }
}
