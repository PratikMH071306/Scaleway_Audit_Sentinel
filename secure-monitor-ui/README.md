# secure-monitor-ui

Alert dashboard for the Secure Monitor API.

## Run

1. Start the Spring Boot API.
2. Point `VITE_API_BASE_URL` at that API. `.env` defaults to `http://localhost:8084`.
3. Install and start the UI:

```
npm install
npm run dev
```

Vite serves the UI at `http://localhost:5173`, or the next free port. The API allows local development origins through `secure-monitor.cors.allowed-origins`.
