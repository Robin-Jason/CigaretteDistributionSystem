<template>
  <div class="quantity-display" v-if="selectedRecord && selectedRecord.cigCode">
    <el-form-item label="预投放量">
      <el-input
        :value="formatQuantity(selectedRecord.advAmount)"
        style="width: 150px"
        readonly
        suffix-icon="el-icon-info"
      />
    </el-form-item>

    <el-form-item label="实际投放量">
      <el-input
        :value="formatQuantity(selectedRecord.actualDelivery)"
        style="width: 150px"
        readonly
        suffix-icon="el-icon-info"
      />
    </el-form-item>
  </div>
</template>

<script>
export default {
  name: 'QuantityDisplay',
  props: {
    selectedRecord: {
      type: Object,
      default: null
    }
  },
  methods: {
    formatQuantity(value) {
      if (value === null || value === undefined || value === '') {
        return '未设置'
      }

      const numValue = parseFloat(value)
      if (!isNaN(numValue)) {
        return numValue.toFixed(2)
      }

      return value
    }
  }
}
</script>

<style scoped>
.quantity-display {
  display: flex;
  gap: 10px;
}
</style>
