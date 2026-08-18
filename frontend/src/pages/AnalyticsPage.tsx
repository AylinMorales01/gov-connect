import { Box, Typography, Grid } from '@mui/material';
import { useMonthlyTrend } from '../hooks/useMonthlyTrend';
import MonthlyTrendChart from '../components/charts/MonthlyTrendChart';
import DepartmentRankingTable from '../components/tables/DepartmentRankingTable';

export default function AnalyticsPage() {
    const trend = useMonthlyTrend();

    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Analytics
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Análisis de tendencias de recaudo y ranking de dependencias.
            </Typography>

            <Grid container spacing={3}>
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
            </Grid>
        </Box>
    );
}
