import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import DashboardPage from '../pages/DashboardPage';
import AnalyticsPage from '../pages/AnalyticsPage';
import ContractsPage from '../pages/ContractsPage';
import AutomationPage from '../pages/AutomationPage';

export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                {/* El MainLayout envuelve las rutas. El contenido de cada página irá en el <Outlet /> */}
                <Route element={<MainLayout />}>
                    <Route path="/" element={<DashboardPage />} />
                    <Route path="/analytics" element={<AnalyticsPage />} />
                    <Route path="/contracts" element={<ContractsPage />} />
                    <Route path="/automation" element={<AutomationPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}