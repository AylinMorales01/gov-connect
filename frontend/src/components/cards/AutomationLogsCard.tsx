import {
    Card,
    CardContent,
    Typography,
    List,
    ListItem,
    ListItemText,
    Box,
    Alert,
    Divider,
    Skeleton,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import { useAutomationLogs } from '../../hooks/useAutomationLogs';
import type { AutomationLogItem } from '../../types/dashboard';

/** Mapea el estado a ícono y color MUI. */
const statusConfig: Record<string, { icon: React.ReactElement; color: string }> = {
    SUCCESS: {
        icon: <CheckCircleIcon fontSize="small" />,
        color: 'success.main',
    },
    ERROR: {
        icon: <ErrorOutlinedIcon fontSize="small" />,
        color: 'error.main',
    },
};

const DEFAULT_CONFIG = {
    icon: <AutorenewIcon fontSize="small" />,
    color: 'warning.main',
};

/** Formatea una fecha ISO a dd/MM/yyyy HH:mm. */
function formatDateTime(iso: string): string {
    const date = new Date(iso);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function LogItem({ log }: { log: AutomationLogItem }) {
    const config = statusConfig[log.status] ?? DEFAULT_CONFIG;

    return (
        <ListItem
            disableGutters
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1.5,
                py: 1.5,
            }}
        >
            <Box sx={{ color: config.color, display: 'flex', mt: 0.3 }}>
                {config.icon}
            </Box>

            <ListItemText
                primary={
                    <Box sx={{ display: 'flex', flexDirection: 'row', gap: 1, alignItems: 'center' }}>
                        <Typography
                            variant="caption"
                            sx={{
                                fontWeight: 'bold',
                                color: config.color,
                                textTransform: 'uppercase',
                                letterSpacing: 0.5,
                            }}
                        >
                            {log.status}
                        </Typography>
                        <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                            {log.process}
                        </Typography>
                    </Box>
                }
                secondary={
                    <Box component="span" sx={{ mt: 0.5, display: 'block' }}>
                        {log.message && (
                            <Typography variant="body2" color="text.secondary">
                                {log.message}
                            </Typography>
                        )}
                        <Typography variant="caption" color="text.disabled">
                            {log.executionTimeMs != null ? `${log.executionTimeMs} ms` : '— ms'}
                            {' — '}
                            {formatDateTime(log.createdAt)}
                        </Typography>
                    </Box>
                }
                slotProps={{ secondary: { component: 'div' } }}
            />
        </ListItem>
    );
}

function LoadingSkeleton() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {Array.from({ length: 4 }).map((_, i) => (
                <Box key={i}>
                    <Box sx={{ display: 'flex', gap: 1.5 }}>
                        <Skeleton variant="circular" width={24} height={24} />
                        <Box sx={{ flex: 1 }}>
                            <Skeleton variant="text" width="60%" height={20} />
                            <Skeleton variant="text" width="90%" height={18} />
                            <Skeleton variant="text" width="40%" height={16} />
                        </Box>
                    </Box>
                    {i < 3 && <Divider sx={{ mt: 2 }} />}
                </Box>
            ))}
        </Box>
    );
}

export default function AutomationLogsCard() {
    const { data, isLoading, isError } = useAutomationLogs();

    const logs: AutomationLogItem[] = data ?? [];

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Últimas Automatizaciones
                </Typography>

                {isLoading && <LoadingSkeleton />}

                {isError && (
                    <Alert severity="error" sx={{ mt: 1 }}>
                        No fue posible cargar el historial de automatizaciones.
                    </Alert>
                )}

                {!isLoading && !isError && logs.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin ejecuciones de automatización registradas.
                    </Typography>
                )}

                {!isLoading && !isError && logs.length > 0 && (
                    <List disablePadding>
                        {logs.map((log, index) => (
                            <Box key={log.id}>
                                <LogItem log={log} />
                                {index < logs.length - 1 && <Divider />}
                            </Box>
                        ))}
                    </List>
                )}
            </CardContent>
        </Card>
    );
}