import { Chip } from '@mui/material';

const statusColorMap: Record<string, 'success' | 'warning' | 'default'> = {
    ACTIVE: 'success',
    SUSPENDED: 'warning',
    FINISHED: 'default',
};

/** Chip de estado de contrato con color semántico. */
export default function ContractStatusChip({ status }: { status: string }) {
    return (
        <Chip
            label={status}
            size="small"
            color={statusColorMap[status] ?? 'default'}
            variant="outlined"
            sx={{ fontWeight: 'bold' }}
        />
    );
}
