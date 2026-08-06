import { useQuery } from '@tanstack/react-query';
import { getExpiringContracts } from '../api/dashboardApi';

export function useExpiringContracts() {
    return useQuery({
        queryKey: ['expiring-contracts'],
        queryFn: getExpiringContracts,
    });
}