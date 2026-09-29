import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext.jsx'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAlertById } from '../api/alertApi.js'
import { ApiError } from '../api/http.js'
import AlertDetailsPage from './AlertDetailsPage.jsx'
import { alert } from '../test/fixtures.js'

vi.mock('../api/alertApi.js', () => ({
  getAlerts: vi.fn(),
  getAlertById: vi.fn(),
}))

describe('AlertDetailsPage', () => {
  beforeEach(() => {
    vi.mocked(getAlertById).mockReset()
  })

  it('shows the alert and the stored related event ids', async () => {
    const payload = alert()
    vi.mocked(getAlertById).mockResolvedValue(payload)
    renderDetails(payload.id)

    expect(await screen.findByRole('heading', { name: payload.title })).toBeInTheDocument()
    expect(screen.getByText(payload.description)).toBeInTheDocument()
    expect(screen.getByText('HIGH')).toBeInTheDocument()
    expect(screen.getByText(payload.sourceIp)).toBeInTheDocument()
    expect(screen.getByText(payload.rule)).toBeInTheDocument()
    for (const eventId of payload.relatedEventIds) {
      expect(screen.getByText(eventId)).toBeInTheDocument()
    }
    expect(vi.mocked(getAlertById)).toHaveBeenCalledWith(payload.id)
  })

  it('explains when an alert has no related events', async () => {
    vi.mocked(getAlertById).mockResolvedValue(alert({ relatedEventIds: [] }))
    renderDetails('8e5c0000-0000-4000-8000-000000000001')

    expect(await screen.findByText('No related events were stored for this alert.')).toBeInTheDocument()
  })

  it('shows not found when the API returns 404', async () => {
    vi.mocked(getAlertById).mockRejectedValue(new ApiError('The requested alert was not found.', { status: 404 }))
    renderDetails('missing')

    expect(await screen.findByRole('alert')).toHaveTextContent('The requested alert was not found.')
    expect(screen.queryByRole('heading', { name: 'Multiple failed authentication attempts' })).not.toBeInTheDocument()
  })

  it('shows a network failure and can retry', async () => {
    const payload = alert()
    vi.mocked(getAlertById)
      .mockRejectedValueOnce(new ApiError('Unable to reach the security monitor API.'))
      .mockResolvedValueOnce(payload)
    const user = userEvent.setup()
    renderDetails(payload.id)

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the security monitor API.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: payload.title })).toBeInTheDocument()
  })
})

function renderDetails(id) {
  return render(
    <MemoryRouter initialEntries={[`/alerts/${id}`]}>
      <AuthProvider>
        <Routes>
          <Route path="/alerts/:id" element={<AlertDetailsPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}
