import { Card, CardContent, Typography, Box, CircularProgress, Alert } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';
import { useBudgetExecution } from '../../hooks/useBudgetExecution';
import type { BudgetExecutionItem } from '../../types/dashboard';

export default function BudgetExecutionChart() {
    const { data, isLoading, isError } = useBudgetExecution();
    const items: BudgetExecutionItem[] = data ?? [];
    const chartData = items.map((item) => ({ name: item.department, percentage: item.percentage }));

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, p: 1 }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                    Ejecución Presupuestal por Dependencia
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar la ejecución presupuestal.</Alert>
                )}

                {!isLoading && !isError && items.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de ejecución presupuestal.
                    </Typography>
                )}

                {!isLoading && !isError && items.length > 0 && (
                    <Box sx={{ width: '100%', height: 320 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart
                                data={chartData}
                                layout="vertical"
                                margin={{ top: 10, right: 30, left: 10, bottom: 0 }}
                            >
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                                <XAxis
                                    type="number"
                                    domain={[0, 100]}
                                    tickFormatter={(value: number) => `${value}%`}
                                />
                                <YAxis type="category" dataKey="name" width={140} />
                                <Tooltip
                                    formatter={(value) => [`${Number(value).toFixed(1)}%`, 'Ejecución']}
                                />
                                <Bar dataKey="percentage" fill="#1976D2" barSize={22} />
                            </BarChart>
                        </ResponsiveContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}
