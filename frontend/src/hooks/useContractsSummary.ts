import { useQuery } from '@tanstack/react-query';
import { getContractsSummary } from '../api/contractsApi';

export function useContractsSummary() {
    return useQuery({
        queryKey: ['contracts-summary'],
        queryFn: getContractsSummary,
    });
}
