import { useState, useMemo } from 'react';
import {
    Box,
    Typography,
    CircularProgress,
    Alert,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    Paper,
    Grid,
    Skeleton,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    IconButton,
    Stack,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlined';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CloseIcon from '@mui/icons-material/Close';
import ClearIcon from '@mui/icons-material/Clear';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useAutomationLogs } from '../hooks/useAutomationLogs';
import DashboardCard from '../components/cards/DashboardCard';
import type { AutomationLogItem } from '../types/dashboard';

type StatusFilter = 'ALL' | 'SUCCESS' | 'ERROR';

/**
 * Formatea una fecha (ISO string o timestamp ms) a formato local.
 * @param dateOrTs fecha ISO (string) o timestamp Unix en milisegundos (number).
 * @param showSeconds si es true, incluye segundos en la salida.
 */
function formatDateTime(dateOrTs: string | number, showSeconds = false): string {
    const date = new Date(dateOrTs);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const base = `${day}/${month}/${year} ${hours}:${minutes}`;
    if (showSeconds) {
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${base}:${seconds}`;
    }
    return base;
}

/**
 * Devuelve un texto legible indicando cuánto tiempo pasó desde la última
 * actualización. Ej: "ahora", "hace 3 min", "12:30".
 */
function formatTimeAgo(ts: number): string {
    const seconds = Math.floor((Date.now() - ts) / 1000);
    if (seconds < 60) return 'ahora';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `hace ${minutes} min`;
    return formatDateTime(ts);
}

/** Renderiza el chip de estado con ícono y color. */
function StatusChip({ status }: { status: string }) {
    const isSuccess = status === 'SUCCESS';

    return (
        <Chip
            icon={isSuccess ? <CheckCircleIcon /> : <ErrorOutlineIcon />}
            label={isSuccess ? '✓ SUCCESS' : '✕ ERROR'}
            color={isSuccess ? 'success' : 'error'}
            size="small"
            variant="outlined"
            sx={{ fontWeight: 'bold', minWidth: 130 }}
        />
    );
}

/** Campo individual del detalle. */
function DetailField({ label, value }: { label: string; value: string }) {
    return (
        <Box sx={{ mb: 2.5 }}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {label}
            </Typography>
            <Typography variant="body1" sx={{ mt: 0.5, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {value}
            </Typography>
        </Box>
    );
}

export default function AutomationPage() {
    const { data, isLoading, isError, isFetching, refetch, dataUpdatedAt } = useAutomationLogs();

    // ── Raw data from API (siempre para las tarjetas resumen) ──
    const allLogs: AutomationLogItem[] = data ?? [];
    const total = allLogs.length;
    const successful = allLogs.filter((log) => log.status === 'SUCCESS').length;
    const errorCount = allLogs.filter((log) => log.status === 'ERROR').length;

    // ── Filtros ──
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');

    // ── Detalle ──
    const [selectedLog, setSelectedLog] = useState<AutomationLogItem | null>(null);

    // ── Datos filtrados para la tabla ──
    const filteredLogs = useMemo(() => {
        return allLogs.filter((log) => {
            const matchesStatus = statusFilter === 'ALL' || log.status === statusFilter;
            const matchesSearch =
                search === '' || log.process.toLowerCase().includes(search.toLowerCase());
            return matchesStatus && matchesSearch;
        });
    }, [allLogs, statusFilter, search]);

    const hasActiveFilters = search !== '' || statusFilter !== 'ALL';

    // ── Diferenciar carga inicial vs actualización en segundo plano ──
    const isInitialLoading = isLoading && !data;
    const isBackgroundRefreshing = isFetching && !isInitialLoading;
    const isInitialError = isError && !data;
    const isBackgroundError = isError && !!data;

    const handleClearFilters = () => {
        setSearch('');
        setStatusFilter('ALL');
    };

    const handleOpenDetail = (log: AutomationLogItem) => {
        setSelectedLog(log);
    };

    const handleCloseDetail = () => {
        setSelectedLog(null);
    };

    const handleRefresh = () => {
        refetch();
    };

    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Automatizaciones
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Historial de ejecuciones de los procesos automatizados.
            </Typography>

            {/* ── Tarjetas de resumen ── */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid size={{ xs: 12, sm: 4 }}>
                    {isInitialLoading ? (
                        <Skeleton variant="rounded" height={135} sx={{ borderRadius: 2 }} />
                    ) : (
                        <DashboardCard
                            title="Total de ejecuciones"
                            value={total}
                            icon={<AutorenewIcon />}
                            color="primary"
                        />
                    )}
                </Grid>

                <Grid size={{ xs: 12, sm: 4 }}>
                    {isInitialLoading ? (
                        <Skeleton variant="rounded" height={135} sx={{ borderRadius: 2 }} />
                    ) : (
                        <DashboardCard
                            title="Exitosas"
                            value={successful}
                            icon={<CheckCircleIcon />}
                            color="success"
                        />
                    )}
                </Grid>

                <Grid size={{ xs: 12, sm: 4 }}>
                    {isInitialLoading ? (
                        <Skeleton variant="rounded" height={135} sx={{ borderRadius: 2 }} />
                    ) : (
                        <DashboardCard
                            title="Errores"
                            value={errorCount}
                            icon={<ErrorOutlineIcon />}
                            color="error"
                        />
                    )}
                </Grid>
            </Grid>

            {/* ── Error en carga inicial ── */}
            {isInitialError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    No fue posible cargar el historial de automatizaciones. Verifica la conexión con el backend.
                </Alert>
            )}

            {/* ── Sin datos ── */}
            {!isLoading && !isError && allLogs.length === 0 && (
                <Box sx={{ py: 6, textAlign: 'center' }}>
                    <Typography variant="body1" color="text.secondary">
                        No hay ejecuciones de automatización registradas.
                    </Typography>
                </Box>
            )}

            {/* ── Carga inicial sin datos aún ── */}
            {isInitialLoading && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                    <CircularProgress />
                </Box>
            )}

            {/* ── Datos disponibles (accesible incluso durante actualizaciones en segundo plano) ── */}
            {!isInitialLoading && !isInitialError && allLogs.length > 0 && (
                <>
                    {/* ── Barra de búsqueda y filtros ── */}
                    <Paper
                        sx={{
                            p: 2,
                            mb: 2,
                            borderRadius: 2,
                            boxShadow: 1,
                        }}
                    >
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={2}
                            sx={{ alignItems: { xs: 'stretch', sm: 'center' } }}
                        >
                            <TextField
                                size="small"
                                placeholder="Buscar proceso..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                sx={{ minWidth: 240 }}
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
                                <ToggleButton value="SUCCESS">SUCCESS</ToggleButton>
                                <ToggleButton value="ERROR">ERROR</ToggleButton>
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

                    {/* ── Error en actualización en segundo plano ── */}
                    {isBackgroundError && (
                        <Alert severity="warning" sx={{ mb: 2 }}>
                            No se pudo actualizar el historial. Mostrando información anterior.
                        </Alert>
                    )}

                    {/* ── Barra de estado: última actualización + botón actualizar ── */}
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: { xs: 'column', sm: 'row' },
                            justifyContent: 'space-between',
                            alignItems: { xs: 'stretch', sm: 'center' },
                            gap: 1.5,
                            mb: 2,
                        }}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" color="text.secondary">
                                Última actualización:{' '}
                                {dataUpdatedAt ? formatTimeAgo(dataUpdatedAt) : '—'}
                            </Typography>
                            {isBackgroundRefreshing && (
                                <CircularProgress size={14} sx={{ color: 'text.secondary' }} />
                            )}
                        </Box>

                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={
                                isBackgroundRefreshing ? (
                                    <CircularProgress size={16} />
                                ) : (
                                    <RefreshIcon />
                                )
                            }
                            onClick={handleRefresh}
                            disabled={isBackgroundRefreshing}
                            sx={{ textTransform: 'none' }}
                        >
                            {isBackgroundRefreshing ? 'Actualizando...' : 'Actualizar'}
                        </Button>
                    </Box>

                    {/* ── Sin coincidencias con filtros ── */}
                    {filteredLogs.length === 0 ? (
                        <Box sx={{ py: 6, textAlign: 'center' }}>
                            <Typography variant="body1" color="text.secondary">
                                No se encontraron ejecuciones con los filtros seleccionados.
                            </Typography>
                        </Box>
                    ) : (
                        <TableContainer component={Paper} sx={{ borderRadius: 2, boxShadow: 2 }}>
                            <Table>
                                <TableHead>
                                    <TableRow sx={{ backgroundColor: 'action.hover' }}>
                                        <TableCell sx={{ fontWeight: 'bold' }}>Proceso</TableCell>
                                        <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
                                        <TableCell sx={{ fontWeight: 'bold' }}>Mensaje</TableCell>
                                        <TableCell sx={{ fontWeight: 'bold' }}>Tiempo de ejecución</TableCell>
                                        <TableCell sx={{ fontWeight: 'bold' }}>Fecha de ejecución</TableCell>
                                        <TableCell sx={{ fontWeight: 'bold', width: 48 }} />
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {filteredLogs.map((log) => (
                                        <TableRow
                                            key={log.id}
                                            hover
                                            onClick={() => handleOpenDetail(log)}
                                            sx={{ cursor: 'pointer' }}
                                        >
                                            <TableCell>
                                                <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                                                    {log.process}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <StatusChip status={log.status} />
                                            </TableCell>
                                            <TableCell>
                                                <Typography
                                                    variant="body2"
                                                    color="text.secondary"
                                                    sx={{
                                                        maxWidth: 340,
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                        whiteSpace: 'nowrap',
                                                    }}
                                                >
                                                    {log.message || '—'}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2">
                                                    {log.executionTimeMs != null ? `${log.executionTimeMs} ms` : '— ms'}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2" color="text.secondary">
                                                    {formatDateTime(log.createdAt)}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <IconButton
                                                    size="small"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleOpenDetail(log);
                                                    }}
                                                >
                                                    <InfoOutlinedIcon fontSize="small" />
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </>
            )}

            {/* ── Diálogo de detalle ── */}
            <Dialog
                open={selectedLog !== null}
                onClose={handleCloseDetail}
                maxWidth="sm"
                fullWidth
                slotProps={{ paper: { sx: { borderRadius: 2 } } }}
            >
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pr: 1 }}>
                    <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                        Detalle de ejecución
                    </Typography>
                    <IconButton onClick={handleCloseDetail} size="small">
                        <CloseIcon />
                    </IconButton>
                </DialogTitle>

                <DialogContent dividers>
                    {selectedLog && (
                        <>
                            <DetailField label="ID" value={String(selectedLog.id)} />

                            <DetailField label="Proceso" value={selectedLog.process} />

                            <Box sx={{ mb: 2.5 }}>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                    sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}
                                >
                                    Estado
                                </Typography>
                                <Box sx={{ mt: 0.5 }}>
                                    <StatusChip status={selectedLog.status} />
                                </Box>
                            </Box>

                            <DetailField label="Mensaje" value={selectedLog.message || 'Sin mensaje'} />

                            <DetailField
                                label="Tiempo de ejecución"
                                value={
                                    selectedLog.executionTimeMs != null
                                        ? `${selectedLog.executionTimeMs} ms`
                                        : 'No disponible'
                                }
                            />

                            <DetailField
                                label="Fecha de ejecución"
                                value={formatDateTime(selectedLog.createdAt, true)}
                            />

                            <DetailField
                                label="Usuario"
                                value={selectedLog.userId != null ? `ID: ${selectedLog.userId}` : 'No disponible'}
                            />
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
