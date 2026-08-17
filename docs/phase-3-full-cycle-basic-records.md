# Phase 3 全周期基础入口说明

Phase 3 的目标是把备孕、生产、产后、宝宝从“首页入口”推进到“可以创建基础记录、进入统一时间线”的状态，同时不破坏 Phase 2 已完成的孕期深度功能。

## 当前已完成

- 后端统一记录系统扩展 `family` 主体。
- 新增全周期基础记录类型：
  - 备孕：周期记录、排卵试纸、同房记录、基础体温、补剂。
  - 生产：生产事项、宫缩、备注。
  - 产后：恶露、产后情绪、用药、备注。
  - 宝宝：喂养、睡眠、尿布、成长、备注。
- 后端对新增记录类型做基础 payload 校验和数值范围校验。
- 小程序新增通用阶段记录页 `pages/stage-records/index`。
- 首页阶段入口已接入：
  - 备孕、生产、产后、宝宝进入通用阶段记录页。
  - 孕期继续进入原孕期记录页，保留深度功能。
- 宝宝阶段支持创建宝宝基础档案。
- 备孕、生产、产后、宝宝基础记录均通过统一 `/api/v1/records` 接口保存。
- Phase 3 第二段已将宝宝记录从家庭主体升级为具体 `baby` 主体。
- 小程序宝宝阶段页支持选择宝宝，喂养、睡眠、尿布、成长、备注会写入当前选中的宝宝时间线。
- 后端会拒绝将宝宝记录误写入 `family` 主体，避免多宝宝数据混杂。
- Phase 3 第三段新增全周期统一时间线 `pages/timeline/index`：
  - 按 `family`、`pregnancy`、`baby` 主体分别读取记录，再在前端合并排序。
  - 支持全部、备孕、孕期、生产、产后、宝宝阶段筛选。
  - 每条记录展示阶段、记录类型、主体和 payload 摘要，并可跳转到记录详情页。
  - 首页“全部时间线”入口已接入，最近记录数量也可进入统一时间线。
- 备孕补剂和产后用药已拆成独立记录类型：
  - `fertility_supplement` 使用 `family` 主体。
  - `postpartum_medication` 使用 `family` 主体。
  - 孕期原有 `supplement`、`medication` 继续只服务 `pregnancy` 主体。
- Phase 3 第四段在阶段记录页补充专用交互：
  - 生产阶段新增宫缩计时器，开始/结束后自动写入 `contraction` 记录，并计算本次持续秒数与距离上次间隔分钟数。
  - 宝宝阶段新增快速喂养记录，可保存奶量和左侧、右侧、奶瓶来源，仍写入 `baby_feeding`。
  - 宝宝阶段新增睡眠计时器，结束后自动写入 `baby_sleep`，并保存睡眠开始时间。
  - 宝宝阶段新增成长趋势预览，根据当前宝宝的 `baby_growth` 记录展示体重和身高趋势，并可快速切换到成长记录表单。

## 已覆盖测试

后端测试 `phaseThreeFamilyStageRecordsShareUnifiedRecordApi` 已覆盖：

- 使用统一记录 API 创建备孕、生产、产后、宝宝阶段基础记录。
- 按 `subjectType=family` 和 `subjectId=familyId` 查询阶段记录。
- 按 `subjectType=pregnancy` 和 `subjectId=pregnancyId` 查询孕期记录，供统一时间线聚合。
- 按 `subjectType=baby` 和 `subjectId=babyId` 查询宝宝记录，供统一时间线聚合。
- 创建并校验 `fertility_supplement`、`postpartum_medication` 两类 family 主体记录。
- 创建并校验带扩展 payload 的 `baby_feeding`、`baby_sleep` 记录，确保快捷工具写入的数据仍兼容后端记录校验。
- 校验伪造 family subjectId 会被拒绝。
- 校验宝宝记录必须使用 `subjectType=baby` 和具体宝宝 `subjectId`。

## 后续建议

- 将宝宝喂养继续增强为母乳左右侧计时、奶瓶、亲喂、配方奶、辅食等更细类型。
- 将宝宝成长趋势从阶段页预览升级为正式报表，支持 WHO/中国儿童生长曲线参考线。
- 给宫缩计时增加“5-1-1”规则提示、医院待产包提醒和紧急情况免责声明。
- 后续如统一时间线数据量增长，可增加后端聚合接口，例如 `GET /records/timeline?familyId=...&stage=...`，将多主体聚合和分页下沉到服务端。
