# Phase 5 收口总结与剩余风险清单

本文档用于 Phase 5「AI 与 OCR/ASR」阶段收口，汇总当前已完成能力、接口、后台配置、测试覆盖和公开发布前仍需补齐的风险项。

## 阶段结论

Phase 5 已完成可公开发布前的 AI 能力骨架：文字、图片 OCR、语音 ASR 都统一进入 AI Gateway，输出均保持为「草稿」，必须由用户确认后才写入正式记录、报告或待办。DeepSeek、阿里云 OCR/ASR 目前具备供应商抽象、配置中心、错误码、审计和降级路径，但真实外部调用默认关闭或仍为占位实现。

当前实现已经满足首版产品继续开发所需的架构边界：

- 业务代码不直接耦合 DeepSeek、阿里云 OCR 或阿里云 ASR。
- AI 输出不会自动入库，所有正式数据写入前会再次校验家庭权限、主体归属和 payload 结构。
- 高风险问答和医疗决策类输入会被服务端拦截，不能通过后台 prompt 配置绕过。
- 审计日志只保存脱敏摘要和运营元数据，不保存完整病历、完整报告、原始语音、原始图片或密钥。
- 小程序前端已经按三语展示 AI 失败兜底和预处理 fallback 提示，避免暴露供应商内部错误。

## 已完成功能清单

### AI Gateway

- 支持记录草稿抽取：`POST /api/v1/ai/extract-record`。
- 支持报告草稿抽取：`POST /api/v1/ai/extract-report`。
- 统一返回 `status=draft`、`source=ai_draft`、`needsUserConfirmation=true`。
- 返回 provider、model、promptVersion、schemaVersion、providerConfigKey 等运行时元数据。
- 返回 fallback、错误码、token、费用估算和安全 warnings。
- 高风险诊断、急症、用药决策输入在模型调用前拦截。

### DeepSeek 接入骨架

- 新增 `AiModelClient` 抽象。
- 新增本地规则 fallback 客户端。
- 新增 DeepSeek HTTP 客户端骨架，兼容 OpenAI-style `/chat/completions`。
- 新增组合客户端：DeepSeek 不可用、未启用、密钥缺失、HTTP 失败或响应无效时回退到规则客户端。
- 支持从 `usage` 解析 token，并按后台 provider pricing 估算费用。
- 支持模型输出基础 schema 校验和脱敏预览。

### OCR/ASR 预处理

- 支持 OCR 报告预处理：`POST /api/v1/ai/ocr-report`。
- 支持 ASR 语音预处理：`POST /api/v1/ai/asr-record`。
- 新增 `AiPreprocessorClient` 抽象。
- 新增阿里云 OCR/ASR 占位客户端。
- 新增规则 fallback 预处理客户端。
- OCR/ASR 预处理结果继续进入 AI Gateway 草稿生成流程。
- 返回 `preprocessor`、`fallbackUsed`、`errorCode`、`processedAt`。

### AI 草稿确认入库

- 新增 `POST /api/v1/ai/confirm-draft`。
- 只允许确认 `source=ai_draft`、`needsUserConfirmation=true`、`blocked=false` 的草稿。
- 确认时重新校验家庭成员权限、主体归属、记录类型、记录 payload、报告指标。
- 确认后可批量写入正式记录、检查报告和待办。
- 新增确认历史：`ai_draft_confirmations`。
- 新增家庭侧确认历史查询：`GET /api/v1/ai/draft-confirmations?familyId=...`。

### AI 安全问答

- 新增 `POST /api/v1/ai/qa`。
- 低风险问题只返回科普整理、记录建议和复诊沟通问题清单。
- 高风险问题返回 `answerType=safety`、`blocked=true`、`HIGH_RISK_BLOCKED`。
- 后台可配置三语问答文案，但不能关闭 `no_medical_decision` 安全策略。
- QA 审计记录 policyVersion、policyConfigured、safetyPolicy、riskReasons。

### 后台运营能力

- 新增 AI 配置中心：`GET/POST /api/v1/admin/ai-configs`。
- 支持配置类型：`provider`、`prompt`、`schema`、`preprocessor`、`qa_policy`。
- Provider 支持模型、密钥引用、baseUrl、pricing。
- Prompt/Schema 配置强制保持「仅生成草稿」边界。
- Preprocessor 支持阿里云 OCR/ASR 的 service、preprocessor、credentialRef、region、endpoint、enabled。
- QA policy 支持三语安全问答文案。
- 后台新增 AI Audit、AI Preprocess、AI Confirmations 面板。

### 前端小程序能力

- `AI 整理` 页面支持记录草稿、报告草稿、OCR 报告、语音记录和安全问答模式。
- 支持展示 provider/model、风险等级、错误码、fallback、token、费用估算和 warnings。
- 支持确认保存 AI 草稿。
- 支持展示确认结果 `confirmationId`。
- 支持三语 AI 错误提示。
- OCR/ASR fallback 时显示人工核对提示。

## 接口清单

| 方法 | 路径 | 当前用途 |
| --- | --- | --- |
| `POST` | `/api/v1/ai/extract-record` | 从文字生成记录、待办草稿。 |
| `POST` | `/api/v1/ai/extract-report` | 从报告文字生成检查报告草稿。 |
| `POST` | `/api/v1/ai/ocr-report` | OCR 预处理报告图片并生成报告草稿。 |
| `POST` | `/api/v1/ai/asr-record` | ASR 预处理语音并生成记录/待办草稿。 |
| `POST` | `/api/v1/ai/qa` | 安全边界内的 AI 问答。 |
| `POST` | `/api/v1/ai/confirm-draft` | 用户确认 AI 草稿后写入正式数据。 |
| `GET` | `/api/v1/ai/draft-confirmations` | 查询家庭最近 AI 草稿确认历史。 |
| `GET` | `/api/v1/ai/audit-logs` | 开发期 AI 审计查询，发布前需移除或收进后台权限。 |
| `GET` | `/api/v1/admin/ai-configs` | 后台查询 AI 配置。 |
| `POST` | `/api/v1/admin/ai-configs` | 后台创建 AI 配置。 |
| `GET` | `/api/v1/admin/ai-audit-logs` | 后台查询 AI 调用审计。 |
| `GET` | `/api/v1/admin/ai-preprocess-audit-logs` | 后台查询 OCR/ASR 预处理审计。 |
| `GET` | `/api/v1/admin/ai-draft-confirmations` | 后台查询 AI 草稿确认历史。 |

## 数据与审计清单

| 表/模型 | 用途 | 隐私边界 |
| --- | --- | --- |
| `ai_configs` | 保存 provider、prompt、schema、preprocessor、qa_policy 配置。 | 不保存真实 API Key，只保存 `credentialRef`。 |
| `ai_audit_logs` | 记录 AI 草稿和 QA 调用审计。 | 只保存脱敏 inputPreview，不保存完整输入或完整回答。 |
| `ai_preprocess_audit_logs` | 记录 OCR/ASR 预处理调用审计。 | 只保存文件地址摘要、文本长度和错误元数据。 |
| `ai_draft_confirmations` | 记录用户确认 AI 草稿后的正式入库轨迹。 | 只保存草稿短摘要和正式资源 ID。 |

## 单元测试与接口测试清单

后端测试当前覆盖以下 Phase 5 场景：

- `phaseFiveAiGatewayReturnsDraftsAndBlocksHighRiskMedicalDecisions`：草稿生成、高风险拦截、AI 审计、脱敏摘要。
- `phaseFiveOcrAndAsrPreprocessorsReturnDraftsAndAuditLogs`：OCR/ASR 预处理、下游草稿生成、预处理审计。
- `phaseFiveAdminAiConfigsPersistProviderPromptAndSchema`：AI 配置创建、查询和非法 JSON 校验。
- `phaseFiveAiGatewayUsesActiveAdminConfigVersions`：active provider/prompt/schema 运行时生效。
- `phaseFiveDeepSeekEnabledWithoutApiKeyFallsBackToRuleClient`：DeepSeek 启用但密钥缺失时 fallback。
- `phaseFiveAdminProviderConfigsRejectInlineSecretsAndInvalidPricing`：provider 配置安全校验。
- `phaseFiveAdminPromptAndSchemaConfigsValidateDraftSafetyContract`：prompt/schema 草稿安全边界校验。
- `phaseFiveConfirmAiDraftPersistsFormalRecordsReportsAndTodosOnlyAfterUserConfirmation`：AI 草稿确认入库闭环。
- `phaseFiveAdminAiAuditLogsSupportOperationalFilters`：后台 AI 审计过滤。
- `phaseFiveAdminPreprocessorConfigsValidateAliyunSafetyContract`：preprocessor 配置安全校验。
- `phaseFiveAdminQaPolicyConfigsValidateSafetyContract`：QA policy 安全策略校验。
- `phaseFiveAiQaAllowsEducationAndBlocksMedicalDecisions`：AI 问答低风险放行、高风险拦截。
- `phaseFiveAiQaUsesActiveAdminQaPolicyWithoutWeakeningSafetyBoundary`：后台 QA policy 生效且不能削弱安全边界。
- `phaseFiveAiRateLimitReturnsStableErrorCode`：AI Web 接口限流返回稳定错误码。
- `DeepSeekAiModelClientTest`：DeepSeek 响应解析、schema 校验、usage 解析、脱敏预览。
- `AiPreprocessorClientTest`：阿里云预处理占位行为、fallback 和 active 配置影响。
- `AiRateLimiterTest`：单机限流窗口、主体隔离、操作隔离和关闭限流。

最近一次完整验证结果：

- 后端 `mvn test`：`47 tests, 0 failures`。
- 小程序 `npm run typecheck`：通过。
- 小程序 `npm run build:mp-weixin`：通过。
- 管理后台 `npm run build`：通过。
- 文档与页面 JSON 编码检查：通过。

## 公开发布前剩余风险

### 必须发布前解决

- 管理后台鉴权仍是占位能力，必须补齐管理员登录、RBAC、操作审计和敏感操作权限控制。
- 开发期接口 `GET /api/v1/ai/audit-logs` 必须移除或强制后台权限保护。
- 真实微信登录仍需接入 code-to-openid 校验，不能继续使用开发期 pseudo-openid。
- AI/OCR/ASR 外部调用默认仍未真实启用，发布前必须明确首版是否以「手动文本+规则 fallback」上线，或完成真实 DeepSeek、阿里云 OCR/ASR 联调。
- 隐私政策、用户协议、敏感个人信息单独授权、医疗免责声明、注销和删除流程必须补齐。
- AI 医疗安全文案需要医学顾问或至少权威资料来源留痕，不能把模型回答包装成诊疗建议。

### 多实例上线前解决

- `AiRateLimiter` 当前为单机内存滑窗，多实例部署前必须切换为 Redis 共享计数。
- AI 审计和预处理审计需要增加分页、时间范围过滤和数据保留策略。
- OSS 文件访问需要私有桶、签名 URL、跨家庭授权校验和过期控制。
- AI token/费用估算需要明确价格版本、生效时间和币种，不得直接作为正式账单。

### 真实供应商接入前解决

- DeepSeek 真实调用需要补齐超时、重试、熔断、请求大小限制和完整错误码映射。
- DeepSeek 输出需要更严格的业务 schema 校验，不能只做 records/todos/reports 数组级校验。
- 阿里云 OCR/ASR 需要接入真实 SDK 或 HTTP 客户端、签名、region、endpoint、重试和错误码归一。
- 供应商密钥需要存储在环境变量、KMS 或 Secret Manager，后台只能保存引用。
- 模型输出和 OCR/ASR 识别文本进入日志前必须继续执行脱敏、截断和敏感字段扫描。

### 产品体验风险

- OCR/ASR 当前支持文件地址输入，正式用户体验需要接入 OSS 上传组件。
- AI 草稿确认目前首版默认写入当前孕期档案，后续需要支持显式选择妈妈、孕期、宝宝等主体。
- 小程序缺少自动化 UI 验收，三语长文本、弱网、上传失败、AI 失败仍需端到端测试。
- AI 问答入口需要更醒目的安全边界和「联系医生」引导，尤其是出血、腹痛、发热、用药等场景。

## 后续建议计划

下一阶段建议进入 Phase 6「运营后台与公开发布准备」，优先级如下：

1. 完成管理员登录、RBAC、后台操作审计。
2. 收口所有开发期接口，把 AI 审计、预处理审计、确认历史全部纳入后台权限。
3. 补齐用户协议、隐私政策、敏感个人信息授权、医疗免责声明、注销与删除。
4. 接入真实微信登录和小程序提审所需域名、HTTPS、业务域名配置。
5. 接入 OSS 上传和私有文件访问控制。
6. 决定首版 AI 真实接入范围：先启用 DeepSeek 文本草稿，OCR/ASR 可继续以手动文本和占位 fallback 上线；或同步完成阿里云 OCR/ASR 联调后再提审。
7. 增加小程序端到端验收：三语、弱网、上传失败、限流、AI 失败、草稿确认、跨家庭权限。

## 当前收口判断

Phase 5 的架构、接口和安全边界已经可以支撑后续公开发布准备工作。真正阻塞上线的不是 AI Gateway 骨架，而是后台权限、合规材料、真实登录、文件权限和外部供应商联调这些发布级能力。下一阶段应优先处理这些发布阻塞项，避免继续堆 AI 功能导致安全和合规债务扩大。
