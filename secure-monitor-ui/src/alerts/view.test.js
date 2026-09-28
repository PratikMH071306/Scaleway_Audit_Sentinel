import { describe, expect, it } from 'vitest'
import { filterAlerts, ruleOptions, sortAlerts, summarizeAlerts } from './view.js'
import { alert } from '../test/fixtures.js'

const alerts = [
  alert({
    id: 'high-open',
    severity: 'HIGH',
    status: 'OPEN',
    detectedAt: '2026-09-28T10:30:00Z',
    rule: 'FAILED_AUTHENTICATION_SPIKE',
  }),
  alert({
    id: 'critical-open',
    severity: 'CRITICAL',
    status: 'OPEN',
    detectedAt: '2026-09-28T09:00:00Z',
    rule: 'FORBIDDEN_RESOURCE',
    title: 'Forbidden resource access',
  }),
  alert({
    id: 'medium-resolved',
    severity: 'MEDIUM',
    status: 'RESOLVED',
    detectedAt: '2026-09-28T11:00:00Z',
    rule: 'UNUSUAL_SOURCE_IP',
    title: 'Unusual source IP',
  }),
]

describe('summarizeAlerts', () => {
  it('counts open alerts, high severity, and the total', () => {
    expect(summarizeAlerts(alerts)).toEqual({
      open: 2,
      highSeverity: 2,
      total: 3,
    })
  })
})

describe('filterAlerts', () => {
  it('filters by severity, status, and rule', () => {
    expect(filterAlerts(alerts, {
      severity: 'HIGH',
      status: 'ALL',
      rule: 'ALL',
    }).map((item) => item.id)).toEqual(['high-open'])

    expect(filterAlerts(alerts, {
      severity: 'ALL',
      status: 'RESOLVED',
      rule: 'UNUSUAL_SOURCE_IP',
    }).map((item) => item.id)).toEqual(['medium-resolved'])
  })
})

describe('sortAlerts', () => {
  it('sorts newest, oldest, and highest severity', () => {
    expect(sortAlerts(alerts, 'newest').map((item) => item.id)).toEqual([
      'medium-resolved',
      'high-open',
      'critical-open',
    ])
    expect(sortAlerts(alerts, 'oldest').map((item) => item.id)).toEqual([
      'critical-open',
      'high-open',
      'medium-resolved',
    ])
    expect(sortAlerts(alerts, 'severity').map((item) => item.id)).toEqual([
      'critical-open',
      'high-open',
      'medium-resolved',
    ])
  })
})

describe('ruleOptions', () => {
  it('lists unique rules in alphabetical order', () => {
    expect(ruleOptions(alerts)).toEqual([
      'FAILED_AUTHENTICATION_SPIKE',
      'FORBIDDEN_RESOURCE',
      'UNUSUAL_SOURCE_IP',
    ])
  })
})
