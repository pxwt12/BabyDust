# Phase 1 / Phase 2 总结与接口测试报告

## 总体结论

Phase 1 已完成账号、家庭、档案和三语框架的基础能力。Phase 2 已完成孕期模块 P0 核心闭环：从孕 6 周建档开始，可以记录体重、血压、症状、用药/补剂，查看产检计划，完成待办，录入检查报告指标，查看时间线、首页摘要和基础趋势。

当前实现仍保持多端架构：小程序、未来 H5/App、运营后台都通过统一后端 API 访问业务数据，没有把业务逻辑写死在一次性小程序项目里。

## Phase 1 已完成功能

- 微信登录换取后端 token。
- 家庭空间创建。
- 孕期档案创建。
- 管理后台基础页面和构建链路。
- 三语框架：简体中文、繁体中文、英文。
- 前端本地语言切换。
- 后端统一响应结构：`success`、`data`、`error`、`requestId`。
- 基础权限控制：登录、家庭成员校验、跨家庭访问拦截。

## Phase 2 已完成功能

- 孕期首页摘要：
  - 孕周。
  - 预产期倒计时。
  - 今日/近期待办。
  - 近期记录。
  - 近期报告。
  - 核心趋势点数。
- 记录模块：
  - 体重。
  - 血压。
  - 症状。
  - 用药。
  - 补剂。
  - 胎动。
  - 心情。
  - 备注。
- 记录详情：
  - 查看。
  - 结构化编辑。
  - 删除确认。
  - 删除后刷新列表和首页。
- 产检计划：
  - 建档时自动生成默认产检待办。
  - 待办列表。
  - 完成状态更新。
  - 首页产检进度联动。
- 报告模块：
  - 报告手动录入。
  - 指标字典。
  - 结构化指标录入。
  - 报告详情查看、编辑、删除。
- 报表模块：
  - 体重趋势。
  - 收缩压趋势。
  - 舒张压趋势。
- 交互体验：
  - 加载骨架。
  - 失败重试。
  - 空状态说明。
  - 防重复提交。
  - 日期选择器。
  - 前端基础数值校验。

## 已开发接口设计

### 账号与会话

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/wechat-login` | 微信登录换取后端 token。 |

### 家庭空间

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/families` | 查询当前用户家庭空间。 |
| `POST` | `/api/v1/families` | 创建家庭空间。 |
| `POST` | `/api/v1/families/{id}/invites` | 创建家庭邀请。 |

### 档案

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/profiles/pregnancies` | 查询孕期档案。 |
| `POST` | `/api/v1/profiles/pregnancies` | 创建孕期档案，并生成默认产检计划。 |
| `GET` | `/api/v1/profiles/mothers` | 查询妈妈档案。 |
| `POST` | `/api/v1/profiles/mothers` | 创建妈妈档案。 |
| `GET` | `/api/v1/profiles/babies` | 查询宝宝档案。 |
| `POST` | `/api/v1/profiles/babies` | 创建宝宝档案。 |

### 记录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/records/types` | 查询记录类型字典。 |
| `GET` | `/api/v1/records` | 查询记录列表，支持家庭、主体、类型、日期过滤。 |
| `POST` | `/api/v1/records` | 创建记录。 |
| `GET` | `/api/v1/records/{recordId}` | 查询记录详情。 |
| `POST` | `/api/v1/records/{recordId}` | 更新记录发生时间和 payload。 |
| `DELETE` | `/api/v1/records/{recordId}` | 删除记录。 |

### 检查报告

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/reports/indicator-definitions` | 查询报告指标字典。 |
| `GET` | `/api/v1/reports` | 查询报告列表。 |
| `POST` | `/api/v1/reports` | 创建报告。 |
| `GET` | `/api/v1/reports/{reportId}` | 查询报告详情。 |
| `POST` | `/api/v1/reports/{reportId}` | 更新报告标题、检查日期和指标。 |
| `DELETE` | `/api/v1/reports/{reportId}` | 删除报告。 |

### 待办与产检计划

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/todos` | 查询待办列表。 |
| `POST` | `/api/v1/todos` | 创建自定义待办。 |
| `POST` | `/api/v1/todos/{todoId}/status` | 更新待办状态。 |

### 首页与报表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/home/summary` | 查询首页摘要。 |
| `GET` | `/api/v1/analytics/series` | 查询指标趋势序列。 |

### 管理后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/admin/content` | 内容配置草稿接口。 |
| `POST` | `/api/v1/admin/templates` | 模板配置草稿接口。 |
| `POST` | `/api/v1/admin/i18n` | 多语言配置草稿接口。 |

## 单元测试与接口测试设计

### 服务层单元测试

| 测试类 | 覆盖内容 |
| --- | --- |
| `PregnancyServiceTest` | 根据末次月经计算孕周、孕天、孕期第几天、孕早中晚期。 |

### 接口集成测试

| 测试类 | 覆盖内容 |
| --- | --- |
| `AuthControllerTest` | 登录响应、未登录拦截、开发用户头、宝宝档案字段校验。 |
| `FamilyFlowTest` | 家庭、孕期档案、记录、报告、待办、首页、报表、权限和字典接口。 |

### 关键测试场景

- `wechatLoginReturnsUnifiedResponse`：验证登录接口返回统一响应和 token。
- `protectedEndpointRejectsMissingSession`：验证未登录访问受保护接口会失败。
- `createsFamilyPregnancyAndPregnancyRecord`：验证家庭、孕期档案、记录、待办、首页摘要基本链路。
- `pregnancyP0AcceptanceFlowFromWeekSixProfileToSummaryAnalyticsAndReports`：验证 Phase 2 P0 端到端验收路径。
- `recordPayloadMustMatchSupportedType`：验证记录 payload 必须符合类型规则。
- `reportIndicatorsUseStructuredArrayAndDefinitions`：验证报告指标必须使用结构化数组。
- `recordDetailUpdateAndDeleteRequireFamilyAccess`：验证记录详情、编辑、删除。
- `reportDetailUpdateAndDeleteRequireFamilyAccess`：验证报告详情、编辑、删除。
- `dictionariesExposeRecordTypesIndicatorsAndI18nForClientBootstrap`：验证记录字典、报告指标字典和后台 i18n 配置接口。
- `crossFamilyRecordReportTodoAndAnalyticsAccessIsRejected`：验证跨家庭访问记录、报告、待办、首页和趋势会被拒绝。

## 本次执行结果

- 后端测试：`15 tests, 0 failures, 0 errors`。
- 小程序类型检查：通过。
- 微信小程序构建：通过。
- 管理后台构建：通过。
- i18n 编码检查：通过，无中文乱码。

## 当前风险与后续建议

- 小程序目前完成类型检查和构建验证，但还没有自动化 UI 验收。后续建议引入端到端 UI 测试或微信开发者工具自动化检查。
- 管理后台仍是基础草稿接口和基础页面，后续 Phase 6 需要补 RBAC、审核流、回滚和发布状态。
- OCR、ASR、AI 结构化整理不属于 Phase 2，建议在 Phase 5 统一接入 AI Gateway。
- 提醒触发和微信订阅消息不属于 Phase 2，建议在 Phase 4 统一实现。
