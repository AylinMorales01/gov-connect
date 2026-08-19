import { Grid, Skeleton } from '@mui/material';
import { useContractsSummary } from '../../hooks/useContractsSummary';
import DashboardCard from './DashboardCard';
import { formatCompactCurrency } from '../../utils/formatCompactCurrency';

import DescriptionIcon from '@mui/icons-material/Description';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PauseCircleIcon from '@mui/icons-material/PauseCircle';

export default function ContractsSummaryCards() {
    const { data, isLoading } = useContractsSummary();

    if (isLoading || !data) {
        return (
            <Grid container spacing={3}>
                {[0, 1, 2, 3].map((i) => (
                    <Grid size={{ xs: 12, sm: 6, md: 3 }} key={i}>
                        <Skeleton variant="rounded" height={135} sx={{ borderRadius: 2 }} />
                    </Grid>
                ))}
            </Grid>
        );
    }

    return (
        <Grid container spacing={3}>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <DashboardCard
                    title="Total de Contratos"
                    value={data.totalContracts}
                    icon={<DescriptionIcon />}
                    color="primary"
                />
            </Grid>

            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <DashboardCard
                    title="Valor Total Contratado"
                    value={formatCompactCurrency(data.totalValue)}
                    icon={<AttachMoneyIcon />}
                    color="secondary"
                />
            </Grid>

            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <DashboardCard
                    title="Activos"
                    value={data.activeContracts}
                    icon={<CheckCircleIcon />}
                    color="success"
                />
            </Grid>

            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                <DashboardCard
                    title="Suspendidos"
                    value={data.suspendedContracts}
                    icon={<PauseCircleIcon />}
                    color="warning"
                />
            </Grid>
        </Grid>
    );
}
