import {
    Card,
    CardContent,
    Typography,
    List,
    ListItem,
    ListItemText,
    Chip,
    CircularProgress,
    Box,
    Alert,
    Divider,
} from '@mui/material';
import { useExpiringContracts } from '../../hooks/useExpiringContracts';
import type { ExpiringContractItem } from '../../types/dashboard';

export default function ExpiringContractsList() {
    const { data, isLoading, isError } = useExpiringContracts();

    const contracts: ExpiringContractItem[] = data ?? [];

    return (
        <Card sx={{ boxShadow: 2, borderRadius: 2, height: '100%' }}>
            <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Contratos Próximos a Vencer
                </Typography>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error">No se pudieron cargar los contratos.</Alert>
                )}

                {!isLoading && !isError && contracts.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                        Sin contratos próximos a vencer.
                    </Typography>
                )}

                {!isLoading && !isError && contracts.length > 0 && (
                    <List disablePadding>
                        {contracts.map((contract, index) => (
                            <Box key={contract.contractNumber || index}>
                                <ListItem
                                    disableGutters
                                    sx={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        py: 1.5,
                                    }}
                                >
                                    <ListItemText
                                        primary={
                                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                                                {contract.contractNumber} — {contract.contractor}
                                            </Typography>
                                        }
                                        secondary={`Vencimiento: ${contract.endDate}`}
                                    />
                                    <Chip
                                        label={`vence en ${contract.remainingDays} días`}
                                        color={contract.remainingDays <= 5 ? 'error' : 'warning'}
                                        size="small"
                                        sx={{ fontWeight: 'bold' }}
                                    />
                                </ListItem>
                                {index < contracts.length - 1 && <Divider />}
                            </Box>
                        ))}
                    </List>
                )}
            </CardContent>
        </Card>
    );
}