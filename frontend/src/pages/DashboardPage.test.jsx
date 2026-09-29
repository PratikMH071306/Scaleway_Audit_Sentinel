import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAlerts } from '../api/alertApi.js'
import { ApiError } from '../api/http.js'
import DashboardPage from './DashboardPage.jsx'
import { alert } from '../test/fixtures.js'

vi.mock('../api/alertApi.js', () => ({
  getAlerts: vi.fn(),
  getAlertById: vi.fn(),
}))

const openHigh = alert()
const resolvedMedium = alert({
  id: '8e5c0000-0000-4000-8000-000000000002',
  severity: 'MEDIUM',
  status: 'RESOLVED',
  rule: 'UNUSUAL_SOURCE_IP',
  title: 'Unusual source IP',
  sourceIp: '10.0.0.8',
  detectedAt: '2026-09-28T08:00:00Z',
  relatedEventIds: [],
})

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(getAlerts).mockReset()
  })

  it('shows summary counts and alert rows from the API', async () => {
    vi.mocked(getAlerts).mockResolvedValue([openHigh, resolvedMedium])
    renderDashboard()

    expect(await screen.findByRole('link', { name: openHigh.title })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: resolvedMedium.title })).toBeInTheDocument()
    expect(valueOf('Open Alerts')).toHaveTextContent('1')
    expect(valueOf('High Severity')).toHaveTextContent('1')
    expect(valueOf('Total Alerts')).toHaveTextContent('2')
    const table = screen.getByRole('table')
    expect(within(table).getByText('HIGH')).toBeInTheDocument()
    expect(within(table).getByText('122.167.118.87')).toBeInTheDocument()
    expect(within(table).getByText('OPEN')).toBeInTheDocument()
  })

  it('explains an empty alert list', async () => {
    vi.mocked(getAlerts).mockResolvedValue([])
    renderDashboard()

    expect(await screen.findByRole('heading', { name: 'No security alerts' })).toBeInTheDocument()
    expect(screen.getByText('No suspicious activity has been detected.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows an API failure and retries', async () => {
    vi.mocked(getAlerts)
      .mockRejectedValueOnce(new ApiError('Unable to reach the security monitor API.'))
      .mockResolvedValueOnce([openHigh])
    const user = userEvent.setup()
    renderDashboard()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the security monitor API.')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: openHigh.title })).toBeInTheDocument()
  })

  it('keeps the last alerts visible when refresh fails', async () => {
    vi.mocked(getAlerts)
      .mockResolvedValueOnce([openHigh])
      .mockRejectedValueOnce(new ApiError('The security monitor API failed. Try again in a moment.'))
    const user = userEvent.setup()
    renderDashboard()

    expect(await screen.findByRole('link', { name: openHigh.title })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Refresh' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The security monitor API failed')
    expect(screen.getByRole('link', { name: openHigh.title })).toBeInTheDocument()
  })

  it('filters and sorts on the client', async () => {
    const olderCritical = alert({
      id: '8e5c0000-0000-4000-8000-000000000003',
      severity: 'CRITICAL',
      title: 'Forbidden resource access',
      detectedAt: '2026-09-27T08:00:00Z',
      rule: 'FORBIDDEN_RESOURCE',
    })
    vi.mocked(getAlerts).mockResolvedValue([openHigh, resolvedMedium, olderCritical])
    const user = userEvent.setup()
    renderDashboard()

    expect(await screen.findByRole('link', { name: openHigh.title })).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Severity'), 'CRITICAL')
    expect(screen.getByRole('link', { name: 'Forbidden resource access' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: openHigh.title })).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Severity'), 'ALL')
    await user.selectOptions(screen.getByLabelText('Sort'), 'severity')
    const titles = screen.getAllByRole('link')
      .map((link) => link.textContent)
      .filter((text) => text !== 'Security Monitor')
    expect(titles[0]).toBe('Forbidden resource access')
  })

  it('explains when filters match nothing', async () => {
    vi.mocked(getAlerts).mockResolvedValue([openHigh])
    const user = userEvent.setup()
    renderDashboard()

    expect(await screen.findByRole('link', { name: openHigh.title })).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Status'), 'RESOLVED')
    expect(screen.getByRole('heading', { name: 'No alerts match these filters' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

function valueOf(label) {
  const heading = screen.getByRole('heading', { name: label })
  const value = heading.nextElementSibling
  if (!value) {
    throw new Error(`Missing value for ${label}`)
  }
  return value
}
