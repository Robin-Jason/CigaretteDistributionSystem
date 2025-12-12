<template>
  <div class="distribution-type-selector">
    <el-form-item label="投放类型">
      <el-select
        v-model="localDistributionType"
        placeholder="请选择投放类型"
        style="width: 180px"
        clearable
        :disabled="disabled"
        @change="handleDistributionTypeChange"
      >
        <el-option label="按档位统一投放" value="按档位统一投放" />
        <el-option label="按档位扩展投放" value="按档位扩展投放" />
      </el-select>
    </el-form-item>

    <el-form-item 
      label="扩展投放类型" 
      v-if="localDistributionType === '按档位扩展投放'"
    >
      <el-select
        v-model="localExtendedType"
        placeholder="请选择扩展类型"
        style="width: 160px"
        clearable
        :disabled="disabled"
        @change="handleExtendedTypeChange"
      >
        <el-option label="档位+区县" value="档位+区县" />
        <el-option label="档位+市场类型" value="档位+市场类型" />
        <el-option label="档位+城乡分类代码" value="档位+城乡分类代码" />
        <el-option label="档位+业态" value="档位+业态" />
      </el-select>
    </el-form-item>
  </div>
</template>

<script>
export default {
  name: 'DistributionTypeSelector',
  props: {
    distributionType: {
      type: String,
      default: ''
    },
    extendedType: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:distributionType', 'update:extendedType', 'type-change'],
  computed: {
    localDistributionType: {
      get() {
        return this.distributionType
      },
      set(value) {
        this.$emit('update:distributionType', value)
      }
    },
    localExtendedType: {
      get() {
        return this.extendedType
      },
      set(value) {
        this.$emit('update:extendedType', value)
      }
    }
  },
  methods: {
    handleDistributionTypeChange(value) {
      this.$emit('type-change', 'distribution', value)
    },
    handleExtendedTypeChange(value) {
      this.$emit('type-change', 'extended', value)
    }
  }
}
</script>

<style scoped>
.distribution-type-selector {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
