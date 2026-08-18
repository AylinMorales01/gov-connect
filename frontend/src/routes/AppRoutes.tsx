import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import DashboardPage from '../pages/DashboardPage';
import AnalyticsPage from '../pages/AnalyticsPage';
import ContractsPage from '../pages/ContractsPage';
import AutomationPage from '../pages/AutomationPage';
import LoginPage from '../pages/LoginPage';
import ForbiddenPage from '../pages/ForbiddenPage';
import ProtectedRoute from '../components/auth/ProtectedRoute';

export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Ruta pública: login */}
                <Route path="/login" element={<LoginPage />} />

                {/* 403 explícito (por si el backend redirige aquí) */}
                <Route path="/forbidden" element={<ForbiddenPage />} />

                {/* ── Rutas protegidas: requieren JWT válido ── */}
                <Route element={<ProtectedRoute />}>
                    <Route element={<MainLayout />}>
                        {/* ADMIN exclusivo */}
                        <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                            <Route path="/" element={<DashboardPage />} />
                        </Route>

                        {/* USER o ADMIN */}
                        <Route path="/analytics" element={<AnalyticsPage />} />
                        <Route path="/contracts" element={<ContractsPage />} />
                        <Route path="/automation" element={<AutomationPage />} />
                    </Route>
                </Route>

                {/* Cualquier otra ruta → redirigir al dashboard */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
