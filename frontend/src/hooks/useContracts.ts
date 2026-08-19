import { useQuery } from '@tanstack/react-query';
import { getContracts } from '../api/contractsApi';

export function useContracts(status?: string, search?: string) {
    return useQuery({
        queryKey: ['contracts', status, search],
        queryFn: () => getContracts(status, search),
    });
}
