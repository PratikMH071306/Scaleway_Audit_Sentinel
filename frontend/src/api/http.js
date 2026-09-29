export class ApiError extends Error {
  /**
   * @param {string} message
   * @param {{ status?: number, cause?: unknown }} [options]
   */
  constructor(message, options = {}) {
    super(message, options.cause ? { cause: options.cause } : undefined)
    this.name = 'ApiError'
    this.status = options.status ?? 0
  }
}

let tokenProvider = () => null
let unauthorizedHandler = () => {}

/**
 * @param {{ token: () => string | null, onUnauthorized: () => void }} handlers
 */
export function configureApiAuth(handlers) {
  tokenProvider = handlers.token
  unauthorizedHandler = handlers.onUnauthorized
}

export function apiBaseUrl() {
  const base = import.meta.env.VITE_API_BASE_URL
  if (typeof base !== 'string' || base.trim() === '') {
    throw new ApiError('VITE_API_BASE_URL is not configured.')
  }
  return base.replace(/\/$/, '')
}

/**
 * @param {string} path
 */
/**
 * @param {string} path
 */
export async function apiGet(path, options = {}) {
  return request(path, { method: 'GET', logoutOnUnauthorized: options.logoutOnUnauthorized })
}

/**
 * @param {string} path
 */
export async function apiPost(path, body, options = {}) {
  return request(path, { method: 'POST', body, logoutOnUnauthorized: options.logoutOnUnauthorized })
}

/**
 * @param {string} path
 * @param {{ method: string, body?: unknown }} options
 */
async function request(path, options) {
  const url = `${apiBaseUrl()}${path}`
  const headers = { Accept: 'application/json' }
  const token = tokenProvider()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response
  try {
    response = await fetch(url, {
      method: options.method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch (cause) {
    throw new ApiError('Unable to reach the security monitor API.', { cause })
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    const serverMessage = payload && typeof payload.message === 'string' ? payload.message : ''
    const sessionEnded = serverMessage === 'Authentication is required'
    if (response.status === 401 && sessionEnded && options.logoutOnUnauthorized !== false) {
      unauthorizedHandler()
    }
    throw new ApiError(serverMessage || messageForStatus(response.status), { status: response.status })
  }

  if (response.status === 204) {
    return null
  }

  try {
    return await response.json()
  } catch (cause) {
    throw new ApiError('The API returned a response that was not JSON.', {
      status: response.status,
      cause,
    })
  }
}

/**
 * @param {unknown} error
 */
export function errorMessage(error) {
  if (error instanceof ApiError) {
    return error.message
  }
  return 'Something went wrong while loading alerts.'
}

/**
 * @param {number} status
 */
function messageForStatus(status) {
  if (status === 401) {
    return 'Your session has ended. Sign in again.'
  }
  if (status === 403) {
    return 'You do not have permission to perform this action.'
  }
  if (status === 404) {
    return 'The requested alert was not found.'
  }
  if (status >= 500) {
    return 'The security monitor API failed. Try again in a moment.'
  }
  return `The security monitor API rejected the request (${status}).`
}
