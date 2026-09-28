/**
 * @param {string} iso
 * @param {Date} [now]
 */
export function formatRelativeTime(iso, now = new Date()) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return 'Unknown time'
  }

  const seconds = Math.round((now.getTime() - date.getTime()) / 1000)
  if (seconds < 0 || seconds >= 60 * 60 * 24 * 7) {
    return formatAbsoluteTime(iso)
  }
  if (seconds < 60) {
    return `${Math.max(seconds, 0)}s ago`
  }

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes}m ago`
  }

  const hours = Math.floor(minutes / 60)
  if (hours < 24) {
    return `${hours}h ago`
  }

  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

/**
 * @param {string} iso
 */
export function formatAbsoluteTime(iso) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return 'Unknown time'
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
