import { useQuery } from '@tanstack/react-query';
import { getTopContractors } from '../api/analyticsApi';

export function useTopContractors() {
    return useQuery({
        queryKey: ['top-contractors'],
        queryFn: getTopContractors,
    });
}
