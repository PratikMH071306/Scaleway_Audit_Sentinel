import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AlertDetailsPage from './pages/AlertDetailsPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/alerts" replace />} />
        <Route path="/alerts" element={<DashboardPage />} />
        <Route path="/alerts/:id" element={<AlertDetailsPage />} />
      </Routes>
    </BrowserRouter>
  )
}
