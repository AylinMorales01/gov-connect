import { useEffect, useRef, useState } from 'react';
import {
    Box,
    Typography,
    Paper,
    Grid,
    Button,
    Stack,
    Alert,
    LinearProgress,
    List,
    ListItem,
    ListItemText,
    Chip,
    Divider,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlined';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    uploadContracts,
    uploadBudgets,
    uploadCollections,
    getIngestionStatus,
    getEtlStatus,
} from '../api/ingestionApi';
import type { EtlTask, IngestionTask } from '../types/ingestion';

/** Definición de cada tipo de importación soportado. */
interface ImportConfig {
    kind: 'contracts' | 'budgets' | 'collections';
    title: string;
    description: string;
    columns: string[];
    upload: (file: File) => Promise<IngestionTask>;
}

const IMPORT_CONFIGS: ImportConfig[] = [
    {
        kind: 'contracts',
        title: 'Contratos (SECOP)',
        description: 'Exportación CSV de SECOP con los contratos de la entidad.',
        columns: [
            'numero_contrato',
            'contratista',
            'objeto',
            'valor',
            'fecha_inicio',
            'fecha_fin',
            'estado',
            'dependencia',
        ],
        upload: uploadContracts,
    },
    {
        kind: 'budgets',
        title: 'Ejecución presupuestal',
        description: 'Asignado y ejecutado por dependencia y vigencia fiscal.',
        columns: ['dependencia', 'anio', 'asignado', 'ejecutado', 'disponible?'],
        upload: uploadBudgets,
    },
    {
        kind: 'collections',
        title: 'Recaudos',
        description: 'Eventos de recaudo por fecha, concepto y medio de pago.',
        columns: ['fecha', 'concepto', 'contribuyente?', 'monto', 'medio_pago?', 'dependencia'],
        upload: uploadCollections,
    },
];

/** Etiqueta legible para el estado de una tarea ETL. */
const ETL_STATE_LABEL: Record<EtlTask['state'], string> = {
    PENDING: 'Encolada',
    RUNNING: 'Procesando',
    COMPLETED: 'Completada',
    FAILED: 'Fallida',
};

/** Etiqueta legible para el estado de una importación. */
const INGESTION_STATE_LABEL: Record<IngestionTask['state'], string> = {
    PENDING: 'Encolada',
    RUNNING: 'Importando',
    COMPLETED: 'Completada',
    FAILED: 'Fallida',
};

/** Cadencia del polling de estado, en milisegundos. */
const POLL_INTERVAL_MS = 1500;

/** Máximo de errores por fila que se listan en pantalla. */
const VISIBLE_ERRORS = 20;

/** Extrae el mensaje de error devuelto por el backend (contrato ApiResponse). */
function extractErrorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'response' in error) {
        const response = (error as { response?: { data?: { message?: string } } }).response;
        if (response?.data?.message) {
            return response.data.message;
        }
    }
    return 'No se pudo importar el archivo. Verifica el formato y vuelve a intentarlo.';
}

/** Panel de importación: selección de archivo + resultado + estado ETL. */
function ImportPanel({ config }: { config: ImportConfig }) {
    const queryClient = useQueryClient();
    const inputRef = useRef<HTMLInputElement>(null);
    const [file, setFile] = useState<File | null>(null);
    const [ingestion, setIngestion] = useState<IngestionTask | null>(null);
    const [etl, setEtl] = useState<EtlTask | null>(null);

    const mutation = useMutation({
        mutationFn: config.upload,
        // La importación es asíncrona: el POST solo devuelve la tarea encolada.
        onSuccess: (task) => setIngestion(task),
    });

    // Al elegir un archivo, se limpia el resultado y los estados previos.
    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const selected = event.target.files?.[0] ?? null;
        setFile(selected);
        mutation.reset();
        setIngestion(null);
        setEtl(null);
        // Permite volver a seleccionar el mismo archivo.
        event.target.value = '';
    };

    const handleClearFile = () => {
        setFile(null);
        mutation.reset();
        setIngestion(null);
        setEtl(null);
        if (inputRef.current) {
            inputRef.current.value = '';
        }
    };

    const handleImport = () => {
        if (file) {
            mutation.mutate(file);
        }
    };

    // Polling de la importación hasta alcanzar un estado terminal. Al completar,
    // encadena con el ETL que refresca la analítica.
    useEffect(() => {
        if (!ingestion) return;
        if (ingestion.state === 'COMPLETED' || ingestion.state === 'FAILED') return;

        const timer = setInterval(async () => {
            try {
                const status = await getIngestionStatus(ingestion.taskId);
                setIngestion(status);
                // El ETL se encadena aquí, en la transición a COMPLETED, y no en
                // el cuerpo del efecto (dispararía renders en cascada).
                if (status.summary?.etlTaskId) {
                    setEtl({
                        taskId: status.summary.etlTaskId,
                        state: 'PENDING',
                        message: null,
                        startedAt: null,
                        completedAt: null,
                    });
                }
            } catch {
                clearInterval(timer);
            }
        }, POLL_INTERVAL_MS);
        return () => clearInterval(timer);
    }, [ingestion]);

    // Polling del estado ETL hasta alcanzar un estado terminal.
    useEffect(() => {
        if (!etl) return;
        if (etl.state === 'COMPLETED' || etl.state === 'FAILED') {
            if (etl.state === 'COMPLETED') {
                queryClient.invalidateQueries();
            }
            return;
        }
        const timer = setInterval(async () => {
            try {
                const status = await getEtlStatus(etl.taskId);
                setEtl(status);
            } catch {
                clearInterval(timer);
            }
        }, POLL_INTERVAL_MS);
        return () => clearInterval(timer);
    }, [etl, queryClient]);

    const summary = ingestion?.state === 'COMPLETED' ? ingestion.summary : null;
    const isUploading = mutation.isPending;
    const ingestionRunning =
        ingestion !== null && (ingestion.state === 'PENDING' || ingestion.state === 'RUNNING');
    const etlRunning = etl !== null && (etl.state === 'PENDING' || etl.state === 'RUNNING');
    // Los errores vienen topados desde el backend; el total de filas con
    // problema es `skipped`.
    const hiddenErrors = summary
        ? summary.skipped - Math.min(summary.errors.length, VISIBLE_ERRORS)
        : 0;

    return (
        <Paper sx={{ p: 3, borderRadius: 2, boxShadow: 1, height: '100%' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                {config.title}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                {config.description}
            </Typography>

            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                Columnas esperadas:
            </Typography>
            <Typography
                variant="body2"
                component="div"
                sx={{
                    fontFamily: 'monospace',
                    fontSize: '0.8rem',
                    color: 'text.secondary',
                    mb: 2,
                    wordBreak: 'break-word',
                }}
            >
                {config.columns.join(', ')}
            </Typography>

            {/* Selección de archivo */}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: 'center', mb: 2 }}>
                <Button
                    component="label"
                    variant="outlined"
                    startIcon={<UploadFileIcon />}
                    disabled={isUploading}
                    sx={{ textTransform: 'none' }}
                >
                    Elegir archivo
                    <input
                        ref={inputRef}
                        type="file"
                        hidden
                        accept=".csv,text/csv,application/csv"
                        onChange={handleFileChange}
                    />
                </Button>

                {file && (
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', minWidth: 0 }}>
                        <Typography variant="body2" sx={{ fontStyle: 'italic', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {file.name}
                        </Typography>
                        <Button size="small" onClick={handleClearFile} disabled={isUploading} sx={{ textTransform: 'none' }}>
                            Quitar
                        </Button>
                    </Stack>
                )}
            </Stack>

            <Button
                variant="contained"
                onClick={handleImport}
                disabled={!file || isUploading || ingestionRunning}
                sx={{ textTransform: 'none' }}
            >
                {isUploading ? 'Subiendo…' : ingestionRunning ? 'Importando…' : 'Importar'}
            </Button>

            {/* Progreso de la subida y de la importación en el servidor */}
            {isUploading && <LinearProgress sx={{ mt: 2, borderRadius: 1 }} />}

            {ingestionRunning && !isUploading && (
                <Stack spacing={1} sx={{ mt: 2 }}>
                    <Chip
                        label={`Importación: ${INGESTION_STATE_LABEL[ingestion.state]}`}
                        color="info"
                        size="small"
                        variant="outlined"
                        sx={{ alignSelf: 'flex-start' }}
                    />
                    <LinearProgress />
                </Stack>
            )}

            {/* Fallo de la importación en el servidor */}
            {ingestion?.state === 'FAILED' && (
                <Alert severity="error" icon={<ErrorOutlineIcon />} sx={{ mt: 2, borderRadius: 2 }}>
                    La importación falló: {ingestion.message || 'error desconocido'}.
                </Alert>
            )}

            {/* Resultado de la importación */}
            {summary && (
                <Box sx={{ mt: 2 }}>
                    <Alert severity={summary.skipped > 0 ? 'warning' : 'success'} sx={{ borderRadius: 2 }}>
                        {summary.totalRows} fila{summary.totalRows !== 1 ? 's' : ''} procesada
                        {summary.totalRows !== 1 ? 's' : ''}: {summary.imported} importada
                        {summary.imported !== 1 ? 's' : ''}, {summary.updated} actualizada
                        {summary.updated !== 1 ? 's' : ''}, {summary.skipped} omitida
                        {summary.skipped !== 1 ? 's' : ''}.
                    </Alert>

                    {summary.errors.length > 0 && (
                        <Alert severity="warning" sx={{ mt: 1, borderRadius: 2 }}>
                            <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                                Errores por fila:
                            </Typography>
                            <List dense disablePadding sx={{ maxHeight: 160, overflow: 'auto' }}>
                                {summary.errors.slice(0, VISIBLE_ERRORS).map((error, index) => (
                                    <ListItem key={index} disablePadding>
                                        <ListItemText
                                            primary={error}
                                            slotProps={{ primary: { variant: 'body2', sx: { fontFamily: 'monospace' } } }}
                                        />
                                    </ListItem>
                                ))}
                            </List>
                            {hiddenErrors > 0 && (
                                <Typography variant="caption" color="text.secondary">
                                    …y {hiddenErrors} fila{hiddenErrors !== 1 ? 's' : ''} más con problemas.
                                </Typography>
                            )}
                        </Alert>
                    )}
                </Box>
            )}

            {/* Estado del ETL */}
            {etl && (
                <Box sx={{ mt: 2 }}>
                    <Divider sx={{ mb: 1.5 }} />
                    {etlRunning ? (
                        <Stack spacing={1}>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                <Chip label={`ETL: ${ETL_STATE_LABEL[etl.state]}`} color="info" size="small" variant="outlined" />
                            </Stack>
                            <LinearProgress />
                        </Stack>
                    ) : etl.state === 'COMPLETED' ? (
                        <Alert severity="success" icon={<CheckCircleIcon />} sx={{ borderRadius: 2 }}>
                            Datos analíticos actualizados correctamente.
                        </Alert>
                    ) : (
                        <Alert severity="error" icon={<ErrorOutlineIcon />} sx={{ borderRadius: 2 }}>
                            El ETL falló: {etl.message || 'error desconocido'}.
                        </Alert>
                    )}
                </Box>
            )}

            {/* Error de la petición de importación */}
            {mutation.isError && (
                <Alert severity="error" sx={{ mt: 2, borderRadius: 2 }}>
                    {extractErrorMessage(mutation.error)}
                </Alert>
            )}
        </Paper>
    );
}

export default function IngestionPage() {
    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Importación de datos
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Carga datos reales desde CSV para alimentar el dashboard y la analítica.
                Cada importación refresca automáticamente el motor analítico (DuckDB).
            </Typography>

            <Grid container spacing={3}>
                {IMPORT_CONFIGS.map((config) => (
                    <Grid key={config.kind} size={{ xs: 12, md: 4 }}>
                        <ImportPanel config={config} />
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
}
