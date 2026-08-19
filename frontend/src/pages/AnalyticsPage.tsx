import { Box, Typography, Grid } from '@mui/material';
import { useMonthlyTrend } from '../hooks/useMonthlyTrend';
import MonthlyTrendChart from '../components/charts/MonthlyTrendChart';
import DepartmentRankingTable from '../components/tables/DepartmentRankingTable';
import FinancialOverviewCards from '../components/cards/FinancialOverviewCards';
import ConceptBreakdownChart from '../components/charts/ConceptBreakdownChart';
import PaymentMethodBreakdownChart from '../components/charts/PaymentMethodBreakdownChart';

export default function AnalyticsPage() {
    const trend = useMonthlyTrend();

    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Analytics
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Motor analítico DuckDB: tendencias, ranking y desgloses de recaudo.
            </Typography>

            <Grid container spacing={3}>
                {/* Resumen financiero */}
                <Grid size={{ xs: 12 }}>
                    <FinancialOverviewCards />
                </Grid>

                {/* Tendencia mensual */}
                <Grid size={{ xs: 12, md: 8 }}>
                    <MonthlyTrendChart
                        data={trend.data ?? []}
                        loading={trend.isLoading}
                        error={trend.isError}
                    />
                </Grid>

                {/* Ranking de dependencias */}
                <Grid size={{ xs: 12, md: 4 }}>
                    <DepartmentRankingTable />
                </Grid>

                {/* Desglose por concepto */}
                <Grid size={{ xs: 12, md: 6 }}>
                    <ConceptBreakdownChart />
                </Grid>

                {/* Desglose por método de pago */}
                <Grid size={{ xs: 12, md: 6 }}>
                    <PaymentMethodBreakdownChart />
                </Grid>
            </Grid>
        </Box>
    );
}
