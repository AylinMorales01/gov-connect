import { Card, CardContent, Typography, Box, CircularProgress, Alert } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from 'recharts';
import { useCollectionsByPaymentMethod } from '../../hooks/useCollectionsByPaymentMethod';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';

export default function PaymentMethodBreakdownChart() {
    const { data, isLoading, isError } = useCollectionsByPaymentMethod();
    const items = data ?? [];
    const chartData = items.map((item) => ({ name: item.paymentMethod, total: item.totalAmount }));

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Recaudo por Método de Pago
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar el desglose por método de pago.</Alert>
                )}

                {!isLoading && !isError && items.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de método de pago.
                    </Typography>
                )}

                {!isLoading && !isError && items.length > 0 && (
                    <Box sx={{ width: '100%', height: 300 }}>
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
                                <YAxis type="category" dataKey="name" width={110} />
                                <Tooltip
                                    formatter={(value) => [formatCompactCurrency(Number(value)), 'Recaudo']}
                                />
                                <Bar dataKey="total" fill="#009688" barSize={24} />
                            </BarChart>
                        </ResponsiveContainer>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}
