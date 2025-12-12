import axios from 'axios'

// 创建axios实例 - 基础配置
const createApiInstance = (baseURL) => {
  // 后端服务运行在28080端口
  const backendBaseUrl = process.env.VUE_APP_API_BASE_URL || `http://localhost:28080${baseURL}`
  const instance = axios.create({
    baseURL: backendBaseUrl,
    timeout: 10000
    // 不设置默认Content-Type，让每个请求自己决定
  })

  // 请求拦截器
  instance.interceptors.request.use(
    config => {
      console.log('发送请求:', config.method && config.method.toUpperCase(), config.url, config.data || config.params)
      return config
    },
    error => {
      console.error('请求错误:', error)
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  instance.interceptors.response.use(
    response => {
      console.log('响应数据:', response.data)
      return response
    },
    error => {
      console.error('响应错误:', error)
      return Promise.reject(error)
    }
  )

  return instance
}

// 创建各模块的API实例
const dataApi = createApiInstance('/api/data')        // 数据管理接口
const commonApi = createApiInstance('/api/common')    // 通用功能接口  
const calculateApi = createApiInstance('/api/calculate') // 分配计算接口
const importApi = createApiInstance('/api/import')    // 数据导入接口
const calRegionCustomerNumApi = createApiInstance('/api/cal-region-customer-num') // 区域客户数计算接口

// 卷烟分配服务相关API（重构为多模块接口）
export const cigaretteDistributionAPI = {
  // =================== 通用功能接口 (/api/common) ===================

  // 健康检查
  healthCheck() {
    return commonApi.get('/health')
  },

  // =================== 数据管理接口 (/api/data) ===================

  // 查询卷烟分配数据
  queryDistribution(params) {
    return dataApi.post('/query', params, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 更新卷烟信息（主要接口）
  updateCigaretteInfo(data) {
    return dataApi.post('/update-cigarette', data, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 删除投放区域
  deleteDeliveryAreas(data) {
    return dataApi.post('/delete-delivery-areas', data, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 基于编码表达式批量更新投放信息
  batchUpdateFromExpressions(data) {
    return dataApi.post('/batch-update-from-expressions', data, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 重建区域客户统计
  rebuildRegionCustomerStatistics(data) {
    return dataApi.post('/region-customer-statistics/rebuild', data, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // =================== 分配计算接口 (/api/calculate) ===================

  // 写回分配矩阵
  writeBackDistribution(params) {
    // 注意：后端接口使用URL参数
    let url = `/write-back?year=${params.year}&month=${params.month}&weekSeq=${params.weekSeq}`

    if (params.urbanRatio !== undefined && params.ruralRatio !== undefined) {
      const urbanRatio = params.urbanRatio / 100
      const ruralRatio = params.ruralRatio / 100
      url += `&urbanRatio=${urbanRatio}&ruralRatio=${ruralRatio}`
    }

    return calculateApi.post(url)
  },

  // 生成分配方案
  generateDistributionPlan(params) {
    // 注意：后端接口使用URL参数而非请求体
    // 新增：城网和农网分配比例参数（需要转换为小数格式 0.0-1.0）
    let url = `/generate-distribution-plan?year=${params.year}&month=${params.month}&weekSeq=${params.weekSeq}`

    // 如果传入了比例参数，需要转换为小数格式（API要求0.0-1.0）
    if (params.urbanRatio !== undefined && params.ruralRatio !== undefined) {
      const urbanRatio = params.urbanRatio / 100  // 40 -> 0.4
      const ruralRatio = params.ruralRatio / 100  // 60 -> 0.6
      url += `&urbanRatio=${urbanRatio}&ruralRatio=${ruralRatio}`
    }
    // 如果不传比例参数，后端使用默认值（城网0.4，农网0.6）

    return calculateApi.post(url)
  },

  // 计算总实际投放量
  calculateTotalActualDelivery(params) {
    return calculateApi.post(`/total-actual-delivery?year=${params.year}&month=${params.month}&weekSeq=${params.weekSeq}`)
  },

  // =================== 数据导入接口 (/api/import) ===================

  // 导入卷烟投放基础信息
  importBasicInfo(formData) {
    return importApi.post('/cigarette-info', formData, {
      headers: {
        // 不设置Content-Type，让浏览器自动设置multipart/form-data和boundary
      }
    })
  },

  // 导入客户基础信息表
  importBaseCustomerInfo(formData) {
    return importApi.post('/base-customer-info', formData, {
      headers: {
        // 不设置Content-Type，让浏览器自动设置multipart/form-data和boundary
      }
    })
  },

  // =================== 区域客户数计算接口 (/api/cal-region-customer-num) ===================

  // 计算并生成区域客户数表
  calculateRegionCustomerNum(params) {
    return calRegionCustomerNumApi.post('/calculate', params, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 区域客户数计算服务健康检查
  calRegionCustomerNumHealthCheck() {
    return calRegionCustomerNumApi.get('/health')
  },

  // =================== 方法别名（向后兼容） ===================

  // generatePlan 是 generateDistributionPlan 的别名
  generatePlan(params) {
    return this.generateDistributionPlan(params)
  },

  // calculateCustomerNum 是 calculateRegionCustomerNum 的别名
  calculateCustomerNum(params) {
    return this.calculateRegionCustomerNum(params)
  },

  // executeWriteBack 是 writeBackDistribution 的别名
  executeWriteBack(params) {
    return this.writeBackDistribution(params)
  },

  // rebuildRegionStats 是 rebuildRegionCustomerStatistics 的别名
  rebuildRegionStats(params) {
    return this.rebuildRegionCustomerStatistics(params)
  },

  // importCustomerInfo 是 importBaseCustomerInfo 的别名
  importCustomerInfo(formData) {
    return this.importBaseCustomerInfo(formData)
  },

  // =================== 废弃的接口（向后兼容） ===================

  // 查询原始数据（如需要可联系后端添加）
  queryRawData(params) {
    console.warn('queryRawData 接口已废弃，请使用 queryDistribution')
    return this.queryDistribution(params)
  },

  // 更新卷烟分配（废弃，建议使用updateCigaretteInfo）
  updateDistribution(data) {
    console.warn('updateDistribution 接口已废弃，请使用 updateCigaretteInfo')
    return this.updateCigaretteInfo(data)
  },

  // 测试分配算法（如需要可联系后端添加）
  testAlgorithm() {
    console.warn('testAlgorithm 接口暂不可用，请联系后端开发')
    return Promise.reject(new Error('接口暂不可用'))
  }
}

// =================== 废弃的API模块（保留以兼容现有代码） ===================

// 卷烟投放方案相关API（废弃，建议使用cigaretteDistributionAPI）
export const distributionPlanAPI = {
  // 查询卷烟投放方案
  queryPlan(params) {
    console.warn('distributionPlanAPI.queryPlan 已废弃，请使用 cigaretteDistributionAPI.queryDistribution')
    return cigaretteDistributionAPI.queryDistribution(params)
  },

  // 保存档位设置
  savePositions(data) {
    console.warn('distributionPlanAPI.savePositions 已废弃，请使用 cigaretteDistributionAPI.updateCigaretteInfo')
    return cigaretteDistributionAPI.updateCigaretteInfo(data)
  },

  // 获取档位数据
  getPositionData(cigaretteName, year, month, week) {
    console.warn('distributionPlanAPI.getPositionData 已废弃，请使用 cigaretteDistributionAPI.queryDistribution 查询特定卷烟')
    return cigaretteDistributionAPI.queryDistribution({ year, month, weekSeq: week })
  }
}

// 卷烟数据相关API（暂未在后端接口文档中找到对应接口）
export const cigaretteAPI = {
  // 获取卷烟列表
  getCigaretteList() {
    console.warn('cigaretteAPI.getCigaretteList 接口暂不可用，请联系后端开发')
    return Promise.reject(new Error('接口暂不可用'))
  },

  // 搜索卷烟
  searchCigarettes(keyword) {
    console.warn('cigaretteAPI.searchCigarettes 接口暂不可用，请联系后端开发')
    return Promise.reject(new Error('接口暂不可用'))
  }
}

// =================== 导出配置 ===================

// 导出主要的API实例和方法
export { dataApi, commonApi, calculateApi, importApi, calRegionCustomerNumApi }
export default cigaretteDistributionAPI
