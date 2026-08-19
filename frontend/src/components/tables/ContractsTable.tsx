import {
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    CircularProgress,
    Box,
    Alert,
    Typography,
} from '@mui/material';
import type { ContractItem } from '../../types/contracts';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';
import { formatISODate } from '../../utils/formatDate';
import ContractStatusChip from '../chips/ContractStatusChip';

interface ContractsTableProps {
    contracts: ContractItem[];
    loading: boolean;
    error: boolean;
    onSelect: (contract: ContractItem) => void;
}

export default function ContractsTable({ contracts, loading, error, onSelect }: ContractsTableProps) {
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return <Alert severity="error">No se pudieron cargar los contratos.</Alert>;
    }

    if (contracts.length === 0) {
        return (
            <Typography variant="body2" color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
                No hay contratos que coincidan con los filtros.
            </Typography>
        );
    }

    return (
        <TableContainer component={Paper} sx={{ borderRadius: 2, boxShadow: 2 }}>
            <Table>
                <TableHead>
                    <TableRow sx={{ backgroundColor: 'action.hover' }}>
                        <TableCell sx={{ fontWeight: 'bold' }}>Número</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Contratista</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Objeto</TableCell>
                        <TableCell align="right" sx={{ fontWeight: 'bold' }}>Valor</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Fin</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Dependencia</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {contracts.map((contract) => (
                        <TableRow
                            key={contract.id}
                            hover
                            onClick={() => onSelect(contract)}
                            sx={{ cursor: 'pointer' }}
                        >
                            <TableCell>
                                <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                                    {contract.contractNumber}
                                </Typography>
                            </TableCell>
                            <TableCell>{contract.contractorName}</TableCell>
                            <TableCell>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{
                                        maxWidth: 260,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                    }}
                                >
                                    {contract.object}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">{formatCompactCurrency(contract.contractValue)}</TableCell>
                            <TableCell>{formatISODate(contract.endDate)}</TableCell>
                            <TableCell>
                                <ContractStatusChip status={contract.status} />
                            </TableCell>
                            <TableCell>{contract.department}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
