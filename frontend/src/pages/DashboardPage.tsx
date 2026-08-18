import { Box, Typography, CircularProgress, Alert, Grid } from '@mui/material';
import { useDashboard } from '../hooks/useDashboard';
import { useMonthlyTrend } from '../hooks/useMonthlyTrend';
import DashboardCard from '../components/cards/DashboardCard';

import MonthlyTrendChart from '../components/charts/MonthlyTrendChart';
import DepartmentRankingTable from '../components/tables/DepartmentRankingTable';
import ExpiringContractsList from '../components/tables/ExpiringContractsList';

// Íconos
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import DescriptionIcon from '@mui/icons-material/Description';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';

// Helpers
import { formatCompactCurrency } from '../utils/formatCompactCurrency';
import { formatPercentage } from '../utils/formatPercentage';

export default function DashboardPage() {
    const { data, isLoading, isError } = useDashboard();
    const trend = useMonthlyTrend();

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (isError || !data) {
        return (
            <Alert severity="error" sx={{ mt: 2 }}>
                Error al obtener los datos del dashboard. Verifica la conexión con el backend.
            </Alert>
        );
    }

    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 4.5 }}>
                Dashboard Ejecutivo
            </Typography>

            <Grid container spacing={3}>
                {/* 1. Recaudos del Mes */}
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <DashboardCard
                        title="Recaudos del Mes"
                        value={formatCompactCurrency(data.collectionsThisMonth)}
                        icon={<AttachMoneyIcon />}
                        color="success"
                        subtitle="Actualizado hoy"
                    />
                </Grid>

                {/* 2. Contratos Activos */}
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <DashboardCard
                        title="Contratos Activos"
                        value={data.activeContracts}
                        icon={<DescriptionIcon />}
                        color="primary"
                    />
                </Grid>

                {/* 3. Próximos a Vencer */}
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <DashboardCard
                        title="Próximos a Vencer"
                        value={data.contractsExpiring}
                        icon={<WarningAmberIcon />}
                        color="warning"
                        subtitle="Requieren atención"
                    />
                </Grid>

                {/* 4. Ejecución Presupuestal */}
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <DashboardCard
                        title="Ejecución Presupuestal"
                        value={formatPercentage(data.budgetExecutionPercentage)}
                        icon={<TrendingUpIcon />}
                        color="secondary"
                    />
                </Grid>

                {/* === FILA DE GRÁFICA PRINCIPAL Y RANKING (Grid Mejorado) === */}
                <Grid size={{ xs: 12, md: 8 }}>
                    <MonthlyTrendChart
                        data={trend.data ?? []}
                        loading={trend.isLoading}
                        error={trend.isError}
                    />
                </Grid>

                <Grid size={{ xs: 12, md: 4 }}>
                    <DepartmentRankingTable />
                </Grid>

                {/* === FILA INFERIOR: CONTRATOS PRÓXIMOS A VENCER === */}
                <Grid size={{ xs: 12 }}>
                    <ExpiringContractsList />
                </Grid>

            </Grid>
        </Box>
    );
}