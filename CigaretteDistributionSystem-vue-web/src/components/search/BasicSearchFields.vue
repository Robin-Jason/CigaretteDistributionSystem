<template>
  <div class="basic-search-fields">
    <el-form-item label="年份">
      <el-select
        v-model="searchForm.year"
        placeholder="选择或输入年份"
        style="width: 120px"
        clearable
        filterable
        allow-create
        default-first-option
      >
        <el-option 
          v-for="year in yearOptions" 
          :key="year" 
          :label="year" 
          :value="year"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="月份">
      <el-select
        v-model="searchForm.month"
        placeholder="请选择月份"
        style="width: 120px"
        clearable
      >
        <el-option 
          v-for="month in 12" 
          :key="month" 
          :label="`${month}月`" 
          :value="month" 
        />
      </el-select>
    </el-form-item>

    <el-form-item label="周序号">
      <el-input-number
        v-model="searchForm.week"
        placeholder="请输入周序号"
        style="width: 120px"
        :min="1"
        :max="5"
        clearable
      />
    </el-form-item>

    <el-form-item label="卷烟名称">
      <el-input
        v-model="searchForm.cigaretteName"
        :placeholder="cigaretteNamePlaceholder"
        style="width: 200px"
        clearable
        :disabled="!isDateComplete"
        @input="handleCigaretteNameInput"
        @change="handleCigaretteNameChange"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </el-form-item>
  </div>
</template>

<script>
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'BasicSearchFields',
  components: {
    Search
  },
  props: {
    searchForm: {
      type: Object,
      required: true
    },
    yearOptions: {
      type: Array,
      required: true
    },
    isDateComplete: {
      type: Boolean,
      default: false
    },
    cigaretteNamePlaceholder: {
      type: String,
      required: true
    },
    tableData: {
      type: Array,
      default: () => []
    }
  },
  emits: ['cigarette-name-matched'],
  methods: {
    handleCigaretteNameInput(value) {
      // 实时搜索时去除前后空格
      this.searchForm.cigaretteName = value.trim()
    },
    handleCigaretteNameChange() {
      // 检查是否已填充日期表单
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请先填充年份、月份和周序号，然后再搜索卷烟名称')
        return
      }

      // 当卷烟名称输入完成时，尝试匹配表格中的记录
      if (this.searchForm.cigaretteName && this.tableData.length > 0) {
        const matchedRecords = this.tableData.filter(record =>
          record.cigName &&
          record.cigName.includes(this.searchForm.cigaretteName) &&
          record.year === this.searchForm.year &&
          record.month === this.searchForm.month &&
          record.weekSeq === this.searchForm.week
        )

        if (matchedRecords.length > 0) {
          this.$emit('cigarette-name-matched', matchedRecords)
          ElMessage.success(`已自动选中匹配的卷烟：${matchedRecords[0].cigName}，共找到 ${matchedRecords.length} 个投放区域`)
        } else {
          ElMessage.info('未找到匹配的卷烟记录')
        }
      }
    }
  }
}
</script>

<style scoped>
.basic-search-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
