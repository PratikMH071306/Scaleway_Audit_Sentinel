import { ApiError } from '../api/http.js'

/** @typedef {'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'} AlertSeverity */
/** @typedef {'OPEN' | 'RESOLVED'} AlertStatus */

/**
 * Frontend shape of com.secure_monitor.alert.AlertResponse.
 * @typedef {{
 *   id: string,
 *   rule: string,
 *   severity: AlertSeverity,
 *   status: AlertStatus,
 *   title: string,
 *   description: string,
 *   detectedAt: string,
 *   sourceIp: string,
 *   eventCount: number,
 *   relatedEventIds: string[]
 * }} Alert
 */

export const SEVERITIES = /** @type {AlertSeverity[]} */ (['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'])
export const STATUSES = /** @type {AlertStatus[]} */ (['OPEN', 'RESOLVED'])

const SEVERITY_RANK = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  CRITICAL: 4,
}

/**
 * @param {unknown} value
 * @returns {Alert}
 */
export function parseAlert(value) {
  if (!value || typeof value !== 'object') {
    throw new ApiError('The API returned an alert in an unexpected shape.')
  }

  const alert = /** @type {Record<string, unknown>} */ (value)
  if (typeof alert.id !== 'string' || alert.id.length === 0) {
    throw new ApiError('The API returned an alert in an unexpected shape.')
  }
  if (!SEVERITIES.includes(/** @type {AlertSeverity} */ (alert.severity))) {
    throw new ApiError('The API returned an alert in an unexpected shape.')
  }
  if (!STATUSES.includes(/** @type {AlertStatus} */ (alert.status))) {
    throw new ApiError('The API returned an alert in an unexpected shape.')
  }

  return {
    id: alert.id,
    rule: typeof alert.rule === 'string' ? alert.rule : '',
    severity: /** @type {AlertSeverity} */ (alert.severity),
    status: /** @type {AlertStatus} */ (alert.status),
    title: typeof alert.title === 'string' ? alert.title : '',
    description: typeof alert.description === 'string' ? alert.description : '',
    detectedAt: typeof alert.detectedAt === 'string' ? alert.detectedAt : '',
    sourceIp: typeof alert.sourceIp === 'string' ? alert.sourceIp : '',
    eventCount: typeof alert.eventCount === 'number' ? alert.eventCount : 0,
    relatedEventIds: Array.isArray(alert.relatedEventIds)
      ? alert.relatedEventIds.filter((id) => typeof id === 'string')
      : [],
  }
}

/**
 * @param {unknown} value
 * @returns {Alert[]}
 */
export function parseAlertList(value) {
  if (!Array.isArray(value)) {
    throw new ApiError('The API returned an alert list in an unexpected shape.')
  }
  return value.map(parseAlert)
}

/**
 * @param {AlertSeverity} severity
 */
export function severityRank(severity) {
  return SEVERITY_RANK[severity]
}
