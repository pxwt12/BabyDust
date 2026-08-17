# API Contract v0.1

Base URL: `/api/v1`

All responses use:

```json
{
  "requestId": "uuid",
  "success": true,
  "data": {},
  "error": null
}
```

Initial endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/auth/wechat-login` | Exchange a WeChat login code for an app session. Current implementation uses a dev pseudo-openid. |
| GET/POST | `/families` | List and create family spaces. |
| POST | `/families/{id}/invites` | Create a family invitation code placeholder. |
| GET/POST | `/profiles/mothers` | List and create mother profiles. |
| GET/POST | `/profiles/pregnancies` | List and create pregnancy profiles. |
| GET/POST | `/profiles/babies` | List and create baby profiles. |
| GET/POST | `/records` | List and create JSON-backed records. |
| GET | `/records/types` | List supported record types and required payload fields. |
| GET/POST | `/reports` | List and create medical report drafts/records. |
| GET/POST | `/todos` | List and create todos. |
| GET/POST | `/reminders` | List and create reminders. |
| GET | `/home/summary` | Stage-aware home summary. |
| GET | `/analytics/series` | Initial analytics series endpoint. |
| POST | `/ai/extract-record` | AI draft extraction from text. |
| POST | `/ai/extract-report` | AI report draft extraction from OCR text. |
| POST | `/ai/ocr-report` | OCR preprocessing for report images through the AI preprocessor abstraction. Current implementation accepts supplied text or falls back to demo text when Alibaba Cloud OCR is disabled, then returns an AI report draft. |
| POST | `/ai/asr-record` | ASR preprocessing for voice input through the AI preprocessor abstraction. Current implementation accepts supplied transcript text or falls back to demo text when Alibaba Cloud ASR is disabled, then returns AI record/todo drafts. |
| POST | `/ai/qa` | AI Q&A safety-bound response. Allows education, organization and follow-up question preparation; blocks diagnosis, emergency and medication-decision questions. |
| POST | `/ai/confirm-draft` | Confirm a user-reviewed AI draft and persist formal records, reports and todos for a family subject. |
| GET | `/ai/draft-confirmations` | List recent AI draft confirmation logs for a family. Requires `familyId`. |
| GET | `/ai/audit-logs` | Development-stage AI audit log listing. Keep only for local verification and remove or protect before production. |
| POST | `/admin/content` | Admin content draft placeholder. |
| POST | `/admin/templates` | Admin template draft placeholder. |
| POST | `/admin/i18n` | Admin i18n draft placeholder. |
| GET/POST | `/admin/ai-configs` | Admin AI provider, prompt, schema, preprocessor and Q&A policy configuration entries. Supports optional `configType=provider|prompt|schema|preprocessor|qa_policy` filter. |
| GET | `/admin/ai-audit-logs` | Admin AI audit log listing. Supports optional `purpose`, `provider`, `model`, `riskLevel`, `blocked`, `fallbackUsed`, `policyConfigured`, `safetyPolicy`, `errorCode` and `limit` filters. |
| GET | `/admin/ai-preprocess-audit-logs` | Admin OCR/ASR preprocess audit log listing. Supports optional `purpose`, `provider`, `preprocessor`, `fallbackUsed`, `errorCode` and `limit` filters. |
| GET | `/admin/ai-draft-confirmations` | Admin AI draft confirmation history listing. Supports optional `familyId`, `userId`, `provider`, `model`, `purpose`, `subjectType` and `limit` filters. |

Security notes:

- `POST /auth/wechat-login` returns an opaque bearer token with a 30-day expiry in the current skeleton.
- Client requests to protected user/family endpoints must send `Authorization: Bearer <token>`.
- `X-User-Id` is accepted only when `babydust.security.dev-user-header-enabled=true`; the default is disabled and production must keep it disabled.
- Production must replace the current dev pseudo-openid with WeChat code-to-openid verification.
- All family-scoped read/write endpoints must call server-side family membership checks.
- AI extraction endpoints return drafts. Clients must show the draft for explicit user confirmation before creating records or reports.
- `POST /ai/confirm-draft` accepts only drafts with `source=ai_draft`, `needsUserConfirmation=true` and `blocked=false`; it re-checks family membership, subject ownership, record payloads and report indicators before persisting formal data.
- AI draft confirmations are written to `ai_draft_confirmations` with user, family, subject, provider/model/purpose, a short draft preview and the created record/report/todo IDs. Full sensitive draft payloads are not stored in confirmation logs.
- AI draft responses include provider/model metadata, risk level, safety warnings, and `needsUserConfirmation=true`. High-risk medical decisions return `blocked=true` and no record draft.
- AI endpoints are protected by a first-pass server-side rate limiter. When the caller exceeds the configured window, the API returns HTTP `429` with error code `RATE_LIMITED`. Current implementation uses in-memory buckets and must be moved to Redis or another shared counter before multi-instance production deployment.
- Clients should preserve API `error.code` and map stable AI errors to localized user messages. Current mini program maps rate limiting, network failure, DeepSeek configuration/HTTP/parse errors, and Alibaba OCR/ASR preprocessor errors without exposing provider internals to users.
- AI Q&A responses are safety-bound. `answerType=education` may provide general organization guidance and clinician question prompts; `answerType=safety` with `blocked=true` is used for diagnosis, emergency or medication-decision questions and must not provide medical conclusions.
- AI Q&A wording can be managed through active admin `qa_policy` config, but the server still enforces `safetyPolicy=no_medical_decision` and keeps diagnosis, emergency and medication-decision blocking outside operator-editable prompts. QA audit entries include `policyVersion`, `policyConfigured`, `safetyPolicy` and stable `riskReasons` metadata, while still storing only sanitized question previews.
- AI draft responses also include `providerConfigKey`, `promptVersion` and `schemaVersion`. The gateway resolves these from active admin AI config entries and falls back to application defaults when no active config exists.
- AI draft responses include `fallbackUsed`, `errorCode` and `rawOutputPreview`. Current Phase 5 uses the rule-based fallback client with `MODEL_CLIENT_NOT_CONFIGURED`; real provider errors must use stable error codes without leaking secrets or full raw model output.
- DeepSeek HTTP client scaffolding is present but disabled by default with `DEEPSEEK_ENABLED=false`. When enabled without a resolved API key, the gateway returns `DEEPSEEK_API_KEY_MISSING` and falls back to local rule extraction.
- DeepSeek draft parsing accepts only JSON object content with `records`, `todos` and `reports` arrays. Invalid model output returns stable errors such as `DEEPSEEK_INVALID_JSON`, `DEEPSEEK_INVALID_DRAFT_SCHEMA` or `DEEPSEEK_EMPTY_CONTENT`, then falls back to local rule extraction.
- AI previews are sanitized before being returned or audited. Phone numbers, emails, ID-card-like values, bearer tokens and API-key-like values are redacted. AI audit logs include reserved token/cost fields: `promptTokens`, `completionTokens`, `totalTokens`, `costCurrency`, `estimatedCost`.
- When DeepSeek returns `usage`, the gateway extracts prompt/completion/total tokens and estimates cost from active provider config `configJson.pricing`, for example `{"pricing":{"currency":"CNY","promptPer1K":"0.002","completionPer1K":"0.008"}}`. Missing pricing keeps estimated cost at `0`.
- OCR/ASR preprocessing endpoints accept OSS file URLs plus optional recognized/transcript text, return drafts only, and must not persist formal records before user confirmation. Responses include `fallbackUsed`, `errorCode` and `processedAt`; current Alibaba Cloud clients are placeholders and read active admin `preprocessor` config for `enabled` and `credentialRef`, then return stable errors such as `ALIYUN_PREPROCESSOR_DISABLED`, `ALIYUN_ACCESS_KEY_MISSING` or `ALIYUN_CLIENT_NOT_IMPLEMENTED` before the rule-based fallback supplies demo text.
- Admin AI config entries must not store raw API keys. Use `credentialRef` or similar secret references in `configJson`, with actual secrets stored in environment variables or cloud secret management.
- The admin UI provides structured provider fields for model, credential reference, base URL and per-1K token pricing, and structured preprocessor fields for Alibaba Cloud OCR/ASR service, credential reference, region, endpoint and enabled status. `/admin/ai-configs` still stores the canonical payload as JSON so future providers can add advanced parameters without an API change.
- Admin AI audit log responses expose only sanitized previews and operational metadata. QA rows additionally expose policy and risk-classification metadata, not full answers or full original questions. Production must place `/admin/ai-audit-logs` behind admin authentication, RBAC and audit trails before public release.
- Admin preprocess audit log responses expose OCR/ASR operational metadata only: provider, preprocessor, fallback/error status, latency, text length and sanitized file URL preview. Full report text, original audio and image contents must not be stored in these audit logs.
- Admin AI draft confirmation responses expose confirmation IDs, family/user/subject IDs, provider/model/purpose, created record/report/todo ID JSON and short `draftPreview` only. Production must place `/admin/ai-draft-confirmations` behind admin authentication, RBAC and audit trails before public release.

Validation notes:

- Mother profiles require `familyId`; optional height and pre-pregnancy weight are range checked when present.
- Pregnancy profiles require `familyId`, `lmpDate` and `fetusCount`; `dueDate` defaults to `lmpDate + 280 days`.
- Baby profiles require `familyId`, `name` and `gender`; optional birth weight and length are range checked when present.
- Pregnancy records currently support `weight`, `blood_pressure`, `symptom`, `medication`, `supplement`, `fetal_movement`, `mood` and `note`.
- Record creation validates that the `subjectId` belongs to the submitted `familyId`, the `recordType` supports the submitted `subjectType`, and `payloadJson` contains the required fields.
- `GET /home/summary` returns a structured pregnancy summary when a pregnancy profile exists: due date, fetus count, gestational week display, pregnancy day, trimester, days until due, recent records, recent reports and today todos.
- `POST /admin/ai-configs` accepts only `provider`, `prompt`, `schema`, `preprocessor` and `qa_policy` config types, only `draft`, `active` and `archived` statuses, and requires `configJson` to be valid JSON object or array text.
- Provider AI config JSON must be an object with `model` and `credentialRef`. `credentialRef` must start with `env:`, `kms:` or `secret:`; inline secret fields such as `apiKey`, `secret`, `token`, `authorization` or `password` are rejected. Optional `pricing.promptPer1K` and `pricing.completionPer1K` must be non-negative numbers.
- Prompt AI config JSON must be an object with `purpose` in `record_extraction`, `report_extraction`, `ocr_report` or `asr_record`. Optional `safetyPolicy` must be `draft_only`, and `systemPrompt` must stay within 4000 characters.
- Schema AI config JSON must be an object schema with non-empty `required` draft arrays. Required entries may only be `records`, `todos` or `reports`; matching properties must be arrays when present.
- Preprocessor AI config JSON must be an object with `service` in `ocr` or `asr`, matching `preprocessor` value `aliyun_ocr` or `aliyun_asr`, and a non-inline `credentialRef`. Optional `endpoint` must be an HTTP(S) URL and optional `enabled` must be boolean.
- Q&A policy config JSON must be an object with `safetyPolicy=no_medical_decision`. It may include top-level wording or localized entries under `locales`, with bounded `educationAnswer`, `safetyAnswer`, `suggestedQuestions`, `safetyQuestions` and `warnings` fields. Inline secret fields remain rejected.
