import { Card, CardContent, Typography, Grid, Box, CircularProgress, Alert, Chip } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import RemoveIcon from '@mui/icons-material/Remove';
import { useFinancialOverview } from '../../hooks/useFinancialOverview';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';
import { formatMonth } from '../../utils/formatMonth';
import { formatPercentage } from '../../utils/formatPercentage';

/** Bloque individual de métrica. */
function Stat({ label, value }: { label: string; value: string }) {
    return (
        <Box>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {label}
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mt: 0.5 }}>
                {value}
            </Typography>
        </Box>
    );
}

/** Chip de tendencia con ícono y color. */
function TrendChip({ trend }: { trend: string }) {
    const config = {
        CRECIMIENTO: { color: 'success' as const, icon: <TrendingUpIcon /> },
        DESCENSO: { color: 'error' as const, icon: <TrendingDownIcon /> },
        ESTABLE: { color: 'default' as const, icon: <RemoveIcon /> },
    }[trend] ?? { color: 'default' as const, icon: <RemoveIcon /> };

    return (
        <Chip
            icon={config.icon}
            label={trend}
            color={config.color}
            size="small"
            variant="outlined"
            sx={{ fontWeight: 'bold', mt: 1 }}
        />
    );
}

export default function FinancialOverviewCards() {
    const { data, isLoading, isError } = useFinancialOverview();

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2 }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Resumen Financiero
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar el resumen financiero.</Alert>
                )}

                {!isLoading && !isError && data && (
                    <Grid container spacing={3}>
                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <Stat
                                label="Mejor mes"
                                value={`${formatMonth(data.bestCollectionMonth)} · ${formatCompactCurrency(data.bestCollectionAmount)}`}
                            />
                        </Grid>

                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <Stat
                                label="Peor mes"
                                value={`${formatMonth(data.worstCollectionMonth)} · ${formatCompactCurrency(data.worstCollectionAmount)}`}
                            />
                        </Grid>

                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <Stat
                                label="Promedio mensual"
                                value={formatCompactCurrency(data.averageMonthlyCollection)}
                            />
                        </Grid>

                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <Stat
                                label="Crecimiento intermensual"
                                value={formatPercentage(data.lastMonthGrowthPercentage)}
                            />
                            <TrendChip trend={data.trend} />
                        </Grid>
                    </Grid>
                )}
            </CardContent>
        </Card>
    );
}
