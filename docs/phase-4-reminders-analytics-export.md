# Phase 4 提醒、报表、导出说明

Phase 4 的目标是把前面已经沉淀的记录、报告、计划数据变成可提醒、可观察、可导出的闭环。第一段完成站内提醒中心，第二段先落地结构化导出能力，为后续 Excel/PDF 文件导出打基础。

## 当前已完成

- 后端提醒 API 已形成基础闭环：
  - `GET /api/v1/reminders?familyId=...` 查询家庭提醒。
  - `POST /api/v1/reminders` 创建提醒。
  - `POST /api/v1/reminders/{reminderId}/status` 更新提醒状态。
- 提醒状态支持：
  - `scheduled`：待提醒。
  - `done`：已完成。
  - `cancelled`：已取消。
- 后端所有提醒写入和状态更新都会校验家庭权限。
- 小程序新增提醒中心 `pages/reminders/index`：
  - 支持新增自定义提醒。
  - 支持选择提醒场景：产检、补剂、产后复查、疫苗、自定义。
  - 支持日期和时间选择。
  - 支持按状态筛选提醒。
  - 支持完成、取消、恢复提醒。
- 首页已接入提醒中心入口。
- 当前阶段先完成站内提醒；微信订阅消息待小程序模板、授权策略和运营文案确认后接入。
- Phase 4 第二段新增结构化导出接口：
  - `GET /api/v1/exports/pregnancy-records?familyId=...` 导出当前家庭的孕期记录。
  - `GET /api/v1/exports/reports?familyId=...` 导出当前家庭的检查报告。
  - 导出接口均先校验家庭权限，避免跨家庭数据泄露。
  - 返回结构化 `exportType`、`format`、`generatedAt`、`rowCount`、`rows`、`csvContent`，后续可替换为异步导出任务和 OSS 文件地址。
- 小程序报表页新增数据导出面板：
  - 支持生成孕期记录导出预览。
  - 支持生成检查报告导出预览。
  - 支持复制 CSV 导出内容，方便首版验证数据口径并用 Excel 打开。
- Phase 4 第三段新增宝宝成长报表：
  - `GET /api/v1/analytics/series?metric=baby_weight` 返回宝宝体重趋势，单位 kg。
  - `GET /api/v1/analytics/series?metric=baby_height` 返回宝宝身高趋势，单位 cm。
  - 当前版本按家庭聚合 `baby_growth` 记录，后续多宝宝场景可增加 `subjectType=baby&subjectId=...` 精确筛选。
  - 小程序报表页已展示宝宝体重趋势、宝宝身高趋势。
- Phase 4 第四段新增报表总览：
  - `GET /api/v1/analytics/overview?familyId=...` 返回产检计划完成率和用药/补剂记录统计。
  - 产检计划完成率统计 `prenatal_checkup` 和 `delivery_prepare` 两类待办。
  - 用药/补剂统计覆盖孕期用药、产后用药、孕期补剂、备孕补剂。
  - 小程序报表页已新增“报表总览”卡片，展示产检完成率、用药/补剂记录数。
- Phase 4 第五段增强导出格式：
  - 导出结果新增 `format=csv` 和 `csvContent` 字段。
  - 孕期记录 CSV 包含 `id, subjectType, subjectId, recordType, occurredAt, payloadJson, privacyLevel`。
  - 检查报告 CSV 包含 `id, subjectType, subjectId, reportType, title, examinedAt, indicatorsJson`。
  - CSV 单元格统一双引号包裹并转义内部双引号，降低 JSON payload 破坏列结构的风险。

## 已覆盖测试

后端测试 `phaseFourReminderCenterSupportsCreateListAndStatusFlow` 已覆盖：

- 创建孕期产检提醒。
- 创建家庭补剂提醒。
- 按触发时间升序查询提醒。
- 将提醒标记为完成。
- 将提醒标记为取消。
- 非法状态会返回 `INVALID_ARGUMENT`。

跨家庭权限测试已补充：

- 其他家庭成员不能更新本家庭提醒状态。

后端测试 `phaseFourExportsPregnancyRecordsAndReportsWithFamilyIsolation` 已覆盖：

- 导出孕期记录，并返回记录类型、发生时间、payload、隐私级别。
- 导出检查报告，并返回报告标题、类型、检查日期、指标 JSON。
- 校验导出结果包含 CSV 格式、CSV 表头和关键业务内容。
- 其他家庭成员不能导出本家庭记录。

后端测试 `phaseFourAnalyticsIncludesBabyGrowthSeries` 已覆盖：

- 使用宝宝主体创建两条 `baby_growth` 记录。
- 查询 `baby_weight` 趋势并校验 kg 单位和点位顺序。
- 查询 `baby_height` 趋势并校验 cm 单位和点位顺序。

后端测试 `phaseFourAnalyticsOverviewIncludesCheckupProgressAndMedicationCounts` 已覆盖：

- 创建孕期用药和补剂记录。
- 将一条产检待办标记为完成。
- 查询报表总览并校验产检完成率、用药记录数、补剂记录数。

## 后续建议

- 将产检计划、补剂、42 天复查、宝宝疫苗自动生成提醒。
- 接入微信订阅消息模板，处理用户授权、模板 ID 配置、发送失败重试和发送日志。
- 报表增强后续建议：
  - 宝宝成长曲线增加单宝宝筛选和参考曲线。
  - 用药/补剂从“记录数”升级为“计划-打卡-漏服”完成情况。
  - 报表总览增加时间范围筛选，例如本周、本月、全部。
- 导出能力后续增强：
  - 将 CSV 内容升级为真正的 Excel/PDF 文件。
  - 增加异步导出任务、OSS 私有文件、短期有效下载 URL。
  - 增加导出审计日志、文件过期清理、下载次数限制。
