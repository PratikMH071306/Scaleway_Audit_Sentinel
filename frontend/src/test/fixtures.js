export function alert(overrides = {}) {
  return {
    id: '8e5c0000-0000-4000-8000-000000000001',
    rule: 'SUSPICIOUS_AUTHENTICATION',
    severity: 'HIGH',
    status: 'OPEN',
    title: 'Suspicious authentication activity',
    description: '5 failed password login attempts for user test01 from 122.167.118.87 within 10 minutes',
    detectedAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
    sourceIp: '122.167.118.87',
    organizationId: 'e37c8fea-0284-4f90-bbfe-04666c7ecfd6',
    username: 'test01',
    eventCount: 5,
    relatedEventIds: ['e1', 'e2', 'e3', 'e4', 'e5'],
    ...overrides,
  }
}
