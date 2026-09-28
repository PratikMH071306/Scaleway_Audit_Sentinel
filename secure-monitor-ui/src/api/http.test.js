import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet, errorMessage } from './http.js'

afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
})

describe('apiGet', () => {
  it('reads JSON from the configured API base URL', async () => {
    const fetchMock = vi.fn(async () => jsonResponse({ ok: true }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiGet('/api/alerts')).resolves.toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8084/api/alerts', {
      method: 'GET',
      headers: { Accept: 'application/json' },
    })
  })

  it('reports a missing base URL before calling the network', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    vi.stubEnv('VITE_API_BASE_URL', '')

    await expect(apiGet('/api/alerts')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'VITE_API_BASE_URL is not configured.',
    })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('turns a network failure into an API error', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch')
    }))

    await expect(apiGet('/api/alerts')).rejects.toMatchObject({
      message: 'Unable to reach the security monitor API.',
      status: 0,
    })
  })

  it('turns an HTTP failure into a status-specific API error', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse({}, 503)))

    await expect(apiGet('/api/alerts')).rejects.toMatchObject({
      message: 'The security monitor API failed. Try again in a moment.',
      status: 503,
    })
  })

  it('uses a not-found message for 404 responses', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse({}, 404)))

    await expect(apiGet('/api/alerts/missing')).rejects.toEqual(
      expect.objectContaining({ status: 404, message: 'The requested alert was not found.' }),
    )
  })

  it('rejects a body that is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => {
        throw new SyntaxError('Unexpected token')
      },
    })))

    await expect(apiGet('/api/alerts')).rejects.toMatchObject({
      message: 'The API returned a response that was not JSON.',
    })
  })
})

describe('errorMessage', () => {
  it('returns the API error message and a fallback for anything else', () => {
    expect(errorMessage(new ApiError('down'))).toBe('down')
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong while loading alerts.')
  })
})

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  }
}
