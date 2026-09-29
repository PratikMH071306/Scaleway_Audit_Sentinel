import { ApiError, apiBaseUrl } from '../api/http.js'

/**
 * @param {string} username
 * @param {string} password
 */
export async function loginRequest(username, password) {
  let response
  try {
    response = await fetch(`${apiBaseUrl()}/api/auth/login`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    })
  } catch (cause) {
    throw new ApiError('Unable to reach the security monitor API.', { cause })
  }

  if (response.status === 401) {
    throw new ApiError('Invalid username or password.', { status: 401 })
  }
  if (!response.ok) {
    throw new ApiError('Sign-in failed. Try again in a moment.', { status: response.status })
  }

  const body = await response.json()
  if (!body?.accessToken || !body.username || !body.role || typeof body.expiresIn !== 'number') {
    throw new ApiError('The API returned an unexpected sign-in response.', { status: response.status })
  }
  return body
}
