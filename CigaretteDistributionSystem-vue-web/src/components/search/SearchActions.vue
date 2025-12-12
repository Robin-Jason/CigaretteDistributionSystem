<template>
  <div class="search-actions">
    <el-form-item>
      <el-button 
        type="primary" 
        @click="handleSearch"
        :disabled="!canSearch"
      >
        <el-icon><Search /></el-icon>
        查询
      </el-button>

      <el-button 
        type="info" 
        @click="handleSearchNext"
        :disabled="!canSearchNext"
      >
        <el-icon><ArrowDown /></el-icon>
        下一个
      </el-button>

      <el-button 
        type="warning" 
        @click="handleFilterDeviation"
        :disabled="!hasTableData"
        :loading="filteringDeviation"
      >
        <el-icon><Filter /></el-icon>
        筛选误差>200
        <span v-if="deviationCount > 0" style="margin-left: 5px;">
          ({{ currentIndex + 1 }}/{{ deviationCount }})
        </span>
      </el-button>

      <el-button @click="handleReset">
        <el-icon><RefreshLeft /></el-icon>
        重置
      </el-button>

      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </el-form-item>
  </div>
</template>

<script>
import { Search, RefreshLeft, Download, ArrowDown, Filter } from '@element-plus/icons-vue'

export default {
  name: 'SearchActions',
  components: {
    Search,
    RefreshLeft,
    Download,
    ArrowDown,
    Filter
  },
  props: {
    canSearch: {
      type: Boolean,
      required: true
    },
    canSearchNext: {
      type: Boolean,
      required: true
    },
    hasTableData: {
      type: Boolean,
      default: false
    },
    filteringDeviation: {
      type: Boolean,
      default: false
    },
    deviationCount: {
      type: Number,
      default: 0
    },
    currentIndex: {
      type: Number,
      default: -1
    }
  },
  emits: ['search', 'search-next', 'filter-deviation', 'reset', 'export'],
  methods: {
    handleSearch() {
      this.$emit('search')
    },
    handleSearchNext() {
      this.$emit('search-next')
    },
    handleFilterDeviation() {
      this.$emit('filter-deviation')
    },
    handleReset() {
      this.$emit('reset')
    },
    handleExport() {
      this.$emit('export')
    }
  }
}
</script>

<style scoped>
.search-actions {
  margin-top: 10px;
}
</style>
