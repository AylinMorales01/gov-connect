import { Card, CardContent, Typography, Box, CircularProgress, Alert } from '@mui/material';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { useCollectionsByConcept } from '../../hooks/useCollectionsByConcept';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';

const COLORS = ['#1976D2', '#009688', '#2E7D32', '#ED6C02', '#D32F2F', '#7B1FA2', '#0288D1', '#C2185B'];

export default function ConceptBreakdownChart() {
    const { data, isLoading, isError } = useCollectionsByConcept();
    const items = data ?? [];
    const chartData = items.map((item) => ({ name: item.concept, value: item.totalAmount }));

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Recaudo por Concepto
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar el desglose por concepto.</Alert>
                )}

                {!isLoading && !isError && items.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de concepto.
                    </Typography>
                )}

                {!isLoading && !isError && items.length > 0 && (
                    <Box sx={{ width: '100%', height: 300 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={chartData}
                                    dataKey="value"
                                    nameKey="name"
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={90}
                                    paddingAngle={2}
                                >
                                    {chartData.map((_, index) => (
                                        <Cell key={index} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    formatter={(value) => [formatCompactCurrency(Number(value)), 'Recaudo']}
                                />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}
