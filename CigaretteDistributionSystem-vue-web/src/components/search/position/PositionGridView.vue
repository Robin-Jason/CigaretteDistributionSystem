<template>
  <div class="position-grid">
    <div 
      v-for="(position, index) in positionData" 
      :key="`d${30 - index}`" 
      class="position-item"
    >
      <span class="position-label">D{{ 30 - index }}:</span>
      <el-input-number
        v-model="positionData[index]"
        :min="0"
        :precision="0"
        :step="1"
        size="small"
        style="width: 90px"
        :disabled="disabled"
        @change="handleChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'PositionGridView',
  props: {
    positionData: {
      type: Array,
      required: true
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['change'],
  methods: {
    handleChange() {
      this.$emit('change')
    }
  }
}
</script>

<style scoped>
.position-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  padding: 15px;
  background: white;
  border-radius: 6px;
}

.position-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.position-label {
  font-weight: 500;
  color: #606266;
  min-width: 40px;
  font-size: 13px;
}
</style>
