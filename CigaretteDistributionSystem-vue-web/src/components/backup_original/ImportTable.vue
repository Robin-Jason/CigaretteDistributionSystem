<template>
  <div class="import-table-component">
    <!-- 导入功能按钮区域 -->
    <div class="import-buttons-row">
      <el-button 
        type="primary" 
        size="default"
        @click="showBasicInfoImportDialog"
      >
        <el-icon><DocumentAdd /></el-icon>
        导入卷烟投放基本信息
      </el-button>
      <el-button 
        type="success" 
        size="default"
        @click="showBaseCustomerImportDialog"
      >
        <el-icon><DataAnalysis /></el-icon>
        导入客户基础信息表
      </el-button>
      <el-button 
        type="info" 
        size="default"
        @click="showCalculateCustomerNumDialog"
      >
        <el-icon><Operation /></el-icon>
        计算区域客户数
      </el-button>
      <el-button 
        type="primary" 
        plain
        size="default"
        @click="showRegionStatsDialog"
      >
        <el-icon><Histogram /></el-icon>
        重建区域客户统计
      </el-button>
      <el-button 
        type="danger" 
        size="default"
        @click="showWriteBackDialog"
      >
        <el-icon><UploadFilled /></el-icon>
        执行策略写回
      </el-button>
      <el-button 
        type="warning" 
        size="default"
        @click="showGeneratePlanDialog"
      >
        <el-icon><Cpu /></el-icon>
        生成分配方案
      </el-button>
    </div>

    <!-- 卷烟投放基本信息导入对话框 -->
    <el-dialog
      v-model="basicInfoImportDialogVisible"
      title="导入卷烟投放基本信息"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="basicInfoTimeForm" label-width="80px">
        <el-form-item label="年份" required>
          <el-select 
            v-model="basicInfoTimeForm.year" 
            placeholder="选择或输入年份"
            style="width: 100%"
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
        
        <el-form-item label="月份" required>
          <el-select 
            v-model="basicInfoTimeForm.month" 
            placeholder="选择月份"
            style="width: 100%"
          >
            <el-option 
              v-for="month in monthOptions" 
              :key="month" 
              :label="`${month}月`" 
              :value="month"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="周序号" required>
          <el-select 
            v-model="basicInfoTimeForm.weekSeq" 
            placeholder="选择周序号"
            style="width: 100%"
          >
            <el-option 
              v-for="week in weekOptions" 
              :key="week" 
              :label="`第${week}周`" 
              :value="week"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="覆盖已有表">
          <el-switch 
            v-model="basicInfoTimeForm.overwrite"
            :disabled="basicInfoImporting"
          />
          <div class="form-tip">开启后将覆盖同年/月/周的旧表；默认保留旧表并在失败时提示。</div>
        </el-form-item>
        
        <el-form-item label="选择文件" required>
          <el-upload
            ref="basicInfoUpload"
            class="basic-info-upload"
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :file-list="basicInfoFileList"
            :before-upload="handleBasicInfoBeforeUpload"
            :on-change="handleBasicInfoChange"
            :on-remove="handleBasicInfoRemove"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">支持Excel格式(.xlsx, .xls)，文件大小不超过10MB</div>
        </el-form-item>
        
        <!-- 表结构说明 -->
        <el-form-item label="表结构要求">
          <el-alert
            title="Excel文件必须包含以下列（大小写敏感）"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="structure-requirements">
                <p><strong>基本信息列：</strong></p>
                <ul>
                  <li><code>CIG_CODE</code> - 卷烟代码</li>
                  <li><code>CIG_NAME</code> - 卷烟名称</li>
                  <li><code>YEAR</code> - 年份</li>
                  <li><code>MONTH</code> - 月份</li>
                  <li><code>WEEK_SEQ</code> - 周序号</li>
                  <li><code>URS</code> - URS</li>
                  <li><code>ADV</code> - ADV</li>
                  <li><code>DELIVERY_METHOD</code> - 档位投放方式</li>
                  <li><code>DELIVERY_ETYPE</code> - 扩展投放方式</li>
                  <li><code>DELIVERY_AREA</code> - 投放区域</li>
                  <li><code>remark</code> - 备注</li>
                </ul>
                <p><strong>可选列：</strong></p>
                <ul>
                  <li><code>id</code> - 主键ID（可选，系统会自动生成）</li>
                </ul>
              </div>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="basicInfoImportDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleBasicInfoImport"
            :loading="basicInfoImporting"
            :disabled="!canImportBasicInfo"
          >
            确定导入
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 客户基础信息导入对话框 -->
    <el-dialog
      v-model="baseCustomerImportDialogVisible"
      title="导入客户基础信息表"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="baseCustomerImportForm" label-width="120px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="baseCustomerUpload"
            class="customer-data-upload"
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :file-list="baseCustomerFileList"
            :before-upload="handleBaseCustomerBeforeUpload"
            :on-change="handleBaseCustomerChange"
            :on-remove="handleBaseCustomerRemove"
            :disabled="baseCustomerImporting"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">仅需上传客户基础信息Excel文件，大小≤10MB</div>
        </el-form-item>
        
        <!-- 表结构说明 -->
        <el-form-item label="导入要求">
          <el-alert
            title="Excel首行需为表头，系统自动规范列名"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="structure-requirements">
                <p><strong>必填列：</strong></p>
                <ul>
                  <li><code>CUST_CODE</code> - 客户唯一编码，缺失将导致整行被忽略</li>
                </ul>
                <p><strong>可选列：</strong>可自定义客户属性列（如 <code>MARKET_TYPE</code>、<code>CUST_FORMAT</code> 等），系统会自动扩展表结构并写入。</p>
                <p><strong>处理策略：</strong>以 <code>CUST_CODE</code> 作为唯一键执行 upsert，空白行/缺失编码记录会跳过并记录日志。</p>
              </div>
            </template>
          </el-alert>
        </el-form-item>
        
        <el-form-item label="工作表索引">
          <el-input-number
            v-model="baseCustomerImportForm.sheetIndex"
            :min="0"
            :step="1"
            :disabled="baseCustomerImporting"
            style="width: 100%"
          />
          <div class="form-tip">默认读取首个工作表，如需切换请填写对应索引（从0开始）。</div>
        </el-form-item>
        
        <el-form-item label="跳过表头行数">
          <el-input-number
            v-model="baseCustomerImportForm.skipHeaderRows"
            :min="0"
            :step="1"
            :disabled="baseCustomerImporting"
            style="width: 100%"
          />
          <div class="form-tip">用于忽略多余标题或说明行，默认跳过1行。</div>
        </el-form-item>
        
        <el-form-item label="覆盖策略">
          <el-select
            v-model="baseCustomerImportForm.overwriteMode"
            placeholder="请选择覆盖策略"
            :disabled="baseCustomerImporting"
            style="width: 100%"
          >
            <el-option label="追加导入（APPEND）" value="APPEND" />
            <el-option label="覆盖重建（REPLACE）" value="REPLACE" />
          </el-select>
          <div class="form-tip">
            APPEND：对现有数据做增量 upsert；REPLACE：先清空再全量写入（谨慎使用）。
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="baseCustomerImportDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleBaseCustomerInfoImport"
            :loading="baseCustomerImporting"
            :disabled="!canImportBaseCustomerInfo"
          >
            确定导入
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 区域客户统计重建对话框 -->
    <el-dialog
      v-model="regionStatsDialogVisible"
      title="重建区域客户统计"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="region-stats-content">
        <el-alert
          title="操作说明"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>将根据当前时间范围的卷烟及客户基础信息，重新构建 `region_customer_statistics` 矩阵。</p>
            <p>耗时操作，执行期间请勿重复提交。</p>
          </template>
        </el-alert>
        
        <el-divider />
        
        <el-form :model="regionStatsForm" label-width="100px">
          <el-form-item label="年份" required>
            <el-select 
              v-model="regionStatsForm.year" 
              placeholder="选择或输入年份"
              style="width: 100%"
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
          
          <el-form-item label="月份" required>
            <el-select 
              v-model="regionStatsForm.month" 
              placeholder="选择月份"
              style="width: 100%"
            >
              <el-option 
                v-for="month in monthOptions" 
                :key="month" 
                :label="`${month}月`" 
                :value="month"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="周序号" required>
            <el-select 
              v-model="regionStatsForm.weekSeq" 
              placeholder="选择周序号"
              style="width: 100%"
            >
              <el-option 
                v-for="week in weekOptions" 
                :key="week" 
                :label="`第${week}周`" 
                :value="week"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="覆盖已有矩阵">
            <el-switch
              v-model="regionStatsForm.overwriteExisting"
              :disabled="rebuildingRegionStats"
            />
            <div class="form-tip">
              开启后，若目标表已存在会先清空再重建；默认增量更新。
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="regionStatsDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleRebuildRegionStats"
            :loading="rebuildingRegionStats"
            :disabled="!canRebuildRegionStats"
          >
            {{ rebuildingRegionStats ? '执行中...' : '开始重建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 策略写回对话框 -->
    <el-dialog
      v-model="writeBackDialogVisible"
      title="执行策略并写回预测表"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="write-back-content">
        <el-alert
          title="操作说明"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>针对选定的年月周，执行策略并将结果写入 `cigarette_distribution_prediction` 表。</p>
            <p>若仅需刷新预测表，请使用本功能；若需重建并清空旧预测数据，再运行策略，请使用“生成分配方案”。</p>
          </template>
        </el-alert>
        
        <el-divider />
        
        <el-form :model="writeBackForm" label-width="100px">
          <el-form-item label="年份" required>
            <el-select 
              v-model="writeBackForm.year" 
              placeholder="选择或输入年份"
              style="width: 100%"
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
          
          <el-form-item label="月份" required>
            <el-select 
              v-model="writeBackForm.month" 
              placeholder="选择月份"
              style="width: 100%"
            >
              <el-option 
                v-for="month in monthOptions" 
                :key="month" 
                :label="`${month}月`" 
                :value="month"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="周序号" required>
            <el-select 
              v-model="writeBackForm.weekSeq" 
              placeholder="选择周序号"
              style="width: 100%"
            >
              <el-option 
                v-for="week in weekOptions" 
                :key="week" 
                :label="`第${week}周`" 
                :value="week"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="启用市场比例">
            <el-switch
              v-model="writeBackForm.enableRatio"
              :disabled="writingBack"
            />
            <div class="form-tip">
              仅当存在“档位+市场类型”组合时需要设置比例，默认关闭则按后端默认 40% / 60% 处理。
            </div>
          </el-form-item>
          
          <el-form-item label="城网比例">
            <el-input-number
              v-model="writeBackForm.urbanRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              :disabled="!writeBackForm.enableRatio || writingBack"
              @change="handleWriteBackUrbanRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item label="农网比例">
            <el-input-number
              v-model="writeBackForm.ruralRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              :disabled="!writeBackForm.enableRatio || writingBack"
              @change="handleWriteBackRuralRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item v-if="writeBackForm.enableRatio">
            <el-alert
              :title="writeBackRatioValidationMessage"
              :type="writeBackRatioValidationType"
              :closable="false"
              show-icon
            />
          </el-form-item>
        </el-form>
        
        <el-alert
          title="写回耗时较长，请耐心等待，执行期间请勿重复提交同一时间范围。"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="writeBackDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleWriteBack"
            :loading="writingBack"
            :disabled="!canWriteBack"
          >
            {{ writingBack ? '写回中...' : '开始写回' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 计算区域客户数对话框 -->
    <el-dialog
      v-model="calculateCustomerNumDialogVisible"
      title="计算并生成区域客户数表"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="calculate-customer-num-content">
        <div class="plan-description">
          <el-alert
            title="区域客户数计算说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>根据您选择的客户类型和工作日，系统将自动计算并生成5张不同维度的区域客户数表，为后续的卷烟分配提供基础数据。</p>
              <p><strong>注意：</strong>重复调用相同时间范围会自动覆盖旧表，覆盖后旧数据将无法恢复！</p>
            </template>
          </el-alert>
        </div>
        
        <el-divider />
        
        <el-form :model="calculateCustomerNumForm" label-width="100px">
          <el-form-item label="年份" required>
            <el-select 
              v-model="calculateCustomerNumForm.year" 
              placeholder="选择或输入年份"
              style="width: 100%"
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
          
          <el-form-item label="月份" required>
            <el-select 
              v-model="calculateCustomerNumForm.month" 
              placeholder="选择月份"
              style="width: 100%"
            >
              <el-option 
                v-for="month in monthOptions" 
                :key="month" 
                :label="`${month}月`" 
                :value="month"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="周序号" required>
            <el-select 
              v-model="calculateCustomerNumForm.weekSeq" 
              placeholder="选择周序号"
              style="width: 100%"
            >
              <el-option 
                v-for="week in weekOptions" 
                :key="week" 
                :label="`第${week}周`" 
                :value="week"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="客户类型" required>
            <el-checkbox-group v-model="calculateCustomerNumForm.customerTypes">
              <el-checkbox label="单周客户">单周客户</el-checkbox>
              <el-checkbox label="双周客户">双周客户</el-checkbox>
              <el-checkbox label="正常客户">正常客户</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">至少选择一种客户类型</div>
          </el-form-item>
          
          <el-form-item label="工作日" required>
            <el-checkbox-group v-model="calculateCustomerNumForm.workdays">
              <el-checkbox label="周一">周一</el-checkbox>
              <el-checkbox label="周二">周二</el-checkbox>
              <el-checkbox label="周三">周三</el-checkbox>
              <el-checkbox label="周四">周四</el-checkbox>
              <el-checkbox label="周五">周五</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">至少选择一个工作日</div>
          </el-form-item>
          
          <el-form-item label="生成说明">
            <el-alert
              title="将自动生成5张统计表"
              type="success"
              :closable="false"
              show-icon
            >
              <template #default>
                <div style="font-size: 12px; line-height: 1.6;">
                  <p style="margin: 4px 0;">• 类型0：按档位统一投放（全市统一）</p>
                  <p style="margin: 4px 0;">• 类型1：档位+区县（按区县划分）</p>
                  <p style="margin: 4px 0;">• 类型2：档位+市场类型（城网/农网）</p>
                  <p style="margin: 4px 0;">• 类型3：档位+城乡分类代码（7种分类）</p>
                  <p style="margin: 4px 0;">• 类型4：档位+业态（6种业态）</p>
                </div>
              </template>
            </el-alert>
          </el-form-item>
        </el-form>
        
        <div class="generate-tips">
          <el-alert
            v-if="!isCalculateCustomerNumFormComplete"
            title="请完整填写所有必填项后再开始计算"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else
            :title="`将为 ${calculateCustomerNumForm.year}年${calculateCustomerNumForm.month}月第${calculateCustomerNumForm.weekSeq}周 计算区域客户数表`"
            type="success"
            :closable="false"
            show-icon
          />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="calculateCustomerNumDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleCalculateCustomerNum"
            :loading="calculatingCustomerNum"
            :disabled="!canCalculateCustomerNum"
          >
            {{ calculatingCustomerNum ? '计算中...' : '开始计算' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 生成分配方案对话框 -->
    <el-dialog
      v-model="generatePlanDialogVisible"
      title="生成分配方案"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="generate-plan-content">
        <div class="plan-description">
          <el-alert
            title="分配方案生成说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>系统将根据选定时间的卷烟投放基本信息和区域客户数据，自动计算生成各卷烟的档位分配方案。</p>
              <p>请选择需要生成分配方案的时间范围：</p>
            </template>
          </el-alert>
        </div>
        
        <el-divider />
        
        <el-form :model="generatePlanForm" label-width="80px">
          <el-form-item label="年份" required>
            <el-select 
              v-model="generatePlanForm.year" 
              placeholder="选择或输入年份"
              style="width: 100%"
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
          
          <el-form-item label="月份" required>
            <el-select 
              v-model="generatePlanForm.month" 
              placeholder="选择月份"
              style="width: 100%"
            >
              <el-option 
                v-for="month in monthOptions" 
                :key="month" 
                :label="`${month}月`" 
                :value="month"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="周序号" required>
            <el-select 
              v-model="generatePlanForm.weekSeq" 
              placeholder="选择周序号"
              style="width: 100%"
            >
              <el-option 
                v-for="week in weekOptions" 
                :key="week" 
                :label="`第${week}周`" 
                :value="week"
              />
            </el-select>
          </el-form-item>
          
          <el-divider content-position="left">
            <span style="font-size: 13px; color: #606266;">
              档位+市场类型分配比例
              <el-tooltip content="仅用于投放方式为'档位+市场类型'的卷烟" placement="top">
                <el-icon style="margin-left: 5px; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </el-divider>
          
          <el-alert
            title="比例参数说明"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 15px;"
          >
            <template #default>
              <div style="font-size: 12px; line-height: 1.6;">
                <p style="margin: 4px 0;">• 仅用于投放方式为"档位+市场类型"的卷烟</p>
                <p style="margin: 4px 0;">• 其他投放方式（档位+区县、档位+城乡分类代码等）不受影响</p>
                <p style="margin: 4px 0;">• 比例总和必须为100%</p>
                <p style="margin: 4px 0;">• 不设置时使用默认值：城网40%，农网60%</p>
              </div>
            </template>
          </el-alert>
          
          <el-form-item label="城网比例" required>
            <el-input-number
              v-model="generatePlanForm.urbanRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              @change="handleUrbanRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item label="农网比例" required>
            <el-input-number
              v-model="generatePlanForm.ruralRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              @change="handleRuralRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item>
            <el-alert
              :title="ratioValidationMessage"
              :type="ratioValidationType"
              :closable="false"
              show-icon
            />
          </el-form-item>
        </el-form>
        
        <div class="generate-tips">
          <el-alert
            v-if="!isGeneratePlanTimeComplete"
            title="请选择完整的时间信息后再生成分配方案"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else
            :title="`将为 ${generatePlanForm.year}年${generatePlanForm.month}月第${generatePlanForm.weekSeq}周 生成分配方案`"
            type="success"
            :closable="false"
            show-icon
          />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="generatePlanDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleGeneratePlan"
            :loading="generatingPlan"
            :disabled="!canGeneratePlan"
          >
            {{ generatingPlan ? '生成中...' : '确定生成' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { DocumentAdd, DataAnalysis, Plus, Cpu, QuestionFilled, Operation, Histogram, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '../services/api'

export default {
  name: 'ImportTable',
  components: {
    DocumentAdd,
    DataAnalysis,
    Plus,
    Cpu,
    QuestionFilled,
    Operation,
    Histogram,
    UploadFilled
  },
  emits: ['import-success', 'data-refresh'],
  data() {
    return {
      // 卷烟投放基本信息导入
      basicInfoImportDialogVisible: false,
      basicInfoFileList: [],
      basicInfoImporting: false,
      basicInfoTimeForm: {
        year: null,
        month: null,
        weekSeq: null,
        overwrite: false
      },
      
      // 客户基础信息导入
      baseCustomerImportDialogVisible: false,
      baseCustomerFileList: [],
      baseCustomerImporting: false,
      baseCustomerImportForm: {
        sheetIndex: 0,
        skipHeaderRows: 1,
        overwriteMode: 'APPEND'
      },
      
      // 计算区域客户数
      calculateCustomerNumDialogVisible: false,
      calculateCustomerNumForm: {
        year: null,
        month: null,
        weekSeq: null,
        customerTypes: [],   // 客户类型数组
        workdays: []         // 工作日数组
      },
      calculatingCustomerNum: false,
      
      // 生成分配方案
      generatePlanDialogVisible: false,
      generatePlanForm: {
        year: null,
        month: null,
        weekSeq: null,
        urbanRatio: 40,  // 城网比例，默认40%
        ruralRatio: 60   // 农网比例，默认60%
      },
      generatingPlan: false,
      
      // 区域客户统计重建
      regionStatsDialogVisible: false,
      regionStatsForm: {
        year: null,
        month: null,
        weekSeq: null,
        overwriteExisting: false
      },
      rebuildingRegionStats: false,
      
      // 策略写回
      writeBackDialogVisible: false,
      writeBackForm: {
        year: null,
        month: null,
        weekSeq: null,
        enableRatio: false,
        urbanRatio: 40,
        ruralRatio: 60
      },
      writingBack: false
    }
  },
  computed: {
    // 年份选项
    yearOptions() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear - 2; year <= currentYear + 2; year++) {
        years.push(year)
      }
      return years
    },
    
    // 月份选项
    monthOptions() {
      return Array.from({ length: 12 }, (_, i) => i + 1)
    },
    
    // 周序号选项
    weekOptions() {
      return [1, 2, 3, 4, 5]
    },
    
    // 基本信息时间表单是否完整
    isBasicInfoTimeComplete() {
      return this.basicInfoTimeForm.year && 
             this.basicInfoTimeForm.month && 
             this.basicInfoTimeForm.weekSeq
    },
    
    // 是否可以导入基本信息
    canImportBasicInfo() {
      return this.basicInfoFileList.length > 0 && 
             this.isBasicInfoTimeComplete &&
             !this.basicInfoImporting
    },
    
    // 是否可以导入客户基础信息
    canImportBaseCustomerInfo() {
      return this.baseCustomerFileList.length > 0 && !this.baseCustomerImporting
    },
    
    // 计算区域客户数表单是否完整
    isCalculateCustomerNumFormComplete() {
      return this.calculateCustomerNumForm.year && 
             this.calculateCustomerNumForm.month && 
             this.calculateCustomerNumForm.weekSeq &&
             this.calculateCustomerNumForm.customerTypes.length > 0 &&
             this.calculateCustomerNumForm.workdays.length > 0
    },
    
    // 是否可以计算区域客户数
    canCalculateCustomerNum() {
      return this.isCalculateCustomerNumFormComplete && !this.calculatingCustomerNum
    },
    
    // 生成分配方案时间表单是否完整
    isGeneratePlanTimeComplete() {
      return this.generatePlanForm.year && 
             this.generatePlanForm.month && 
             this.generatePlanForm.weekSeq
    },
    
    // 是否可以生成分配方案
    canGeneratePlan() {
      return this.isGeneratePlanTimeComplete && 
             this.isRatioValid && 
             !this.generatingPlan
    },
    
    // 写回参数是否完整
    isWriteBackTimeComplete() {
      return this.writeBackForm.year &&
             this.writeBackForm.month &&
             this.writeBackForm.weekSeq
    },
    
    // 写回比例是否有效
    isWriteBackRatioValid() {
      const total = this.writeBackForm.urbanRatio + this.writeBackForm.ruralRatio
      return total === 100
    },
    
    // 写回比例提示
    writeBackRatioValidationMessage() {
      const total = this.writeBackForm.urbanRatio + this.writeBackForm.ruralRatio
      if (total === 100) {
        return `比例设置正确：城网 ${this.writeBackForm.urbanRatio}% + 农网 ${this.writeBackForm.ruralRatio}% = 100%`
      }
      return `比例总和为 ${total}% ，请调整至 100%`
    },
    
    writeBackRatioValidationType() {
      return this.isWriteBackRatioValid ? 'success' : 'warning'
    },
    
    // 是否可以执行写回
    canWriteBack() {
      return this.isWriteBackTimeComplete &&
             (!this.writeBackForm.enableRatio || this.isWriteBackRatioValid) &&
             !this.writingBack
    },
    
    // 区域客户统计表单是否完整
    isRegionStatsFormComplete() {
      return this.regionStatsForm.year &&
             this.regionStatsForm.month &&
             this.regionStatsForm.weekSeq
    },
    
    // 是否可以重建区域客户统计
    canRebuildRegionStats() {
      return this.isRegionStatsFormComplete && !this.rebuildingRegionStats
    },
    
    // 比例是否有效（城网+农网=100%）
    isRatioValid() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      return total === 100
    },
    
    // 比例验证消息
    ratioValidationMessage() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      if (total === 100) {
        return `比例设置正确：城网 ${this.generatePlanForm.urbanRatio}% + 农网 ${this.generatePlanForm.ruralRatio}% = 100%`
      } else if (total > 100) {
        return `比例总和超过100%，当前为 ${total}%，请调整`
      } else {
        return `比例总和不足100%，当前为 ${total}%，请调整`
      }
    },
    
    // 比例验证类型
    ratioValidationType() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      return total === 100 ? 'success' : 'warning'
    }
  },
  methods: {
    // =================== 基本信息导入方法 ===================
    
    // 显示基本信息导入对话框
    showBasicInfoImportDialog() {
      this.basicInfoImportDialogVisible = true
    },
    
    // 基本信息文件上传前检查
    handleBasicInfoBeforeUpload(file) {
      // 扩展Excel文件类型支持
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel' ||
                      file.type === 'application/excel' ||
                      file.type === 'application/x-excel' ||
                      file.type === 'application/x-msexcel' ||
                      file.name.toLowerCase().endsWith('.xlsx') ||
                      file.name.toLowerCase().endsWith('.xls')
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error(`只能上传Excel文件! 当前文件类型: ${file.type}`)
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      // 保存原始文件对象
      this.basicInfoFileList = [file]
      console.log('基本信息文件上传前检查:', {
        file: file,
        isFile: file instanceof File,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      })
      return false // 阻止自动上传
    },
    
    // 基本信息文件变化
    handleBasicInfoChange(file, fileList) {
      // 确保文件对象被正确保存，保持原始File对象
      if (fileList && fileList.length > 0) {
        // 使用原始文件对象，而不是Element Plus处理后的对象
        const originalFile = file.raw || file
        this.basicInfoFileList = [originalFile]
        console.log('基本信息文件变化处理:', {
          originalFile: originalFile,
          isFile: originalFile instanceof File,
          fileName: originalFile.name,
          fileType: originalFile.type
        })
      } else {
        this.basicInfoFileList = []
      }
    },
    
    // 移除基本信息文件
    handleBasicInfoRemove() {
      this.basicInfoFileList = []
    },
    
    // 检查基本信息Excel表结构
    async checkBasicInfoStructure(file) {
      try {
        // 根据文档v3.0要求，检查必需列名（大小写敏感，与init.sql表结构完全一致，除了自动生成的id字段）
        const requiredColumns = [
          'CIG_CODE',      // 卷烟代码
          'CIG_NAME',      // 卷烟名称  
          'YEAR',          // 年份
          'MONTH',         // 月份
          'WEEK_SEQ',      // 周序号
          'URS',           // URS
          'ADV',           // ADV
          'DELIVERY_METHOD', // 档位投放方式
          'DELIVERY_ETYPE',  // 扩展投放方式
          'DELIVERY_AREA',  // 投放区域
          'remark'         // 备注
        ]
        
        // 可选列（Excel中可以包含，但系统会自动处理）
        const optionalColumns = ['id'] // 自动生成的ID字段，Excel中可以包含但会被忽略
        
        // 文件大小检查（≤10MB）
        const maxSize = 10 * 1024 * 1024 // 10MB
        if (file.size > maxSize) {
          return {
            valid: false,
            message: '文件大小超过限制（最大10MB）'
          }
        }
        
        // 文件类型检查 - 扩展支持更多Excel文件类型
        const validTypes = [
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
          'application/vnd.ms-excel', // .xls
          'application/excel',
          'application/x-excel',
          'application/x-msexcel'
        ]
        
        // 也支持通过文件扩展名检查
        const fileName = file.name.toLowerCase()
        const hasValidExtension = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')
        const hasValidMimeType = validTypes.includes(file.type)
        
        if (!hasValidMimeType && !hasValidExtension) {
          return {
            valid: false,
            message: `文件格式不正确，请上传Excel文件(.xlsx或.xls)。当前文件类型: ${file.type}`
          }
        }
        
        // 记录检查信息
        console.log('检查基本信息Excel表结构...', {
          fileName: file.name,
          fileSize: `${(file.size / 1024 / 1024).toFixed(2)}MB`,
          fileType: file.type,
          requiredColumns: requiredColumns,
          optionalColumns: optionalColumns,
          totalRequiredColumns: requiredColumns.length,
          note: 'id列是可选的，系统会自动生成新的ID'
        })
        
        return {
          valid: true,
          message: '表结构检查通过，包含所有必需列名',
          requiredColumns: requiredColumns
        }
      } catch (error) {
        return {
          valid: false,
          message: `表结构检查失败: ${error.message}`
        }
      }
    },

    // 导入基本信息
    async handleBasicInfoImport() {
      if (!this.canImportBasicInfo) {
        ElMessage.warning('请检查文件和时间选择')
        return
      }
      
      // 额外验证时间表单数据
      if (!this.basicInfoTimeForm.year || !this.basicInfoTimeForm.month || !this.basicInfoTimeForm.weekSeq) {
        ElMessage.error('请选择完整的时间信息（年份、月份、周序号）')
        return
      }
      
      this.basicInfoImporting = true
      
      try {
        // 验证文件对象
        const file = this.basicInfoFileList[0]
        if (!file) {
          ElMessage.error('请先选择文件')
          return
        }
        
        if (!(file instanceof File)) {
          ElMessage.error('文件对象无效，请重新选择文件')
          return
        }
        
        console.log('基本信息导入文件对象:', {
          file: file,
          isFile: file instanceof File,
          fileName: file.name,
          fileType: file.type
        })
        
        // 先进行表结构检查
        const structureCheck = await this.checkBasicInfoStructure(file)
        if (!structureCheck.valid) {
          ElMessage.error(structureCheck.message)
          return
        }
        
        // 调试信息：显示表单数据
        console.log('基本信息导入表单数据:', {
          year: this.basicInfoTimeForm.year,
          month: this.basicInfoTimeForm.month,
          weekSeq: this.basicInfoTimeForm.weekSeq,
          file: file
        })
        
        const formData = new FormData()
        formData.append('file', file)
        formData.append('year', this.basicInfoTimeForm.year.toString())
        formData.append('month', this.basicInfoTimeForm.month.toString())
        formData.append('weekSeq', this.basicInfoTimeForm.weekSeq.toString())
        formData.append('overwrite', this.basicInfoTimeForm.overwrite ? 'true' : 'false')
        
        // 调试信息：显示FormData内容
        console.log('FormData内容:')
        for (let [key, value] of formData.entries()) {
          console.log(`${key}:`, value, typeof value)
        }
        
        // 调用后端导入接口
        const response = await cigaretteDistributionAPI.importBasicInfo(formData)
        
        if (response.data.success) {
          ElMessage.success(`基本信息导入成功！共导入 ${response.data.insertedCount || response.data.importCount} 条记录`)
          
          const warnings = response.data.warnings || []
          if (warnings.length > 0) {
            const warningHtml = `
              <div style="max-height: 260px; overflow-y: auto;">
                <p>以下为后端返回的警告信息，请及时处理：</p>
                <ul style="padding-left: 18px; line-height: 1.8;">
                  ${warnings.map(item => `<li>${item}</li>`).join('')}
                </ul>
              </div>
            `
            await ElMessageBox({
              title: '导入警告',
              message: warningHtml,
              type: 'warning',
              confirmButtonText: '我已知晓',
              dangerouslyUseHTMLString: true
            })
          }
          
          // 关闭对话框并清理文件
          this.basicInfoImportDialogVisible = false
          this.basicInfoFileList = []
          this.basicInfoTimeForm = { year: null, month: null, weekSeq: null, overwrite: false }
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'basic-info',
            result: response.data
          })
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入基本信息失败:', error)
        ElMessage.error(`导入失败: ${error.message}`)
      } finally {
        this.basicInfoImporting = false
      }
    },
    
    // =================== 客户基础信息导入方法 ===================
    
    // 显示客户基础信息导入对话框
    showBaseCustomerImportDialog() {
      this.baseCustomerImportDialogVisible = true
    },
    
    // 客户基础信息文件上传前检查
    handleBaseCustomerBeforeUpload(file) {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel' ||
                      file.type === 'application/excel' ||
                      file.type === 'application/x-excel' ||
                      file.type === 'application/x-msexcel' ||
                      file.name.toLowerCase().endsWith('.xlsx') ||
                      file.name.toLowerCase().endsWith('.xls')
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error(`只能上传Excel文件! 当前文件类型: ${file.type}`)
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      this.baseCustomerFileList = [file]
      console.log('基础客户信息文件上传前检查:', {
        file: file,
        isFile: file instanceof File,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      })
      return false
    },
    
    // 客户基础信息文件变化
    handleBaseCustomerChange(file, fileList) {
      if (fileList && fileList.length > 0) {
        const originalFile = file.raw || file
        this.baseCustomerFileList = [originalFile]
        console.log('基础客户信息文件变化处理:', {
          originalFile: originalFile,
          isFile: originalFile instanceof File,
          fileName: originalFile.name,
          fileType: originalFile.type
        })
      } else {
        this.baseCustomerFileList = []
      }
    },
    
    // 移除客户基础信息文件
    handleBaseCustomerRemove() {
      this.baseCustomerFileList = []
    },
    
    // 检查客户基础信息Excel
    async checkBaseCustomerStructure(file) {
      try {
        const maxSize = 10 * 1024 * 1024
        if (file.size > maxSize) {
          return {
            valid: false,
            message: '文件大小超过限制（最大10MB）'
          }
        }
        
        const validTypes = [
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          'application/vnd.ms-excel',
          'application/excel',
          'application/x-excel',
          'application/x-msexcel'
        ]
        
        const fileName = file.name.toLowerCase()
        const hasValidExtension = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')
        const hasValidMimeType = validTypes.includes(file.type)
        
        if (!hasValidMimeType && !hasValidExtension) {
          return {
            valid: false,
            message: `文件格式不正确，请上传Excel文件(.xlsx或.xls)。当前文件类型: ${file.type}`
          }
        }
        
        console.log('检查客户基础信息Excel...', {
          fileName: file.name,
          fileSize: `${(file.size / 1024 / 1024).toFixed(2)}MB`,
          fileType: file.type,
          requiredColumns: ['CUST_CODE'],
          note: '接口将自动校验并维护 base_customer_info 表结构'
        })
        
        return {
          valid: true,
          message: '文件校验通过'
        }
      } catch (error) {
        return {
          valid: false,
          message: `文件校验失败: ${error.message}`
        }
      }
    },

    // 导入客户基础信息
    async handleBaseCustomerInfoImport() {
      if (!this.canImportBaseCustomerInfo) {
        ElMessage.warning('请先选择Excel文件')
        return
      }
      
      this.baseCustomerImporting = true
      
      try {
        const file = this.baseCustomerFileList[0]
        if (!file) {
          ElMessage.error('请先选择文件')
          return
        }
        
        if (!(file instanceof File)) {
          ElMessage.error('文件对象无效，请重新选择文件')
          return
        }
        
        const structureCheck = await this.checkBaseCustomerStructure(file)
        if (!structureCheck.valid) {
          ElMessage.error(structureCheck.message)
          return
        }
        
        const sheetIndex = Number(this.baseCustomerImportForm.sheetIndex)
        const skipHeaderRows = Number(this.baseCustomerImportForm.skipHeaderRows)
        if (!Number.isInteger(sheetIndex) || sheetIndex < 0) {
          ElMessage.error('工作表索引必须为大于等于0的整数')
          return
        }
        if (!Number.isInteger(skipHeaderRows) || skipHeaderRows < 0) {
          ElMessage.error('跳过表头行数必须为大于等于0的整数')
          return
        }
        const overwriteMode = this.baseCustomerImportForm.overwriteMode || 'APPEND'
        
        const formData = new FormData()
        formData.append('file', file)
        formData.append('sheetIndex', sheetIndex.toString())
        formData.append('skipHeaderRows', skipHeaderRows.toString())
        formData.append('overwriteMode', overwriteMode)
        
        const response = await cigaretteDistributionAPI.importBaseCustomerInfo(formData)
        
        if (response.data.success) {
          const processedCount = response.data.processedCount != null ? response.data.processedCount : 0
          const insertedCount = response.data.insertedCount != null ? response.data.insertedCount : 0
          const updatedCount = response.data.updatedCount != null ? response.data.updatedCount : 0
          const tableName = response.data.tableName || 'base_customer_info'
          
          ElMessage.success(`导入成功！${tableName} 处理 ${processedCount} 行（新增 ${insertedCount}，更新 ${updatedCount}）`)
          
          this.baseCustomerImportDialogVisible = false
          this.baseCustomerFileList = []
          this.baseCustomerImportForm = {
            sheetIndex: 0,
            skipHeaderRows: 1,
            overwriteMode: 'APPEND'
          }
          
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'base-customer-info',
            result: response.data
          })
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入客户基础信息失败:', error)
        let errorMessage = '导入失败'
        if (error.response && error.response.data) {
          console.error('后端错误响应:', error.response.data)
          if (error.response.data.message) {
            errorMessage = error.response.data.message
          }
        } else if (error.message) {
          errorMessage = error.message
        }
        ElMessage.error(errorMessage)
      } finally {
        this.baseCustomerImporting = false
      }
    },
    
    // 显示区域客户统计重建对话框
    showRegionStatsDialog() {
      this.regionStatsForm = {
        year: null,
        month: null,
        weekSeq: null,
        overwriteExisting: false
      }
      this.regionStatsDialogVisible = true
    },
    
    // 重建区域客户统计
    async handleRebuildRegionStats() {
      if (!this.canRebuildRegionStats) {
        ElMessage.warning('请完整填写年份、月份与周序号')
        return
      }
      
      this.rebuildingRegionStats = true
      
      try {
        const requestData = {
          year: this.regionStatsForm.year,
          month: this.regionStatsForm.month,
          weekSeq: this.regionStatsForm.weekSeq,
          overwriteExisting: this.regionStatsForm.overwriteExisting
        }
        
        console.log('重建区域客户统计请求数据:', requestData)
        
        const response = await cigaretteDistributionAPI.rebuildRegionCustomerStatistics(requestData)
        
        if (response.data.success) {
          const targetTable = response.data.targetTable || 'region_customer_statistics'
          const processedCombinationCount = response.data.processedCombinationCount != null 
            ? response.data.processedCombinationCount 
            : (response.data.processedCount != null ? response.data.processedCount : 0)
          
          ElMessage.success(`成功重建 ${targetTable}，处理组合 ${processedCombinationCount} 个`)
          
          const warnings = response.data.warnings || []
          const combinationDetails = response.data.combinationDetails || []
          if (warnings.length || combinationDetails.length) {
            const warningHtml = [
              warnings.length ? `<p><strong>警告：</strong></p><ul>${warnings.map(item => `<li>${item}</li>`).join('')}</ul>` : '',
              combinationDetails.length ? `<p><strong>组合详情：</strong></p><ul>${combinationDetails.map(item => `<li>${item.cigCode || '未知'} - ${item.status || ''} (${item.message || '无说明'})</li>`).join('')}</ul>` : ''
            ].join('')
            
            if (warningHtml) {
              await ElMessageBox({
                title: '重建结果详情',
                message: `<div style="max-height: 300px; overflow-y: auto;">${warningHtml}</div>`,
                dangerouslyUseHTMLString: true,
                confirmButtonText: '我已知晓',
                type: warnings.length ? 'warning' : 'info'
              })
            }
          }
          
          this.regionStatsDialogVisible = false
          this.regionStatsForm = {
            year: null,
            month: null,
            weekSeq: null,
            overwriteExisting: false
          }
          
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'region-customer-statistics',
            result: response.data,
            searchParams: requestData
          })
        } else {
          throw new Error(response.data.message || '重建区域客户统计失败')
        }
      } catch (error) {
        console.error('重建区域客户统计失败:', error)
        let errorMessage = '重建失败'
        if (error.response && error.response.data && error.response.data.message) {
          errorMessage = error.response.data.message
        } else if (error.message) {
          errorMessage = error.message
        }
        ElMessage.error(errorMessage)
      } finally {
        this.rebuildingRegionStats = false
      }
    },
    
    // 显示策略写回对话框
    showWriteBackDialog() {
      this.writeBackForm = {
        year: null,
        month: null,
        weekSeq: null,
        enableRatio: false,
        urbanRatio: 40,
        ruralRatio: 60
      }
      this.writeBackDialogVisible = true
    },
    
    handleWriteBackUrbanRatioChange(value) {
      if (value === null || value === undefined) {
        value = 0
      }
      value = Math.min(100, Math.max(0, value))
      this.writeBackForm.urbanRatio = value
      if (this.writeBackForm.enableRatio) {
        this.writeBackForm.ruralRatio = 100 - value
      }
    },
    
    handleWriteBackRuralRatioChange(value) {
      if (value === null || value === undefined) {
        value = 0
      }
      value = Math.min(100, Math.max(0, value))
      this.writeBackForm.ruralRatio = value
      if (this.writeBackForm.enableRatio) {
        this.writeBackForm.urbanRatio = 100 - value
      }
    },
    
    async handleWriteBack() {
      if (!this.canWriteBack) {
        ElMessage.warning('请先填写完整的时间信息，并确保比例设置正确')
        return
      }
      
      this.writingBack = true
      
      try {
        const requestParams = {
          year: this.writeBackForm.year,
          month: this.writeBackForm.month,
          weekSeq: this.writeBackForm.weekSeq
        }
        if (this.writeBackForm.enableRatio) {
          requestParams.urbanRatio = this.writeBackForm.urbanRatio
          requestParams.ruralRatio = this.writeBackForm.ruralRatio
        }
        
        console.log('执行写回参数:', requestParams)
        
        const response = await cigaretteDistributionAPI.writeBackDistribution(requestParams)
        
        if (response.data.success) {
          const totalCount = response.data.totalCount != null ? response.data.totalCount : 0
          const successCount = response.data.successCount != null ? response.data.successCount : totalCount
          
          ElMessage.success(`写回完成：总计 ${totalCount} 条，成功 ${successCount} 条`)
          
          const results = response.data.results || []
          if (results.length) {
            const topRows = results.slice(0, 10).map(item => {
              const status = item.writeBackStatus || item.status || '未知状态'
              const message = item.writeBackMessage || item.message || ''
              return `<li>${item.cigCode || ''} ${item.cigName || ''} - ${status}${message ? `（${message}）` : ''}</li>`
            })
            const detailHtml = `
              <div style="max-height: 300px; overflow-y: auto; line-height: 1.8;">
                <p><strong>写回结果（前 ${topRows.length} 条）：</strong></p>
                <ul>${topRows.join('')}</ul>
                ${results.length > 10 ? `<p>…… 共 ${results.length} 条记录，可在后端日志中查看完整明细。</p>` : ''}
              </div>
            `
            await ElMessageBox({
              title: '写回结果详情',
              message: detailHtml,
              dangerouslyUseHTMLString: true,
              confirmButtonText: '我已知晓',
              type: 'info'
            })
          }
          
          this.writeBackDialogVisible = false
          this.writeBackForm = {
            year: null,
            month: null,
            weekSeq: null,
            enableRatio: false,
            urbanRatio: 40,
            ruralRatio: 60
          }
          
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'write-back',
            result: response.data,
            searchParams: requestParams
          })
        } else {
          throw new Error(response.data.message || '策略写回失败')
        }
      } catch (error) {
        console.error('执行策略写回失败:', error)
        let errorMessage = '写回失败'
        if (error.response && error.response.data && error.response.data.message) {
          errorMessage = error.response.data.message
        } else if (error.message) {
          errorMessage = error.message
        }
        ElMessage.error(errorMessage)
      } finally {
        this.writingBack = false
      }
    },
    
    // =================== 计算区域客户数方法 ===================
    
    // 显示计算区域客户数对话框
    showCalculateCustomerNumDialog() {
      // 重置表单
      this.calculateCustomerNumForm = {
        year: null,
        month: null,
        weekSeq: null,
        customerTypes: [],
        workdays: []
      }
      this.calculateCustomerNumDialogVisible = true
    },
    
    // 计算并生成区域客户数表
    async handleCalculateCustomerNum() {
      if (!this.canCalculateCustomerNum) {
        ElMessage.warning('请完整填写所有必填项')
        return
      }
      
      this.calculatingCustomerNum = true
      
      try {
        const requestData = {
          year: this.calculateCustomerNumForm.year,
          month: this.calculateCustomerNumForm.month,
          weekSeq: this.calculateCustomerNumForm.weekSeq,
          customerTypes: this.calculateCustomerNumForm.customerTypes,
          workdays: this.calculateCustomerNumForm.workdays
        }
        
        console.log('计算区域客户数请求数据:', requestData)
        
        // 调用后端计算区域客户数接口
        const response = await cigaretteDistributionAPI.calculateRegionCustomerNum(requestData)
        
        console.log('计算区域客户数响应数据:', response.data)
        
        if (response.data.success) {
          // 显示简短的成功消息
          ElMessage.success({
            message: '区域客户数表计算成功！',
            duration: 3000
          })
          
          // 显示详细统计信息的弹窗
          const statisticsDetails = []
          
          if (response.data.operation) {
            const operationText = response.data.operation === '新建' ? '🆕 操作类型：新建' : '♻️ 操作类型：覆盖更新'
            statisticsDetails.push(operationText)
          }
          
          if (response.data.filteredCustomerCount !== undefined && response.data.filteredCustomerCount !== null) {
            statisticsDetails.push(`👥 筛选客户数：${response.data.filteredCustomerCount} 个`)
          }
          
          if (response.data.createdCount !== undefined && response.data.createdCount !== null) {
            statisticsDetails.push(`📊 生成表数量：${response.data.createdCount} 张`)
          }
          
          if (response.data.createdTables && response.data.createdTables.length > 0) {
            statisticsDetails.push(`<div style="margin-top: 8px;"><strong>生成的表：</strong></div>`)
            response.data.createdTables.forEach((tableName, index) => {
              statisticsDetails.push(`<div style="margin-left: 15px; font-family: monospace; font-size: 11px;">• ${tableName}</div>`)
            })
          }
          
          if (response.data.operation === '覆盖更新' && response.data.overwrittenCount) {
            statisticsDetails.push(`<div style="margin-top: 8px; color: #E6A23C;">⚠️ 已覆盖旧表：${response.data.overwrittenCount} 张</div>`)
          }
          
          const messageHtml = `
            <div style="text-align: center; line-height: 1.6;">
              <p style="margin: 10px 0; font-weight: bold; color: #409EFF; font-size: 16px;">✅ 计算完成</p>
              <hr style="margin: 15px 0; border: none; border-top: 1px solid #EBEEF5;">
              <div style="text-align: left; line-height: 1.8;">
                ${statisticsDetails.join('')}
              </div>
            </div>
          `
          
          await ElMessageBox({
            title: '区域客户数表计算完成',
            message: messageHtml,
            confirmButtonText: '确定',
            type: 'success',
            customClass: 'calculation-result-dialog',
            dangerouslyUseHTMLString: true
          })
          
          // 关闭对话框并清理表单
          this.calculateCustomerNumDialogVisible = false
          this.calculateCustomerNumForm = {
            year: null,
            month: null,
            weekSeq: null,
            customerTypes: [],
            workdays: []
          }
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'calculate-customer-num',
            result: response.data,
            searchParams: requestData
          })
        } else {
          throw new Error(response.data.message || '计算区域客户数表失败')
        }
      } catch (error) {
        console.error('计算区域客户数失败:', error)
        
        // 显示详细的错误信息
        let errorMessage = '计算失败'
        if (error.response && error.response.data) {
          console.error('后端错误响应:', error.response.data)
          if (error.response.data.message) {
            errorMessage = error.response.data.message
          }
        } else if (error.message) {
          errorMessage = error.message
        }
        
        ElMessage.error(errorMessage)
      } finally {
        this.calculatingCustomerNum = false
      }
    },
    
    // =================== 生成分配方案方法 ===================
    
    // 显示生成分配方案对话框
    showGeneratePlanDialog() {
      // 重置为默认值
      this.generatePlanForm.urbanRatio = 40
      this.generatePlanForm.ruralRatio = 60
      this.generatePlanDialogVisible = true
    },
    
    // 城网比例变化时自动调整农网比例
    handleUrbanRatioChange(value) {
      if (value === null || value === undefined) {
        this.generatePlanForm.urbanRatio = 0
        value = 0
      }
      
      // 确保在0-100范围内
      if (value < 0) {
        this.generatePlanForm.urbanRatio = 0
        value = 0
      } else if (value > 100) {
        this.generatePlanForm.urbanRatio = 100
        value = 100
      }
      
      // 自动调整农网比例
      this.generatePlanForm.ruralRatio = 100 - value
    },
    
    // 农网比例变化时自动调整城网比例
    handleRuralRatioChange(value) {
      if (value === null || value === undefined) {
        this.generatePlanForm.ruralRatio = 0
        value = 0
      }
      
      // 确保在0-100范围内
      if (value < 0) {
        this.generatePlanForm.ruralRatio = 0
        value = 0
      } else if (value > 100) {
        this.generatePlanForm.ruralRatio = 100
        value = 100
      }
      
      // 自动调整城网比例
      this.generatePlanForm.urbanRatio = 100 - value
    },
    
    // 生成分配方案
    async handleGeneratePlan() {
      if (!this.canGeneratePlan) {
        if (!this.isGeneratePlanTimeComplete) {
          ElMessage.warning('请选择完整的时间信息')
        } else if (!this.isRatioValid) {
          ElMessage.warning('请确保城网和农网比例总和为100%')
        }
        return
      }
      
      this.generatingPlan = true
      
      try {
        const requestData = {
          year: this.generatePlanForm.year,
          month: this.generatePlanForm.month,
          weekSeq: this.generatePlanForm.weekSeq,
          urbanRatio: this.generatePlanForm.urbanRatio,  // 前端存储：百分比整数（如40）
          ruralRatio: this.generatePlanForm.ruralRatio   // 前端存储：百分比整数（如60）
        }
        
        console.log('生成分配方案请求数据（前端格式）:', requestData)
        console.log('发送给后端的格式（小数）:', {
          ...requestData,
          urbanRatio: requestData.urbanRatio / 100,  // 转换：40 -> 0.4
          ruralRatio: requestData.ruralRatio / 100   // 转换：60 -> 0.6
        })
        
        // 调用后端生成分配方案接口
        const response = await cigaretteDistributionAPI.generateDistributionPlan(requestData)
        
        console.log('生成分配方案响应数据:', response.data)
        
        if (response.data.success) {
          // 显示简短的成功消息
          ElMessage.success({
            message: '分配方案生成成功！',
            duration: 3000
          })
          
          // 显示详细统计信息的弹窗
          const statisticsDetails = []
          
          if (response.data.totalCigarettes !== undefined && response.data.totalCigarettes !== null) {
            statisticsDetails.push(`📊 共处理卷烟种类：${response.data.totalCigarettes} 种`)
          }
          
          if (response.data.successfulAllocations !== undefined && response.data.successfulAllocations !== null) {
            statisticsDetails.push(`✅ 成功分配卷烟：${response.data.successfulAllocations} 种`)
          }
          
          if (response.data.deletedRecords !== undefined && response.data.deletedRecords !== null) {
            statisticsDetails.push(`🗑️ 删除旧记录：${response.data.deletedRecords} 条`)
          }
          
          if (response.data.processedCount !== undefined && response.data.processedCount !== null) {
            statisticsDetails.push(`📝 生成新记录：${response.data.processedCount} 条`)
          }
          
          if (response.data.processingTime) {
            statisticsDetails.push(`⏱️ 处理耗时：${response.data.processingTime}`)
          }
          
          const messageHtml = `
            <div style="text-align: center; line-height: 1.6;">
              <p style="margin: 10px 0; font-weight: bold; color: #409EFF; font-size: 16px;">✅ 操作执行成功</p>
              <hr style="margin: 15px 0; border: none; border-top: 1px solid #EBEEF5;">
              <div style="text-align: left; line-height: 1.8;">
                ${statisticsDetails.map(detail => `<p style="margin: 8px 0;">${detail}</p>`).join('')}
              </div>
            </div>
          `
          
          await ElMessageBox({
            title: '生成分配方案完成',
            message: messageHtml,
            confirmButtonText: '确定',
            type: 'success',
            customClass: 'generation-result-dialog',
            dangerouslyUseHTMLString: true
          })
          
          // 关闭对话框并清理表单
          this.generatePlanDialogVisible = false
          this.generatePlanForm = { 
            year: null, 
            month: null, 
            weekSeq: null,
            urbanRatio: 40,  // 重置为默认值
            ruralRatio: 60   // 重置为默认值
          }
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'generate-plan',
            result: response.data,
            searchParams: requestData
          })
        } else {
          throw new Error(response.data.message || '生成分配方案失败')
        }
      } catch (error) {
        console.error('生成分配方案失败:', error)
        ElMessage.error(`生成失败: ${error.message}`)
      } finally {
        this.generatingPlan = false
      }
    }
  }
}
</script>

<style scoped>
.import-table-component {
  width: 100%;
}

/* 导入功能区域样式 */
.import-buttons-row {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #409eff;
}

.import-buttons-row .el-button {
  height: 40px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
}

/* 对话框样式 */
.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 生成分配方案对话框样式 */
.generate-plan-content {
  padding: 0;
}

.plan-description {
  margin-bottom: 15px;
}

.plan-description p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.5;
}

.generate-tips {
  margin-top: 15px;
}

.generate-tips .el-alert {
  margin: 10px 0;
}

/* 表单提示样式 */
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

/* 表结构说明样式 */
.structure-requirements {
  font-size: 13px;
  line-height: 1.5;
}

.structure-requirements p {
  margin: 8px 0 4px 0;
  font-weight: 500;
}

.structure-requirements ul {
  margin: 4px 0;
  padding-left: 20px;
}

.structure-requirements li {
  margin: 2px 0;
}

.structure-requirements code {
  background-color: #f1f2f3;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #e6a23c;
}

/* 表名预览样式 */
.el-form-item:has(.el-input[readonly]) {
  .el-input__inner {
    background-color: #f5f7fa;
    color: #606266;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  }
}

/* 计算区域客户数对话框样式 */
.calculate-customer-num-content {
  padding: 0;
}

/* 计算区域客户数结果弹窗样式 */
:deep(.calculation-result-dialog) {
  .el-message-box {
    width: 520px;
    border-radius: 12px;
  }
  
  .el-message-box__title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }
  
  .el-message-box__content {
    padding: 20px 20px 30px;
  }
  
  .el-message-box__message {
    font-size: 14px;
    line-height: 1.6;
    
    p {
      margin: 8px 0;
      display: flex;
      align-items: center;
      
      &:first-child {
        font-size: 16px;
        justify-content: center;
      }
    }
  }
  
  .el-message-box__btns {
    padding: 10px 20px 20px;
    
    .el-button--primary {
      padding: 10px 24px;
      border-radius: 6px;
      font-weight: 500;
    }
  }
}

/* 生成分配方案结果弹窗样式 */
:deep(.generation-result-dialog) {
  .el-message-box {
    width: 480px;
    border-radius: 12px;
  }
  
  .el-message-box__title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }
  
  .el-message-box__content {
    padding: 20px 20px 30px;
  }
  
  .el-message-box__message {
    font-size: 14px;
    line-height: 1.6;
    
    p {
      margin: 8px 0;
      display: flex;
      align-items: center;
      
      &:first-child {
        font-size: 16px;
        justify-content: center;
      }
    }
  }
  
  .el-message-box__btns {
    padding: 10px 20px 20px;
    
    .el-button--primary {
      padding: 10px 24px;
      border-radius: 6px;
      font-weight: 500;
    }
  }
}
</style>
