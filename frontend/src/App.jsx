import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext.jsx'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import AlertDetailsPage from './pages/AlertDetailsPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import LoginPage from './pages/LoginPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<Navigate to="/alerts" replace />} />
          <Route
            path="/alerts"
            element={(
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/alerts/:id"
            element={(
              <ProtectedRoute>
                <AlertDetailsPage />
              </ProtectedRoute>
            )}
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
