## 客户基础信息导入接口

### 接口概览
- **URL**：`POST /api/import/base-customer-info`
- **方法**：`POST`
- **内容类型**：`multipart/form-data`
- **请求参数**：
  - `file`：Excel 文件（必填）。支持 `.xlsx` / `.xls`，大小 ≤ 10MB。

### 主要功能
1. **文件校验**  
   - 判空、校验文件类型与大小。  
2. **Excel 解析**  
   - 第一行作为表头，自动规范列名（转大写、去非法字符，必要时添加前缀）。  
   - 必须包含 `CUST_CODE` 列。缺失时直接返回错误。  
   - 允许添加额外客户属性列，将与数据一起写入数据库。  
3. **表结构维护**  
   - 目标表固定：`base_customer_info`。  
   - 若表不存在会自动按规范创建，包含需求文档中列并对 `CUST_CODE` 建唯一索引。  
   - Excel 出现但表中不存在的列将自动 `ALTER TABLE` 新增，字段类型默认为 `varchar(255)`。  
4. **数据写入策略**  
   - 以 `CUST_CODE` 为唯一键执行 upsert。  
   - 插入/更新时同步 Excel 中所有列（含新增的自定义字段）。  
   - 空白行或缺少 `CUST_CODE` 的记录会被忽略并写日志，不影响其他数据。  

### 响应格式
成功：
```json
{
  "success": true,
  "message": "导入成功",
  "tableName": "base_customer_info",
  "processedCount": 1200,
  "insertedCount": 300,
  "updatedCount": 900
}
```

失败（示例）：
```json
{
  "success": false,
  "message": "Excel文件中缺少必填列：CUST_CODE",
  "error": "IMPORT_FAILED"
}
```

### 前端接入建议
- 仅需上传 Excel 文件，无额外表单字段。  
- 建议表头直接使用业务字段名（如 `MARKET_TYPE`、`CUST_FORMAT` 等），可自动兼容大小写和空格。  
- 若需要携带额外客户标签，可在 Excel 中新增列，系统会自动扩展表结构并写入。  
- 成功响应中的 `insertedCount`、`updatedCount`、`processedCount` 可用于提示导入结果。  

