import { Card, CardContent, Typography, Box, CircularProgress, Alert } from '@mui/material';
import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
} from 'recharts';
import type { MonthlyTrendItem } from '../../types/analytics';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';
import { formatMonth } from '../../utils/formatMonth';

interface MonthlyTrendChartProps {
    data: MonthlyTrendItem[];
    loading: boolean;
    error: boolean;
}

export default function MonthlyTrendChart({
    data,
    loading,
    error,
}: MonthlyTrendChartProps) {
    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, p: 1, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                    Tendencia de Recaudos
                </Typography>

                {loading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                        <CircularProgress />
                    </Box>
                )}

                {error && (
                    <Alert severity="error">No se pudo cargar la tendencia mensual.</Alert>
                )}

                {!loading && !error && (
                    <Box sx={{ width: '100%', height: 320 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={data} margin={{ top: 10, right: 30, left: 10, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#1976D2" stopOpacity={0.8} />
                                        <stop offset="95%" stopColor="#1976D2" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis
                                    dataKey="month"
                                    tickFormatter={(value: string) => formatMonth(value)}
                                />
                                <YAxis
                                    tickFormatter={(value: number) => formatCompactCurrency(value)}
                                    width={70}
                                />
                                <Tooltip
                                    formatter={(value) => [
                                        formatCompactCurrency(Number(value)),
                                        'Recaudo',
                                    ]}
                                    labelFormatter={(label) => `Mes: ${formatMonth(String(label))}`}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="amount"
                                    stroke="#1976D2"
                                    strokeWidth={3}
                                    fillOpacity={1}
                                    fill="url(#colorAmount)"
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}