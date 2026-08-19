import { Card, CardContent, Typography, Box, CircularProgress, Alert } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';
import { useContractsValueByDepartment } from '../../hooks/useContractsValueByDepartment';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';

export default function ContractsValueByDepartmentChart() {
    const { data, isLoading, isError } = useContractsValueByDepartment();
    const items = data ?? [];
    const chartData = items.map((item) => ({ name: item.department, total: item.totalValue }));

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Valor Contratado por Dependencia
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar el valor por dependencia.</Alert>
                )}

                {!isLoading && !isError && items.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de contratos.
                    </Typography>
                )}

                {!isLoading && !isError && items.length > 0 && (
                    <Box sx={{ width: '100%', height: 280 }}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart
                                data={chartData}
                                layout="vertical"
                                margin={{ top: 10, right: 30, left: 10, bottom: 0 }}
                            >
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                                <XAxis
                                    type="number"
                                    tickFormatter={(value: number) => formatCompactCurrency(value)}
                                />
                                <YAxis type="category" dataKey="name" width={120} />
                                <Tooltip
                                    formatter={(value) => [formatCompactCurrency(Number(value)), 'Valor']}
                                />
                                <Bar dataKey="total" fill="#7B1FA2" barSize={22} />
                            </BarChart>
                        </ResponsiveContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}
