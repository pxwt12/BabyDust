# Phase 5 AI、OCR、ASR 说明

Phase 5 的目标是把文字、语音、图片输入整理为可确认的记录、报告、待办、提醒草稿。第一段先完成 AI Gateway 草稿边界，不直接接外部 DeepSeek/OCR/ASR 服务，确保产品安全规则和接口形态稳定。

## 当前已完成

- 后端新增 `AiGatewayService`，业务入口不直接耦合 DeepSeek SDK 或供应商接口。
- AI 草稿接口保留：
  - `POST /api/v1/ai/extract-record`
  - `POST /api/v1/ai/extract-report`
- 草稿返回结构包含：
  - `status=draft`
  - `purpose`
  - `provider`
  - `model`
  - `inputType`
  - `source=ai_draft`
  - `needsUserConfirmation=true`
  - `blocked`
  - `riskLevel`
  - `records`
  - `todos`
  - `reports`
  - `warnings`
  - `generatedAt`
- 当前阶段使用规则型草稿生成：
  - 文本包含体重时生成 `weight` 记录草稿。
  - 文本包含血压时生成 `blood_pressure` 记录草稿。
  - 文本包含胎动时生成 `fetal_movement` 记录草稿。
  - 文本包含提醒、复查等关键词时生成待办草稿。
  - 报告文本包含 HCG、孕酮、血等关键词时生成 `blood` 报告草稿。
- 高风险医疗决策拦截：
  - 诊断、用药剂量、明显急症等内容会返回 `blocked=true`。
  - 高风险内容不返回可入库记录草稿。
  - 返回安全提示，要求联系医生或急诊。
- 小程序新增 `pages/ai-draft/index`：
  - 支持记录草稿和报告草稿两种模式。
  - 支持文字输入。
  - 展示草稿 JSON、风险等级和安全提示。
  - 支持复制草稿内容。
- 首页已接入 AI 整理入口。
- Phase 5 第二段新增 AI 调用审计：
  - 新增 `ai_audit_logs` 表。
  - 每次 AI 草稿生成都会记录审计日志。
  - 审计字段包含 `purpose`、`provider`、`model`、`inputType`、`inputLength`、`inputPreview`、`riskLevel`、`blocked`、`latencyMs`、`status`。
  - `inputPreview` 只保存脱敏摘要，不保存完整原文。
  - 当前已对手机号和邮箱做基础脱敏。
  - 新增开发期查询接口 `GET /api/v1/ai/audit-logs`，后续需要收进后台 RBAC 管理权限。
- Phase 5 第三段新增 OCR/ASR 预处理接口：
  - `POST /api/v1/ai/ocr-report`：接收报告图片文件地址和可选识别文本，返回 OCR 文本和报告草稿。
  - `POST /api/v1/ai/asr-record`：接收语音文件地址和可选转写文本，返回 ASR 文本和记录草稿。
  - 当前阶段使用模拟预处理，不调用真实阿里云服务。
  - 接口返回 `preprocessor=aliyun_ocr` 或 `preprocessor=aliyun_asr`，为后续替换真实服务预留供应商标识。
  - OCR/ASR 结果继续走 AI Gateway 草稿生成和审计流程。
- 小程序 AI 整理页新增：
  - OCR 报告模式。
  - 语音记录模式。
  - 文件地址输入框，后续 OSS 上传完成后自动填入。

## 已覆盖测试

后端测试 `phaseFiveAiGatewayReturnsDraftsAndBlocksHighRiskMedicalDecisions` 已覆盖：

- 文本记录抽取返回 `draft`，并要求用户确认。
- 体重文本生成 `weight` 记录草稿。
- 提醒文本生成待办草稿。
- OCR 文本生成报告草稿，并识别为 `blood` 类型。
- 高风险医疗决策输入会被拦截，不返回记录草稿。
- AI 调用会写入审计日志。
- 审计日志包含 provider、latency、blocked、riskLevel。
- 审计输入摘要会脱敏手机号。

后端测试 `phaseFiveOcrAndAsrPreprocessorsReturnDraftsAndAuditLogs` 已覆盖：

- OCR 报告预处理返回 `aliyun_ocr` 标识、OCR 文本和报告草稿。
- ASR 语音预处理返回 `aliyun_asr` 标识、转写文本和记录草稿。
- OCR/ASR 下游草稿分别使用 `ocr_text`、`asr_text` 输入类型。
- OCR/ASR 调用会写入审计日志。

后端测试 `phaseFiveAdminAiConfigsPersistProviderPromptAndSchema` 已覆盖：

- 后台可新增 `provider`、`prompt`、`schema` 三类 AI 配置。
- 后台可查看全部 AI 配置。
- 后台可按 `configType` 筛选配置。
- `configJson` 非法时会返回 `INVALID_ARGUMENT`。

## Phase 5 第四段：AI 后台配置中心雏形

已完成：

- 后端新增 `ai_configs` 表，用于保存 AI Provider、Prompt、Schema 三类后台配置。
- 新增后台接口：
  - `GET /api/v1/admin/ai-configs`：查看最近 50 条 AI 配置。
  - `GET /api/v1/admin/ai-configs?configType=provider|prompt|schema`：按配置类型筛选。
  - `POST /api/v1/admin/ai-configs`：新增 AI 配置。
- 配置校验：
  - `configType` 仅允许 `provider`、`prompt`、`schema`。
  - `status` 仅允许 `draft`、`active`、`archived`。
  - `configJson` 必须是合法 JSON 对象或数组。
- 后台 Vue Admin 新增 AI Gateway 配置面板：
  - 支持 provider、prompt、schema 三类筛选。
  - 支持表单新增配置。
  - 提供三类默认模板。
  - 展示配置状态、版本、供应商和 JSON 内容。

设计边界：

- 后台配置表不保存真实 API Key，只保存 `credentialRef` 等密钥引用。
- 真实密钥后续应放在环境变量、阿里云 KMS 或其他云密钥管理服务中。
- 当前后台鉴权仍是 Phase 1 占位能力，正式发布前必须接入管理员登录、RBAC 和操作审计。

## Phase 5 第五段：AI Gateway 使用后台激活配置

已完成：

- `AiGatewayService` 运行时会读取后台 `active` 状态的 AI 配置。
- Provider 配置：
  - 使用最新的 active `provider` 配置作为当前供应商配置。
  - 从 `configJson.model` 读取模型名，覆盖 `application.yml` 默认模型。
  - 草稿响应新增 `providerConfigKey`，用于标识本次使用的供应商配置。
- Prompt/Schema 配置：
  - 使用最新 active `prompt` 配置的 `versionLabel` 作为 `promptVersion`。
  - 使用最新 active `schema` 配置的 `versionLabel` 作为 `schemaVersion`。
- 无 active 配置时继续回退到应用默认 provider/model，并返回 `application-default` 版本标识。
- AI 审计日志中的 provider/model 会跟随实际解析后的 Gateway 配置。
- 小程序 AI 草稿页同步展示 provider 配置 key、prompt 版本和 schema 版本。

后端测试 `phaseFiveAiGatewayUsesActiveAdminConfigVersions` 已覆盖：

- active provider 配置覆盖模型名。
- active prompt/schema 配置版本进入 AI 草稿响应。
- AI 审计日志记录解析后的模型名。

## Phase 5 第六段：DeepSeek 接入前的模型客户端抽象

已完成：

- 新增 `AiModelClient` 接口，业务层不直接依赖 DeepSeek HTTP API、SDK 或供应商响应格式。
- 新增 `RuleBasedAiModelClient`：
  - 作为当前阶段的本地 fallback 客户端。
  - 继续支持体重、血压、胎动、待办、血检报告等基础规则抽取。
  - 返回统一的 `fallbackUsed`、`errorCode`、`rawOutputPreview`、records、todos、reports。
- `AiGatewayService` 改为通过 `AiModelClient` 获取结构化草稿。
- 高风险医疗内容仍在模型调用前拦截，不进入模型客户端。
- 模型客户端异常会被转换为 `MODEL_CLIENT_EXCEPTION`，接口仍返回草稿结构，避免调用方崩溃。
- AI 草稿响应新增：
  - `fallbackUsed`
  - `errorCode`
  - `rawOutputPreview`
- AI 审计日志新增：
  - `fallbackUsed`
  - `errorCode`
- 小程序 AI 草稿页同步展示 fallback 状态和错误码。

设计边界：

- 当前阶段仍不发起真实外部 DeepSeek 请求。
- 后续接入 DeepSeek 时，应新增 DeepSeek HTTP 客户端实现 `AiModelClient`，并保留 `RuleBasedAiModelClient` 作为降级方案。
- `rawOutputPreview` 只能保存脱敏摘要或短文本，不得保存完整病历、报告全文、手机号、证件号、API Key 或完整模型输出。

## Phase 5 第七段：DeepSeek HTTP 客户端骨架

已完成：

- 新增 `DeepSeekAiModelClient`：
  - 支持 DeepSeek OpenAI-compatible `/chat/completions` 请求结构。
  - 支持 `baseUrl`、`model`、`timeoutMs`、`enabled` 配置。
  - 支持 Bearer API Key 请求头。
  - 支持解析 DeepSeek `choices[0].message.content` 中的 JSON 草稿。
- 新增 `CompositeAiModelClient`：
  - 作为 `AiModelClient` 的主入口。
  - `DEEPSEEK_ENABLED=false` 或非 DeepSeek provider 时走 `RuleBasedAiModelClient`。
  - DeepSeek 启用但不可用、缺少密钥、HTTP 失败或响应解析未完成时，回退到规则客户端继续生成可确认草稿。
- 新增 `AiCredentialResolver`：
  - 支持 `credentialRef=env:DEEPSEEK_API_KEY` 形式从环境变量读取密钥。
  - 直接密钥值优先级高于环境变量引用，但后台配置不应保存真实密钥。
- 新增配置项：
  - `DEEPSEEK_ENABLED=false`
  - `DEEPSEEK_TIMEOUT_MS=8000`
- 新增测试：
  - DeepSeek 启用但密钥无法解析时返回 `DEEPSEEK_API_KEY_MISSING`。
  - `credentialRef` 可从环境变量读取密钥。
  - 默认关闭外部 DeepSeek 调用时，接口继续使用规则 fallback，不发起外部请求。

设计边界：

- 当前阶段仍默认不发真实 DeepSeek 请求。
- 即使开启 DeepSeek，模型响应解析未完成前也不会信任模型输出作为正式草稿来源。
- 后续需要补齐响应 JSON schema 校验、输出脱敏、token/费用审计、重试和限流后，才能启用真实调用。

## Phase 5 第八段：DeepSeek 响应解析与草稿校验

已完成：

- `DeepSeekAiModelClient` 新增响应解析：
  - 从 OpenAI-compatible 响应中读取 `choices[0].message.content`。
  - `content` 必须是 JSON 对象字符串。
  - JSON 对象必须包含 `records`、`todos`、`reports` 三个数组。
  - 数组中的每一项必须是对象。
- 解析成功时返回：
  - `fallbackUsed=false`
  - `errorCode=OK`
  - 解析后的 records/todos/reports 草稿。
- 解析失败时返回稳定错误码，并由 `CompositeAiModelClient` 继续 fallback：
  - `DEEPSEEK_EMPTY_CONTENT`
  - `DEEPSEEK_INVALID_JSON`
  - `DEEPSEEK_INVALID_DRAFT_SCHEMA`
- 新增测试：
  - 合法 DeepSeek JSON 草稿可解析为 records/todos/reports。
  - 缺少必需数组时返回 `DEEPSEEK_INVALID_DRAFT_SCHEMA`。
  - content 不是 JSON 时返回 `DEEPSEEK_INVALID_JSON`。

设计边界：

- 当前 schema 只做基础结构校验，字段级业务校验仍由用户确认和后续正式入库接口负责。
- DeepSeek 输出即使解析成功，也仍然只是草稿，不直接入库。

## Phase 5 第九段：模型输出脱敏与审计增强

已完成：

- 新增 `AiTextSanitizer`：
  - 统一处理 AI 输入摘要和模型输出摘要。
  - 手机号脱敏为 `***PHONE***`。
  - 邮箱脱敏为 `***EMAIL***`。
  - 身份证样式文本脱敏为 `***ID_CARD***`。
  - Bearer token 脱敏为 `Bearer ***TOKEN***`。
  - API key、authorization、token、secret 样式文本脱敏为 `***SECRET***`。
- `DeepSeekAiModelClient` 的 `rawOutputPreview` 已统一走脱敏和 160 字符长度限制。
- `AiGatewayService` 的 `inputPreview` 已统一走脱敏和 120 字符长度限制。
- AI 审计日志新增 token/费用预留字段：
  - `promptTokens`
  - `completionTokens`
  - `totalTokens`
  - `costCurrency`
  - `estimatedCost`
- 当前真实 token 和费用统计尚未接入，预留字段默认写入 0、`CNY`。
- 新增测试：
  - 模型输出摘要会脱敏手机号、邮箱、身份证样式文本和 API key。
  - AI 审计日志返回 token/费用预留字段。

设计边界：

- 脱敏规则是基础防线，不能替代真实接入后的结构化隐私审计。
- 后续真实 DeepSeek 响应接入后，应从供应商 usage 字段填充 token，并按后台配置的价格表估算费用。

## Phase 5 第十段：DeepSeek usage 解析与费用估算

已完成：

- `AiModelResult` 和 AI 草稿响应新增 usage/费用字段：
  - `promptTokens`
  - `completionTokens`
  - `totalTokens`
  - `costCurrency`
  - `estimatedCost`
- `DeepSeekAiModelClient` 会从响应 `usage` 中读取：
  - `prompt_tokens`
  - `completion_tokens`
  - `total_tokens`
- 费用估算从 active provider 的 `configJson.pricing` 读取：
  - `currency`
  - `promptPer1K`
  - `completionPer1K`
- 示例 provider 配置：

```json
{
  "model": "deepseek-chat",
  "credentialRef": "env:DEEPSEEK_API_KEY",
  "pricing": {
    "currency": "CNY",
    "promptPer1K": "0.002",
    "completionPer1K": "0.008"
  }
}
```

- 估算公式：
  - `estimatedCost = (promptTokens * promptPer1K + completionTokens * completionPer1K) / 1000`
- 缺少 usage 或 pricing 时，token 和费用保持 0。
- `AiGatewayService` 会把 usage/费用字段写入 AI 审计日志。
- 小程序 AI 草稿页同步展示 token 总数和估算费用。

新增测试：

- DeepSeek 响应包含 `usage` 时可提取 token。
- Provider 配置包含 pricing 时可估算费用。

设计边界：

- 当前费用是估算值，不能作为正式账单。
- 后续需要在后台增加不同模型、不同币种、不同生效时间的价格配置版本。

## 后续计划

- Phase 5 第十一段：后台 Provider 价格配置表单
  - 后台 AI Gateway 配置面板已把 `provider` 类型从纯 JSON 输入升级为结构化表单。
  - Provider 表单支持维护：
    - `model`
    - `credentialRef`
    - `baseUrl`
    - `pricing.currency`
    - `pricing.promptPer1K`
    - `pricing.completionPer1K`
  - 表单会自动生成 `configJson`，减少运营配置时手写 JSON 出错。
  - 仍保留 JSON 文本区，便于后续扩展 DeepSeek 或 OpenAI-compatible provider 的高级参数。
  - 列表侧会解析 provider 配置并展示模型、密钥引用、Base URL、千 token 价格摘要。
  - 密钥边界不变：后台配置只保存 `credentialRef`，不得保存真实 API Key。
  - 当前价格字段只用于 AI 审计中的费用估算，不作为正式账单。

- Phase 5 第十二段：后台 AI 审计日志页面
  - 后端新增后台接口 `GET /api/v1/admin/ai-audit-logs`，用于替代开发期 `GET /api/v1/ai/audit-logs` 的运营查看场景。
  - 后台审计接口支持筛选：
    - `provider`
    - `model`
    - `riskLevel`
    - `blocked`
    - `fallbackUsed`
    - `errorCode`
    - `limit`
  - Vue Admin 新增 `AI Audit` 面板：
    - 支持按供应商、模型、风险等级、是否拦截、是否 fallback、错误码筛选。
    - 展示 purpose、inputType、provider、model、createdAt、blocked、fallbackUsed。
    - 展示 token、估算费用、延迟、错误码和脱敏输入摘要。
  - 日志页面只展示脱敏后的 `inputPreview`，不展示原始病历、完整报告、手机号、证件号、API Key 或完整模型输出。
  - 当前后台鉴权仍是占位能力，正式发布前必须把该接口纳入管理员登录、RBAC 和操作审计。

- Phase 5 第十三段：AI Provider 配置安全校验强化
  - 后端 `POST /api/v1/admin/ai-configs` 已对 `provider` 类型增加服务端校验，不能只依赖后台页面约束。
  - Provider 配置必须是 JSON 对象，并必须包含：
    - `model`
    - `credentialRef`
  - `credentialRef` 只允许使用密钥引用格式：
    - `env:`
    - `kms:`
    - `secret:`
  - Provider `configJson` 不允许包含裸写密钥字段，例如 `apiKey`、`secret`、`token`、`authorization`、`password` 等。
  - `baseUrl` 如填写，必须是 `http://` 或 `https://` URL。
  - `pricing` 如填写，必须是 JSON 对象；`promptPer1K`、`completionPer1K` 必须是非负数字。
  - 新增后端测试覆盖：
    - 合法 provider pricing 配置可保存。
    - 裸写 `credentialRef` 会失败。
    - 裸写 `apiKey` 会失败。
    - 负数 token 单价会失败。

- Phase 5 第十四段：AI Prompt/Schema 配置安全校验强化
  - 后端 `POST /api/v1/admin/ai-configs` 已对 `prompt` 和 `schema` 类型增加服务端校验。
  - Prompt 配置必须是 JSON 对象，并必须包含 `purpose`。
  - Prompt `purpose` 仅允许：
    - `record_extraction`
    - `report_extraction`
    - `ocr_report`
    - `asr_record`
  - Prompt `safetyPolicy` 如填写，必须为 `draft_only`，避免后台发布可绕过“仅生成草稿”的提示词配置。
  - Prompt `systemPrompt` 长度限制为 4000 字符以内，避免后台误写超长提示词。
  - Prompt 配置同样复用密钥扫描规则，不能藏入 `apiKey`、`secret`、`token` 等裸密钥字段。
  - Schema 配置必须是 JSON 对象，且 `type` 必须为 `object`。
  - Schema `required` 必须是非空数组，只允许包含 `records`、`todos`、`reports` 三类草稿数组。
  - Schema `properties.records`、`properties.todos`、`properties.reports` 如出现，类型必须为 `array`。
  - 后台默认 schema 模板已同步为三类草稿数组结构。
  - 新增后端测试覆盖：
    - 合法 `draft_only` prompt 可保存。
    - 非 `draft_only` prompt 会失败。
    - 合法草稿 schema 可保存。
    - 要求 `diagnosis` 等非草稿字段的 schema 会失败。
    - 将 `records` 配成非数组的 schema 会失败。

- Phase 5 第十五段：AI 草稿用户确认入库闭环
  - 后端新增 `POST /api/v1/ai/confirm-draft`，用于用户明确确认 AI 草稿后写入正式数据。
  - 确认接口要求：
    - `source=ai_draft`
    - `needsUserConfirmation=true`
    - `blocked=false`
  - 确认接口会重新校验：
    - 当前用户是目标 `familyId` 的家庭成员。
    - `subjectType` 和 `subjectId` 属于该家庭。
    - 记录草稿的 `recordType` 支持目标 subject。
    - 记录 `payload` 符合 `RecordTypeCatalog` 字段和范围规则。
    - 报告指标符合 `ReportIndicatorCatalog`。
  - 确认后可批量写入：
    - 正式记录 `records`
    - 正式检查报告 `medical_reports`
    - 正式待办 `todos`
  - 被高风险规则拦截的草稿不能确认入库。
  - 小程序 AI 整理页新增“确认保存草稿”按钮，首版默认写入当前孕期档案；后续再扩展 subject 选择。
  - 新增后端测试覆盖：
    - AI record/todo 草稿确认后写入正式记录和待办。
    - 写入后可通过正式记录接口查询。
    - blocked 草稿确认会失败。

- Phase 5 第十六段：AI 草稿确认审计追踪
  - 后端新增 `ai_draft_confirmations` 表，用于记录用户确认 AI 草稿后的正式入库轨迹。
  - 确认追踪包含：
    - `familyId`
    - `userId`
    - `subjectType`
    - `subjectId`
    - `provider`
    - `model`
    - `purpose`
    - `draftPreview`
    - `recordIdsJson`
    - `reportIdsJson`
    - `todoIdsJson`
    - `confirmedAt`
  - `draftPreview` 只保存短摘要，不保存完整敏感草稿。
  - `POST /api/v1/ai/confirm-draft` 响应新增 `confirmationId`。
  - 新增 `GET /api/v1/ai/draft-confirmations?familyId=...`，用于查看家庭最近 50 条 AI 草稿确认记录。
  - 小程序确认结果展示 `confirmationId`，便于问题反馈和后续后台排查。
  - 新增后端测试覆盖：
    - 确认入库后生成 confirmationId。
    - 可按 familyId 查询确认历史。
    - 确认历史包含正式记录/待办 ID 列表和草稿摘要。

- Phase 5 第十七段：后台 AI 草稿确认历史面板
  - 后端新增后台接口 `GET /api/v1/admin/ai-draft-confirmations`，用于运营和排查 AI 草稿确认后的正式入库轨迹。
  - 后台确认历史接口支持筛选：
    - `familyId`
    - `userId`
    - `provider`
    - `model`
    - `purpose`
    - `subjectType`
    - `limit`
  - 返回字段使用后台专用 DTO：
    - `confirmationId`
    - `familyId`
    - `userId`
    - `subjectType`
    - `subjectId`
    - `provider`
    - `model`
    - `purpose`
    - `draftPreview`
    - `recordIdsJson`
    - `reportIdsJson`
    - `todoIdsJson`
    - `confirmedAt`
  - Vue Admin 新增 `AI Confirmations` 面板：
    - 支持按家庭、用户、provider、model、purpose、subjectType 筛选。
    - 展示确认 ID、确认时间、家庭/用户/主体、provider/model/purpose。
    - 展示确认后生成的正式记录、报告、待办 ID 列表。
    - 仅展示 `draftPreview`，不展示完整敏感 AI 草稿。
  - 当前后台鉴权仍是占位能力，正式发布前必须把该接口纳入管理员登录、RBAC 和后台操作审计。
  - 新增后端测试覆盖：
    - 用户确认 AI 草稿后，后台可查询到确认历史。
    - 可按 `familyId` 和 `purpose` 过滤。
    - 返回内容包含 `confirmationId`、正式资源 ID 列表和草稿摘要。

- Phase 5 第十八段：OCR/ASR 预处理器抽象与阿里云占位客户端
  - 后端新增 `AiPreprocessorClient` 抽象，`AiGatewayService` 不再直接内联 OCR/ASR 模拟文本。
  - 新增 `AliyunAiPreprocessorClient`：
    - 当前不接真实阿里云网络调用。
    - 当请求携带已识别文本时，直接使用该文本生成草稿，返回 `errorCode=OK`。
    - 当未携带文本且阿里云 OCR/ASR 未启用时，返回稳定错误码 `ALIYUN_PREPROCESSOR_DISABLED`。
    - 预留 `babydust.ai.aliyun.ocr.enabled`、`babydust.ai.aliyun.asr.enabled`、`babydust.ai.aliyun.access-key`、`babydust.ai.aliyun.access-key-ref` 配置。
  - 新增 `RuleBasedAiPreprocessorClient` 和 `CompositeAiPreprocessorClient`：
    - 阿里云预处理不可用时，使用规则 fallback 文本继续生成可确认草稿。
    - fallback 结果保留上游错误码，便于后台排查真实失败原因。
  - `POST /api/v1/ai/ocr-report` 和 `POST /api/v1/ai/asr-record` 响应新增：
    - `fallbackUsed`
    - `errorCode`
    - `processedAt`
  - 小程序 AI 整理页同步展示预处理器 fallback 状态、错误码和处理时间。
  - 新增测试覆盖：
    - 传入识别文本时不走 fallback。
    - 阿里云未启用且无识别文本时走规则 fallback，并返回稳定错误码。
    - OCR/ASR 预处理结果仍继续进入 AI Gateway 草稿生成流程。

- Phase 5 第十九段：后台 Preprocessor 配置中心
  - 后端 `POST /api/v1/admin/ai-configs` 新增 `preprocessor` 配置类型，用于维护阿里云 OCR/ASR 预处理器配置。
  - `preprocessor` 配置 JSON 必须是对象，并要求：
    - `service` 只能是 `ocr` 或 `asr`。
    - `preprocessor` 与服务类型匹配：`ocr` 对应 `aliyun_ocr`，`asr` 对应 `aliyun_asr`。
    - `credentialRef` 必须使用 `env:`、`kms:` 或 `secret:` 引用，不能保存真实 access key。
    - `endpoint` 如填写，必须是 HTTP(S) URL。
    - `enabled` 如填写，必须是 boolean。
    - 继续复用密钥字段扫描规则，拒绝 `apiKey`、`secret`、`token`、`authorization`、`password` 等内联敏感字段。
  - 后台 Vue Admin 的 AI Gateway 配置面板新增 `preprocessor` 类型：
    - 支持结构化维护 service、preprocessor、credentialRef、region、endpoint、enabled。
    - 自动生成 canonical `configJson`。
    - 列表侧展示服务类型、预处理器、密钥引用、区域、endpoint 和启用状态摘要。
  - 新增测试覆盖：
    - 合法阿里云 OCR/ASR preprocessor 配置可保存。
    - 可按 `configType=preprocessor` 查询。
    - 裸写 credentialRef、OCR/ASR 类型不匹配、内联 API key 均被服务端拒绝。

- Phase 5 第二十段：Preprocessor 后台配置接入运行时
  - `AliyunAiPreprocessorClient` 运行时会读取后台 `active` 状态的 `preprocessor` 配置。
  - 运行时按 `preprocessor` 匹配当前请求：
    - `aliyun_ocr` 用于 OCR 报告预处理。
    - `aliyun_asr` 用于 ASR 语音预处理。
  - 只有 active 配置中 `enabled=true` 时，才进入阿里云预处理器占位路径；否则继续返回 `ALIYUN_PREPROCESSOR_DISABLED` 并走规则 fallback。
  - active 配置可提供 `credentialRef`，用于替代 application properties 中的默认密钥引用。
  - 当前仍不发真实阿里云请求：
    - active 且 enabled=true 但密钥无法解析时，返回 `ALIYUN_ACCESS_KEY_MISSING`。
    - active 且 enabled=true 且密钥可解析后，仍返回 `ALIYUN_CLIENT_NOT_IMPLEMENTED`，等待后续真实阿里云 SDK/HTTP 实现。
  - 没有 active 配置时，继续沿用 `babydust.ai.aliyun.ocr.enabled`、`babydust.ai.aliyun.asr.enabled` 等 application properties。
  - 新增测试覆盖：
    - 无 active 配置时沿用默认关闭行为。
    - active `preprocessor` 配置可改变运行时错误码，证明后台配置已参与 OCR/ASR 预处理器决策。

- Phase 5 第二十一段：OCR/ASR 预处理审计日志
  - 后端新增 `ai_preprocess_audit_logs` 表，用于记录 OCR/ASR 预处理器调用轨迹。
  - 审计字段包括：
    - `purpose`
    - `provider`
    - `preprocessor`
    - `fileUrlPreview`
    - `textLength`
    - `fallbackUsed`
    - `errorCode`
    - `latencyMs`
    - `status`
  - 隐私边界：
    - 不保存完整报告文字。
    - 不保存原始图片或语音内容。
    - 只保存脱敏/截断后的文件地址摘要和识别文本长度。
  - `AiGatewayService` 在 `ocrReport`、`asrRecord` 完成预处理后写入审计日志；下游 AI 草稿生成仍继续写 `ai_audit_logs`。
  - 后台新增 `GET /api/v1/admin/ai-preprocess-audit-logs`：
    - 支持 `purpose`、`provider`、`preprocessor`、`fallbackUsed`、`errorCode`、`limit` 筛选。
    - 按创建时间倒序返回最近记录。
  - Vue Admin 新增 `AI Preprocess` 面板：
    - 展示 OCR/ASR 预处理器日志。
    - 支持按 purpose、provider、preprocessor、fallback、errorCode 过滤。
    - 展示错误码、fallback 状态、文本长度、耗时和文件地址摘要。
  - 新增测试覆盖：
    - OCR fallback 预处理会写入 `ai_preprocess_audit_logs`。
    - 后台可按 preprocessor 和 errorCode 查询预处理审计日志。

- Phase 5 第二十二段：AI 问答安全边界雏形
  - 后端新增 `POST /api/v1/ai/qa`。
  - 问答能力只允许：
    - 孕期科普信息整理。
    - 用户记录整理建议。
    - 复诊/产检沟通问题清单。
  - 问答能力不允许：
    - 判断是否正常。
    - 输出诊断结论。
    - 推荐处方药、停药、换药或调整剂量。
    - 对急症风险给出替代就医的结论。
  - 响应字段包括：
    - `purpose`
    - `provider`
    - `model`
    - `answerType`
    - `blocked`
    - `riskLevel`
    - `errorCode`
    - `answer`
    - `suggestedQuestions`
    - `warnings`
    - `generatedAt`
  - 高风险问题返回：
    - `answerType=safety`
    - `blocked=true`
    - `errorCode=HIGH_RISK_BLOCKED`
    - 安全回复和就医/问诊问题清单。
  - 低风险问题返回：
    - `answerType=education`
    - `blocked=false`
    - `errorCode=OK`
    - 通用科普整理建议和复诊沟通问题清单。
  - QA 调用会写入现有 `ai_audit_logs`：
    - `purpose=qa`
    - `inputType=question`
    - 高风险问题可按 `blocked=true`、`riskLevel=high`、`errorCode=HIGH_RISK_BLOCKED` 查询。
  - 小程序 API client 新增 `askAiQuestion` 和 `AiQaResponse` 类型，后续可接入正式问答页面。
  - 新增测试覆盖：
    - 低风险问答返回 education 答案。
    - 高风险用药/急症问题被阻断。
    - QA 审计日志可通过后台审计接口查询。

- Phase 5 第二十三段：小程序 AI 安全问答入口
  - 小程序 `AI 整理` 页面新增 `安全问答` 模式。
  - 前端通过 `askAiQuestion` 调用 `POST /api/v1/ai/qa`，并传入当前语言环境。
  - 问答结果只展示为安全回答，不进入 AI 草稿确认保存流程。
  - 页面展示：
    - provider/model/generatedAt。
    - answerType、riskLevel、errorCode。
    - 安全回答正文。
    - 可带给医生的问题清单。
    - warnings 安全提示。
  - 高风险问题使用 blocked 风格提示，保持“不得替代医生诊疗”的产品边界。
  - 三语文案已补齐：简体中文、繁体中文、英文。

- Phase 5 第二十四段：AI 问答安全策略后台配置
  - 后台 AI 配置新增 `qa_policy` 类型。
  - `qa_policy` 只允许配置问答文案，不允许关闭诊断、急症、用药决策拦截。
  - 服务端要求 `safetyPolicy=no_medical_decision`。
  - 配置可包含：
    - `educationAnswer`：低风险科普/整理回答模板。
    - `safetyAnswer`：高风险安全回复模板。
    - `suggestedQuestions`：低风险问题下的医生沟通清单。
    - `safetyQuestions`：高风险问题下的就医/问诊清单。
    - `warnings`：额外安全提示。
    - `locales`：按 `zh-CN`、`zh-TW`、`en-US` 维护多语言文案。
  - `AiGatewayService` 运行时读取 active `qa_policy`，无配置或配置解析失败时回退到内置安全模板。
  - 后台 Vue Admin 新增 `qa_policy` 模板，方便运营创建三语问答安全策略。
  - 新增测试覆盖：
    - 合法 `qa_policy` 可保存。
    - 非 `no_medical_decision` 安全策略会被拒绝。
    - active `qa_policy` 会影响低风险和高风险问答返回内容。

- Phase 5 第二十五段：AI 问答审计增强
  - `ai_audit_logs` 新增 QA 合规追踪字段：
    - `policyVersion`：问答使用的策略版本。
    - `policyConfigured`：是否命中后台 active `qa_policy`。
    - `safetyPolicy`：当前固定为 `no_medical_decision`。
    - `riskReasons`：稳定风险原因，如 `low_risk`、`urgent_symptom`、`medication_decision`、`diagnosis`。
  - QA 审计仍只保存脱敏后的 `inputPreview`，不保存完整问题、完整回答或敏感原文。
  - `GET /api/v1/admin/ai-audit-logs` 新增筛选：
    - `purpose`
    - `policyConfigured`
    - `safetyPolicy`
  - Vue Admin 的 `AI Audit` 面板新增：
    - purpose 筛选。
    - QA policy 是否配置筛选。
    - safety policy 筛选。
    - policy version、safety policy、risk reasons 展示。
  - 新增测试覆盖：
    - QA 审计写入 active `qa_policy` 版本。
    - 高风险问答审计写入稳定风险原因。
    - 后台审计可按 `purpose=qa`、`policyConfigured=true`、`safetyPolicy=no_medical_decision` 查询。

- Phase 5 第二十六段：AI 调用频控与防滥用雏形
  - 后端新增 `AiRateLimiter`，覆盖：
    - `POST /api/v1/ai/extract-record`
    - `POST /api/v1/ai/extract-report`
    - `POST /api/v1/ai/ocr-report`
    - `POST /api/v1/ai/asr-record`
    - `POST /api/v1/ai/qa`
  - 默认按调用主体和操作类型分别限流：
    - QA：默认每 60 秒 30 次。
    - 草稿抽取：默认每 60 秒 20 次。
    - OCR/ASR 预处理：默认每 60 秒 10 次。
  - 调用主体优先使用 `X-Dev-User-Id`，其次使用 `X-Forwarded-For` 首个 IP，最后使用 remote address。
  - 超限返回 HTTP `429`，统一错误码 `RATE_LIMITED`。
  - 当前实现为单机内存滑窗，适合开发和首版雏形；正式多实例发布前需要切换为 Redis 共享计数器。
  - 新增测试覆盖：
    - QA 在窗口内超限后抛出稳定限流异常。
    - 不同主体、不同操作互不影响。
    - 关闭限流后请求放行。
    - Web 接口超限返回 `RATE_LIMITED`。

- Phase 5 第二十七段：AI 失败兜底与前端错误提示优化
  - 小程序 API client 新增 `ApiRequestError`，保留：
    - 后端稳定错误码。
    - HTTP 状态码。
    - requestId。
  - 网络失败统一转为 `NETWORK_ERROR`。
  - 小程序 `AI 整理` 页面新增 AI 错误码三语映射，覆盖：
    - `RATE_LIMITED`
    - `NETWORK_ERROR`
    - `MODEL_CLIENT_EXCEPTION`
    - `DEEPSEEK_API_KEY_MISSING`
    - `DEEPSEEK_HTTP_ERROR`
    - `DEEPSEEK_INVALID_JSON`
    - `DEEPSEEK_INVALID_DRAFT_SCHEMA`
    - `DEEPSEEK_EMPTY_CONTENT`
    - `ALIYUN_PREPROCESSOR_DISABLED`
    - `ALIYUN_ACCESS_KEY_MISSING`
    - `ALIYUN_CLIENT_NOT_IMPLEMENTED`
  - OCR/ASR 预处理发生 fallback 时，页面会显示明确人工核对提示，避免用户误以为已完成真实识别。
  - 设计边界：
    - 前端只显示用户可理解的兜底提示，不展示内部异常栈或供应商原始错误。
    - 用户输入内容不会因 AI 失败自动保存。

- 接入 DeepSeek API：
  - 通过 AI Gateway 调用。
  - 增加 provider、model、prompt version 配置。
  - 增加超时、重试、降级和错误兜底。
- 接入真实阿里云 OCR：
  - 报告图片上传 OSS。
  - OCR 提取文本。
  - DeepSeek 将 OCR 文本结构化为报告草稿。
- 接入真实阿里云 ASR：
  - 语音上传或临时文件识别。
  - ASR 转文字。
  - AI 拆分为记录、待办、提醒草稿。
- 增加 AI 调用审计：
  - 用户、家庭、token、费用、错误码。
  - 输出脱敏日志。
  - 用户确认时间和确认后的正式记录 ID。
- 增加用户自配模型：
  - 用户配置 OpenAI-compatible endpoint、api key、model。
  - 私密配置加密保存。
  - 管理后台控制默认模型和启用场景。
