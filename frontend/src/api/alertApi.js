import { apiGet } from './http.js'
import { parseAlert, parseAlertList } from '../model/alert.js'

export function getAlerts() {
  return apiGet('/api/alerts').then(parseAlertList)
}

/**
 * @param {string} id
 */
export function getAlertById(id, options) {
  return apiGet(`/api/alerts/${encodeURIComponent(id)}`, options).then(parseAlert)
}
