import { Box, Typography } from '@mui/material';
import ExpiringContractsList from '../components/tables/ExpiringContractsList';

export default function ContractsPage() {
    return (
        <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                Contratos
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Seguimiento de contratos activos y próximos a vencer.
            </Typography>

            <ExpiringContractsList />
        </Box>
    );
}
