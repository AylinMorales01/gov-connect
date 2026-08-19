import { useState } from 'react';
import {
    Box,
    Typography,
    Paper,
    Stack,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Grid,
    IconButton,
} from '@mui/material';
import ClearIcon from '@mui/icons-material/Clear';
import CloseIcon from '@mui/icons-material/Close';
import ContractsSummaryCards from '../components/cards/ContractsSummaryCards';
import ContractsTable from '../components/tables/ContractsTable';
import ContractStatusChip from '../components/chips/ContractStatusChip';
import ContractsValueByDepartmentChart from '../components/charts/ContractsValueByDepartmentChart';
import TopContractorsTable from '../components/tables/TopContractorsTable';
import { useContracts } from '../hooks/useContracts';
import type { ContractItem } from '../types/contracts';
import { formatCompactCurrency } from '../utils/formatCompactCurrency';
import { formatISODate } from '../utils/formatDate';

type StatusFilter = 'ALL' | 'ACTIVE' | 'SUSPENDED' | 'FINISHED';

/** Campo individual del detalle. */
function DetailField({ label, value }: { label: string; value: string }) {
    return (
        <Box sx={{ mb: 2.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {label}
            </Typography>
            <Typography variant="body1" sx={{ mt: 0.5, wordBreak: 'break-word' }}>
                {value}
            </Typography>
        </Box>
    );
}

export default function ContractsPage() {
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
    const [selected, setSelected] = useState<ContractItem | null>(null);

    const status = statusFilter === 'ALL' ? undefined : statusFilter;
    const { data, isLoading, isError } = useContracts(status, search);

    const contracts = data ?? [];
    const hasActiveFilters = search !== '' || statusFilter !== 'ALL';

    const handleClearFilters = () => {
        setSearch('');
        setStatusFilter('ALL');
    };

    const handleCloseDetail = () => setSelected(null);

    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Contratos
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Catálogo y seguimiento de contratos.
            </Typography>

            {/* Tarjetas de resumen */}
            <Box sx={{ mb: 4 }}>
                <ContractsSummaryCards />
            </Box>

            {/* Analítica de contratos */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid size={{ xs: 12, md: 6 }}>
                    <ContractsValueByDepartmentChart />
                </Grid>

                <Grid size={{ xs: 12, md: 6 }}>
                    <TopContractorsTable />
                </Grid>
            </Grid>

            {/* Barra de búsqueda y filtros */}
            <Paper sx={{ p: 2, mb: 2, borderRadius: 2, boxShadow: 1 }}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
                >
                    <TextField
                        size="small"
                        placeholder="Buscar por número, contratista u objeto..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        sx={{ minWidth: 280 }}
                    />

                    <ToggleButtonGroup
                        value={statusFilter}
                        exclusive
                        onChange={(_, value) => {
                            if (value !== null) setStatusFilter(value);
                        }}
                        size="small"
                    >
                        <ToggleButton value="ALL">Todos</ToggleButton>
                        <ToggleButton value="ACTIVE">Activos</ToggleButton>
                        <ToggleButton value="SUSPENDED">Suspendidos</ToggleButton>
                        <ToggleButton value="FINISHED">Finalizados</ToggleButton>
                    </ToggleButtonGroup>

                    {hasActiveFilters && (
                        <Button
                            variant="text"
                            size="small"
                            startIcon={<ClearIcon />}
                            onClick={handleClearFilters}
                            sx={{ textTransform: 'none' }}
                        >
                            Limpiar filtros
                        </Button>
                    )}
                </Stack>
            </Paper>

            {/* Tabla de contratos */}
            <ContractsTable
                contracts={contracts}
                loading={isLoading}
                error={isError}
                onSelect={setSelected}
            />

            {/* Diálogo de detalle */}
            <Dialog
                open={selected !== null}
                onClose={handleCloseDetail}
                maxWidth="sm"
                fullWidth
                slotProps={{ paper: { sx: { borderRadius: 2 } } }}
            >
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pr: 1 }}>
                    <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                        Detalle de contrato
                    </Typography>
                    <IconButton onClick={handleCloseDetail} size="small">
                        <CloseIcon />
                    </IconButton>
                </DialogTitle>

                <DialogContent dividers>
                    {selected && (
                        <>
                            <DetailField label="Número" value={selected.contractNumber} />
                            <DetailField label="Contratista" value={selected.contractorName} />
                            <DetailField label="Objeto" value={selected.object} />
                            <DetailField label="Valor" value={formatCompactCurrency(selected.contractValue)} />
                            <DetailField label="Inicio" value={formatISODate(selected.startDate)} />
                            <DetailField label="Fin" value={formatISODate(selected.endDate)} />

                            <Box sx={{ mb: 2.5 }}>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                    sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}
                                >
                                    Estado
                                </Typography>
                                <Box sx={{ mt: 0.5 }}>
                                    <ContractStatusChip status={selected.status} />
                                </Box>
                            </Box>

                            <DetailField label="Dependencia" value={selected.department} />
                        </>
                    )}
                </DialogContent>

                <DialogActions>
                    <Button onClick={handleCloseDetail} variant="outlined" sx={{ textTransform: 'none' }}>
                        Cerrar
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
