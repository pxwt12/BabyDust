import type { IndicatorDefinition, RecordTypeDefinition } from '../api/client'

export type PrimitiveFormValue = number | string
export type StageKey = 'fertility' | 'pregnancy' | 'delivery' | 'postpartum' | 'baby'

type NumericRange = {
  min: number
  max: number
}

const numericRanges: Record<string, NumericRange> = {
  weightKg: { min: 20, max: 250 },
  systolic: { min: 60, max: 240 },
  diastolic: { min: 30, max: 160 },
  severity: { min: 1, max: 5 },
  count: { min: 0, max: 500 },
  durationMinutes: { min: 1, max: 1440 },
  cycleDay: { min: 1, max: 120 },
  temperatureC: { min: 34, max: 43 },
  durationSeconds: { min: 1, max: 600 },
  intervalMinutes: { min: 0, max: 120 },
  amountMl: { min: 0, max: 300 },
  babyWeightKg: { min: 0.2, max: 30 },
  heightCm: { min: 20, max: 120 }
}

export const stageRecordTypes: Record<StageKey, string[]> = {
  fertility: ['fertility_cycle', 'ovulation_test', 'intercourse', 'basal_temperature', 'fertility_supplement'],
  pregnancy: ['weight', 'blood_pressure', 'symptom', 'medication', 'supplement', 'fetal_movement', 'mood', 'note'],
  delivery: ['delivery_event', 'contraction', 'delivery_note'],
  postpartum: ['postpartum_lochia', 'postpartum_mood', 'postpartum_medication', 'postpartum_note'],
  baby: ['baby_feeding', 'baby_sleep', 'baby_diaper', 'baby_growth', 'baby_note']
}

export function recordTypeLabelKey(type: string): string {
  const keyByType: Record<string, string> = {
    weight: 'addWeight',
    blood_pressure: 'addBloodPressure',
    symptom: 'addSymptom',
    medication: 'addMedication',
    supplement: 'addMedication',
    fetal_movement: 'addFetalMovement',
    mood: 'mood',
    note: 'note',
    fertility_cycle: 'fertilityCycle',
    ovulation_test: 'ovulationTest',
    intercourse: 'intercourse',
    basal_temperature: 'basalTemperature',
    fertility_supplement: 'addSupplement',
    delivery_event: 'deliveryEvent',
    delivery_note: 'note',
    contraction: 'contraction',
    postpartum_lochia: 'postpartumLochia',
    postpartum_mood: 'postpartumMood',
    postpartum_medication: 'addMedication',
    postpartum_note: 'note',
    baby_feeding: 'babyFeeding',
    baby_sleep: 'babySleep',
    baby_diaper: 'babyDiaper',
    baby_growth: 'babyGrowth',
    baby_note: 'note'
  }
  return keyByType[type] ?? type
}

export function recordStage(recordType: string): StageKey | 'unknown' {
  for (const [stage, types] of Object.entries(stageRecordTypes) as Array<[StageKey, string[]]>) {
    if (types.includes(recordType)) {
      return stage
    }
  }
  return 'unknown'
}

export function recordStageLabelKey(recordType: string): string {
  const stage = recordStage(recordType)
  return stage === 'unknown' ? 'unknownStage' : stage
}

export function recordPayloadPreview(payloadJson: string): string {
  try {
    return Object.entries(parseJsonObject(payloadJson))
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join('  ')
  } catch {
    return payloadJson
  }
}

export function reportIndicatorsPreview(indicatorsJson: string | undefined): string {
  if (!indicatorsJson) {
    return ''
  }
  try {
    const parsed = JSON.parse(indicatorsJson) as { indicators?: Array<{ code: string; value: number | string }> }
    return (parsed.indicators ?? [])
      .map((indicator) => `${indicator.code}: ${indicator.value}`)
      .join('  ')
  } catch {
    return indicatorsJson
  }
}

export function fieldHintKey(field: string): string {
  const keyByField: Record<string, string> = {
    weightKg: 'rangeWeight',
    systolic: 'rangeSystolic',
    diastolic: 'rangeDiastolic',
    severity: 'rangeSeverity',
    count: 'rangeCount',
    durationMinutes: 'rangeDuration',
    cycleDay: 'rangeCycleDay',
    temperatureC: 'rangeTemperature',
    durationSeconds: 'rangeContractionDuration',
    intervalMinutes: 'rangeContractionInterval',
    amountMl: 'rangeAmountMl',
    babyWeightKg: 'rangeBabyWeight',
    heightCm: 'rangeBabyHeight'
  }
  return keyByField[field] ?? ''
}

export function isNumericRecordField(field: string): boolean {
  return Object.prototype.hasOwnProperty.call(numericRanges, field)
}

export function parseRecordFieldValue(field: string, value: string): PrimitiveFormValue {
  return isNumericRecordField(field) ? Number(value) : value.trim()
}

export function validateRecordValues(fields: string[], values: Record<string, string>): { valid: true } | { valid: false; messageKey: string } {
  for (const field of fields) {
    const value = String(values[field] ?? '').trim()
    if (!value) {
      return { valid: false, messageKey: 'completeRequired' }
    }
    if (isNumericRecordField(field)) {
      const number = Number(value)
      const range = numericRanges[field]
      if (!Number.isFinite(number) || (range && (number < range.min || number > range.max))) {
        return { valid: false, messageKey: fieldHintKey(field) || 'invalidNumber' }
      }
    }
  }
  return { valid: true }
}

export function validateIndicatorValues(definitions: IndicatorDefinition[], values: Record<string, string>): { valid: true } | { valid: false; messageKey: string } {
  const filledValues = definitions
    .map((indicator) => String(values[indicator.code] ?? '').trim())
    .filter(Boolean)
  if (filledValues.length === 0) {
    return { valid: false, messageKey: 'atLeastOneIndicator' }
  }
  if (filledValues.some((value) => !Number.isFinite(Number(value)))) {
    return { valid: false, messageKey: 'invalidNumber' }
  }
  return { valid: true }
}

export function parseJsonObject(json: string): Record<string, unknown> {
  const parsed = JSON.parse(json) as unknown
  return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {}
}

export function createRecordFormValues(payloadJson: string, fields: string[]): Record<string, string> {
  const parsed = parseJsonObject(payloadJson)
  return fields.reduce<Record<string, string>>((values, field) => {
    values[field] = parsed[field] === undefined || parsed[field] === null ? '' : String(parsed[field])
    return values
  }, {})
}

export function buildRecordPayload(existingPayloadJson: string, fields: string[], values: Record<string, string>): Record<string, PrimitiveFormValue | unknown> {
  const payload = parseJsonObject(existingPayloadJson)
  for (const field of fields) {
    payload[field] = parseRecordFieldValue(field, values[field] ?? '')
  }
  return payload
}

export function requiredFieldsForRecord(recordTypes: RecordTypeDefinition[], recordType: string): string[] {
  return recordTypes.find((definition) => definition.type === recordType)?.requiredFields ?? []
}

export function groupIndicatorsByReportType(indicators: IndicatorDefinition[]): Record<string, IndicatorDefinition[]> {
  return indicators.reduce<Record<string, IndicatorDefinition[]>>((groups, indicator) => {
    groups[indicator.reportType] = groups[indicator.reportType] ?? []
    groups[indicator.reportType].push(indicator)
    return groups
  }, {})
}

export function createIndicatorValues(indicatorsJson: string | undefined): Record<string, string> {
  if (!indicatorsJson) {
    return {}
  }
  const parsed = JSON.parse(indicatorsJson) as { indicators?: Array<{ code: string; value: number | string }> }
  return (parsed.indicators ?? []).reduce<Record<string, string>>((values, indicator) => {
    values[indicator.code] = indicator.value === undefined || indicator.value === null ? '' : String(indicator.value)
    return values
  }, {})
}

export function buildIndicatorsJson(definitions: IndicatorDefinition[], values: Record<string, string>): string {
  const indicators = definitions
    .filter((indicator) => String(values[indicator.code] ?? '').trim())
    .map((indicator) => ({
      code: indicator.code,
      value: Number(values[indicator.code])
    }))
  return JSON.stringify({ indicators })
}

export function toDateInput(value: string): string {
  const date = new Date(value)
  const safeDate = Number.isNaN(date.getTime()) ? new Date() : date
  return [
    safeDate.getFullYear(),
    String(safeDate.getMonth() + 1).padStart(2, '0'),
    String(safeDate.getDate()).padStart(2, '0')
  ].join('-')
}

export function toTimeInput(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '09:00'
  }
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function toOffsetDateTime(date: string, time: string): string {
  return `${date}T${time}:00+08:00`
}
