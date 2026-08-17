const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
export const DEMO_USER_ID = '11111111-1111-1111-1111-111111111111'
export const DEMO_FAMILY_ID = '22222222-2222-2222-2222-222222222222'
export const DEMO_PREGNANCY_ID = '33333333-3333-3333-3333-333333333333'
export const DEMO_TOKEN = 'bd_demo_seed_token'

type ApiRequestOptions = {
  method?: UniApp.RequestOptions['method']
  data?: UniApp.RequestOptions['data']
  header?: Record<string, string>
}

export class ApiRequestError extends Error {
  code: string
  statusCode: number
  requestId?: string

  constructor(code: string, message: string, statusCode: number, requestId?: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.statusCode = statusCode
    this.requestId = requestId
  }
}

export type PregnancySummary = {
  id: string
  lmpDate: string
  dueDate: string
  fetusCount: number
  status: string
  gestationalWeeks: number
  gestationalDays: number
  gestationalWeekDisplay: string
  pregnancyDay: number
  trimester: number
  daysUntilDue: number
}

export type RecordSummary = {
  id: string
  subjectType: string
  subjectId: string
  recordType: string
  occurredAt: string
  payloadJson: string
}

export type ReportSummary = {
  id: string
  subjectType: string
  subjectId: string
  reportType: string
  title: string
  examinedAt: string
  indicatorsJson?: string
}

export type TodoSummary = {
  id: string
  title: string
  category: string
  subjectType?: string
  subjectId?: string
  dueAt?: string
  status: string
}

export type ReminderSummary = {
  id: string
  title: string
  scene: string
  subjectType?: string
  subjectId?: string
  triggerAt: string
  status: string
}

export type MetricSeries = {
  metric: string
  unit: string
  points: Array<{
    occurredAt: string
    value: number
  }>
}

export type AnalyticsOverview = {
  prenatalPlan: {
    total: number
    completed: number
    completionRate: number
  }
  medicationSupplement: {
    medicationRecords: number
    supplementRecords: number
    totalRecords: number
  }
}

export type ExportPackage<T> = {
  exportType: string
  format: string
  generatedAt: string
  rowCount: number
  rows: T[]
  csvContent: string
}

export type RecordExportRow = {
  id: string
  subjectType: string
  subjectId: string
  recordType: string
  occurredAt: string
  payloadJson: string
  privacyLevel: string
}

export type ReportExportRow = {
  id: string
  subjectType: string
  subjectId: string
  reportType: string
  title: string
  examinedAt: string
  indicatorsJson: string
}

export type AiDraftResponse = {
  status: string
  purpose: string
  provider: string
  model: string
  providerConfigKey: string
  promptVersion: string
  schemaVersion: string
  inputType: string
  source: string
  needsUserConfirmation: boolean
  blocked: boolean
  riskLevel: string
  fallbackUsed: boolean
  errorCode: string
  rawOutputPreview: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  costCurrency: string
  estimatedCost: number
  records: Array<Record<string, unknown>>
  todos: Array<Record<string, unknown>>
  reports: Array<Record<string, unknown>>
  warnings: string[]
  generatedAt: string
}

export type AiPreprocessResponse = {
  purpose: string
  provider: string
  preprocessor: string
  fileUrl: string
  text: string
  fallbackUsed: boolean
  errorCode: string
  draft: AiDraftResponse
  warnings: string[]
  processedAt: string
}

export type AiQaResponse = {
  purpose: string
  provider: string
  model: string
  answerType: string
  blocked: boolean
  riskLevel: string
  errorCode: string
  answer: string
  suggestedQuestions: string[]
  warnings: string[]
  generatedAt: string
}

export type AiDraftConfirmationResult = {
  status: string
  confirmationId: string
  records: RecordSummary[]
  reports: ReportSummary[]
  todos: TodoSummary[]
  confirmedAt: string
}

export type AiDraftConfirmationLog = {
  id: string
  familyId: string
  userId: string
  subjectType: string
  subjectId: string
  provider: string
  model: string
  purpose: string
  draftPreview: string
  recordIdsJson: string
  reportIdsJson: string
  todoIdsJson: string
  confirmedAt: string
}

export type HomeSummary = {
  stage: 'not_set' | 'pregnancy'
  pregnancy: PregnancySummary | null
  recentRecords: RecordSummary[]
  recentReports: ReportSummary[]
  upcomingTodos: TodoSummary[]
  prenatalPlanProgress: {
    total: number
    completed: number
    completionRate: number
  }
  keyMetrics: MetricSeries[]
  generatedAt: string
}

export type AuthSession = {
  token: string
  userId: string
  nickname: string
  expiresAt: string
}

export type Family = {
  id: string
  name: string
  ownerUserId: string
}

export type BabyProfile = {
  id: string
  familyId: string
  pregnancyId?: string
  name: string
  gender: string
  birthDateTime?: string
  birthWeightKg?: number
  birthLengthCm?: number
}

export type PregnancyProfile = {
  id: string
  familyId: string
  lmpDate: string
  dueDate: string
  fetusCount: number
  status: string
}

export type RecordTypeDefinition = {
  type: string
  subjectType: string
  valueKind: string
  requiredFields: string[]
}

export type IndicatorDefinition = {
  code: string
  name: string
  unit: string
  reportType: string
  valueKind: string
}

export type CreateRecordInput = {
  familyId: string
  subjectType: string
  subjectId: string
  recordType: string
  occurredAt: string
  payloadJson: string
}

export type CreateReportInput = {
  familyId: string
  subjectType: string
  subjectId: string
  reportType: string
  title: string
  examinedAt: string
  indicatorsJson: string
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${path}`,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(options.header || {})
      },
      success: (res) => {
        const body = res.data as { requestId?: string; success: boolean; data: T; error?: { code?: string; message: string } }
        if (body?.success) {
          resolve(body.data)
        } else {
          reject(new ApiRequestError(body?.error?.code || 'REQUEST_FAILED', body?.error?.message || 'Request failed', res.statusCode, body?.requestId))
        }
      },
      fail: (error) => {
        reject(new ApiRequestError('NETWORK_ERROR', error.errMsg || 'Network request failed', 0))
      }
    })
  })
}

export function apiErrorCode(error: unknown): string {
  return error instanceof ApiRequestError ? error.code : ''
}

export function authHeaders(): Record<string, string> {
  const token = uni.getStorageSync('token') as string | undefined
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export function getStoredFamilyId(): string {
  return (uni.getStorageSync('familyId') as string | undefined) ?? ''
}

export function getStoredPregnancyId(): string {
  return (uni.getStorageSync('pregnancyId') as string | undefined) ?? ''
}

export function storeSession(session: AuthSession) {
  uni.setStorageSync('token', session.token)
  uni.setStorageSync('userId', session.userId)
  uni.setStorageSync('nickname', session.nickname)
}

export function storeFamily(family: Family) {
  uni.setStorageSync('familyId', family.id)
  uni.setStorageSync('familyName', family.name)
}

export function storePregnancy(pregnancy: PregnancyProfile) {
  uni.setStorageSync('pregnancyId', pregnancy.id)
  uni.setStorageSync('lmpDate', pregnancy.lmpDate)
}

export function clearLocalSession() {
  for (const key of ['token', 'userId', 'nickname', 'familyId', 'familyName', 'pregnancyId', 'lmpDate']) {
    uni.removeStorageSync(key)
  }
}

export function useDemoSeedSession() {
  uni.setStorageSync('token', DEMO_TOKEN)
  uni.setStorageSync('userId', DEMO_USER_ID)
  uni.setStorageSync('nickname', '林小满')
  uni.setStorageSync('familyId', DEMO_FAMILY_ID)
  uni.setStorageSync('familyName', '小满和阿哲的家')
  uni.setStorageSync('pregnancyId', DEMO_PREGNANCY_ID)
}

export function wechatLogin(code: string, nickname?: string): Promise<AuthSession> {
  return apiRequest<AuthSession>('/auth/wechat-login', {
    method: 'POST',
    data: { code, nickname }
  })
}

export function getWeixinLoginCode(): Promise<string> {
  return new Promise((resolve) => {
    uni.login({
      provider: 'weixin',
      success: (result) => {
        resolve(result.code || `dev-${Date.now()}`)
      },
      fail: () => {
        resolve(`dev-${Date.now()}`)
      }
    })
  })
}

export function listFamilies(): Promise<Family[]> {
  return apiRequest<Family[]>('/families', {
    header: authHeaders()
  })
}

export function createFamily(name: string): Promise<Family> {
  return apiRequest<Family>('/families', {
    method: 'POST',
    data: { name },
    header: authHeaders()
  })
}

export function listPregnancies(familyId: string): Promise<PregnancyProfile[]> {
  return apiRequest<PregnancyProfile[]>(`/profiles/pregnancies?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function createPregnancy(input: { familyId: string; lmpDate: string; dueDate?: string; fetusCount: number }): Promise<PregnancyProfile> {
  return apiRequest<PregnancyProfile>('/profiles/pregnancies', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function listBabies(familyId: string): Promise<BabyProfile[]> {
  return apiRequest<BabyProfile[]>(`/profiles/babies?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function createBaby(input: { familyId: string; pregnancyId?: string; name: string; gender: string; birthDateTime?: string; birthWeightKg?: number; birthLengthCm?: number }): Promise<BabyProfile> {
  return apiRequest<BabyProfile>('/profiles/babies', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export async function bootstrapPregnancySpace(input: { nickname: string; familyName: string; lmpDate: string; fetusCount: number }): Promise<{ session: AuthSession; family: Family; pregnancy: PregnancyProfile }> {
  const loginCode = await getWeixinLoginCode()
  const session = await wechatLogin(loginCode, input.nickname)
  storeSession(session)
  const existingFamilies = await listFamilies()
  const family = existingFamilies[0] ?? await createFamily(input.familyName)
  storeFamily(family)
  const existingPregnancies = await listPregnancies(family.id)
  const pregnancy = existingPregnancies[0] ?? await createPregnancy({
    familyId: family.id,
    lmpDate: input.lmpDate,
    fetusCount: input.fetusCount
  })
  storePregnancy(pregnancy)
  return { session, family, pregnancy }
}

export function getHomeSummary(familyId: string): Promise<HomeSummary> {
  return apiRequest<HomeSummary>(`/home/summary?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function getRecordTypes(): Promise<RecordTypeDefinition[]> {
  return apiRequest<RecordTypeDefinition[]>('/records/types')
}

export function getRecords(familyId: string, options: { subjectType?: string; subjectId?: string; recordType?: string } = {}): Promise<RecordSummary[]> {
  const params = new URLSearchParams({ familyId })
  if (options.subjectType) {
    params.set('subjectType', options.subjectType)
  }
  if (options.subjectId) {
    params.set('subjectId', options.subjectId)
  }
  if (options.recordType) {
    params.set('recordType', options.recordType)
  }
  return apiRequest<RecordSummary[]>(`/records?${params.toString()}`, {
    header: authHeaders()
  })
}

export function createRecord(input: CreateRecordInput): Promise<RecordSummary> {
  return apiRequest<RecordSummary>('/records', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function getRecord(recordId: string): Promise<RecordSummary> {
  return apiRequest<RecordSummary>(`/records/${encodeURIComponent(recordId)}`, {
    header: authHeaders()
  })
}

export function updateRecord(recordId: string, input: { occurredAt: string; payloadJson: string }): Promise<RecordSummary> {
  return apiRequest<RecordSummary>(`/records/${encodeURIComponent(recordId)}`, {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function deleteRecord(recordId: string): Promise<{ deleted: boolean; id: string }> {
  return apiRequest<{ deleted: boolean; id: string }>(`/records/${encodeURIComponent(recordId)}`, {
    method: 'DELETE',
    header: authHeaders()
  })
}

export function getTodos(familyId: string, options: { subjectType?: string; subjectId?: string } = {}): Promise<TodoSummary[]> {
  const params = new URLSearchParams({ familyId })
  if (options.subjectType) {
    params.set('subjectType', options.subjectType)
  }
  if (options.subjectId) {
    params.set('subjectId', options.subjectId)
  }
  return apiRequest<TodoSummary[]>(`/todos?${params.toString()}`, {
    header: authHeaders()
  })
}

export function updateTodoStatus(todoId: string, status: 'pending' | 'done' | 'cancelled'): Promise<TodoSummary> {
  return apiRequest<TodoSummary>(`/todos/${encodeURIComponent(todoId)}/status`, {
    method: 'POST',
    data: { status },
    header: authHeaders()
  })
}

export function getReminders(familyId: string): Promise<ReminderSummary[]> {
  return apiRequest<ReminderSummary[]>(`/reminders?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function createReminder(input: { familyId: string; title: string; scene: string; subjectType?: string; subjectId?: string; triggerAt: string }): Promise<ReminderSummary> {
  return apiRequest<ReminderSummary>('/reminders', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function updateReminderStatus(reminderId: string, status: 'scheduled' | 'done' | 'cancelled'): Promise<ReminderSummary> {
  return apiRequest<ReminderSummary>(`/reminders/${encodeURIComponent(reminderId)}/status`, {
    method: 'POST',
    data: { status },
    header: authHeaders()
  })
}

export function getMetricSeries(familyId: string, metric: string): Promise<MetricSeries> {
  return apiRequest<MetricSeries>(`/analytics/series?familyId=${encodeURIComponent(familyId)}&metric=${encodeURIComponent(metric)}`, {
    header: authHeaders()
  })
}

export function getAnalyticsOverview(familyId: string): Promise<AnalyticsOverview> {
  return apiRequest<AnalyticsOverview>(`/analytics/overview?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function getIndicatorDefinitions(): Promise<IndicatorDefinition[]> {
  return apiRequest<IndicatorDefinition[]>('/reports/indicator-definitions')
}

export function exportPregnancyRecords(familyId: string): Promise<ExportPackage<RecordExportRow>> {
  return apiRequest<ExportPackage<RecordExportRow>>(`/exports/pregnancy-records?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function exportReports(familyId: string): Promise<ExportPackage<ReportExportRow>> {
  return apiRequest<ExportPackage<ReportExportRow>>(`/exports/reports?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}

export function createReport(input: CreateReportInput): Promise<ReportSummary> {
  return apiRequest<ReportSummary>('/reports', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function getReport(reportId: string): Promise<ReportSummary> {
  return apiRequest<ReportSummary>(`/reports/${encodeURIComponent(reportId)}`, {
    header: authHeaders()
  })
}

export function updateReport(reportId: string, input: { title: string; examinedAt: string; indicatorsJson: string }): Promise<ReportSummary> {
  return apiRequest<ReportSummary>(`/reports/${encodeURIComponent(reportId)}`, {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function deleteReport(reportId: string): Promise<{ deleted: boolean; id: string }> {
  return apiRequest<{ deleted: boolean; id: string }>(`/reports/${encodeURIComponent(reportId)}`, {
    method: 'DELETE',
    header: authHeaders()
  })
}

export function extractRecordDraft(input: { text: string; inputType?: string }): Promise<AiDraftResponse> {
  return apiRequest<AiDraftResponse>('/ai/extract-record', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function extractReportDraft(input: { text: string; inputType?: string }): Promise<AiDraftResponse> {
  return apiRequest<AiDraftResponse>('/ai/extract-report', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function ocrReportDraft(input: { fileUrl: string; text?: string }): Promise<AiPreprocessResponse> {
  return apiRequest<AiPreprocessResponse>('/ai/ocr-report', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function asrRecordDraft(input: { fileUrl: string; text?: string }): Promise<AiPreprocessResponse> {
  return apiRequest<AiPreprocessResponse>('/ai/asr-record', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function askAiQuestion(input: { question: string; locale?: string }): Promise<AiQaResponse> {
  return apiRequest<AiQaResponse>('/ai/qa', {
    method: 'POST',
    data: input,
    header: authHeaders()
  })
}

export function confirmAiDraft(input: { familyId: string; subjectType: string; subjectId: string; draft: AiDraftResponse }): Promise<AiDraftConfirmationResult> {
  return apiRequest<AiDraftConfirmationResult>('/ai/confirm-draft', {
    method: 'POST',
    data: {
      familyId: input.familyId,
      subjectType: input.subjectType,
      subjectId: input.subjectId,
      draft: JSON.stringify(input.draft)
    },
    header: authHeaders()
  })
}

export function getAiDraftConfirmations(familyId: string): Promise<AiDraftConfirmationLog[]> {
  return apiRequest<AiDraftConfirmationLog[]>(`/ai/draft-confirmations?familyId=${encodeURIComponent(familyId)}`, {
    header: authHeaders()
  })
}
