export function alert(overrides = {}) {
  return {
    id: '8e5c0000-0000-4000-8000-000000000001',
    rule: 'FAILED_AUTHENTICATION_SPIKE',
    severity: 'HIGH',
    status: 'OPEN',
    title: 'Multiple failed authentication attempts',
    description: '5 failed authentication attempts detected within 5 minutes',
    detectedAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
    sourceIp: '122.167.118.87',
    eventCount: 5,
    relatedEventIds: ['e1', 'e2', 'e3', 'e4', 'e5'],
    ...overrides,
  }
}
