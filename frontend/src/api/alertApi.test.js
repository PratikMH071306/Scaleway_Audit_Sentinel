import { afterEach, describe, expect, it, vi } from 'vitest'
import { getAlertById, getAlerts } from './alertApi.js'
import { alert } from '../test/fixtures.js'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('alertApi', () => {
  it('loads the alert list from /api/alerts', async () => {
    const payload = [alert()]
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      expect(url).toBe('http://localhost:8084/api/alerts')
      return json(payload)
    }))

    await expect(getAlerts()).resolves.toEqual(payload)
  })

  it('loads one alert from /api/alerts/:id', async () => {
    const payload = alert()
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      expect(url).toBe(`http://localhost:8084/api/alerts/${payload.id}`)
      return json(payload)
    }))

    await expect(getAlertById(payload.id)).resolves.toEqual(payload)
  })

  it('encodes the alert id', async () => {
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      expect(url).toBe('http://localhost:8084/api/alerts/a%2Fb')
      return json(alert({ id: 'a/b' }))
    }))

    await expect(getAlertById('a/b')).resolves.toMatchObject({ id: 'a/b' })
  })

  it('rejects a payload that does not match AlertResponse', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => json([{ id: 'x', severity: 'URGENT', status: 'OPEN' }])))

    await expect(getAlerts()).rejects.toMatchObject({
      message: 'The API returned an alert in an unexpected shape.',
    })
  })

  it('rejects a list payload that is not an array', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => json({ alerts: [] })))

    await expect(getAlerts()).rejects.toMatchObject({
      message: 'The API returned an alert list in an unexpected shape.',
    })
  })

  it('surfaces a missing alert as not found', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: false,
      status: 404,
      json: async () => ({}),
    })))

    await expect(getAlertById('missing')).rejects.toMatchObject({
      status: 404,
      message: 'The requested alert was not found.',
    })
  })
})

function json(body) {
  return {
    ok: true,
    status: 200,
    json: async () => body,
  }
}
