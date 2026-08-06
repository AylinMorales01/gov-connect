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
    Chip,
} from '@mui/material';
import { useDepartmentRanking } from '../../hooks/useDepartmentRanking';
import type { DepartmentRankingItem } from '../../types/analytics';

export default function DepartmentRankingTable() {
    const { data, isLoading, isError } = useDepartmentRanking();

    const ranking: DepartmentRankingItem[] = data ?? [];

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Ranking de Dependencias
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">Error al cargar el ranking.</Alert>
                )}

                {!isLoading && !isError && ranking.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin datos de ranking disponibles.
                    </Typography>
                )}

                {!isLoading && !isError && ranking.length > 0 && (
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell><b>#</b></TableCell>
                                <TableCell><b>Dependencia</b></TableCell>
                                <TableCell align="right"><b>Score</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {ranking.map((row) => {
                                const position = row.rank ?? 0;
                                return (
                                    <TableRow key={row.department} hover>
                                        <TableCell>
                                            {position === 1 && '🥇'}
                                            {position === 2 && '🥈'}
                                            {position === 3 && '🥉'}
                                            {position > 3 && position}
                                        </TableCell>
                                        <TableCell>{row.department}</TableCell>
                                        <TableCell align="right">
                                            <Chip
                                                label={row.score.toFixed(1)}
                                                size="small"
                                                color={position <= 3 ? 'primary' : 'default'}
                                                variant={position <= 3 ? 'filled' : 'outlined'}
                                            />
                                        </TableCell>
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