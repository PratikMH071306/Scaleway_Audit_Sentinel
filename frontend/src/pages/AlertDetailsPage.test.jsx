import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext.jsx'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAlertById } from '../api/alertApi.js'
import { ApiError, apiGet, apiPost } from '../api/http.js'
import { storeSession } from '../auth/session.js'
import AlertDetailsPage from './AlertDetailsPage.jsx'
import { alert } from '../test/fixtures.js'

vi.mock('../api/alertApi.js', () => ({
  getAlerts: vi.fn(),
  getAlertById: vi.fn(),
}))

vi.mock('../api/http.js', async () => {
  const actual = await vi.importActual('../api/http.js')
  return {
    ...actual,
    apiGet: vi.fn().mockResolvedValue([]),
    apiPost: vi.fn(),
  }
})

describe('AlertDetailsPage', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.mocked(getAlertById).mockReset()
    vi.mocked(apiGet).mockReset()
    vi.mocked(apiGet).mockResolvedValue([])
    vi.mocked(apiPost).mockReset()
  })

  it('shows the alert and the stored related event ids', async () => {
    const payload = alert()
    vi.mocked(getAlertById).mockResolvedValue(payload)
    renderDetails(payload.id)

    expect(await screen.findByRole('heading', { name: payload.title })).toBeInTheDocument()
    expect(screen.getByText(payload.description)).toBeInTheDocument()
    expect(screen.getByText('HIGH')).toBeInTheDocument()
    expect(screen.getByText(payload.sourceIp)).toBeInTheDocument()
    expect(screen.getAllByText(payload.username).length).toBeGreaterThan(0)
    expect(screen.getByText(payload.rule)).toBeInTheDocument()
    for (const eventId of payload.relatedEventIds) {
      expect(screen.getByText(eventId)).toBeInTheDocument()
    }
    expect(vi.mocked(getAlertById)).toHaveBeenCalledWith(payload.id)
  })

  it('shows Lock User for an open alert whose Scaleway id is not stored yet', async () => {
    storeSession({ accessToken: 'token', expiresIn: 3600, username: 'admin', role: 'ADMIN' })
    vi.mocked(getAlertById).mockResolvedValue(alert({ scalewayUserId: '' }))
    renderDetails('8e5c0000-0000-4000-8000-000000000001')

    expect(await screen.findByRole('button', { name: 'Lock User' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Unlock User' })).not.toBeInTheDocument()
    expect(screen.getByText('Not captured')).toBeInTheDocument()
  })

  it('asks an admin to confirm before locking the Scaleway user', async () => {
    const payload = alert()
    storeSession({ accessToken: 'token', expiresIn: 3600, username: 'admin', role: 'ADMIN' })
    vi.mocked(getAlertById).mockResolvedValue(payload)
    vi.mocked(apiPost).mockResolvedValue({ message: 'User locked successfully' })
    const user = userEvent.setup()
    renderDetails(payload.id)

    await user.click(await screen.findByRole('button', { name: 'Lock User' }))
    expect(screen.getByRole('dialog')).toHaveTextContent(payload.scalewayUserId)
    expect(apiPost).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Confirm Lock' }))
    expect(apiPost).toHaveBeenCalledWith(`/api/alerts/${payload.id}/remediate`, { action: 'LOCK_USER' })
    expect(await screen.findByText('User locked successfully')).toBeInTheDocument()
  })

  it('asks an admin to confirm before unlocking the Scaleway user', async () => {
    const payload = alert()
    storeSession({ accessToken: 'token', expiresIn: 3600, username: 'admin', role: 'ADMIN' })
    vi.mocked(getAlertById).mockResolvedValue(alert({ scalewayUserLocked: true }))
    vi.mocked(apiPost).mockResolvedValue({ message: 'User unlocked successfully', status: 'SUCCESS', afterLocked: false })
    const user = userEvent.setup()
    renderDetails(payload.id)

    await user.click(await screen.findByRole('button', { name: 'Unlock User' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Unlock user?')
    await user.click(screen.getByRole('button', { name: 'Confirm Unlock' }))
    expect(apiPost).toHaveBeenCalledWith(`/api/alerts/${payload.id}/remediate`, { action: 'UNLOCK_USER' })
    expect(await screen.findByText('User unlocked successfully')).toBeInTheDocument()
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
    expect(screen.queryByRole('heading', { name: 'Suspicious authentication activity' })).not.toBeInTheDocument()
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
