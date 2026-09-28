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
export async function apiGet(path) {
  const url = `${apiBaseUrl()}${path}`
  let response
  try {
    response = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    })
  } catch (cause) {
    throw new ApiError('Unable to reach the security monitor API.', { cause })
  }

  if (!response.ok) {
    throw new ApiError(messageForStatus(response.status), { status: response.status })
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
  if (status === 404) {
    return 'The requested alert was not found.'
  }
  if (status >= 500) {
    return 'The security monitor API failed. Try again in a moment.'
  }
  return `The security monitor API rejected the request (${status}).`
}
