import {
    Card,
    CardContent,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Box,
    CircularProgress,
    Alert,
} from '@mui/material';
import { useTopContractors } from '../../hooks/useTopContractors';
import type { TopContractorItem } from '../../types/analytics';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';
import RankPosition from '../RankPosition';

export default function TopContractorsTable() {
    const { data, isLoading, isError } = useTopContractors();
    const contractors: TopContractorItem[] = data ?? [];

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Top Contratistas
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudo cargar el ranking de contratistas.</Alert>
                )}

                {!isLoading && !isError && contractors.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de contratistas.
                    </Typography>
                )}

                {!isLoading && !isError && contractors.length > 0 && (
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell><b>#</b></TableCell>
                                <TableCell><b>Contratista</b></TableCell>
                                <TableCell align="right"><b>Valor</b></TableCell>
                                <TableCell align="right"><b>Contratos</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {contractors.map((row, index) => {
                                const position = index + 1;
                                return (
                                    <TableRow key={row.contractor} hover>
                                        <TableCell>
                                            <RankPosition position={position} />
                                        </TableCell>
                                        <TableCell>{row.contractor}</TableCell>
                                        <TableCell align="right">
                                            {formatCompactCurrency(row.totalValue)}
                                        </TableCell>
                                        <TableCell align="right">{row.contractCount}</TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                )}
            </CardContent>
        </Card>
    );
}
