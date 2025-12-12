<template>
  <div class="time-selector">
    <el-form-item label="年份" required>
      <el-select 
        v-model="localYear" 
        placeholder="选择或输入年份"
        style="width: 100%"
        filterable
        allow-create
        default-first-option
        :disabled="disabled"
      >
        <el-option 
          v-for="year in yearOptions" 
          :key="year" 
          :label="year" 
          :value="year"
        />
      </el-select>
    </el-form-item>
    
    <el-form-item label="月份" required>
      <el-select 
        v-model="localMonth" 
        placeholder="选择月份"
        style="width: 100%"
        :disabled="disabled"
      >
        <el-option 
          v-for="month in 12" 
          :key="month" 
          :label="`${month}月`" 
          :value="month"
        />
      </el-select>
    </el-form-item>
    
    <el-form-item label="周序号" required>
      <el-select 
        v-model="localWeekSeq" 
        placeholder="选择周序号"
        style="width: 100%"
        :disabled="disabled"
      >
        <el-option 
          v-for="week in weekOptions" 
          :key="week" 
          :label="`第${week}周`" 
          :value="week"
        />
      </el-select>
    </el-form-item>
  </div>
</template>

<script>
export default {
  name: 'TimeSelector',
  props: {
    year: {
      type: Number,
      default: null
    },
    month: {
      type: Number,
      default: null
    },
    weekSeq: {
      type: Number,
      default: null
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:year', 'update:month', 'update:weekSeq'],
  computed: {
    yearOptions() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear - 2; year <= currentYear + 2; year++) {
        years.push(year)
      }
      return years
    },
    weekOptions() {
      return [1, 2, 3, 4, 5]
    },
    localYear: {
      get() { return this.year },
      set(value) { this.$emit('update:year', value) }
    },
    localMonth: {
      get() { return this.month },
      set(value) { this.$emit('update:month', value) }
    },
    localWeekSeq: {
      get() { return this.weekSeq },
      set(value) { this.$emit('update:weekSeq', value) }
    }
  }
}
</script>

<style scoped>
.time-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
