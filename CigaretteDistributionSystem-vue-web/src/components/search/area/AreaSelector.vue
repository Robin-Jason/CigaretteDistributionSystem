<template>
  <el-form-item label="投放区域">
    <el-select
      v-model="localDistributionArea"
      :placeholder="areaPlaceholder"
      style="width: 300px"
      multiple
      collapse-tags
      collapse-tags-tooltip
      clearable
      :disabled="disabled"
      @change="handleChange"
    >
      <el-option
        v-for="option in areaOptions"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
  </el-form-item>
</template>

<script>
export default {
  name: 'AreaSelector',
  props: {
    distributionArea: {
      type: Array,
      default: () => []
    },
    areaOptions: {
      type: Array,
      required: true
    },
    areaPlaceholder: {
      type: String,
      required: true
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:distributionArea', 'change'],
  computed: {
    localDistributionArea: {
      get() {
        return this.distributionArea
      },
      set(value) {
        this.$emit('update:distributionArea', value)
      }
    }
  },
  methods: {
    handleChange(value) {
      this.$emit('change', value)
    }
  }
}
</script>

<style scoped>
/* 区域选择器样式 */
</style>
