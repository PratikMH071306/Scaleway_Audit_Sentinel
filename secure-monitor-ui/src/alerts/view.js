import { severityRank } from '../model/alert.js'

/**
 * @param {import('../model/alert.js').Alert[]} alerts
 */
export function summarizeAlerts(alerts) {
  return {
    open: alerts.filter((alert) => alert.status === 'OPEN').length,
    highSeverity: alerts.filter((alert) => alert.severity === 'HIGH' || alert.severity === 'CRITICAL').length,
    total: alerts.length,
  }
}

/**
 * @param {import('../model/alert.js').Alert[]} alerts
 */
export function ruleOptions(alerts) {
  return [...new Set(alerts.map((alert) => alert.rule).filter(Boolean))].sort((a, b) => a.localeCompare(b))
}

/**
 * @param {import('../model/alert.js').Alert[]} alerts
 * @param {{ severity: string, status: string, rule: string }} filters
 */
export function filterAlerts(alerts, filters) {
  return alerts.filter((alert) => {
    const severityMatches = filters.severity === 'ALL' || alert.severity === filters.severity
    const statusMatches = filters.status === 'ALL' || alert.status === filters.status
    const ruleMatches = filters.rule === 'ALL' || alert.rule === filters.rule
    return severityMatches && statusMatches && ruleMatches
  })
}

/**
 * @param {import('../model/alert.js').Alert[]} alerts
 * @param {'newest' | 'oldest' | 'severity'} sort
 */
export function sortAlerts(alerts, sort) {
  const copy = [...alerts]
  copy.sort((left, right) => {
    if (sort === 'oldest') {
      return compareTime(left, right)
    }
    if (sort === 'severity') {
      const bySeverity = severityRank(right.severity) - severityRank(left.severity)
      return bySeverity !== 0 ? bySeverity : compareTime(right, left)
    }
    return compareTime(right, left)
  })
  return copy
}

/**
 * @param {import('../model/alert.js').Alert} left
 * @param {import('../model/alert.js').Alert} right
 */
function compareTime(left, right) {
  return timeValue(left.detectedAt) - timeValue(right.detectedAt)
}

/**
 * @param {string} value
 */
function timeValue(value) {
  const time = Date.parse(value)
  return Number.isNaN(time) ? 0 : time
}
